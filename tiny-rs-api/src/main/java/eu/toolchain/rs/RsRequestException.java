package eu.toolchain.rs;

public class RsRequestException extends RuntimeException {
    private static final long serialVersionUID = -7023105556925074680L;

    public RsRequestException(final String message) {
        super(message);
    }
}
