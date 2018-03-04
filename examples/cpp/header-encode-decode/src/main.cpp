#include <iostream>

#include "com_example_abc/MessageHeader.h"
#include "com_example_abc/Msg.h"
#include "com_example_xyz/MessageHeader.h"
#include "com_example_xyz/Msg.h"

template<typename Msg, typename Hdr, std::size_t Size>
void encode_hdr(Hdr& hdr, const std::uint64_t version, char (&buf)[Size],
        const std::uint64_t offset = 0) {
    hdr.wrap(buf, offset, version, Size)
        .blockLength(Msg::sbeBlockLength())
        .templateId(Msg::sbeTemplateId())
        .schemaId(Msg::sbeSchemaId())
        .version(Msg::sbeSchemaVersion());
}

template<typename H, std::size_t Size>
void decode_hdr(H& hdr, const std::uint64_t version, char (&buf)[Size],
        const std::uint64_t offset = 0) {
    hdr.wrap(buf, offset, version, Size);
}

template<typename H>
void print(auto msg, const H& hdr) {
    std::cout << msg << '\n';
    std::cout << '\t' << "blockLength=" << hdr.blockLength() << '\n'
              << '\t' << "templateId=" << hdr.templateId() << '\n'
              << '\t' << "schemaId=" << hdr.schemaId() << '\n'
              << '\t' << "schemaVersion=" << hdr.version() << '\n'
              << '\t' << "encodedLength=" << hdr.encodedLength()
              << std::endl;
}

constexpr auto version = 0;

int main() {
    char buf[2048];

    com_example_abc::MessageHeader hdr_abc;
    com_example_xyz::MessageHeader hdr_xyz;

    encode_hdr<com_example_abc::Msg>(hdr_abc, version, buf);
    decode_hdr(hdr_abc, version, buf);
    print("abc", hdr_abc);

    encode_hdr<com_example_xyz::Msg>(hdr_xyz, version, buf);
    decode_hdr(hdr_xyz, version, buf);
    print("xyz", hdr_xyz);

    return 0;
}
