package de.kosmos_lab.web.annotations.servers;


import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ServerVariable {
    String name();

    String[] allowableValues() default {""};

    String defaultValue();

    String description() default "";

    Extension[] extensions() default {};
}
