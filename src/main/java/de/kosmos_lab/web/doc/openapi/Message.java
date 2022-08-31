package de.kosmos_lab.web.doc.openapi;


import de.kosmos_lab.web.annotations.headers.Header;
import de.kosmos_lab.web.annotations.media.ExampleObject;
import de.kosmos_lab.web.annotations.media.ObjectSchema;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Messages.class)
public @interface Message {



    Header[] headers() default {};

    String contentType() default "application/json";

    String name() default "";

    String title() default "";

    String summary() default "";

    String description() default "";

    Tag[] tags() default {};

    String externalDocs() default "";

    ExampleObject[] examples() default {};

    String[] payloadRefs() default {};
    String[] xResponseRefs() default {};
    ObjectSchema payload() default @ObjectSchema;
    ObjectSchema xResponse() default @ObjectSchema;

    Schema payloadSchema() default @Schema;
    Schema xResponseSchema() default @Schema;


}
