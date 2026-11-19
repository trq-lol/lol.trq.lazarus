/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import lol.trq.lazarus.agent.LazarusAgent;

/**
 * Replacement bodies for Forge methods that relied on JDK internals. Each hook receives the patched method's
 * declaring class first, then the method's own parameters.
 */
public final class ForgeHooks {
    private ForgeHooks() {}

    /**
     * Replaces {@code ObjectHolderRef.makeWritable(Field)} and {@code ItemStackHolderRef.makeWritable(Field)}.
     *
     * @param owner the holder-ref class
     * @param field the field about to be written, unused; every write goes through the stand-in accessor
     */
    public static void makeWritable(Class<?> owner, Field field) {
        LegacyReflection.install(owner);
    }

    /**
     * Replaces {@code EnumHelper.setup()}.
     *
     * @param owner {@code EnumHelper}
     */
    public static void setupEnumHelper(Class<?> owner) {
        LegacyReflection.install(owner);
    }

    /**
     * Replaces {@code EnumHelper.setFailsafeFieldValue(Field, Object, Object)}.
     *
     * @param owner {@code EnumHelper}
     * @param field the field
     * @param target the instance, or {@code null} for a static field
     * @param value the value
     */
    public static void setFailsafeFieldValue(Class<?> owner, Field field, Object target, Object value) {
        Fields.set(field, target, value);
    }

    /**
     * Replaces {@code System.setSecurityManager} in FML. Java 18 turned it off by default and Java 24 removed it;
     * FML installs one only to stop mods from calling {@code System.exit}, which its exit transformer already does.
     *
     * @param manager the security manager FML wanted to install, ignored
     */
    @SuppressWarnings("removal")
    public static void setSecurityManager(SecurityManager manager) {}

    /**
     * Replaces {@link Method#invoke} in FML's coremod manager. FML adds tweaker jars to the system class loader by
     * calling {@code URLClassLoader.addURL} on it reflectively; on Java 9 and newer that loader is not a {@code
     * URLClassLoader}, so the jar is appended through the agent instead. Every other call is passed through.
     *
     * @param method the method FML invokes
     * @param target the receiver
     * @param args the arguments
     * @return the method's result
     * @throws IllegalAccessException as {@link Method#invoke} does
     * @throws InvocationTargetException as {@link Method#invoke} does
     */
    public static Object invoke(Method method, Object target, Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        if (method.getDeclaringClass() == URLClassLoader.class
                && method.getName().equals("addURL")
                && !(target instanceof URLClassLoader)
                && target == ClassLoader.getSystemClassLoader()) {
            try {
                LazarusAgent.appendToSystemClassPath(new File(((URL) args[0]).toURI()));
            } catch (IOException | URISyntaxException e) {
                throw new InvocationTargetException(e);
            }
            return null;
        }
        return method.invoke(target, args);
    }
}
