package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class DictionaryEncoderTest {

    @Autowired
    private DictionaryEncoder encoder;

    @ParameterizedTest
    @MethodSource("getValidDictionaryTestData")
    void validDictionaryInputShouldPass(Map<?, ?> input, String expected) {
        assertThat(encoder.canEncode(input)).isTrue();
        assertThat(encoder.encode(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidInputTestData")
    void invalidInputShouldFail(Object input) {
        assertThat(encoder.canEncode(input)).isFalse();
        assertThatThrownBy(() -> encoder.encode(input))
                .isInstanceOf(BencodeException.class)
                .hasMessageContaining("Can not encode");
    }

    static Stream<Arguments> getValidDictionaryTestData() {
        return Stream.of(
                // Empty dictionary
                arguments(Map.of(), "de"),

                // Single key-value pairs - string values
                arguments(Map.of("foo", "bar"), "d3:foo3:bare"),
                arguments(Map.of("spam", "eggs"), "d4:spam4:eggse"),
                arguments(Map.of("a", "b"), "d1:a1:be"),
                arguments(Map.of("", ""), "d0:0:e"),
                arguments(Map.of("key", "value"), "d3:key5:valuee"),

                // Single key-value pairs - number values
                arguments(Map.of("foo", 42), "d3:fooi42ee"),
                arguments(Map.of("a", 0), "d1:ai0ee"),
                arguments(Map.of("test", -123), "d4:testi-123ee"),
                arguments(Map.of("count", 1000L), "d5:counti1000ee"),
                arguments(Map.of("big", BigInteger.valueOf(999)), "d3:bigi999ee"),

                // Single key-value pairs - list values
                arguments(Map.of("list", List.of()), "d4:listlee"),
                arguments(Map.of("items", List.of("a", "b")), "d5:itemsl1:a1:bee"),
                arguments(Map.of("nums", List.of(1, 2, 3)), "d4:numsli1ei2ei3eee"),
                arguments(Map.of("mixed", List.of("hello", 42)), "d5:mixedl5:helloi42eee"),

                // Multiple key-value pairs - string values only
                arguments(Map.of("foo", "bar", "spam", "eggs"), "d3:foo3:bar4:spam4:eggse"),
                arguments(Map.of("a", "b", "c", "d"), "d1:a1:b1:c1:de"),
                arguments(Map.of("k1", "v1", "k2", "v2"), "d2:k12:v12:k22:v2e"),

                // Multiple key-value pairs - number values only
                arguments(Map.of("x", 1, "y", 2), "d1:xi1e1:yi2ee"),
                arguments(Map.of("count", 100, "total", 200), "d5:counti100e5:totali200ee"),
                arguments(Map.of("neg", -5, "pos", 10), "d3:negi-5e3:posi10ee"),

                // Multiple key-value pairs - mixed values
                arguments(Map.of("name", "test", "count", 42), "d5:counti42e4:name4:teste"),
                arguments(Map.of("str", "hello", "num", 123, "zero", 0), "d3:numi123e3:str5:hello4:zeroi0ee"),
                arguments(Map.of("items", List.of(1, 2), "name", "list"), "d5:itemsli1ei2ee4:name4:liste"),

                // Special characters in keys and values
                arguments(Map.of("hello\nworld", "value"), "d11:hello\nworld5:valuee"),
                arguments(Map.of("key", "café"), "d3:key4:cafée"),
                arguments(Map.of("a:b", "c:d"), "d3:a:b3:c:de"),
                arguments(Map.of("unicode", "naïve"), "d7:unicode5:naïvee"),

                // Edge cases
                arguments(Map.of("", "empty"), "d0:5:emptye"),
                arguments(Map.of("key", ""), "d3:key0:e"),
                arguments(Map.of("max", Integer.MAX_VALUE), "d3:maxi" + Integer.MAX_VALUE + "ee"),
                arguments(Map.of("large", new BigInteger("12345678901234567890")), "d5:largei12345678901234567890ee"),

                // Nested dictionaries (simple)
                arguments(Map.of("nested", Map.of("inner", "value")), "d6:nestedd5:inner5:valueee"),
                arguments(Map.of("dict", Map.of("key", 42)), "d4:dictd3:keyi42eee"),
                arguments(Map.of("empty_dict", Map.of()), "d10:empty_dictdee"),

                // Deeply nested dictionaries
                arguments(Map.of("level1", Map.of("level2", Map.of("level3", "deep"))), "d6:level1d6:level2d6:level34:deepeee"),
                arguments(Map.of("root", Map.of("child", Map.of("grandchild", 123))), "d4:rootd5:childd10:grandchildi123eeee"),

                // Complex nested structures with lists and dictionaries
                arguments(Map.of("data", Map.of("items", List.of(1, 2, 3), "name", "test")), "d4:datad5:itemsli1ei2ei3ee4:name4:testee"),
                arguments(Map.of("config", Map.of("servers", List.of("server1", "server2"), "port", 8080)), "d6:configd4:porti8080e7:serversl7:server17:server2eee"),

                // Lists containing dictionaries
                arguments(Map.of("users", List.of(Map.of("name", "alice"), Map.of("name", "bob"))), "d5:usersld4:name5:aliceed4:name3:bobeee"),
                arguments(Map.of("records", List.of(Map.of("id", 1, "status", "active"), Map.of("id", 2, "status", "inactive"))), "d7:recordsld2:idi1e6:status6:activeed2:idi2e6:status8:inactiveeee"),

                // Very complex nested structure
                arguments(Map.of(
                        "application", Map.of(
                                "name", "BitTorrent",
                                "version", "1.0",
                                "config", Map.of(
                                        "peers", List.of("peer1", "peer2", "peer3"),
                                        "settings", Map.of(
                                                "max_connections", 100,
                                                "timeout", 30,
                                                "features", List.of("dht", "pex", "utp")
                                        )
                                )
                        )
                ), "d11:applicationd6:configd5:peersl5:peer15:peer25:peer3ee8:settingsd8:featuresl3:dht3:pex3:utpee15:max_connectionsi100e7:timeouti30eee4:name10:BitTorrent7:version3:1.0ee"),

                // Mixed types at different levels
                arguments(Map.of(
                        "metadata", Map.of(
                                "files", List.of(
                                        Map.of("name", "file1.txt", "size", 1024),
                                        Map.of("name", "file2.bin", "size", 2048)
                                ),
                                "total_size", 3072L,
                                "created", "2023-01-01"
                        ),
                        "announce", "http://tracker.example.com"
                ), "d8:announce27:http://tracker.example.com8:metadatad7:created10:2023-01-015:filesl4:name9:file1.txt4:sizei1024eed4:name9:file2.bin4:sizei2048eee10:total_sizei3072eee"),

                // Multiple nested levels with all types
                arguments(Map.of(
                        "torrent", Map.of(
                                "info", Map.of(
                                        "name", "test.torrent",
                                        "pieces", List.of("piece1", "piece2"),
                                        "piece_length", 32768,
                                        "files", List.of(
                                                Map.of("path", List.of("dir", "file.txt"), "length", 512),
                                                Map.of("path", List.of("file2.dat"), "length", 1024)
                                        )
                                ),
                                "announce_list", List.of(
                                        List.of("http://tracker1.com", "http://tracker2.com"),
                                        List.of("udp://tracker3.com")
                                )
                        )
                ), "d7:torrentd13:announce_listlll16:http://tracker1.com16:http://tracker2.comeell15:udp://tracker3.comeee4:infod5:filesld6:lengthi512e4:pathl3:dir8:file.txteeed6:lengthi1024e4:pathl9:file2.dateee4:name12:test.torrent12:piece_lengthi32768e6:piecesl6:piece16:piece2eeee"),

                // Extreme nesting depth
                arguments(Map.of("a", Map.of("b", Map.of("c", Map.of("d", Map.of("e", "deep"))))),
                        "d1:ad1:bd1:cd1:dd1:e4:deepeeeee"),

                // Large dictionary with many keys (alphabetically sorted)
                arguments(Map.of(
                        "alpha", 1,
                        "beta", "two",
                        "gamma", List.of(3, 4),
                        "delta", Map.of("nested", true),
                        "epsilon", BigInteger.valueOf(999999999L)
                ), "d5:alphai1e4:beta3:two5:deltad6:nested1:truee7:epsiloni999999999e5:gammal3ei4eee"),

                // Edge case: Dictionary with special key ordering
                arguments(Map.of("z", 1, "a", 2, "m", 3), "d1:ai2e1:mi3e1:zi1ee"),

                // Very large values
                arguments(Map.of(
                        "huge_number", new BigInteger("9".repeat(100)),
                        "long_string", "a".repeat(1000),
                        "big_list", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                ), "d8:big_listli1ei2ei3ei4ei5ei6ei7ei8ei9ei10ee10:huge_numberi" + "9".repeat(100) + "e11:long_string1000:" + "a".repeat(1000) + "e")
        );
    }

    static Stream<Object> getInvalidInputTestData() {
        return Stream.of(
                // Non-map types
                "string",
                123,
                3.14,
                true,
                new Object(),
                List.of("a", "b"),

                // Maps with non-string keys
                Map.of(123, "value"),
                Map.of(true, "value"),
                Map.of(new Object(), "value"),

                // Maps with unsupported value types
                Map.of("key", 3.14),
                Map.of("key", 3.14f),
                Map.of("key", new Object()),
                Map.of("key", true),

                // Maps with mixed valid and invalid values
                Map.of("valid", "string", "invalid", 3.14),
                Map.of("string", "value", "number", 123, "invalid", new Object()),

                // Maps with null keys or values (if supported by Map implementation)
                Map.of("key", List.of(1, 3.14)) // List with invalid element
        );
    }

}