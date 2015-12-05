package eu.toolchain.rs;

public class RsMissingQueryParameter extends RsRequestException {
    private static final long serialVersionUID = -7711898267066720517L;

    final String name;

    public RsMissingQueryParameter(final String name) {
        super("Missing query parameter: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
