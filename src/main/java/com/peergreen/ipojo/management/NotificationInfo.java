package com.peergreen.ipojo.management;

import org.apache.felix.ipojo.annotations.Ignore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * User: guillaume
 * Date: 29/10/13
 * Time: 14:36
 */
@Ignore
@Target({ANNOTATION_TYPE, FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationInfo {
    /**
     * Notification type.
     */
    String value();
}
