package de.kosmos_lab.web.doc.openapi;


import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.tags.Tag;

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


    Channel[] channels() default {};

    boolean enableMQTT() default false;

    boolean enableWS() default true;
}
