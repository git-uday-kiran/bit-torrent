package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
class NumberEncoder implements BencodeEncoder {

    @Override
    public <T> boolean canEncode(T data) {
        return data instanceof Integer || data instanceof Long || data instanceof BigInteger;
    }

    @Override
    public <T> String encode(T data) {
        if (canEncode(data)) {
            var encodedData = switch (data) {
                case Integer integerData -> integerData.toString();
                case Long longData -> longData.toString();
                case BigInteger bigIntegerData -> bigIntegerData.toString();
                default -> throw new BencodeException("Can not encode '%s' as number.".formatted(data));
            };
            return "i" + encodedData + "e";
        }
        throw new BencodeException("Can not encode '%s' as number.".formatted(data));
    }

}
