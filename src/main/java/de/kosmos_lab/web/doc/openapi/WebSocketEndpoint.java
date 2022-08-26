package de.kosmos_lab.web.doc.openapi;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface WebSocketEndpoint {

    String path();

    int userLevel() default -1;

    boolean hidden() default false;

    boolean load() default true;

    Message[] publishMessages() default {};

    String[] publishRefs() default {};

    Message[] subscribeMessages() default {};

    String[] subscribeRefs() default {};
}
