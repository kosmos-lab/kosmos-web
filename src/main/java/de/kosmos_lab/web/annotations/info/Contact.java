package de.kosmos_lab.web.annotations.info;

import de.kosmos_lab.web.annotations.extensions.Extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Contact {
    /**
     * The identifying name of the contact person/organization.
     *
     * @return the name of the contact
     **/
    String name() default "";

    /**
     * The URL pointing to the contact information. Must be in the format of a URL.
     *
     * @return the URL of the contact
     **/
    String url() default "";

    /**
     * The email address of the contact person/organization. Must be in the format of an email address.
     *
     * @return the email address of the contact
     **/
    String email() default "";

    /**
     * The list of optional extensions
     *
     * @return an optional array of extensions
     */
    Extension[] extensions() default {};

}