package de.kosmos_lab.web.doc.openapi;


import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Channels.class)
public @interface Channel {

    String path() default "";

    int userLevel() default -1;

    boolean hidden() default false;

    boolean load() default true;

    Message[] publishMessages() default {};

    String[] publishRefs() default {};

    Message[] subscribeMessages() default {};

    String[] subscribeRefs() default {};
    Tag[] tags() default  {};
    Parameter[] parameters() default {};

    String description() default "";
    boolean needsMessage() default true;

}
