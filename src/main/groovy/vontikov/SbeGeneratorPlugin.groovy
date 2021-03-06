package vontikov

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import static vontikov.Const.*
import static vontikov.Util.*

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.compile.JavaCompile

class SbeGeneratorPlugin implements Plugin<Project> {

    Set<File> input
    FileCollection sbeClasspath

    @Override
    void apply(Project project) {
        prepareRepositories(project)

        def ext = project.extensions.create(EXTENSION_NAME,
                SbeGeneratorPluginExtension, project)

        project.afterEvaluate {
            def cfg = sbeDependency(project, ext, CONFIGURATION_NAME)
            sbeClasspath = sbeClasspath(project, ext, cfg)
            input = tree(project, ext.src).files.absolutePath.toSet()

            validateTask(project)

            genJavaTask(project, ext)
            compileJavaTask(project, ext)
            packJavaTask(project, ext)

            genCppTask(project, ext)
            cmakeCppTask(project, ext)
            packCppTask(project, ext)
        }
    }

    void validateTask(Project project) {
        project.task(VALIDATE_TASK, type: DefaultTask) {
            group = PLUGIN_GROUP
            description = 'Validates definitions'

            def logger = project.logger
            def ext = project.extensions.sbeGenerator

            // extract sbe.xsd from sbe-all.jar
            def sbeAllJar = sbeClasspath.filter {
                it.name.startsWith('sbe-all')
            }.singleFile
            def xsdFile = project.zipTree(sbeAllJar).matching {
                include '**/sbe.xsd'
            }.singleFile
            if (!xsdFile) {
                throw new IllegalStateException('SBE schema not found')
            }

            def validator = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)
                    .newSchema(xsdFile)
                    .newValidator()

            tree(project, ext.src).visit {
                def f = it.file
                logger.info("Validating: $f")
                validator.validate(new StreamSource(f))
            }
        }
    }

    void genJavaTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(GENERATE_JAVA_TASK, type: JavaExec) {
            group = PLUGIN_GROUP
            description = 'Generates Java stubs'

            def props = [
                'sbe.target.language': 'Java',
                'sbe.output.dir': ext.javaGenDir,
            ]
            if (ext.targetNamespace) {
                props.put('sbe.target.namespace', ext.targetNamespace)
            }

            systemProperties = props
            main = 'uk.co.real_logic.sbe.SbeTool'
            args = input
            classpath = sbeClasspath
        }
    }

    void compileJavaTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(COMPILE_JAVA_TASK, type: JavaCompile,
                dependsOn: GENERATE_JAVA_TASK) {
            group = PLUGIN_GROUP
            description = 'Compiles Java stubs'

            source = project.fileTree(dir: ext.javaGenDir, include: '**/*.java')
            destinationDir = project.file(ext.javaClassesDir)
            sourceCompatibility = ext.javaSourceCompatibility
            targetCompatibility = ext.javaTargetCompatibility
            classpath = sbeClasspath
        }
    }

    void packJavaTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(PACK_JAVA_TASK, type: Jar,
                dependsOn: COMPILE_JAVA_TASK) {
            group = PLUGIN_GROUP
            description = 'Packs compiled Java stubs'

            from {
                project.files(ext.javaClassesDir, ext.javaGenDir)
            }

            destinationDir = project.file(ext.archivesDir)
            archiveName = "${project.name}-${project.version}.jar"

            manifest {
                attributes(
                    'Manifest-Version': '1.0',
                    'Implementation-Title': project.name,
                    'Implementation-Version': project.version
                )
            }
        }
    }

    void genCppTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(GENERATE_CPP_TASK, type: JavaExec) {
            group = PLUGIN_GROUP
            description = 'Generates CPP stubs'

            def props = [
                'sbe.target.language': 'CPP',
                'sbe.cpp.namespaces.collapse': 'true',
                'sbe.output.dir': ext.cppGenDir,
            ]
            if (ext.targetNamespace) {
                props.put('sbe.target.namespace', ext.targetNamespace)
            }

            systemProperties = props
            main = 'uk.co.real_logic.sbe.SbeTool'
            args = input
            classpath = sbeClasspath
        }
    }

    void cmakeCppTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(CMAKE_CPP_TASK, type: DefaultTask,
                dependsOn: GENERATE_CPP_TASK) {
            group = PLUGIN_GROUP
            description = 'Prepares CMake scripts'

            doLast {
                // normalize project name
                def nm = project.name.replaceAll('-|\\.', '_')

                // stubs
                project.copy {
                    into(project.file("${ext.cppCmakeDir}/include/"))
                    from(project.file(ext.cppGenDir))
                }

                // sbe.h
                def sbeToolJar = sbeClasspath.filter {
                    it.name.matches('sbe-tool-[0-9.]+-sources\\.jar')
                }.singleFile
                def hdrFile = project.zipTree(sbeToolJar).matching {
                    include '**/sbe.h'
                }.singleFile
                if (!hdrFile) {
                    throw new IllegalStateException('SBE header not found')
                }
                project.copy {
                    into(project.file("${ext.cppCmakeDir}/include/sbe/"))
                    from(hdrFile)
                }

                // copy scripts
                final def cmakeScript = 'CMakeLists.txt'
                final def cmakeTemplate =  'Config.cmake.in'

                copyCmakeResourceToTmp(project, cmakeScript)
                copyCmakeResourceToTmp(project, cmakeTemplate)

                project.copy {
                    into(ext.cppCmakeDir)
                    from(project.file(TMP_DIR + '/' + cmakeScript))
                    filter {
                        it.startsWith('@') ?
                                "project(${nm} VERSION ${project.version})"
                                : it
                    }
                }

                project.copy {
                    into(ext.cppCmakeDir)
                    from(project.file(TMP_DIR + '/' + cmakeTemplate))
                    rename { 'cmake/' + it  }
                }
            }
        }
    }

    void packCppTask(Project project, SbeGeneratorPluginExtension ext) {
        project.task(PACK_CPP_TASK, type: Tar, dependsOn: CMAKE_CPP_TASK) {
            group = PLUGIN_GROUP
            description = 'Packs generated CPP stubs'

            from {
                project.files(ext.cppCmakeDir)
            }

            destinationDir = project.file(ext.archivesDir)
            archiveName = "${project.name}-${project.version}-cpp-cmake.tar.gz"

            compression = Compression.GZIP
        }
    }

    void copyCmakeResourceToTmp(Project project, String fn) {
        project.file(TMP_DIR).mkdirs()
        def is = getClass().getClassLoader().getResourceAsStream('cmake/' + fn)
        def os = new FileOutputStream(project.file(TMP_DIR + '/' + fn))

        int read
        byte[] bytes = new byte[4096]
        while ((read = is.read(bytes)) != -1) {
            os.write(bytes, 0, read)
        }
        is.close()
        os.close()
    }
}
