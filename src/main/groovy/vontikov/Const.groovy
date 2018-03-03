package vontikov

final class Const {

    static final SBE_VERSION = '1.7.8'

    static final SBE_ALL_ARTIFACT =
            "uk.co.real-logic:sbe-all:$SBE_VERSION"
    static final SBE_TOOL_ARTIFACT =
            "uk.co.real-logic:sbe-tool:$SBE_VERSION:sources"

    static final PLUGIN_ID = 'sbe.generator'
    static final PLUGIN_GROUP = 'SBE Generator'
    static final EXTENSION_NAME = 'sbeGenerator'
    static final CONFIGURATION_NAME = 'sbeGenerator'

    static final VALIDATE_TASK = 'sbeValidate'
    static final GENERATE_JAVA_TASK = 'sbeGenerateJava'
    static final GENERATE_CPP_TASK = 'sbeGenerateCpp'
    static final COMPILE_JAVA_TASK = 'sbeCompileJava'
    static final PACK_JAVA_TASK = 'sbePackJava'
    static final CMAKE_CPP_TASK = 'sbeCMakeCpp'
    static final PACK_CPP_TASK = 'sbePackCpp'

    static final TMP_DIR = 'build/tmp'

    static final DEFAULT_SRC_DIR = 'src/main/resources/xml'
    static final DEFAULT_JAVA_GEN_DIR = 'build/generated/src/main/java'
    static final DEFAULT_CPP_GEN_DIR = 'build/generated/src/main/cpp'
    static final DEFAULT_JAVA_CLASSES_DIR = 'build/generated/classes'
    static final DEFAULT_ARCHIVES_DIR = 'build/archives'
    static final DEFAULT_CPP_CMAKE_PROJECT_DIR = 'build/generated/cmake-project'

    static final DEFAULT_JAVA_SOURCE_COMPATIBILITY = '1.8'
    static final DEFAULT_JAVA_TARGET_COMPATIBILITY = '1.8'

    private Const() {
    }
}
