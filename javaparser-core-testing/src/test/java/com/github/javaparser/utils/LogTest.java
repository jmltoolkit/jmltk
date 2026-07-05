/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogTest {
    private static class TestAdapter implements Log.Adapter {
        String output = "";

        @Override
        public void info(Supplier<String> messageSupplier) {
            output += "I" + messageSupplier.get();
        }

        @Override
        public void trace(Supplier<String> messageSupplier) {
            output += "T" + messageSupplier.get();
        }

        @Override
        public void error(Supplier<Throwable> throwableSupplier, Supplier<String> messageSupplier) {
            Throwable throwable = throwableSupplier.get();
            String s = messageSupplier.get();
            output += "E" + s + "M" + (throwable == null ? "null" : throwable.getMessage());
        }
    }

    private final TestAdapter testAdapter = new TestAdapter();

    @BeforeEach
    void setAdapter() {
        Log.setAdapter(testAdapter);
    }

    @AfterEach
    void resetAdapter() {
        Log.setAdapter(new Log.SilentAdapter());
    }

    @Test
    void testTrace() {
        Log.trace("abc");
        Log.trace("a%sc%s", () -> "b", () -> "d");
        assertEquals("TabcTabcd", testAdapter.output);
    }

    @Test
    void testInfo() {
        Log.info("abc");
        Log.info("a%sc", () -> "b");
        assertEquals("IabcIabc", testAdapter.output);
    }

    @Test
    void testError() {
        Log.error("abc");
        Log.error("a%sc", () -> "b");
        Log.error(new Throwable("!!!"), "abc");
        Log.error(new Throwable("!!!"), "a%sc%s", () -> "b", () -> "d");
        Log.error(new Throwable("!!!"));
        assertEquals("EabcMnullEabcMnullEabcM!!!EabcdM!!!EnullM!!!", testAdapter.output);
    }
}
