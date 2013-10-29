package com.peergreen.ipojo.management.internal.test;

import com.peergreen.ipojo.management.Broadcaster;
import com.peergreen.ipojo.management.Description;
import com.peergreen.ipojo.management.MBean;
import com.peergreen.ipojo.management.NotificationInfo;
import com.peergreen.ipojo.management.Permission;
import com.peergreen.ipojo.management.Rights;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import java.util.Date;

import static java.lang.Thread.sleep;

/**
 * The instance will have an associated DynamicMBean that will exposes all of the configuration properties through JMX.
 * By default, attributes are READ_WRITE, that may be overidden with the @Permission annotation on the type.
 *
 * @Description provides informative message about the attribute/mbean/notification
 */
@Component
@Instantiate
@Provides(properties = @StaticServiceProperty(name = "static-property", value = "I'm static", type = "java.lang.String"))
@MBean("pg:type=Coucou,name=${string.a}")
@Description("This is a test component")
public class TestComponent implements Tutut {

    @ServiceProperty(value = "pouet")
    private String tutut;

    @Property(name = "string.a", value = "toto")
    @Description("And this is a string")
    private String stringAttribute;

    @Property(value = "READ")
    @Description("And this is an Enum")
    private Rights rights;

    private String bidule;
    private boolean run = true;

    @NotificationInfo("toto")
    private Broadcaster broadcaster;

    private Broadcaster broadcaster2;

    @Property(value = "bidule")
    @Permission(Rights.WRITE)
    @Description("A method setter")
    public void setBidule(String bidule) {
        this.bidule = bidule;
    }

    public String getStringAttribute() {
        return stringAttribute;
    }

    public Rights getRights() {
        return rights;
    }

    public String getBidule() {
        return bidule;
    }

    @Validate
    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (run) {
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        run = false;
                    }
                    System.out.printf("stringAttribute -> %s%n", stringAttribute);
                    System.out.printf("permission -> %s%n", rights);
                    System.out.printf("bidule -> %s%n", bidule);

                    broadcaster.broadcast("Hello " + new Date());
                    broadcaster2.broadcast("Hello " + new Date());
                }
            }
        }).start();

    }

    @Invalidate
    public void stop() {
        run = false;
    }


    @NotificationInfo("aaaaaaaaaaa")
    @Description("This is a broadcaster for ..")
    public void setBroadcaster2(final Broadcaster broadcaster2) {
        this.broadcaster2 = broadcaster2;
    }
}
