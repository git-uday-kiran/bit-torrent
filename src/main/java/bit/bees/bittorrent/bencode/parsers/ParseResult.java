package bit.bees.bittorrent.bencode.parsers;

public record ParseResult<T>(
        String input,
        Status status,
        T parsedData,
        int parsedLength,
        Throwable error) {

    public enum Status {
        SUCCESS, FAILURE
    }

    static <T> ParseResult<T> success(String input, T parsedData, int parsedLength) {
        return new ParseResult<>(input, Status.SUCCESS, parsedData, parsedLength, null);
    }

    static <T> ParseResult<T> failure(String input, Throwable error) {
        return new ParseResult<>(input, Status.FAILURE, null, -1, error);
    }
}
