/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/** OpenGL context attributes, as in LWJGL 2. Immutable; each {@code with} returns a copy. */
public final class ContextAttribs {
    final int major;
    final int minor;
    final boolean debug;
    final boolean core;
    final boolean forwardCompatible;

    /** Creates attributes for a default (legacy-compatible) context. */
    public ContextAttribs() {
        this(1, 0, false, false, false);
    }

    /**
     * Creates attributes requesting a version.
     *
     * @param major major version
     * @param minor minor version
     */
    public ContextAttribs(int major, int minor) {
        this(major, minor, false, false, false);
    }

    private ContextAttribs(int major, int minor, boolean debug, boolean core, boolean forwardCompatible) {
        this.major = major;
        this.minor = minor;
        this.debug = debug;
        this.core = core;
        this.forwardCompatible = forwardCompatible;
    }

    /**
     * {@return a copy that requests a debug context}
     *
     * @param debug whether to request one
     */
    public ContextAttribs withDebug(boolean debug) {
        return new ContextAttribs(major, minor, debug, core, forwardCompatible);
    }

    /**
     * {@return a copy that requests the core profile}
     *
     * @param core whether to request it
     */
    public ContextAttribs withProfileCore(boolean core) {
        return new ContextAttribs(major, minor, debug, core, forwardCompatible);
    }

    /**
     * {@return a copy that requests a forward-compatible context}
     *
     * @param forwardCompatible whether to request one
     */
    public ContextAttribs withForwardCompatible(boolean forwardCompatible) {
        return new ContextAttribs(major, minor, debug, core, forwardCompatible);
    }

    /** {@return the requested major version} */
    public int getMajorVersion() {
        return major;
    }

    /** {@return the requested minor version} */
    public int getMinorVersion() {
        return minor;
    }

    /** {@return whether a debug context is requested} */
    public boolean isDebug() {
        return debug;
    }
}
