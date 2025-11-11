package bit.bees.bittorrent.bencode.encoders;

import java.util.function.Consumer;

interface BencodeEncoder {

    <T> boolean canEncode(T data);

    <T> String encode(T data);

    default <T> boolean encode(T data, Consumer<String> callback) {
        if (canEncode(data)) {
            var encodedData = encode(data);
            callback.accept(encodedData);
            return true;
        }
        return false;
    }

}
