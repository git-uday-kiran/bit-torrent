package bit.bees.bittorrent.bencode.parsers;

import java.math.BigInteger;
import java.util.Optional;

public final class ParserUtil {

    public static boolean isNumber(String string) {
        return getAsNumber(string).isPresent();
    }

    public static Optional<BigInteger> getAsNumber(String data) {
        try {
            return Optional.of(new BigInteger(data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private ParserUtil() {
    }
}
