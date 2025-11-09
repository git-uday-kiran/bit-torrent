package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class NumberParserTest {

    @Autowired
    private NumberParser parser;

    @ParameterizedTest
    @MethodSource("getValidNumberTestData")
    void validNumberInputShouldPass(String input, BigInteger expected, int parsedLength) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.SUCCESS);
        assertThat(result.parsedData()).isEqualTo(expected);
        assertThat(result.parsedLength())
                .as("parsed data length should be %d for '%s'", parsedLength, input)
                .isEqualTo(parsedLength);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidNumberTestData")
    void invalidNumberInputShouldFail(String input) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.FAILURE);
        assertThat(result.error())
                .isInstanceOf(BencodeException.class)
                .hasMessage("'%s' is not parsable", input);
    }

    static Stream<Arguments> getValidNumberTestData() {
        return Stream.of(
                arguments("i0e", BigInteger.valueOf(0), 3),
                arguments("i9e", BigInteger.valueOf(9), 3),
                arguments("i1e", BigInteger.valueOf(1), 3),
                arguments("i12e", BigInteger.valueOf(12), 4),
                arguments("i123e", BigInteger.valueOf(123), 5),
                arguments("i+0e", BigInteger.valueOf(0), 4),
                arguments("i+1e", BigInteger.valueOf(1), 4),
                arguments("i+9e", BigInteger.valueOf(9), 4),
                arguments("i+12e", BigInteger.valueOf(12), 5),
                arguments("i+123e", BigInteger.valueOf(123), 6),
                arguments("i-1e", BigInteger.valueOf(-1), 4),
                arguments("i-12e", BigInteger.valueOf(-12), 5),
                arguments("i-123e", BigInteger.valueOf(-123), 6),
                arguments("i" + "9".repeat(10000) + "e", new BigInteger("9".repeat(10000)), (10000 + 2))
        );
    }

    static Stream<String> getInvalidNumberTestData() {
        return Stream.of(
                "",
                "abc",
                "ie",
                "i1234",
                "1234e",
                "i-0e",
                "i--0e",
                "i--123e",
                "i++123e",
                "i001234",
                "i+001234",
                "i-001234"
        );
    }
}