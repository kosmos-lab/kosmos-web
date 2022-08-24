package de.kosmos_lab.web.annotations.links;



import de.kosmos_lab.web.annotations.extensions.Extension;
import de.kosmos_lab.web.annotations.servers.Server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Link {
    String name() default "";

    String operationRef() default "";

    String operationId() default "";

    LinkParameter[] parameters() default {};

    String description() default "";

    String requestBody() default "";

    Server server() default @Server;

    Extension[] extensions() default {};

    String ref() default "";
}
