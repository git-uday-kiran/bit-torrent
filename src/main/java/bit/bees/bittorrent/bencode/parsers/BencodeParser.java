package bit.bees.bittorrent.bencode.parsers;

public interface BencodeParser<T> {

    boolean isParsable(String data);

    T parse(String data);

}
