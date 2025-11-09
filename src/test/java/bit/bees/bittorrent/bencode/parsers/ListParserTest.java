package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class ListParserTest {

    @Autowired
    private ListParser parser;

    @ParameterizedTest
    @MethodSource("getValidListTestData")
    void validListInputShouldPass(String input, List<Object> expected, int parsedLength) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.SUCCESS);
        assertThat(result.parsedData()).isEqualTo(expected);
        assertThat(result.parsedLength())
                .as("parsed data length should be %d for '%s'", parsedLength, input)
                .isEqualTo(parsedLength);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidListTestData")
    void invalidListInputShouldFail(String input) {
        var result = parser.parse(input);
        assertThat(result.status()).isEqualTo(ParseResult.Status.FAILURE);
        assertThat(result.error())
                .isInstanceOf(BencodeException.class)
                .hasMessage("'%s' is not parsable", input);
    }

    static Stream<Arguments> getValidListTestData() {
        return Stream.of(
                // Empty list
                arguments("le", List.of(), 2),
                arguments("le1234", List.of(), 2),
                arguments("le4:hell", List.of(), 2),

                // Single element lists
                arguments("l4:spame", List.of("spam"), 8),
                arguments("li42ee", List.of(BigInteger.valueOf(42)), 6),
                arguments("li0ee", List.of(BigInteger.valueOf(0)), 5),
                arguments("li-5ee", List.of(BigInteger.valueOf(-5)), 6),

                // Multiple element lists - strings only
                arguments("l4:spam4:eggse", List.of("spam", "eggs"), 14),
                arguments("l3:foo3:bar3:baze", List.of("foo", "bar", "baz"), 17),
                arguments("l0:1:a2:abe", List.of("", "a", "ab"), 11),

                // Multiple element lists - numbers only
                arguments("li1ei2ei3ee", List.of(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3)), 11),
                arguments("li0ei-1ei100ee", List.of(BigInteger.valueOf(0), BigInteger.valueOf(-1), BigInteger.valueOf(100)), 14),

                // Mixed string and number lists
                arguments("l4:spami42ee", List.of("spam", BigInteger.valueOf(42)), 12),
                arguments("li123e5:helloe", List.of(BigInteger.valueOf(123), "hello"), 14),
                arguments("l4:testi1e3:fooe", List.of("test", BigInteger.valueOf(1), "foo"), 16),
                arguments("li0e0:i-5e2:abe", List.of(BigInteger.valueOf(0), "", BigInteger.valueOf(-5), "ab"), 15),

                // Lists with special string content
                arguments("l3:\n\r\t4:teste", List.of("\n\r\t", "test"), 13),
                arguments("l2:[]2:{}e", List.of("[]", "{}"), 10),
                arguments("l1: 3:   e", List.of(" ", "   "), 10),

                // Large lists
                arguments("l" + "1:a".repeat(10) + "e", List.of("a", "a", "a", "a", "a", "a", "a", "a", "a", "a"), 32),

                // Lists with extra data after (should parse only the list)
                arguments("l4:spameextra", List.of("spam"), 8),
                arguments("lei42e", List.of(), 2),

                // Complex mixed content
                arguments("l11:hello worldi-123e0:3:abce", List.of("hello world", BigInteger.valueOf(-123), "", "abc"), 29),

                // Lists with numbers containing multiple digits
                arguments("li999ei1000ei-2000ee", List.of(BigInteger.valueOf(999), BigInteger.valueOf(1000), BigInteger.valueOf(-2000)), 20),

                // Lists with strings containing colons
                arguments("l9:key:valuei42ee", List.of("key:value", BigInteger.valueOf(42)), 17),
                arguments("l7:a:b:c:d3:foo4:teste", List.of("a:b:c:d", "foo", "test"), 22),

                // Complex mixed content with remaining content
                arguments("l11:hello worldi-123e0:3:abcel11:hello worldi-123e0:3:abce", List.of("hello world", BigInteger.valueOf(-123), "", "abc"), 29),
                arguments("l11:hello worldi-123e0:3:abcelelelelele", List.of("hello world", BigInteger.valueOf(-123), "", "abc"), 29),
                arguments("l11:hello worldi-123e0:3:abce12345", List.of("hello world", BigInteger.valueOf(-123), "", "abc"), 29)
        );
    }

    static Stream<String> getInvalidListTestData() {
        return Stream.of(
                // Empty or null inputs
                "",

                // Missing prefix 'l'
                "4:spame",
                "i42ee",
                "e",

                // Missing suffix 'e'
                "l",
                "l4:spam",
                "li42e",
                "l4:spam4:eggs",

                // Invalid list structure
                "l4:spamx",
                "li1e512351",
                "li42ex",
                "lx4:spame",

                // Lists with invalid elements
                "linvalide",
                "l4spame",
                "li42xe",
                "l4:spam:invalid",

                // Incomplete elements
                "l4:spa",
                "l4:",
                "li4",
                "li",

                // Lists with unparsable elements
                "labce",
                "l123e",
                "l:4spame",
                "li42i43ee",

                // Mixed valid and invalid elements
                "l4:spaminvalide",
                "li42e4spame",
                "l4:spami42einvalide",

                // Nested structures (not supported by current parser)
                "lle",
                "ll4:spamee",

                // Malformed numbers in list
                "li-0ee",
                "li++42ee",
                "li--42ee",
                "li001ee",

                // Malformed strings in list
                "l-1:teste",
                "l:teste",
                "la:teste",
                "l4teste",
                "l4:tes",

                // Lists starting with invalid chars
                "x4:spame",
                "14:spame",
                " l4:spame",

                // Lists with internal structural issues
                "l4:spam4:eggs4:test",
                "l4:spami42e4:eggs",
                "li42e4:spami43e",

                // Additional invalid cases
                "l4:spam4:eggsinvalid",
                "li42e4spame",
                "l4:spami42einvalide",
                "l4:spam:invalid",
                "li42xe",
                "l4spame",
                "linvalide",
                "labce",
                "l123e",
                "l:4spame",
                "li42i43ee",

                // Incomplete lists
                "l4:spa",
                "l4:",
                "li4",
                "li",
                "l4:spam4:egg",
                "li42ei99",

                // Invalid characters in list context
                "l4:spamx",
                "li42ex",
                "lx4:spame",
                "l4:spam!e",
                "li42@e",

                // Malformed elements within valid list structure
                "li-0ee",
                "li++42ee",
                "li--42ee",
                "li001ee",
                "l-1:teste",
                "l:teste",
                "la:teste",
                "l4teste",
                "l4:tes",
                "l01:ae",
                "l4:spam-1:teste"
        );
    }
}