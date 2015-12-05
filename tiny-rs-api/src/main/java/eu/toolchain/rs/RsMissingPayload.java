package eu.toolchain.rs;

public class RsMissingPayload extends RsRequestException {
    private static final long serialVersionUID = -5569766335104427802L;

    public RsMissingPayload() {
        super("Missing payload");
    }
}
