package bit.bees.bittorrent.bencode.parsers;

public interface BencodeParser {

    boolean isParsable(String data);

    String parse(String data);

}
