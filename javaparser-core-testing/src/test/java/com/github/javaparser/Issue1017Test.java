/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Issue1017Test {

    @Test()
    void test() {
        String code = "class X{int x(){ a(1+1 -> 10);}}";

        Assertions.assertThrows(ParseProblemException.class, () -> {
            CompilationUnit cu = StaticJavaParser.parse(code);
        });
    }
}
