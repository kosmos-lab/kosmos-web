package de.kosmos_lab.web.annotations.info;

import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface License {
    /**
     * The license name used for the API.
     *
     * @return the name of the license
     **/
    String name() default "";

    /**
     * A URL to the license used for the API. MUST be in the format of a URL.
     *
     * @return the URL of the license
     **/
    String url() default "";

    /**
     * The list of optional extensions
     *
     * @return an optional array of extensions
     */
    Extension[] extensions() default {};

}