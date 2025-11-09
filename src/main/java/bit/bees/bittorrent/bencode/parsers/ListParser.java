package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ListParser implements BencodeParser<List<Object>> {

    private static final Logger log = LoggerFactory.getLogger(ListParser.class);
    private static final char PREFIX = 'l';
    private static final char SUFFIX = 'e';

    private final List<BencodeParser<?>> parsers = List.of(new StringParser(), new NumberParser());

    @Override
    public boolean isParsable(String data) {
        log.info("Checking if '{}' is parsable as list.", data);
        boolean isParsable = false;

        if (data != null && !data.isBlank() && data.startsWith(String.valueOf(PREFIX))) {
            int itemStartIndex = 1;
            ParseResult<?> result;

            while ((result = tryToParse(data, itemStartIndex)).status() == ParseResult.Status.SUCCESS) {
                itemStartIndex = (itemStartIndex + result.parsedLength());
            }

            // If next item start index is the actual end position of the list. i.e, char at index is 'e'
            if (isEndOfTheList(data, itemStartIndex)) {
                isParsable = true;
            }
        }

        log.info("'{}' is {} parsable", data, (isParsable ? "" : "not"));
        return isParsable;
    }

    ParseResult<?> tryToParse(String data, int itemStartIndex) {
        if (itemStartIndex >= data.length()) {
            return ParseResult.failure(data, new BencodeException("No parser found for '" + data + "'"));
        }
        for (BencodeParser<?> parser : parsers) {
            var result = parser.parse(data.substring(itemStartIndex));
            if (result.status() == ParseResult.Status.SUCCESS) {
                return result;
            }
        }
        return ParseResult.failure(data, new BencodeException("No parser found for '" + data + "'"));
    }


    @Override
    public ParseResult<List<Object>> parse(String data) {
        if (!isParsable(data)) {
            var error = new BencodeException("'%s' is not parsable".formatted(data));
            return ParseResult.failure(data, error);
        }

        int itemStartIndex = 1;
        ParseResult<?> result;
        List<Object> items = new ArrayList<>();

        while ((result = tryToParse(data, itemStartIndex)).status() == ParseResult.Status.SUCCESS) {
            items.add(result.parsedData());
            itemStartIndex = (itemStartIndex + result.parsedLength());
        }

        int listEndIndex = (itemStartIndex - 1);
        int parsedLength = (listEndIndex + 2);

        return ParseResult.success(data, items, parsedLength);
    }

    private static boolean isEndOfTheList(String data, int index) {
        return index < data.length() && data.charAt(index) == SUFFIX;
    }
}
