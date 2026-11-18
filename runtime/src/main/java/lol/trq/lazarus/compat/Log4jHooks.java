/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Log4Shell mitigations for the Log4j 2.0-beta9 that Minecraft 1.8.9 ships.
 *
 * <p>Log4j 2.0-beta9 through 2.14.1 expand {@code ${...}} lookups inside logged messages, so any text that reaches a
 * log line (a chat message from a server, a player name) could trigger a JNDI request (CVE-2021-44228) or a recursive
 * lookup that crashes the client (CVE-2021-45105). Log4j 2.15+ stopped interpolating messages; these hooks do the same.
 */
public final class Log4jHooks {
    private Log4jHooks() {}

    /**
     * Replaces {@code StrSubstitutor.replace(LogEvent, String)} in {@code MessagePatternConverter}: the message is
     * written as is, never interpolated.
     *
     * @param substitutor the substitutor, unused
     * @param event the event, unused
     * @param message the formatted message
     * @return {@code message}, unchanged
     */
    public static String messageWithoutLookups(StrSubstitutor substitutor, LogEvent event, String message) {
        return message;
    }
}
