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
        log.debug("Checking if data is isParsable as string: '{}'", data);
        boolean isParsable = false;

        if (data != null && !data.isEmpty()) {
            int colonIndex = data.indexOf(':');
            if (colonIndex > 0) {
                var possibleNumberString = data.substring(0, colonIndex);
                var lengthOpt = getAsNumber(possibleNumberString);
                if (lengthOpt.isPresent()) {
                    BigInteger stringLength = lengthOpt.get();
                    isParsable = (data.length() - colonIndex - 1) >= stringLength.intValue();
                }
            }
        }

        log.info("'{}' is parsable: {}", data, isParsable);
        return isParsable;
    }

    @Override
    public String parse(String data) {
        if (!isParsable(data)) {
            throw new BencodeException("%s is not parsable".formatted(data));
        }
        int colonIndex = data.indexOf(':');
        var lengthString = data.substring(0, colonIndex);
        var length = getAsNumber(lengthString).orElse(BigInteger.ZERO);

        int stringStartIndex = colonIndex + 1;
        return data.substring(stringStartIndex, stringStartIndex + length.intValue());
    }
}
