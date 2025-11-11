package bit.bees.bittorrent.bencode.encoders;

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
class NumberEncoderTest {

    @Autowired
    private NumberEncoder encoder;

    @ParameterizedTest
    @MethodSource("getValidNumberTestData")
    void validNumberInputShouldPass(Number input, String expected) {
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

    static Stream<Arguments> getValidNumberTestData() {
        return Stream.of(
                // Integer values
                arguments(0, "i0e"),
                arguments(1, "i1e"),
                arguments(9, "i9e"),
                arguments(12, "i12e"),
                arguments(123, "i123e"),
                arguments(-1, "i-1e"),
                arguments(-12, "i-12e"),
                arguments(-123, "i-123e"),
                arguments(Integer.MAX_VALUE, "i" + Integer.MAX_VALUE + "e"),
                arguments(Integer.MIN_VALUE, "i" + Integer.MIN_VALUE + "e"),

                // Long values
                arguments(0L, "i0e"),
                arguments(1L, "i1e"),
                arguments(123L, "i123e"),
                arguments(-123L, "i-123e"),
                arguments(Long.MAX_VALUE, "i" + Long.MAX_VALUE + "e"),
                arguments(Long.MIN_VALUE, "i" + Long.MIN_VALUE + "e"),
                arguments(9999999999L, "i9999999999e"),
                arguments(-9999999999L, "i-9999999999e"),

                // BigInteger values
                arguments(BigInteger.ZERO, "i0e"),
                arguments(BigInteger.ONE, "i1e"),
                arguments(BigInteger.valueOf(123), "i123e"),
                arguments(BigInteger.valueOf(-123), "i-123e"),
                arguments(new BigInteger("12345678901234567890"), "i12345678901234567890e"),
                arguments(new BigInteger("-12345678901234567890"), "i-12345678901234567890e"),
                arguments(new BigInteger("9".repeat(1000)), "i" + "9".repeat(1000) + "e"),
                arguments(new BigInteger("-" + "9".repeat(1000)), "i-" + "9".repeat(1000) + "e"),

                // Edge cases
                arguments(BigInteger.valueOf(Long.MAX_VALUE), "i" + Long.MAX_VALUE + "e"),
                arguments(BigInteger.valueOf(Long.MIN_VALUE), "i" + Long.MIN_VALUE + "e")
        );
    }

    static Stream<Object> getInvalidInputTestData() {
        return Stream.of(
                "123",
                "0",
                3.14,
                3.14f,
                true,
                false,
                new Object(),
                new int[]{1, 2, 3},
                Stream.of(123)
        );
    }

}