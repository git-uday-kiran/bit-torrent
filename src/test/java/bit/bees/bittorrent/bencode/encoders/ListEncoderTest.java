package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class ListEncoderTest {

    @Autowired
    private ListEncoder encoder;

    @ParameterizedTest
    @MethodSource("getValidListTestData")
    void validListInputShouldPass(List<?> input, String expected) {
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

    static Stream<Arguments> getValidListTestData() {
        return Stream.of(
                // Empty list
                arguments(List.of(), "le"),

                // Single element lists - strings
                arguments(List.of("spam"), "l4:spame"),
                arguments(List.of(""), "l0:e"),
                arguments(List.of("a"), "l1:ae"),
                arguments(List.of("hello"), "l5:helloe"),

                // Single element lists - numbers
                arguments(List.of(42), "li42ee"),
                arguments(List.of(0), "li0ee"),
                arguments(List.of(-5), "li-5ee"),
                arguments(List.of(123L), "li123ee"),
                arguments(List.of(BigInteger.valueOf(999)), "li999ee"),

                // Multiple element lists - strings only
                arguments(List.of("spam", "eggs"), "l4:spam4:eggse"),
                arguments(List.of("foo", "bar", "baz"), "l3:foo3:bar3:baze"),
                arguments(List.of("", "a", "ab"), "l0:1:a2:abe"),
                arguments(List.of("hello", "world"), "l5:hello5:worlde"),

                // Multiple element lists - numbers only
                arguments(List.of(1, 2, 3), "li1ei2ei3ee"),
                arguments(List.of(0, -1, 100), "li0ei-1ei100ee"),
                arguments(List.of(1L, 2L, 3L), "li1ei2ei3ee"),
                arguments(List.of(BigInteger.valueOf(123), BigInteger.valueOf(456)), "li123ei456ee"),

                // Mixed string and number lists
                arguments(List.of("spam", 42), "l4:spami42ee"),
                arguments(List.of(123, "hello"), "li123e5:helloe"),
                arguments(List.of("test", 1, "foo"), "l4:testi1e3:fooe"),
                arguments(List.of(0, "", -5, "ab"), "li0e0:i-5e2:abe"),
                arguments(List.of("a", 1, "b", 2), "l1:ai1e1:bi2ee"),

                // Lists with larger numbers
                arguments(List.of(Integer.MAX_VALUE), "li" + Integer.MAX_VALUE + "ee"),
                arguments(List.of(Long.MAX_VALUE), "li" + Long.MAX_VALUE + "ee"),
                arguments(List.of(new BigInteger("12345678901234567890")), "li12345678901234567890ee"),

                // Lists with special string characters
                arguments(List.of("hello\nworld"), "l11:hello\nworlde"),
                arguments(List.of("café", "naïve"), "l4:café5:naïvee"),
                arguments(List.of("a:b", "c:d"), "l3:a:b3:c:de"),

                // Edge cases
                arguments(List.of("", "", ""), "l0:0:0:e"),
                arguments(List.of(0, 0, 0), "li0ei0ei0ee"),

                arguments(List.of(Map.of("name", "alice"), Map.of("name", "bob")), "ld4:name5:aliceed4:name3:bobee")
        );
    }

    static Stream<Object> getInvalidInputTestData() {
        return Stream.of(
                // Non-collection types
                "string",
                123,
                3.14,
                true,
                new Object(),

                // Collections with unsupported types
                List.of(3.14),
                List.of(3.14f),
                List.of(new Object()),
                List.of("valid", 3.14),
                List.of(1, new Object()),
                Arrays.asList(1, 2, null),

                // Other collection types with unsupported elements
                Set.of(3.14),

                // Mixed valid and invalid
                List.of("string", 123, 3.14),
                List.of(true, false)
        );
    }

}