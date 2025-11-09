package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictionaryParser implements BencodeParser<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryParser.class);
    private static final char PREFIX = 'd';
    private static final char SUFFIX = 'e';

    private static final List<BencodeParser<String>> keyParsers = List.of(new StringParser());
    private static final List<BencodeParser<?>> valueParsers = List.of(
            new ListParser(),
            new NumberParser(),
            new StringParser(),
            new DictionaryParser()
    );

    @Override
    public boolean isParsable(String data) {
        log.info("Checking if '{}' is parsable", data);
        boolean isParsable = false;

        if (data != null && !data.isBlank() && data.startsWith(String.valueOf(PREFIX))) {

            int keyValuePairStartIndex = 1;
            KeyValueParseResult result;

            String lastKey = "";
            boolean keysInOrder = true;

            while ((result = tryToParse(data, keyValuePairStartIndex)).isSuccess()) {
                String parsedKey = result.keyResult.parsedData();
                if (lastKey.compareTo(parsedKey) > 0) {
                    keysInOrder = false;
                    log.info("Keys are not in lexicographic order: last key = '{}', current key = '{}'", lastKey, parsedKey);
                    break;
                }
                lastKey = parsedKey;
                keyValuePairStartIndex = keyValuePairStartIndex + result.parsedLength();
            }
            if (keysInOrder && isEndOfTheDictionary(data, keyValuePairStartIndex)) {
                isParsable = true;
            }
        }

        log.info("'{}' is{} parsable", data, (isParsable ? "" : " not"));
        return isParsable;
    }

    private static boolean isEndOfTheDictionary(String data, int index) {
        return index < data.length() && data.charAt(index) == SUFFIX;
    }

    private KeyValueParseResult tryToParse(String data, int keyValuePairStartIdx) {
        if (keyValuePairStartIdx >= data.length()) {
            return KeyValueParseResult.failure(data, new BencodeException("Invalid key pair starting index provided"));
        }

        int keyStartIndex = keyValuePairStartIdx;
        var keyResult = tryToParseKey(data, keyStartIndex);
        if (keyResult.status() == ParseResult.Status.FAILURE) {
            return KeyValueParseResult.failure(data, new BencodeException("Failed parsing key", keyResult.error()));
        }

        int valueStartIndex = keyValuePairStartIdx + keyResult.parsedLength();
        var valueResult = tryToParseValue(data, valueStartIndex);
        if (valueResult.status() == ParseResult.Status.FAILURE) {
            return KeyValueParseResult.failure(data, new BencodeException("Failed parsing value", valueResult.error()));
        }

        return KeyValueParseResult.success(data, keyResult, valueResult);
    }

    private ParseResult<String> tryToParseKey(String data, int keyStartIndex) {
        String inputData = data.substring(keyStartIndex);
        for (var keyParser : keyParsers) {
            var result = keyParser.parse(inputData);
            if (result.status() == ParseResult.Status.SUCCESS) {
                return result;
            }
        }
        return ParseResult.failure(inputData, new BencodeException("Not parsable by available parsers"));
    }

    private ParseResult<?> tryToParseValue(String data, int valueStartIndex) {
        String inputData = data.substring(valueStartIndex);
        for (var valueParser : valueParsers) {
            var result = valueParser.parse(inputData);
            if (result.status() == ParseResult.Status.SUCCESS) {
                return result;
            }
        }
        return ParseResult.failure(inputData, new BencodeException("Not parsable by available parsers"));
    }

    @Override
    public ParseResult<Map<String, Object>> parse(String data) {
        if (!isParsable(data)) {
            return ParseResult.failure(data, new BencodeException("'%s' is not parsable".formatted(data)));
        }

        int keyValuePairStartIndex = 1;
        KeyValueParseResult result;
        Map<String, Object> keyValues = new HashMap<>();

        int parsedLength = 2;
        while ((result = tryToParse(data, keyValuePairStartIndex)).isSuccess()) {
            keyValues.put(result.keyResult.parsedData(), result.valueResult.parsedData());
            parsedLength += result.parsedLength();
            keyValuePairStartIndex = keyValuePairStartIndex + result.parsedLength();
        }
        return ParseResult.success(data, keyValues, parsedLength);
    }

    private record KeyValueParseResult(
            String data,
            ParseResult<String> keyResult,
            ParseResult<?> valueResult,
            int parsedLength,
            Throwable error) {

        boolean isSuccess() {
            return (keyResult != null && valueResult != null)
                    && keyResult.status() == ParseResult.Status.SUCCESS
                    && valueResult.status() == ParseResult.Status.SUCCESS;
        }

        static KeyValueParseResult success(String data, ParseResult<String> keyResult, ParseResult<?> valueResult) {
            return new KeyValueParseResult(
                    data,
                    keyResult,
                    valueResult,
                    keyResult.parsedLength() + valueResult.parsedLength(),
                    null);
        }

        static KeyValueParseResult failure(String data, Throwable error) {
            return new KeyValueParseResult(data, null, null, -1, error);
        }
    }

}
