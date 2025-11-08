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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class NumberParserTest {

    @Autowired
    private NumberParser parser;

    @ParameterizedTest
    @MethodSource("getValidNumberTestData")
    void validNumberInputShouldPass(String input, BigInteger expected) {
        assertThat(parser.parse(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidNumberTestData")
    void invalidNumberInputShouldFail(String input) {
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(BencodeException.class)
                .hasMessage("'%s' is not parsable", input);
    }

    static Stream<Arguments> getValidNumberTestData() {
        return Stream.of(
                arguments("i0e", BigInteger.valueOf(0)),
                arguments("i9e", BigInteger.valueOf(9)),
                arguments("i1e", BigInteger.valueOf(1)),
                arguments("i12e", BigInteger.valueOf(12)),
                arguments("i123e", BigInteger.valueOf(123)),
                arguments("i+0e", BigInteger.valueOf(0)),
                arguments("i+1e", BigInteger.valueOf(1)),
                arguments("i+9e", BigInteger.valueOf(9)),
                arguments("i+12e", BigInteger.valueOf(12)),
                arguments("i+123e", BigInteger.valueOf(123)),
                arguments("i-1e", BigInteger.valueOf(-1)),
                arguments("i-12e", BigInteger.valueOf(-12)),
                arguments("i-123e", BigInteger.valueOf(-123)),
                arguments("i" + "9".repeat(10000) + "e", new BigInteger("9".repeat(10000)))
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