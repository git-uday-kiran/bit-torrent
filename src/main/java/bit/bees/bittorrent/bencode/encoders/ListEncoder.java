package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Component
class ListEncoder implements BencodeEncoder {

    private final Collection<BencodeEncoder> bencodeEncoders;

    ListEncoder(StringEncoder stringEncoder, NumberEncoder numberEncoder, DictionaryEncoder dictionaryEncoder) {
        this.bencodeEncoders = List.of(stringEncoder, numberEncoder, dictionaryEncoder);
    }

    @Override
    public <T> boolean canEncode(T data) {
        if (data instanceof Collection<?> collectionData) {
            return collectionData.stream().allMatch(this::isItemCanEncoded);
        }
        return false;
    }

    @Override
    public <T> String encode(T data) {
        if (canEncode(data) && data instanceof Collection<?> collectionData) {
            var result = new StringBuilder();
            for (Object item : collectionData) {
                encodeItem(item, result::append);
            }
            return "l" + result + "e";
        }
        throw new BencodeException("Can not encode '%s' as dictionary.".formatted(data));
    }

    private void encodeItem(Object item, Consumer<String> encodedDataCollector) {
        for (BencodeEncoder encoder : bencodeEncoders) {
            encoder.encode(item, encodedDataCollector);
        }
    }

    private boolean isItemCanEncoded(Object item) {
        for (BencodeEncoder encoder : bencodeEncoders) {
            if (encoder.canEncode(item)) {
                return true;
            }
        }
        return false;
    }

}
