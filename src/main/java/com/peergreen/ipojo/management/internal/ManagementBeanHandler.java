package com.peergreen.ipojo.management.internal;

import com.peergreen.ipojo.management.Broadcaster;
import com.peergreen.ipojo.management.Description;
import com.peergreen.ipojo.management.MBean;
import com.peergreen.ipojo.management.NotificationInfo;
import com.peergreen.ipojo.management.Permission;
import com.peergreen.ipojo.management.Rights;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Property;
import org.ow2.util.substitution.IPropertyResolver;
import org.ow2.util.substitution.engine.DefaultSubstitutionEngine;
import org.ow2.util.substitution.resolver.ChainedResolver;

import javax.management.AttributeChangeNotification;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * User: guillaume
 * Date: 25/10/13
 * Time: 14:42
 */
@Handler(namespace = ManagementBeanHandler.NAMESPACE,
         name = ManagementBeanHandler.NAME,
         level = 5)
public class ManagementBeanHandler extends PrimitiveHandler {
    public static final String NAMESPACE = "com.peergreen.ipojo";
    public static final String NAME = "management";

    private List<Property> attributes = new ArrayList<>();

    private ObjectName objectName;
    private ComponentDynamicMBean mbean;

    private static final List<String> IGNORED_PROPERTIES = Arrays.asList("instance.name", "factory.name", "service.pid");

    private List<MBeanAttributeInfo> mBeanAttributeInfoList = new ArrayList<>();
    private List<MBeanNotificationInfo> mBeanNotificationInfoList = new ArrayList<>();

    @Override
    public void configure(final Element metadata, final Dictionary instance) throws ConfigurationException {

        List<Property> configuration = collectProperties();

        mbean = new ComponentDynamicMBean(getInstanceManager(), configuration);

        // 1. Compute ObjectName
        try {
            objectName = computeObjectName(configuration, instance);
        } catch (MalformedObjectNameException e) {
            throw new ConfigurationException("Cannot create ObjectName", e);
        }

        processConfigurationProperties(configuration);

        processNotificationInfo();

        Class<?> type = getInstanceManager().getClazz();
        String description = getDescription(type.getAnnotation(Description.class));
        MBeanInfo info = new MBeanInfo(
                getFactory().getClassName(),
                description,
                mBeanAttributeInfoList.toArray(new MBeanAttributeInfo[mBeanAttributeInfoList.size()]),
                null,
                null,
                mBeanNotificationInfoList.toArray(new MBeanNotificationInfo[mBeanNotificationInfoList.size()]));
        mbean.setMBeanInfo(info);

    }

    private List<Property> collectProperties() throws ConfigurationException {

        Map<String, Property> fields = new HashMap<>();
        Map<String, Property> methods = new HashMap<>();
        Map<String, Property> statics = new HashMap<>();

        ProvidedServiceHandler psh = (ProvidedServiceHandler) getHandler("org.apache.felix.ipojo:provides");
        for (ProvidedService providedService : psh.getProvidedServices()) {
            for (Property property : providedService.getProperties()) {
                if (property.hasField()) {
                    fields.put(property.getField(), property);
                } else if (property.hasMethod()) {
                    methods.put(property.getMethod(), property);
                } else {
                    // We should ignore some iPOJO auto-added properties
                    if (!IGNORED_PROPERTIES.contains(property.getName())) {
                        statics.put(property.getName(), property);
                    }
                }
            }
        }


        ConfigurationHandler handler = (ConfigurationHandler) getHandler("org.apache.felix.ipojo:properties");
        List<Property> configuration = new ArrayList<>();
        if (handler != null) {
            configuration = getPropertiesFromConfigurationHandler(handler);
        }
        for (Property property : configuration) {
            if (property.hasField()) {
                fields.put(property.getField(), property);
            } else if (property.hasMethod()) {
                methods.put(property.getMethod(), property);
            } else {
                statics.put(property.getName(), property);
            }
        }

        List<Property> collected = new ArrayList<>();
        collected.addAll(fields.values());
        collected.addAll(methods.values());
        collected.addAll(statics.values());
        return collected;

    }

    private void processNotificationInfo() throws ConfigurationException {
        Class<?> type = getInstanceManager().getClazz();

        for (Field field : type.getDeclaredFields()) {
            if (Broadcaster.class.isAssignableFrom(field.getType())) {
                final NotificationInfo info = field.getAnnotation(NotificationInfo.class);
                if (info != null) {
                    String description = getDescription(field.getAnnotation(Description.class));
                    MBeanNotificationInfo notification = new MBeanNotificationInfo(new String[] {info.value()}, "Unnamed", description);
                    mBeanNotificationInfoList.add(notification);

                    FieldMetadata metadata = getPojoMetadata().getField(field.getName(), field.getType().getName());
                    getInstanceManager().register(metadata, new BroadcasterInterceptor(new DefaultBroadcaster(info.value())));
                }
            }
        }

        for (Method method : type.getMethods()) {
            if (method.getParameterTypes().length == 1) {
                if (Broadcaster.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    final NotificationInfo info = method.getAnnotation(NotificationInfo.class);
                    if (info != null) {
                        String description = getDescription(method.getAnnotation(Description.class));
                        MBeanNotificationInfo notification = new MBeanNotificationInfo(new String[] {info.value()}, "Unnamed", description);
                        mBeanNotificationInfoList.add(notification);

                        // Initialize the setter
                        try {
                            method.invoke(getInstanceManager().getPojoObject(), new DefaultBroadcaster(info.value()));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new ConfigurationException(format("Cannot call %s.%s for Broadcaster injection", type.getName(), method.getName()));
                        }
                    }
                }
            }
        }
    }

    private void processConfigurationProperties(final List<Property> properties) throws ConfigurationException {
        Class<?> type = getInstanceManager().getClazz();

        // Compute default rights for exposed attributes
        Rights global = getRights(type.getAnnotation(Permission.class), Rights.READ_WRITE);

        for (Property property : properties) {

            // Required MBeanAttributeInfo properties
            String name = property.getName();
            String description = "";
            Rights rights = global;
            String attributeType = property.getType();

            if (property.hasField()) {
                Field field = null;
                try {
                    field = type.getDeclaredField(property.getField());
                } catch (NoSuchFieldException e) {
                    // Should never happen
                    throw new ConfigurationException(
                            format(
                                    "Supporting field %s for property %s is unknown in declared fields of class %s",
                                    property.getField(),
                                    property.getName(),
                                    type.getName()
                            )
                    );
                }

                description = getDescription(field.getAnnotation(Description.class));
                rights = getRights(field.getAnnotation(Permission.class), global);

                // If type is enum, "downcast" it to String
                if (field.getType().isEnum()) {
                    attributeType = String.class.getName();
                }

                // Register a field interceptor to be notified when the value change
                FieldMetadata fieldMetadata = getPojoMetadata().getField(property.getField(), property.getType());
                getInstanceManager().register(fieldMetadata, new AttributeValueChangedInterceptor(mbean, property));
            } else if (property.hasMethod()) {
                // Setter
                Class<?> parameterType = null;
                Method method = null;
                try {
                    parameterType = type.getClassLoader().loadClass(property.getType());
                    method = type.getMethod(property.getMethod(), parameterType);
                } catch (ClassNotFoundException e) {
                    // Should never happen
                    throw new ConfigurationException(
                            format(
                                    "Cannot load type %s of property %s from class %s",
                                    property.getType(),
                                    property.getName(),
                                    type.getName()
                            )
                    );
                } catch (NoSuchMethodException e) {
                    // Should never happen
                    throw new ConfigurationException(
                            format(
                                    "Supporting setter method %s for property %s is unknown in methods of class %s",
                                    property.getMethod(),
                                    property.getName(),
                                    type.getName()
                            )
                    );
                }

                description = getDescription(method.getAnnotation(Description.class));
                rights = getRights(method.getAnnotation(Permission.class), global);

                // If type is enum, "downcast" it to String
                if (parameterType.isEnum()) {
                    attributeType = String.class.getName();
                }

                // Register a method interceptor to be notified when the value change
                MethodMetadata methodMetadata = getPojoMetadata().getMethod(property.getMethod(), new String[]{property.getType()});
                getInstanceManager().register(methodMetadata, new AttributeValueChangedInterceptor(mbean, property));
            } // else static properties


            // Create MBean***Info
            // -------------------------
            MBeanAttributeInfo info = new MBeanAttributeInfo(
                    name,
                    attributeType,
                    description,
                    rights.isReadable(),
                    rights.isWritable(),
                    false
            );
            mBeanAttributeInfoList.add(info);

            MBeanNotificationInfo notificationInfo = new MBeanNotificationInfo(
                    new String[] {AttributeChangeNotification.ATTRIBUTE_CHANGE},
                    format("%s-attribute-notification", name),
                    description
            );
            mBeanNotificationInfoList.add(notificationInfo);

        }

    }

    private Rights getRights(final Permission permission, Rights defaults) {
        if (permission != null) {
            defaults = permission.value();
        }
        return defaults;
    }

    private String getDescription(final Description annotation) {
        String description = null;
        if (annotation != null) {
            description = annotation.value();
        }
        return description;
    }

    private Property findPropertyField(final List<Property> properties, final String name) {
        for (Property property : properties) {
            if (property.hasField() && name.equals(property.getField())) {
                return property;
            }
        }

        return null;
    }

    private Property findPropertySetter(final List<Property> properties, final String name) {
        for (Property property : properties) {
            if (property.hasMethod() && name.equals(property.getMethod())) {
                return property;
            }
        }

        return null;
    }

    private static List<Property> getPropertiesFromConfigurationHandler(final ConfigurationHandler handler) throws ConfigurationException {
        try {
            Field field = handler.getClass().getDeclaredField("m_configurableProperties");
            field.setAccessible(true);
            return (List<Property>) field.get(handler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ConfigurationException("Properties are not available", e);
        }
    }

    private ObjectName computeObjectName(final List<Property> properties, final Dictionary instance) throws MalformedObjectNameException {
        Class<?> type = getInstanceManager().getClazz();
        MBean mb = type.getAnnotation(MBean.class);
        return ObjectName.getInstance(substitute(mb.value(), properties, instance));
    }

    private String substitute(final String value, final List<Property> properties, final Dictionary instance) {
        DefaultSubstitutionEngine engine = new DefaultSubstitutionEngine();
        ChainedResolver resolver = new ChainedResolver();
        resolver.getResolvers().add(new ConfigurationPropertyResolver(properties));
        resolver.getResolvers().add(new InstanceConfigurationPropertyResolver(instance));
        resolver.getResolvers().add(new FactoryPropertyResolver(getFactory()));
        engine.setResolver(resolver);
        return engine.substitute(value);
    }

    @Override
    public void stop() {
        try {
            getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            // Ignore
        }
    }

    private MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public void start() {
        try {
            getPlatformMBeanServer().registerMBean(mbean, objectName);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            error("MBean registration failed", e);
            setValidity(false);
        }
    }

    /**
     * Simplistic PropertyResolver
     */
    private static class ConfigurationPropertyResolver implements IPropertyResolver {
        private final List<Property> properties;

        public ConfigurationPropertyResolver(final List<Property> properties) {
            this.properties = properties;
        }

        @Override
        public String resolve(final String expression) {
            for (Property property : properties) {
                if (expression.equals(property.getName())) {
                    Object value = property.getValue();
                    if (value != null) {
                        return value.toString();
                    }
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Simplistic PropertyResolver
     */
    private static class InstanceConfigurationPropertyResolver implements IPropertyResolver {
        private final Dictionary configuration;

        public InstanceConfigurationPropertyResolver(final Dictionary configuration) {
            this.configuration = configuration;
        }

        @Override
        public String resolve(final String expression) {
            for (Object key : Collections.list(configuration.keys())) {
                if (expression.equals(key.toString())) {
                    Object value = configuration.get(key);
                    if (value != null) {
                        return value.toString();
                    }
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Simplistic PropertyResolver
     */
    private static class FactoryPropertyResolver implements IPropertyResolver {
        private final ComponentFactory factory;

        public FactoryPropertyResolver(final ComponentFactory factory) {
            this.factory = factory;
        }

        @Override
        public String resolve(final String expression) {
            if ("factory.name".equals(expression)) {
                return factory.getName();
            }
            if ("factory.classname".equals(expression)) {
                return factory.getClassName();
            }
            if ("factory.version".equals(expression)) {
                return factory.getVersion();
            }
            return null;
        }
    }

    private static class BroadcasterInterceptor implements FieldInterceptor {
        private final Broadcaster broadcaster;

        public BroadcasterInterceptor(final Broadcaster broadcaster) {
            this.broadcaster = broadcaster;
        }

        @Override
        public void onSet(final Object pojo, final String fieldName, final Object value) {

        }

        @Override
        public Object onGet(final Object pojo, final String fieldName, final Object value) {
            return broadcaster;
        }
    }

    private class DefaultBroadcaster implements Broadcaster {

        private final String type;
        private long sequence;

        public DefaultBroadcaster(final String type) {
            this.type = type;
            sequence = 0;
        }

        @Override
        public void broadcast(final String message) {
            Notification n = new Notification(type, mbean, sequence++, message);
            mbean.sendNotification(n);
        }

        @Override
        public void broadcast(final Object userdata) {
            Notification n = new Notification(type, mbean, sequence++);
            n.setUserData(userdata);
            mbean.sendNotification(n);
        }

        @Override
        public void broadcast(final String message, final Object userdata) {
            Notification n = new Notification(type, mbean, sequence++, message);
            n.setUserData(userdata);
            mbean.sendNotification(n);
        }
    }
}
