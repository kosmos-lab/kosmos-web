package de.kosmos_lab.web.annotations.responses;


import de.kosmos_lab.web.annotations.extensions.Extension;
import de.kosmos_lab.web.annotations.headers.Header;
import de.kosmos_lab.web.annotations.links.Link;
import de.kosmos_lab.web.annotations.media.Content;
import de.kosmos_lab.web.doc.openapi.ResponseCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(ApiResponses.class)
public @interface ApiResponse {
    String componentName()  default "";
    String description() default "";

    ResponseCode responseCode() ;

    Header[] headers() default {};

    Link[] links() default {};

    Content[] content() default {};

    Extension[] extensions() default {};

    String ref() default "";

    boolean useReturnTypeSchema() default false;


}
