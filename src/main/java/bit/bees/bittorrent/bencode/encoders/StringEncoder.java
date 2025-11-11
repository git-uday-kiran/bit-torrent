package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.springframework.stereotype.Component;

@Component
class StringEncoder implements BencodeEncoder {

    @Override
    public <T> boolean canEncode(T data) {
        return data instanceof String;
    }

    @Override
    public <T> String encode(T data) {
        if (canEncode(data)) {
            if (data instanceof String stringData) {
                return stringData.length() + ":" + stringData;
            }
        }
        throw new BencodeException("Can not encode '%s' as string.".formatted(data));
    }

}
