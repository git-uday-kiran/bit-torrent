package bit.bees.bittorrent.bencode.parsers;

import bit.bees.bittorrent.bencode.BencodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class NumberParser implements BencodeParser<BigInteger> {

    private final Logger log = LoggerFactory.getLogger(NumberParser.class);

    @Override
    public boolean isParsable(String data) {
        log.info("Checking if '{}' is parsable as integer", data);
        boolean isParsable = false;

        if (data != null && data.startsWith("i")) {
            int numberEndIndex = data.indexOf('e') - 1;
            if (numberEndIndex >= 1) {
                int numberStartIndex = 1;
                var numberString = data.substring(numberStartIndex, numberEndIndex + 1);
                isParsable = isValidNumber(numberString);
            }
        }

        log.info("'{}' is parsable: {}", data, isParsable);
        return isParsable;
    }

    private boolean isValidNumber(String number) {
        if (number.isBlank() || number.startsWith("-0")) {
            return false;
        }
        if (ParserUtil.isNumber(number)) {
            if (number.startsWith("+")) {
                return (number.length() == 2) || (number.charAt(1) != '0');
            }
            return (number.length() == 1) || (number.charAt(0) != '0');
        }
        return false;
    }

    @Override
    public BigInteger parse(String data) {
        if (!isParsable(data)) {
            throw new BencodeException("%s is not parsable".formatted(data));
        }
        String numberString = data.substring(1, data.indexOf('e'));
        return ParserUtil.getAsNumber(numberString).orElseThrow();
    }
}
