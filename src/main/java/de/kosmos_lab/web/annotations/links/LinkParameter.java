package de.kosmos_lab.web.annotations.links;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LinkParameter {
    String name() default "";

    String expression() default "";
}
