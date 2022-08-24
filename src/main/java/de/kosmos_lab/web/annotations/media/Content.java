package de.kosmos_lab.web.annotations.media;




import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Content {
    String mediaType() default "";

    ExampleObject[] examples() default {};


    Schema schema() default @Schema;

    SchemaProperty[] schemaProperties() default {};

    Schema additionalPropertiesSchema() default @Schema;

    ArraySchema array() default @ArraySchema;

    Encoding[] encoding() default {};

    Extension[] extensions() default {};
}
