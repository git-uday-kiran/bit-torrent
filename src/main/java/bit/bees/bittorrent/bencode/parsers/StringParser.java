package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;

import static bit.bees.bittorrent.bencode.parsers.ParserUtil.getAsNumber;

@Component
public class StringParser implements BencodeParser {

    private final Logger log = LoggerFactory.getLogger(StringParser.class);

    @Override
    public boolean isParsable(String data) {
        log.debug("Checking if data is isParsable as string: '{}'", data);
        boolean isParsable = false;

        if (data != null && !data.isEmpty()) {
            int colonIndex = data.indexOf(':');
            if (colonIndex > 0) {
                String possibleNumberString = data.substring(0, colonIndex);
                Optional<BigInteger> lengthOpt = getAsNumber(possibleNumberString);
                if (lengthOpt.isPresent()) {
                    BigInteger stringLength = lengthOpt.get();
                    isParsable = (data.length() - colonIndex - 1) >= stringLength.intValue();
                }
            }
        }

        log.info("Date is parsable: {}", isParsable);
        return isParsable;
    }

    @Override
    public String parse(String data) {
        if (!isParsable(data)) {
            throw new BencodeException("Date '" + data + "' is not parsable");
        }
        int colonIndex = data.indexOf(':');
        String lengthString = data.substring(0, colonIndex);
        BigInteger length = getAsNumber(lengthString).orElse(BigInteger.ZERO);

        int stringStartIndex = colonIndex + 1;
        return data.substring(stringStartIndex, stringStartIndex + length.intValue());
    }

}
