package com.peergreen.ipojo.management.internal;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Property;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ReflectionException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static java.lang.String.format;

/**
 * User: guillaume
 * Date: 25/10/13
 * Time: 22:04
 */
public class ComponentDynamicMBean extends NotificationBroadcasterSupport implements DynamicMBean {

    private List<Property> attributes = new ArrayList<>();
    private MBeanInfo info;
    private final InstanceManager manager;

    public ComponentDynamicMBean(final InstanceManager manager, List<Property> attributes) {
        this.manager = manager;
        this.attributes = attributes;
    }

    @Override
    public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        Object value = findProperty(attribute).getValue();
        if (value instanceof Enum) {
            Enum e = (Enum<?>) value;
            return e.name();
        }
        return value;
    }

    private Property findProperty(final String name) throws AttributeNotFoundException {
        for (Property property : attributes) {
            if (name.equals(property.getName())) {
                return property;
            }
        }
        throw new AttributeNotFoundException(format("Attribute %s not found", name));
    }

    @Override
    public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Dictionary<String, Object> newConfiguration = getInstanceConfiguration();
        newConfiguration.put(attribute.getName(), attribute.getValue());
        manager.reconfigure(newConfiguration);
    }

    private Dictionary<String, Object> getInstanceConfiguration() {
        Hashtable<String, Object> table = new Hashtable<>();
        for (Property attribute : attributes) {
            table.put(attribute.getName(), attribute.getValue());
        }
        return table;
    }

    @Override
    public AttributeList getAttributes(final String[] attributes) {
        List<Attribute> list = new ArrayList<>();
        for (String name : attributes) {
            try {
                Property property = findProperty(name);
                list.add(new Attribute(property.getName(), property.getValue()));
            } catch (AttributeNotFoundException e) {
                // Ignore if attribute not found
            }
        }
        return new AttributeList(list);
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        Dictionary<String, Object> newConfiguration = getInstanceConfiguration();
        for (Attribute attribute : attributes.asList()) {
            newConfiguration.put(attribute.getName(), attribute.getValue());
        }
        manager.reconfigure(newConfiguration);
        return attributes;
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    public void setMBeanInfo(final MBeanInfo info) {
        this.info = info;
    }
}
