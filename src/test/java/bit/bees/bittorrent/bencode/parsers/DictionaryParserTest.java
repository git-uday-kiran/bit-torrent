package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class DictionaryParserTest {

    @Autowired
    private DictionaryParser parser;

    @ParameterizedTest
    @MethodSource("getValidDictionaryTestData")
    void validDictionaryInputShouldPass(String input, Map<String, Object> expected, int parsedLength) {
        var result = parser.parse(input);
        if (result.status() == ParseResult.Status.FAILURE)
            result.error().printStackTrace();
        assertThat(result.status()).isEqualTo(ParseResult.Status.SUCCESS);
        assertThat(result.parsedData()).isEqualTo(expected);
        assertThat(result.parsedLength())
                .as("parsed data length should be %d for '%s'", parsedLength, input)
                .isEqualTo(parsedLength);
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getInvalidDictionaryTestData")
    void invalidDictionaryInputShouldFail(String input) {
        var result = parser.parse(input);
        System.out.println("result = " + result);
        assertThat(result.status()).isEqualTo(ParseResult.Status.FAILURE);
        assertThat(result.error())
                .isInstanceOf(BencodeException.class)
                .hasMessage("'%s' is not parsable", input);
    }

    static Stream<Arguments> getValidDictionaryTestData() {
        return Stream.of(
                // Empty dictionary
                arguments("de", Map.of(), 2),
                arguments("de1234", Map.of(), 2),
                arguments("de4:spam", Map.of(), 2),

                // Single key-value pairs - string values
                arguments("d3:foo3:bare", Map.of("foo", "bar"), 12),
                arguments("d4:spam4:eggse", Map.of("spam", "eggs"), 14),
                arguments("d1:a1:be", Map.of("a", "b"), 8),
                arguments("d0:0:e", Map.of("", ""), 6),
                arguments("d3:key5:valuee", Map.of("key", "value"), 14),

                // Single key-value pairs - number values
                arguments("d3:fooi42ee", Map.of("foo", BigInteger.valueOf(42)), 11),
                arguments("d1:ai0ee", Map.of("a", BigInteger.valueOf(0)), 8),
                arguments("d4:testi-123ee", Map.of("test", BigInteger.valueOf(-123)), 14),
                arguments("d5:counti1000ee", Map.of("count", BigInteger.valueOf(1000)), 15),

                // Multiple key-value pairs - string values only
                arguments("d3:foo3:bar4:spam4:eggse", Map.of("foo", "bar", "spam", "eggs"), 24),
                arguments("d1:a1:b1:c1:de", Map.of("a", "b", "c", "d"), 14),
                arguments("d2:k13:v112:k23:v2ee", Map.of("k1", "v11", "k2", "v2e"), 20),

                // Multiple key-value pairs - number values only
                arguments("d1:ai1e1:bi2ee", Map.of("a", BigInteger.valueOf(1), "b", BigInteger.valueOf(2)), 14),
                arguments("d1:xi42e1:yi-5ee", Map.of("x", BigInteger.valueOf(42), "y", BigInteger.valueOf(-5)), 16),
                arguments("d5:counti0e4:testi100ee", Map.of("count", BigInteger.valueOf(0), "test", BigInteger.valueOf(100)), 23),

                // Mixed string and number values
                arguments("d3:agei25e4:name4:Johne", Map.of("age", BigInteger.valueOf(25), "name", "John"), 23),
                arguments("d3:bar4:test3:fooi42ee", Map.of("bar", "test", "foo", BigInteger.valueOf(42)), 22),
                arguments("d1:5i25e1:a4:spam1:c3:fooe", Map.of("5", BigInteger.valueOf(25), "a", "spam", "c", "foo"), 26),

                // Keys with special characters
                arguments("d9:key:value5:helloe", Map.of("key:value", "hello"), 20),
                arguments("d7:a:b:c:d4:teste", Map.of("a:b:c:d", "test"), 17),
                arguments("d3:\n\r\t4:datae", Map.of("\n\r\t", "data"), 13),

                // Values with special characters
                arguments("d4:data3:\n\r\te", Map.of("data", "\n\r\t"), 13),
                arguments("d3:msg2:[]e", Map.of("msg", "[]"), 11),
                arguments("d4:json2:{}e", Map.of("json", "{}"), 12),
                arguments("d5:space1: e", Map.of("space", " "), 12),

                // Large dictionaries
                arguments("d1:a1:b1:c1:d1:e1:f1:g1:h1:i1:je",
                        Map.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), 32),

                // Dictionaries with extra data after (should parse only the dictionary)
                arguments("d3:foo3:bareextra", Map.of("foo", "bar"), 12),
                arguments("dei42e", Map.of(), 2),
                arguments("d1:a1:be4:spam", Map.of("a", "b"), 8),
                arguments("d1:a1:b1:c1:de4:spam4:eggs", Map.of("a", "b", "c", "d"), 14),
                arguments("d3:fooi42ee123", Map.of("foo", BigInteger.valueOf(42)), 11),
                arguments("d4:name4:Johnei999e", Map.of("name", "John"), 14),

                // Complex mixed content
                arguments("d0:4:test3:agei-123e4:datai999e4:name11:hello worlde",
                        Map.of("", "test", "age", BigInteger.valueOf(-123), "data", BigInteger.valueOf(999), "name", "hello world"), 52),

                // Dictionary with numbers containing multiple digits
                arguments("d5:largei99999e5:smalli1ee", Map.of("large", BigInteger.valueOf(99999), "small", BigInteger.valueOf(1)), 26),

                // Dictionary with string values containing numbers
                arguments("d4:code3:123e", Map.of("code", "123"), 13),
                arguments("d2:id6:user42e", Map.of("id", "user42"), 14),

                // Empty string keys and values
                arguments("d0:4:test4:data0:e", Map.of("", "test", "data", ""), 18),

                // Keys with numeric content (but still strings)
                arguments("d1:15:hello1:25:worlde", Map.of("1", "hello", "2", "world"), 22),
                arguments("d3:123i456ee", Map.of("123", BigInteger.valueOf(456)), 12),

                // Nested dictionaries
                arguments("d4:userd3:foo3:baree3:far", Map.of("user", Map.of("foo", "bar")), 20),
                arguments("d4:datad1:a1:bee", Map.of("data", Map.of("a", "b")), 16),
                arguments("d3:cfgd4:host9:localhost4:porti8080ee2:ip9:127.0.0.1e", Map.of("cfg", Map.of("host", "localhost", "port", BigInteger.valueOf(8080)), "ip", "127.0.0.1"), 53)
        );
    }

    static Stream<String> getInvalidDictionaryTestData() {
        return Stream.of(
                // Empty or null inputs
                "",

                // Missing prefix 'd'
                "3:foo3:bare",
                "i42ee",
                "e",
                "4:spam",

                // Missing suffix 'e'
                "d",
                "d3:foo3:bar",
                "d1:ai42e",
                "d3:foo3:bar4:spam4:eggs",

                // Invalid dictionary structure
                "d3:foox",
                "di1e512351",
                "di42ex",
                "dx3:bare",

                // Dictionaries with invalid keys (non-string keys)
                "di42e3:bare", // number as key
                "dinvalide", // invalid key
                "dl3:fooe3:bare", // list as key

                // Dictionaries with invalid values
                "d3:fooinvalide",
                "d3:foo3bare", // missing colon in string value
                "d3:fooi42xe", // invalid number value
                "d3:foo3:bar:invalide", // malformed value

                // Incomplete key-value pairs
                "d3:foo", // missing value
                "d3:", // incomplete key
                "d3:foo3:ba", // incomplete value
                "d", // just prefix

                // Dictionaries with unparsable keys
                "dabce", // invalid key
                "d123e", // number instead of string key
                "d:3bare", // malformed key
                "di42i43ee", // number key

                // Mixed valid and invalid pairs
                "d3:foo3:barinvalide", // valid pair followed by invalid
                "d3:fooi42e3bare", // valid pair followed by invalid string
                "d3:foo3:bari42einvalide", // valid pairs followed by invalid

                // Malformed keys (string format issues)
                "d-1:foo3:bare", // negative length key
                "d:foo3:bare", // missing length
                "da:foo3:bare", // non-numeric length
                "d3foo3:bare", // missing colon in key
                "d3:fo", // incomplete key string
                "d01:a3:bare", // leading zero in key length

                // Malformed values
                "d3:fooi-0ee", // invalid negative zero
                "d3:fooi++42ee", // double plus
                "d3:fooi--42ee", // double minus
                "d3:fooi001ee", // leading zeros in number
                "d3:foo-1:teste", // negative string length
                "d3:foo:teste", // missing string length
                "d3:fooa:teste", // non-numeric string length
                "d3:foo3teste", // missing colon in string value
                "d3:foo3:te", // incomplete string value

                // Dictionary starting with invalid chars
                "x3:foo3:bare",
                "13:foo3:bare",
                " d3:foo3:bare",

                // Dictionaries with internal structural issues
                "d3:foo3:bar3:baz", // missing value for second key
                "d3:fooi42e3:bar", // missing value for second key
                "di42e3:bar3:baze", // number as first key

                // Additional invalid cases
                "d3:foo3:barinvalid",
                "di42e3bare",
                "d3:fooi42einvalide",
                "d3:foo:invalid",
                "di42xe",
                "d3fooe",
                "dinvalide",
                "dabce",
                "d123e",
                "d:3bare",
                "di42i43ee",

                // Incomplete dictionaries
                "d3:fo",
                "d3:",
                "di4",
                "di",
                "d3:foo3:ba",
                "di42ei9",

                // Invalid characters in dictionary context
                "d3:foox",
                "di42ex",
                "dx3:bare",
                "d3:foo!e",
                "di42@e",

                // Nested structures (assuming not supported)
                "dde", // empty nested dict as key
                "dd3:foo3:baree", // dict as key
                "d3:foodd3:bar3:bazee", // dict as value (if not supported)
                "dlee", // list as key
                "d3:foole", // list as value (if not supported)

                // Malformed key-value pairs within valid dictionary structure
                "di-0ee",
                "di++42ee",
                "di--42ee",
                "di001ee",
                "d-1:teste",
                "d:teste",
                "da:teste",
                "d3teste",
                "d3:te",
                "d01:ae",
                "d3:foo-1:teste",

                // Key ordering issues (bencoding requires lexicographic ordering)
                "d3:foo3:bar1:a1:be", // keys not in order: "foo" should come after "a"
                "d1:c1:d1:a1:be", // keys not in order: "c" should come after "a"
                "d1:z1:a1:a1:be" // keys not in order: "z" should come after "a"
        );
    }
}