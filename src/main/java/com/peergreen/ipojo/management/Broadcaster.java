package com.peergreen.ipojo.management;

/**
 * User: guillaume
 * Date: 29/10/13
 * Time: 14:36
 */
public interface Broadcaster {
    void broadcast(String message);
    void broadcast(Object userdata);
    void broadcast(String message, Object userdata);
}
