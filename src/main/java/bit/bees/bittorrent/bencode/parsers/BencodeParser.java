package bit.bees.bittorrent.bencode.parsers;

public interface BencodeParser<T> {

    boolean isParsable(String data);

    ParseResult<T> parse(String data);

}
