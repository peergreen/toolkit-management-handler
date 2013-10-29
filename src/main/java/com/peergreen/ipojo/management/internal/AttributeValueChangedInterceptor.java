package com.peergreen.ipojo.management.internal;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.felix.ipojo.util.Property;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

import java.lang.reflect.Member;

import static java.lang.String.format;

/**
 * User: guillaume
 * Date: 25/10/13
 * Time: 22:14
 */
public class AttributeValueChangedInterceptor implements FieldInterceptor, MethodInterceptor {

    private final NotificationBroadcasterSupport broadcaster;
    private final Property property;
    private long sequence = 0;
    private Object oldValue = null;

    public AttributeValueChangedInterceptor(final NotificationBroadcasterSupport broadcaster, final Property property) {
        this.broadcaster = broadcaster;
        this.property = property;
    }

    @Override
    public synchronized void onSet(final Object pojo, final String fieldName, final Object value) {
        broadcastAttributeChangedNotification(value);
    }

    private void broadcastAttributeChangedNotification(final Object value) {
        broadcaster.sendNotification(
                new AttributeChangeNotification(
                        broadcaster,
                        sequence++,
                        System.currentTimeMillis(),
                        format("Attribute value has changed"),
                        property.getName(),
                        property.getType(),
                        oldValue,
                        value)
        );
        oldValue = value;
    }

    @Override
    public Object onGet(final Object pojo, final String fieldName, final Object value) {
        return value;
    }

    @Override
    public void onEntry(final Object pojo, final Member method, final Object[] args) {
        broadcastAttributeChangedNotification(args[0]);
    }

    @Override
    public void onExit(final Object pojo, final Member method, final Object returnedObj) {

    }

    @Override
    public void onError(final Object pojo, final Member method, final Throwable throwable) {

    }

    @Override
    public void onFinally(final Object pojo, final Member method) {

    }
}
