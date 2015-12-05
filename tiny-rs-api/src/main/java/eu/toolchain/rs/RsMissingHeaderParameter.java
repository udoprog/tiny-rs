package eu.toolchain.rs;

public class RsMissingHeaderParameter extends RsRequestException {
    private static final long serialVersionUID = -169632928749730026L;

    final String name;

    public RsMissingHeaderParameter(final String name) {
        super("Missing header parameter: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
