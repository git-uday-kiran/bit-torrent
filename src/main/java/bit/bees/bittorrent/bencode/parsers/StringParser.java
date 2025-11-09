package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static bit.bees.bittorrent.bencode.parsers.ParserUtil.getAsNumber;

@Component
public class StringParser implements BencodeParser<String> {

    private final Logger log = LoggerFactory.getLogger(StringParser.class);

    @Override
    public boolean isParsable(String data) {
        log.debug("Checking if '{}' is parsable as string", data);
        boolean isParsable = false;

        if (data != null && !data.isEmpty()) {
            int colonIndex = data.indexOf(':');
            if (colonIndex > 0) {
                var possibleNumberString = data.substring(0, colonIndex);
                if (isValidNumberPrefix(possibleNumberString)) {
                    var lengthOpt = getAsNumber(possibleNumberString);
                    if (lengthOpt.isPresent()) {
                        int stringLength = lengthOpt.get().intValue();
                        isParsable = (stringLength >= 0) && ((data.length() - colonIndex - 1) >= stringLength);
                    }
                }
            }
        }

        log.info("'{}' is{} parsable", data, (isParsable ? "" : " not"));
        return isParsable;
    }

    private static boolean isValidNumberPrefix(String possibleNumberString) {
        return possibleNumberString.equals("0")
                || possibleNumberString.matches("^[1-9].*");
    }

    @Override
    public ParseResult<String> parse(String data) {
        if (!isParsable(data)) {
            BencodeException error = new BencodeException("'%s' is not parsable".formatted(data));
            return ParseResult.failure(data, error);
        }

        int colonIndex = data.indexOf(':');
        var lengthString = data.substring(0, colonIndex);
        var length = getAsNumber(lengthString).orElse(BigInteger.ZERO);

        int stringStartIndex = colonIndex + 1;
        int stringEndIndex = stringStartIndex + length.intValue() - 1;

        String parsedData = data.substring(stringStartIndex, stringEndIndex + 1);
        int parsedLength = (stringEndIndex + 1);

        return ParseResult.success(data, parsedData, parsedLength);
    }
}
