/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Issue3045Test extends AbstractResolutionTest {

    @Test
    void createAnonymousClassWithUnsolvableParent() {
        String sourceCode = "import com.google.common.base.Function;\n" + "public class A {\n"
                + "    private static final Function<Object, Object> MAP = new Function<Object, Object>() {\n"
                + "        @Override\n"
                + "        public Object apply(Object input) {\n"
                + "            return null;\n"
                + "        }\n"
                + "    };\n"
                + "}";

        // Create the parser
        JavaParser parser = createParserWithResolver(defaultTypeSolver());

        // Get the method return type that is declared inside of anonymous class
        Optional<Type> methodType = parser.parse(sourceCode)
                .getResult()
                .flatMap(cu -> cu.findFirst(MethodDeclaration.class))
                .map(MethodDeclaration::getType);
        assertTrue(methodType.isPresent());

        // Try to resolve the given type and expect an unsolved exception
        assertThrows(UnsolvedSymbolException.class, methodType.get()::resolve);
    }
}
