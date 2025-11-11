package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class StringEncoderTest {

    @Autowired
    private StringEncoder encoder;

    @ParameterizedTest
    @MethodSource("getValidStringTestData")
    void validStringInputShouldPass(String input, String expected) {
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

    static Stream<Arguments> getValidStringTestData() {
        return Stream.of(
                // Empty string
                arguments("", "0:"),

                // Single character
                arguments("a", "1:a"),

                // Basic strings
                arguments("spam", "4:spam"),
                arguments("hello", "5:hello"),
                arguments("hello world", "11:hello world"),
                arguments("hello, world!", "13:hello, world!"),

                // Strings with special characters
                arguments("hello\nworld\ttab", "15:hello\nworld\ttab"),
                arguments("\n\r\t", "3:\n\r\t"),
                arguments("  ", "2:  "),
                arguments(" ", "1: "),

                // Strings with Unicode characters
                arguments("café", "4:café"),
                arguments("äöüß", "4:äöüß"),

                // Strings containing numbers
                arguments("123", "3:123"),
                arguments("123456", "6:123456"),

                // Strings with colon characters
                arguments("a:b", "3:a:b"),
                arguments("key:value", "9:key:value"),
                arguments("a:b:c:d", "7:a:b:c:d"),
                arguments("1:23", "4:1:23"),

                // Binary-like content
                arguments("\0\1\2\3", "4:\0\1\2\3"),
                arguments("[]", "2:[]"),
                arguments("{}", "2:{}"),

                // Very long string
                arguments("a".repeat(1000), "1000:" + "a".repeat(1000)),
                arguments("x".repeat(255), "255:" + "x".repeat(255))
        );
    }

    static Stream<Object> getInvalidInputTestData() {
        return Stream.of(
                42,
                123L,
                3.14,
                true,
                new Object(),
                new int[]{1, 2, 3},
                Stream.of("test")
        );
    }

}