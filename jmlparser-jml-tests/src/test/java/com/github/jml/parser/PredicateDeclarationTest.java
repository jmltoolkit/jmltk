/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.jml.body.JmlPredicateDeclaration;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the JML class-body {@code predicate} declaration
 * ({@code //@ instance predicate Pred(int i) = i > 0;}).
 *
 * @author jmltoolkit
 */
class PredicateDeclarationTest {

    private JavaParser parser;

    @BeforeEach
    void setUp() {
        ParserConfiguration config = new ParserConfiguration();
        config.setProcessJml(true);
        parser = new JavaParser(config);
    }

    private CompilationUnit parse(String source) {
        var result = parser.parse(source);
        result.getProblems().forEach(System.out::println);
        assertThat(result.isSuccessful())
                .as(
                        "parsing failed: %s",
                        result.getProblems().stream().map(p -> p.getMessage()).collect(Collectors.joining("; ")))
                .isTrue();
        return result.getResult().get();
    }

    @Test
    void parseInstanceLemma() {
        var cu = parse("""
                public class P {
                    //@ instance predicate Pred(int i) = i > 0;
                }
                """);
        var predicate = cu.findAll(JmlPredicateDeclaration.class).get(0);
        assertThat(predicate.getNameAsString()).isEqualTo("Pred");
        assertThat(predicate.getModifiers())
                .extracting(m -> m.getKeyword().asString())
                .containsExactly("instance");
        assertThat(predicate.getParameters()).hasSize(1);
        Parameter parameter = predicate.getParameters().get(0);
        assertThat(parameter.getType().asString()).isEqualTo("int");
        assertThat(parameter.getNameAsString()).isEqualTo("i");
        assertThat(predicate.getBody()).isInstanceOf(BinaryExpr.class);
        assertThat(predicate.getBody().toString()).isEqualTo("i > 0");
        assertThat(predicate.hasModifier(Modifier.DefaultKeyword.JML_INSTANCE)).isTrue();
    }

    @Test
    void toStringRoundTrip() {
        var cu = parse("""
                public class P {
                    //@ instance predicate Pred(int i) = i > 0;
                }
                """);
        var predicate = cu.findAll(JmlPredicateDeclaration.class).get(0);
        assertThat(predicate.toString()).isEqualTo("//@ instance predicate Pred(int i) = i > 0;\n");
    }

    @Test
    void lexicallyPreservingRoundTrip() {
        String source = """
                public class P {
                    //@ instance predicate Pred(int i) = i > 0;
                    //@ predicate Even(int n) = (n % 2 == 0);
                }
                """;
        var cu = parse(source);
        LexicalPreservingPrinter.setup(cu);
        assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(source);
    }

    @Test
    void concreteSyntaxModelRoundTripWithoutModifiers() {
        var cu = parse("""
                public class P {
                    //@ predicate StrictlyPositive(int i) = i > 0;
                }
                """);
        var predicate = cu.findAll(JmlPredicateDeclaration.class).get(0);
        assertThat(ConcreteSyntaxModel.genericPrettyPrint(predicate))
                .isEqualTo("predicate StrictlyPositive(int i) = i > 0;");
    }

    @Test
    void bodyCanReferenceParameters() {
        var cu = parse("""
                public class P {
                    //@ predicate Between(int lo, int hi, int v) = lo <= v && v <= hi;
                }
                """);
        var predicate = cu.findAll(JmlPredicateDeclaration.class).get(0);
        assertThat(predicate.getParameters()).hasSize(3);
        // the left-most leaf of the conjunction references the first parameter
        var node = predicate.getBody();
        while (node instanceof BinaryExpr be) {
            node = be.getLeft();
        }
        assertThat(node).isInstanceOf(NameExpr.class);
        assertThat(node.toString()).isEqualTo("lo");
    }

    @Test
    void duplicateParametersAreReported() {
        var result = parser.parse("""
                public class P {
                    //@ predicate Bad(int i, int i) = i > 0;
                }
                """);
        assertThat(result.getProblems()).anyMatch(p -> p.getMessage().contains("already defined"));
    }

    @Test
    void disallowedModifierIsReported() {
        var result = parser.parse("""
                public class P {
                    //@ synchronized predicate Bad() = true;
                }
                """);
        assertThat(result.getProblems()).anyMatch(p -> p.getMessage().contains("'synchronized' is not allowed here."));
    }
}
