package az.tikinti.portal.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Suppresses AOP logging for a method or an entire class.
 * Apply to any service/controller method that handles raw binary data,
 * or to classes that have their own meaningful log statements.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoLog {
}
