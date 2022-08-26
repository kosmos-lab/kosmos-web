package de.kosmos_lab.web.annotations.media;



import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ObjectSchemas.class)
@Inherited
public @interface ObjectSchema {
    String componentName() default "";
    Schema schema() default @Schema;

    SchemaProperty[] properties() default {};


    Extension[] extensions() default {};
    ExampleObject[] examples() default {};

}
