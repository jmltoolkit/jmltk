/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc;

import picocli.CommandLine;

/**
 * The entry point for the program. Initializing piccoli cli and setting costum print streams
 *
 * @author jklamroth
 * @version 1 (1.10.18)
 */
public class Main {
    public static void main(String[] args) throws Exception {
        JJBMCOptions cli = new JJBMCOptions();
        CommandLine cmd = new CommandLine(cli)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO));
        cmd.parseArgs(args);
        Operations ops = new Operations(cli);
        System.exit(ops.call());
    }
}
