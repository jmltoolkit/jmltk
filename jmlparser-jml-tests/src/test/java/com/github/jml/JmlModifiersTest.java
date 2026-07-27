/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JML modifiers.
 * Covers: spec_public, spec_private, pure, strictly_pure, model, ghost, helper,
 * nullable_by_default, non_null, nullable, instance, no_state, two_state,
 * code, code_bigint_math, code_java_math, code_safe_math,
 * peer, rep, read_only, immutable.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlModifiersTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Visibility Modifiers ====================
    @Test
    void testSpecPublic() {
        String code = """
            public class Example {
                /*@ spec_public @*/ int x;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSpecPrivate() {
        String code = """
            public class Example {
                /*@ spec_private @*/ int x;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSpecPackage() {
        String code = """
            public class Example {
                /*@ spec_package @*/ int x;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSpecProtected() {
        String code = """
            public class Example {
                /*@ spec_protected @*/ int x;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Purity Modifiers ====================
    @Test
    void testPureMethod() {
        String code = """
            public class Example {
                /*@ pure @*/ int getValue() { return 42; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testStrictlyPureMethod() {
        String code = """
            public class Example {
                /*@ strictly_pure @*/ int compute(int x) { return x * 2; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Specification Modifiers ====================
    @Test
    void testModelField() {
        String code = """
            public class Example {
                /*@ model @*/ int abstractValue;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostField() {
        String code = """
            public class Example {
                /*@ ghost @*/ int historyCount;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testHelperMethod() {
        String code = """
            public class Example {
                /*@ helper @*/ static int helperMethod(int x) { return x + 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Nullability Modifiers ====================
    @Test
    void testNullableByDefault() {
        String code = """
            /*@ nullable_by_default @*/
            public class Example {
                String field; // implicitly nullable
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNonNullByDefault() {
        String code = """
            /*@ non_null_by_default @*/
            public class Example {
                String field; // implicitly non-null
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNonNullField() {
        String code = """
            public class Example {
                /*@ non_null @*/ String value;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNullableField() {
        String code = """
            public class Example {
                /*@ nullable @*/ String optionalValue;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNonNullElements() {
        String code = """
            public class Example {
                /*@ nonnullelements @*/ String[] values;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== State Modifiers ====================
    @Test
    void testInstanceModifier() {
        String code = """
            public class Example {
                /*@ instance @*/ model int instanceValue;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNoStateModifier() {
        String code = """
            public class Example {
                /*@ no_state @*/ static int compute(int x) { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testTwoStateModifier() {
        String code = """
            public class Example {
                int oldVal;
                /*@ two_state @*/ boolean hasChanged(int current) { return current != oldVal; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Code Modifiers ====================
    @Test
    void testCodeModifier() {
        String code = """
            public class Example {
                /*@ code @*/ void implementation() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testCodeBigIntMath() {
        String code = """
            public class Example {
                /*@ code_bigint_math @*/ void compute() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testCodeJavaMath() {
        String code = """
            public class Example {
                /*@ code_java_math @*/ void compute() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testCodeSafeMath() {
        String code = """
            public class Example {
                /*@ code_safe_math @*/ void compute() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Ownership Modifiers ====================
    @Test
    void testPeerModifier() {
        String code = """
            public class Example {
                /*@ peer @*/ Example partner;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRepModifier() {
        String code = """
            public class Example {
                /*@ rep @*/ Object[] representation;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testReadOnlyModifier() {
        String code = """
            public class Example {
                /*@ read_only @*/ Object data;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testImmutableModifier() {
        String code = """
            public class Example {
                /*@ immutable @*/ Object frozen;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Modifiers ====================
    @Test
    void testCombinedModifiers() {
        String code = """
            public class Example {
                /*@ spec_public ghost @*/ int counter;
                /*@ pure model @*/ int abstractSize;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}