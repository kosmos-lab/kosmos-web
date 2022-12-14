package de.kosmos_lab.web.annotations.parameters;


import de.kosmos_lab.web.annotations.extensions.Extension;
import de.kosmos_lab.web.annotations.media.Content;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequestBody {
    String description() default "";

    Content[] content() default {};

    boolean required() default false;

    Extension[] extensions() default {};

    String ref() default "";
}