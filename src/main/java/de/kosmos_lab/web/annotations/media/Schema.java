package de.kosmos_lab.web.annotations.media;

import de.kosmos_lab.web.annotations.ExternalDocumentation;
import de.kosmos_lab.web.annotations.enums.SchemaType;
import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Schemas.class)
@Inherited
public @interface Schema {


    Class<?> implementation() default Void.class;

    Class<?> not() default Void.class;
    String[] notRef() default {};
    Class<?>[] oneOf() default {};
    String[] oneOfRef() default {};

    Class<?>[] anyOf() default {};
    String[] anyOfRef() default {};
    Class<?>[] allOf() default {};
    String[] allOfRef() default {};
    String name() default "";

    String title() default "";

    double multipleOf() default 0.0;

    String maximum() default "";

    boolean exclusiveMaximum() default false;

    String minimum() default "";

    boolean exclusiveMinimum() default false;

    int maxLength() default Integer.MAX_VALUE;

    int minLength() default 0;

    String pattern() default "";

    int maxProperties() default 0;

    int minProperties() default 0;

    String[] requiredProperties() default {};

    boolean required() default false;

    String description() default "";

    String format() default "";

    String ref() default "";

    boolean nullable() default false;

    /** @deprecated */
    @Deprecated
    boolean readOnly() default false;

    /** @deprecated */
    @Deprecated
    boolean writeOnly() default false;

    AccessMode accessMode() default AccessMode.AUTO;

    String example() default "";
    ExampleObject[] examples() default {};


    ExternalDocumentation externalDocs() default @ExternalDocumentation;

    boolean deprecated() default false;

    SchemaType type() default SchemaType.DEFAULT;

    String[] allowableValues() default {};

    String defaultValue() default "";

    String discriminatorProperty() default "";

    DiscriminatorMapping[] discriminatorMapping() default {};

    boolean hidden() default false;

    boolean enumAsRef() default false;

    Class<?>[] subTypes() default {};

    Extension[] extensions() default {};

    String additionalProperties() default "";


    public static enum AccessMode {
        AUTO,
        READ_ONLY,
        WRITE_ONLY,
        READ_WRITE;

        private AccessMode() {
        }
    }
}