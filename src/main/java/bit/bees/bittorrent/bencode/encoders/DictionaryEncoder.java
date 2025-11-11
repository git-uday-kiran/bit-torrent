package bit.bees.bittorrent.bencode.encoders;

import bit.bees.bittorrent.bencode.BencodeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class DictionaryEncoder implements BencodeEncoder {

    private final StringEncoder keyEncoder;
    private final List<BencodeEncoder> valueEncoders;

    DictionaryEncoder(StringEncoder stringEncoder, NumberEncoder numberEncoder, ListEncoder listEncoder) {
        this.keyEncoder = stringEncoder;
        this.valueEncoders = List.of(stringEncoder, numberEncoder, listEncoder);
    }

    @Override
    public <T> boolean canEncode(T data) {
        return data instanceof Map<?, ?> mapData
                && allKeysCanEncoded(mapData)
                && allValuesCanEncoded(mapData);
    }

    @Override
    public <T> String encode(T data) {
        if (canEncode(data) && data instanceof Map<?, ?> mapData) {
            var result = mapData.entrySet().stream()
                    .sorted(this::compare)
                    .map(this::encodeKeyAndValue)
                    .collect(Collectors.joining(""));
            return "d" + result + "e";
        }
        throw new BencodeException("Can not encode '%s' as dictionary.".formatted(data));
    }

    private int compare(Map.Entry<?, ?> entry1, Map.Entry<?, ?> entry2) {
        return entry1.getKey().toString().compareTo(entry2.getKey().toString());
    }

    private String encodeKeyAndValue(Map.Entry<?, ?> entry) {
        var key = entry.getKey();
        var value = entry.getValue();
        return encodeKey(key) + encodeValue(value);
    }

    private boolean allValuesCanEncoded(Map<?, ?> mapData) {
        return mapData.values().stream().allMatch(this::isValueCanEncoded);
    }

    private boolean allKeysCanEncoded(Map<?, ?> mapData) {
        return mapData.keySet().stream().allMatch(this::isKeyCanEncoded);
    }

    private boolean isKeyCanEncoded(Object keyData) {
        return keyEncoder.canEncode(keyData);
    }

    private String encodeKey(Object keyData) {
        return keyEncoder.encode(keyData);
    }

    private boolean isValueCanEncoded(Object valueData) {
        for (BencodeEncoder encoder : valueEncoders) {
            if (encoder.canEncode(valueData)) {
                return true;
            }
        }
        return this.canEncode(valueData);
    }

    private String encodeValue(Object valueData) {
        for (BencodeEncoder encoder : valueEncoders) {
            if (encoder.canEncode(valueData)) {
                return encoder.encode(valueData);
            }
        }
        if (this.canEncode(valueData)) {
            return this.encode(valueData);
        }
        throw new BencodeException("Can not encode '%s' as value in dictionary.".formatted(valueData));
    }
}
