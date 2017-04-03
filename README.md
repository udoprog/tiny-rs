# Tiny Restful Compiler

A tiny restful compiler for Java.

This project is an Annotation Processor for the javax.ws.rs API that provides
compile-time, programmatic bindings to your javax.ws.rs classes.

## How to Use

Add the following dependencies to your project:

```xml
<dependency>
  <groupId>eu.toolchain.rs</groupId>
  <artifactId>tiny-rs-api</artifactId>
  <version>${rs.version}</version>
</dependency>
<dependency>
  <groupId>eu.toolchain.rs</groupId>
  <artifactId>tiny-rs-processor</artifactId>
  <version>${rs.version}</version>
  <optional>true</optional>
</dependency>
```

## What is this?

This project contains an [Annotation Processor](https://docs.oracle.com/javase/7/docs/api/javax/annotation/processing/Processor.html),
which is an extension method to the Java compiler for performing pre-compile
validation and source generation.

The processor triggers for any class or interface that either annotated with,
or has a method annotated with `@Path`, `@Produces`, `@Consumes`, or one of the
method annotations (`@GET`, `@POST`, `@PUT`, `@DELETE`, `@OPTIONS`).

This will cause a class named `<name>_Binding` to be generated, this expects
an instance of the annotated class as its only argument.
This class can be used to bind and validate your resource *at compile time* in
fairly dynamic fashion.

The following is an example of a generated binding (You can find more in
[processortests](tiny-rs-processor/src/test/resources/processortests)):

```java
public interface Foo {
    @GET
    @Path("hello/{name}")
    String hello(@PathParam("name") final String name, @QueryParam("age") final Optional<Integer> age);
}

public class Foo_Binding {
    private final Foo instance;

    public Foo_Binding(final Foo instance) {
        this.instance = instance;
    }

    public String hello(final RsRequestContext ctx) {
        final String name = ctx.getPathParameter("name").orElseThrow(() -> new RsMissingPathParameter("name")).asString();
        final Optional<Integer> age = ctx.getQueryParameter("age").map(RsParameter::asInteger);
        return instance.hello(name, age);
    }

    public RsMapping<String> hello_mapping() {
        return RsMapping.<String>builder().method("GET").path("hello", "{name}").handle(this::hello).build();
    }

    public List<RsMapping<String>> routes() {
        final List<RsMapping<String>> routes = new ArrayList<>();
        routes.add(hello_mapping());
        return routes;
    }
}
```

A framework is responsible for providing an implementation of
`RsRequestContext`, and all relevant `RsParameter` implementations.

# TODO

The following annotations are **not** supported:

* [`javax.ws.rs.ApplicationPath`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/ApplicationPath.html)
* [`javax.ws.rs.BeanParam`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/BeanParam.html)
* [`javax.ws.rs.ConstrainedTo`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/ConstrainedTo.html)
* [`javax.ws.rs.CookieParam`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/CookieParam.html)
  * Trivial to implement by adding accessors to `RsRequestContext`.
* [`javax.ws.rs.Encoded`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/Encoded.html)
* [`javax.ws.rs.FormParam`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/FormParam.html)
  * Trivial to implement by adding accessors to `RsRequestContext`.
* [`javax.ws.rs.MatrixParam`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/MatrixParam.html)
  * Trivial to implement by adding accessors to `RsRequestContext`.
* [`javax.ws.rs.NameBinding`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/NameBinding.html)

The following packages are **not** supported, and require a lot of
consideration for how they should best be incorporated.

* All of [`javax.ws.rs.client`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/client/package-summary.html)
* All of [`javax.ws.rs.container`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/container/package-summary.html)
* All of [`javax.ws.rs.ext`](https://docs.oracle.com/javaee/7/api/javax/ws/rs/ext/package-summary.html)
