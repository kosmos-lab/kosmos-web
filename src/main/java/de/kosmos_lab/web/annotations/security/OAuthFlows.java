package de.kosmos_lab.web.annotations.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OAuthFlows {
    OAuthFlow implicit() default @OAuthFlow();
    OAuthFlow password() default @OAuthFlow();
    OAuthFlow clientCredentials() default @OAuthFlow();
    OAuthFlow authorizationCode() default @OAuthFlow();


}