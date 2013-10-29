package com.peergreen.ipojo.management.test;

import com.peergreen.ipojo.management.Description;
import com.peergreen.ipojo.management.MBean;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;

/**
 * User: guillaume
 * Date: 26/10/13
 * Time: 14:09
 */
@Component
@Instantiate
@MBean("peergreen:type=Toto,name=${model.name}")
@Description("This MBean is ...")
public class Model {

    @Property(name = "model.name", value = "toto")
    @Description("blah blah ...")
    private String name;

    @Property(name = "model.max")
    private int max;
}
