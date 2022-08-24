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
@Repeatable(ArraySchemas.class)
@Inherited
public @interface ArraySchema {
    Schema schema() default @Schema;

    Schema arraySchema() default @Schema;

    int maxItems() default Integer.MIN_VALUE;

    int minItems() default Integer.MAX_VALUE;

    boolean uniqueItems() default false;


    Extension[] extensions() default {};

    String name() default "";
}
