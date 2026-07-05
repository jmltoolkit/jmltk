/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import java.io.InputStream;

public class TestUtils {

    public static InputStream getSampleStream(String sampleName) {
        InputStream is = TestUtils.class
                .getClassLoader()
                .getResourceAsStream("com/github/javaparser/samples/" + sampleName + ".java");
        if (is == null) {
            throw new RuntimeException("Example not found, check your test. Sample name: " + sampleName);
        }
        return is;
    }
}
