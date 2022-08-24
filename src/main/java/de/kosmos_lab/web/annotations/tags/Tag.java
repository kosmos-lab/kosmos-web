package de.kosmos_lab.web.annotations.tags;



import de.kosmos_lab.web.annotations.ExternalDocumentation;
import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Tags.class)
@Inherited
public @interface Tag {
    String name();

    String description() default "";

    ExternalDocumentation externalDocs() default @ExternalDocumentation;

    Extension[] extensions() default {};
}
