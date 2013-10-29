package com.peergreen.ipojo.management;

import org.apache.felix.ipojo.annotations.Ignore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * <pre>
 *     {@link com.peergreen.ipojo.management.MBean @MBean}
 *     {@code @Description("This is a human description of the MBean")}
 *     {@link org.apache.felix.ipojo.annotations.Component @Component}
 *     public class ManagedComponent {
 *         ...
 *     }
 * </pre>
 */
@Ignore
@Target({TYPE, ANNOTATION_TYPE, FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    /**
     * Description of the MBean, attribute or operation.
     */
    String value();
}
