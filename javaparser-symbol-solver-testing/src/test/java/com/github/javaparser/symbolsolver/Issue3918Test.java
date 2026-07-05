/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class Issue3918Test extends AbstractResolutionTest {

    @Test
    void test() {

        // class ancestor is defined like this
        // public class Ancestor {
        //   public static class Iterator {}
        // }

        String code = "import java.util.ArrayList;\n"
                + "import java.util.List;\n" + "\n"
                + "public class Descendant extends Ancestor {\n"
                + "    public void doAThing() {\n"
                + "        List<Object> list = new ArrayList<>();\n"
                + "        java.util.Iterator<Object> iterator = list.iterator();\n"
                + "    }\n"
                + "}";

        Path testFile = adaptPath("src/test/resources");
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(testFile));
        CompilationUnit cu =
                JavaParserAdapter.of(createParserWithResolver(typeSolver)).parse(code);

        Type type = cu.findFirst(
                        Type.class,
                        n -> n.isReferenceType()
                                && n.asReferenceType().asString().startsWith("java.util.Iterator"))
                .get();
        ResolvedType resolvedType = type.resolve();
        assertEquals("java.util.Iterator<java.lang.Object>", resolvedType.describe());
    }
}
