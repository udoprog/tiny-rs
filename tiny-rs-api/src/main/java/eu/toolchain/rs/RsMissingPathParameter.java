package eu.toolchain.rs;

public class RsMissingPathParameter extends RsRequestException {
    private static final long serialVersionUID = 6237179869390832100L;

    final String name;

    public RsMissingPathParameter(final String name) {
        super("Missing path parameter: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
