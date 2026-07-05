/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.javaparser.StaticJavaParser.parseResource;

class GeneratedJavaParserTokenManagerTest {
    private String makeFilename(String sampleName) {
        return "com/github/javaparser/issue_samples/" + sampleName + ".java.txt";
    }

    @Test
    void issue1003() throws IOException {
        parseResource(makeFilename("issue1003"));
    }
}
