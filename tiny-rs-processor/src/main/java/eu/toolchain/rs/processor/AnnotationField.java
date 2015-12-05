package eu.toolchain.rs.processor;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

import com.google.common.collect.ImmutableList;

import eu.toolchain.rs.processor.result.Result;
import lombok.Data;

@Data
public class AnnotationField {
    private final Element element;
    private final AnnotationMirror annotation;
    private final AnnotationValue value;

    public AnnotationValue get() {
        return value;
    }

    public Result<TypeMirror> asTypeMirror() {
        final TypeMirror typeMirror =
                value.accept(new SimpleAnnotationValueVisitor8<TypeMirror, Void>() {
                    @Override
                    public TypeMirror visitType(TypeMirror t, Void p) {
                        return t;
                    }

                    @Override
                    protected TypeMirror defaultAction(Object o, Void p) {
                        return null;
                    }
                }, null);

        if (typeMirror == null) {
            return Result.brokenAnnotationValue("Could not resolve type", element, annotation,
                    value);
        }

        if (typeMirror instanceof ErrorType) {
            return Result.brokenAnnotationValue("Could not resolve type", element, annotation,
                    value);
        }

        return Result.ok(typeMirror);
    }

    public List<String> asStringArray() {
        return value.accept(new SimpleAnnotationValueVisitor8<List<String>, Void>() {
            @Override
            public List<String> visitString(String s, Void p) {
                return ImmutableList.of(s);
            }

            @Override
            public List<String> visitArray(List<? extends AnnotationValue> vals, Void p) {
                final ImmutableList.Builder<String> values = ImmutableList.builder();

                for (final AnnotationValue value : vals) {
                    values.addAll(new AnnotationField(element, annotation, value).asStringArray());
                }

                return values.build();
            }

            @Override
            protected List<String> defaultAction(Object o, Void p) {
                throw new IllegalArgumentException("Not a string array: " + value);
            }
        }, null);
    }

    public String asString() {
        return value.accept(new SimpleAnnotationValueVisitor8<String, Void>() {
            @Override
            public String visitString(String s, Void p) {
                return s;
            }

            @Override
            protected String defaultAction(Object o, Void p) {
                throw new IllegalArgumentException("Not a string: " + value);
            }
        }, null);
    }

    public short asShort() {
        return value.accept(new SimpleAnnotationValueVisitor8<Short, Void>() {
            @Override
            public Short visitInt(int i, Void p) {
                return Integer.valueOf(i).shortValue();
            }

            @Override
            public Short visitShort(short s, Void p) {
                return s;
            }

            @Override
            protected Short defaultAction(Object o, Void p) {
                throw new IllegalArgumentException(
                        String.format("Could not convert %s to Short", value));
            }
        }, null);
    }

    public int asInteger() {
        return value.accept(new SimpleAnnotationValueVisitor8<Integer, Void>() {
            @Override
            public Integer visitInt(int i, Void p) {
                return i;
            }

            @Override
            public Integer visitShort(short s, Void p) {
                return Short.valueOf(s).intValue();
            }

            @Override
            protected Integer defaultAction(Object o, Void p) {
                throw new IllegalArgumentException(
                        String.format("Could not convert %s to Integer", value));
            }
        }, null);
    }

    public List<AnnotationMirror> asAnnotationMirror() {
        return value.accept(new SimpleAnnotationValueVisitor8<List<AnnotationMirror>, Void>() {
            @Override
            public List<AnnotationMirror> visitAnnotation(AnnotationMirror a, Void p) {
                return ImmutableList.of(a);
            }

            @Override
            public List<AnnotationMirror> visitArray(List<? extends AnnotationValue> vals, Void p) {
                final ImmutableList.Builder<AnnotationMirror> mirrors = ImmutableList.builder();

                for (final AnnotationValue val : vals) {
                    mirrors.add(
                            val.accept(new SimpleAnnotationValueVisitor8<AnnotationMirror, Void>() {
                        public AnnotationMirror visitAnnotation(AnnotationMirror a, Void p) {
                            return a;
                        };

                        @Override
                        protected AnnotationMirror defaultAction(Object o, Void p) {
                            throw new IllegalArgumentException(String
                                    .format("Could not convert %s to AnnotationMirror", value));
                        }
                    }, null));
                }

                return mirrors.build();
            }

            @Override
            protected List<AnnotationMirror> defaultAction(Object o, Void p) {
                throw new IllegalArgumentException(
                        String.format("Could not convert %s to AnnotationMirror", value));
            }
        }, null);
    }

    public boolean asBoolean() {
        return value.accept(new SimpleAnnotationValueVisitor8<Boolean, Void>() {
            @Override
            public Boolean visitBoolean(boolean b, Void p) {
                return b;
            }

            @Override
            protected Boolean defaultAction(Object o, Void p) {
                throw new IllegalArgumentException(
                        String.format("Could not convert %s to Boolean", value));
            }
        }, null);
    }
}
