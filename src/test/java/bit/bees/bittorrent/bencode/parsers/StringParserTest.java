package bit.bees.bittorrent.bencode.parsers;

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
class StringParserTest {

    @Autowired
    private StringParser parser;

    @ParameterizedTest
    @MethodSource("getValidStringTestData")
    void validStringInputShouldPass(String input, String expected, int parsedLength) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.SUCCESS);
        assertThat(result.parsedData()).isEqualTo(expected);
        assertThat(result.parsedLength())
                .as("parsed data length should be %d for '%s'", parsedLength, input)
                .isEqualTo(parsedLength);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidStringTestData")
    void invalidStringInputShouldFail(String input) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.FAILURE);
        assertThat(result.error())
                .isInstanceOf(BencodeException.class)
                .hasMessage("'%s' is not parsable", input);
    }

    static Stream<Arguments> getValidStringTestData() {
        return Stream.of(
                // Basic string cases
                arguments("4:spam", "spam", 6),
                arguments("0:", "", 2),
                arguments("1:a", "a", 3),
                arguments("5:hello", "hello", 7),
                arguments("11:hello world", "hello world", 14),

                // Strings with special characters
                arguments("3:abc", "abc", 5),
                arguments("4:test", "test", 6),
                arguments("2:   ", "  ", 4),
                arguments("1: ", " ", 3),
                arguments("3:\n\r\t", "\n\r\t", 5),
                arguments("4:äöüß", "äöüß", 6),

                // Strings with numbers in content
                arguments("3:123", "123", 5),
                arguments("6:123456", "123456", 8),
                arguments("4:1:23", "1:23", 6),

                // Edge cases with colons in string content
                arguments("9:key:value", "key:value", 11),
                arguments("7:a:b:c:d", "a:b:c:d", 9),

                // Large numbers as length
                arguments("100:" + "x".repeat(100), "x".repeat(100), 104),
                arguments("255:" + "a".repeat(255), "a".repeat(255), 259),

                // Binary-like content
                arguments("4:\0\1\2\3", "\0\1\2\3", 6),
                arguments("2:[]", "[]", 4),
                arguments("2:{}", "{}", 4),

                // Strings with extra parsedData after (should only parse the string part)
                arguments("4:testextra", "test", 6),
                arguments("5:helloworld", "hello", 7),
                arguments("4::spam", ":spa", 6)
        );
    }

    static Stream<String> getInvalidStringTestData() {
        return Stream.of(
                // Empty or null inputs
                "",

                // Missing colon
                "4spam",
                "5hello",
                "0",
                "1",

                // Invalid length format
                ":spam",
                "a:spam",
                "-1:spam",
                "+4:spam",
                "04:spam",
                "00:spam",

                // Length too large for available parsedData
                "5:spam",
                "10:hello",
                "100:short",
                "1:",
                "2:a",
                "3:ab",

                // No colon at all
                "spam",
                "hello",
                "123",

                // Multiple colons but invalid format
                "::",
                "::spam",

                // Negative length
                "-0:spam",
                "-5:hello",

                // Leading zeros in length
                "01:a",
                "05:hello",
                "001:x",

                // Non-numeric length
                "abc:spam",
                "1a:spam",
                "a1:spam",
                "1.5:spam"
        );
    }
}