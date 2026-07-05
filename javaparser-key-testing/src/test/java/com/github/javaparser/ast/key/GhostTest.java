/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.key;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 * @author Alexander Weigl
 * @version 1 (3/4/26)
 */
public class GhostTest {

    @Test
    void m() throws IOException {
        final var configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);
        var parser = new JavaParser(configuration);
        var cu = parser.parse(Paths.get("src/test/resources/Ghost.java"));
        final var result = cu.getResult().get();
        var body = ((MethodDeclaration) result.getPrimaryType().get().members().get(1))
                .getBody()
                .get();
        System.out.println(result);
    }
}
