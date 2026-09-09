/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc;

public class ErrorLogger {
    private static final long START_TIME = System.currentTimeMillis();
    public static final String RESET = "\033[0m"; // Text Reset
    public static final String RED_BOLD = "\033[1;31m"; // RED
    public static final String GREEN_BOLD = "\033[1;32m"; // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW

    private static boolean debugOn = false;

    private static void print(String fmt, Level level, Object... args) {
        if (level == Level.DEBUG && !debugOn) return;

        System.out.printf("%05d \033[1;%dm[%s]\033[0m ", System.currentTimeMillis() - START_TIME, 32, level);
        System.out.printf(fmt, args);
        System.out.println();
    }

    public static void debug(String fmt, Object... args) {
        print(fmt, Level.DEBUG, args);
    }

    public static void debug(Throwable obj) {
        print("%s", Level.DEBUG, obj);
    }

    public static void info(Object fmt, Object... args) {
        print(fmt.toString(), Level.INFO, args);
    }

    public static void fatal(String fmt, Object... args) {
        print(fmt, Level.FATAL, args);
    }

    public static void error(String fmt, Object... args) {
        print(fmt, Level.ERROR, args);
    }

    public static void warn(String fmt, Object... args) {
        print(fmt, Level.WARN, args);
    }

    public static void setDebugOn() {
        debugOn = true;
    }
}
