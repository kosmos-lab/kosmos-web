package de.kosmos_lab.web.annotations.servers;

import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AsyncServers.class)
@Inherited
public @interface AsyncServer {
    String url() default "";
    String name();
    String description() default "";
    String protocol();
    String protocolVersion() default "";
    ServerVariable[] variables() default {};


    Extension[] extensions() default {};
}
