package com.peergreen.ipojo.management;

/**
 * User: guillaume
 * Date: 25/10/13
 * Time: 14:21
 */
public enum Rights {
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true);

    private final boolean readable;
    private final boolean writable;

    Rights(final boolean readable, final boolean writable) {
        this.readable = readable;
        this.writable = writable;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }
}
