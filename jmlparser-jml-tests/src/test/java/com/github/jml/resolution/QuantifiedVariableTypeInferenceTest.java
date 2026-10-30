/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.resolution;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedVariable;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type of a quantified binding with an iterable domain is the domain's
 * element type: a range {@code 0..n} yields {@code int}, an array the component
 * type, and an {@code Iterable}/{@code SequencedCollection} its first type
 * argument. This is what makes {@code (\forall var e : a; ...)} resolvable.
 *
 * @author Alexander Weigl
 */
class QuantifiedVariableTypeInferenceTest {
    private final TypeSolver typeSolver = new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader());

    private CompilationUnit parse(String source) {
        ParserConfiguration config =
                new ParserConfiguration().setProcessJml(true).setSymbolResolver(new JavaSymbolSolver(typeSolver));
        ParseResult<CompilationUnit> result = new JavaParser(config).parse(source);
        assertThat(result.getProblems()).as("parse problems").isEmpty();
        return result.getResult().get();
    }

    @Test
    void infersElementTypesFromIterableDomains() {
        CompilationUnit cu = parse("import java.util.List;\n"
                + "import java.util.SequencedCollection;\n"
                + "class Domains {\n"
                + "    int[] a;\n"
                + "    int[][] m;\n"
                + "    List<String> words;\n"
                + "    SequencedCollection<String> seq;\n"
                + "    //@ invariant (\\forall int e : a; e > 0);\n"
                + "    //@ invariant (\\forall int[] row : m; row.length > 0);\n"
                + "    //@ invariant (\\forall String s : words; s.length() > 0);\n"
                + "    //@ invariant (\\forall String q : seq; q.length() > 0);\n"
                + "    //@ invariant (\\forall var v : a; v > 1);\n"
                + "    //@ invariant (\\forall int i : 0..a.length; a[i] > 0);\n"
                + "}\n");

        Map<String, String> expected = new HashMap<>();
        expected.put("e", "int");
        expected.put("row", "int[]");
        expected.put("s", "java.lang.String");
        expected.put("q", "java.lang.String");
        expected.put("v", "int");
        expected.put("i", "int");

        assertThat(cu.findAll(JmlQuantifiedVariable.class)).hasSize(expected.size());
        for (JmlQuantifiedVariable jqv : cu.findAll(JmlQuantifiedVariable.class)) {
            String name = jqv.getName().asString();
            String actual = JavaParserSymbolDeclaration.quantifiedVar(jqv, typeSolver)
                    .getType()
                    .describe();
            assertThat(actual).as("inferred type of %s", name).isEqualTo(expected.get(name));
        }
    }
}
