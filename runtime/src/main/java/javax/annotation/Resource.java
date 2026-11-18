/* SPDX-License-Identifier: LGPL-3.0-or-later */
package javax.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stand-in for the JSR-250 {@code @Resource} annotation, removed from the JDK in Java 11.
 *
 * <p>Mixin 0.7 reads this annotation from LaunchWrapper transformers. Only its name and retention matter for that;
 * the members match JSR-250 so annotated classes compiled against the original still read correctly.
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resource {
    /** {@return the JNDI name of the resource} */
    String name() default "";

    /** {@return a JNDI lookup name} */
    String lookup() default "";

    /** {@return the Java type of the resource} */
    Class<?> type() default Object.class;

    /** {@return the authentication type} */
    AuthenticationType authenticationType() default AuthenticationType.CONTAINER;

    /** {@return whether the resource can be shared} */
    boolean shareable() default true;

    /** {@return a product-specific name} */
    String mappedName() default "";

    /** {@return a description} */
    String description() default "";

    /** The authentication types JSR-250 defines. */
    enum AuthenticationType {
        /** Container-managed authentication. */
        CONTAINER,
        /** Application-managed authentication. */
        APPLICATION
    }
}
