/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.WhileStmt
import io.github.jmltoolkit.vcg.ir.NfArray
import io.github.jmltoolkit.vcg.ir.NfAssert
import io.github.jmltoolkit.vcg.ir.NfAssign
import io.github.jmltoolkit.vcg.ir.NfAssume
import io.github.jmltoolkit.vcg.ir.NfBreak
import io.github.jmltoolkit.vcg.ir.NfCall
import io.github.jmltoolkit.vcg.ir.NfContinue
import io.github.jmltoolkit.vcg.ir.NfField
import io.github.jmltoolkit.vcg.ir.NfHavoc
import io.github.jmltoolkit.vcg.ir.NfIf
import io.github.jmltoolkit.vcg.ir.NfLocal
import io.github.jmltoolkit.vcg.ir.NfLocation
import io.github.jmltoolkit.vcg.ir.NfLoop
import io.github.jmltoolkit.vcg.ir.NfReturn
import io.github.jmltoolkit.vcg.ir.NfStmt
import io.github.jmltoolkit.vcg.ir.NfSwitch
import io.github.jmltoolkit.vcg.ir.NfThrow
import io.github.jmltoolkit.vcg.ir.NfTryCatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Unit tests for [Normalizer], the first stage of the VCG pipeline (Java/JML source
 *  -> normal-form IR).
 *
 * The parameterized [testNormalizationCase] table covers every statement kind the
 * normalizer supports (assignments with all compound operators, declarations, calls,
 * blocks, if/else, all four loop forms incl. desugared `for`/`foreach`/`do`, switch,
 * try/catch/finally, JML assert/assume/set, return/throw, break/continue). The
 * dedicated tests cover the loop/switch break targets, origin (provenance)
 * tracking, the `key(...)` location mapping and the explicitly unsupported
 * statement kinds that must raise [UnsupportedOperationException].
 *
 * The dumps mask loop/switch ids (`break()`, `continue()`, `switch(...)`) because
 * the ids are deep structural hashes of the parsed nodes; the exact id semantics
 * are asserted separately in the dedicated tests.
 */
@Timeout(120)
class NormalizerTest {

    private val parser: JavaParser = JavaParser(ParserConfiguration().setProcessJml(true))

    //region helpers

    /** Parses `source` as the body of a method `void m()`; JML processing active. */
    private fun blockOf(source: String): BlockStmt {
        val cu = parser.parse("class C { void m() {\n$source\n} }")
        assertTrue(cu.isSuccessful, cu.problems.toString())
        return cu.result.get().types[0].asClassOrInterfaceDeclaration().methods[0].body.get()
    }

    /** Normalizes the given source snippet to the normal-form IR. */
    private fun normalize(source: String): List<NfStmt> = Normalizer().normalize(blockOf(source))

    /** Canonical, id-masked dump of the normalized IR. */
    private fun dump(source: String): String = normalize(source).joinToString(";\n") { it.dump() }

    private fun NfStmt.dump(): String = when (this) {
        is NfAssign -> "assign(${target.dump()}, $rhs)"
        is NfCall -> "call($call)"
        is NfIf -> "if($cond, [${thenStmts.dumpList()}], [${elseStmts.dumpList()}])"
        is NfLoop -> "loop($cond, [${body.dumpList()}])"
        is NfReturn -> if (value != null) "return($value)" else "return()"
        is NfThrow -> "throw($exception)"
        is NfBreak -> "break()"
        is NfContinue -> "continue()"
        is NfAssume -> "assume($expr)"
        is NfAssert -> "assert($expr)"
        is NfHavoc -> "havoc(${location.dump()})"
        is NfTryCatch -> "try([${tryBody.dumpList()}], [${catches.joinToString("; ") { "catch(${it.type}, ${it.parameter}, [${it.body.dumpList()}])" }}], [${finallyBody.dumpList()}])"
        is NfSwitch -> "switch($selector, [${cases.joinToString("; ") { "case(${it.labels.joinToString("|")}, [${it.body.dumpList()}])" }}])"
    }

    private fun List<NfStmt>.dumpList(): String = joinToString("; ") { it.dump() }

    private fun NfLocation.dump(): String = when (this) {
        is NfLocal -> "local:$key${if (declaredType != null) ":${declaredType}" else ""}"
        is NfField -> "field($receiver, $key)"
        is NfArray -> "array($key, $index)"
    }

    /** Recursively visits every statement in the IR. */
    private fun List<NfStmt>.forEachStmt(f: (NfStmt) -> Unit) {
        for (s in this) {
            f(s)
            children(s).forEachStmt(f)
        }
    }

    private fun children(s: NfStmt): List<NfStmt> = when (s) {
        is NfIf -> s.thenStmts + s.elseStmts
        is NfLoop -> s.body
        is NfTryCatch -> s.tryBody + s.catches.flatMap { it.body } + s.finallyBody
        is NfSwitch -> s.cases.flatMap { it.body }
        else -> emptyList()
    }

    private fun List<NfStmt>.firstOfType(kind: (NfStmt) -> Boolean): NfStmt {
        val res = mutableListOf<NfStmt>()
        forEachStmt { if (kind(it)) res.add(it) }
        assertTrue(res.isNotEmpty(), "no matching IR statement found in $this")
        return res[0]
    }
    //endregion

    //region parameterized normalization table (>100 cases)
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("cases")
    fun testNormalizationCase(id: String, source: String, expected: String) {
        val actual = dump(source)
        assertEquals(expected, actual, "case '$id' mismatch")
    }

    companion object {
        @JvmStatic
        fun cases(): Stream<Arguments> = Stream.of(
            // ---------------------------------------------------------------- assignments
            Arguments.of("assign simple", "x = 1;", "assign(local:x, 1)"),
            Arguments.of("assign variable", "x = y;", "assign(local:x, y)"),
            Arguments.of("assign plus", "x += 1;", "assign(local:x, x + 1)"),
            Arguments.of("assign minus", "x -= 1;", "assign(local:x, x - 1)"),
            Arguments.of("assign multiply", "x *= 2;", "assign(local:x, x * 2)"),
            Arguments.of("assign divide", "x /= 2;", "assign(local:x, x / 2)"),
            Arguments.of("assign remainder", "x %= 2;", "assign(local:x, x % 2)"),
            Arguments.of("assign bitand", "x &= 1;", "assign(local:x, x & 1)"),
            Arguments.of("assign bitor", "x |= 1;", "assign(local:x, x | 1)"),
            Arguments.of("assign xor", "x ^= 1;", "assign(local:x, x ^ 1)"),
            Arguments.of("assign shiftleft", "x <<= 1;", "assign(local:x, x << 1)"),
            Arguments.of("assign shiftright", "x >>= 1;", "assign(local:x, x >> 1)"),
            Arguments.of("assign unsigned shiftright", "x >>>= 1;", "assign(local:x, x >>> 1)"),
            Arguments.of("assign complex rhs", "x = a + b * c;", "assign(local:x, a + b * c)"),
            Arguments.of("assign ternary rhs", "x = c ? 1 : 2;", "assign(local:x, c ? 1 : 2)"),
            Arguments.of("assign chained", "x = y = z;", "assign(local:x, y = z)"),
            Arguments.of("postfix increment", "x++;", "assign(local:x, x + 1)"),
            Arguments.of("postfix decrement", "x--;", "assign(local:x, x - 1)"),
            Arguments.of("prefix increment", "++x;", "assign(local:x, x + 1)"),
            Arguments.of("prefix decrement", "--x;", "assign(local:x, x - 1)"),
            Arguments.of("this field assign", "this.f = 1;", "assign(field(this, this.f), 1)"),
            Arguments.of("receiver field assign", "o.f = 1;", "assign(field(o, o.f), 1)"),
            Arguments.of("this field compound", "this.f += 1;", "assign(field(this, this.f), this.f + 1)"),
            Arguments.of("receiver field compound", "o.f += 1;", "assign(field(o, o.f), o.f + 1)"),
            Arguments.of("this field increment", "this.counter++;", "assign(field(this, this.counter), this.counter + 1)"),
            Arguments.of("receiver field predecrement", "--o.f;", "assign(field(o, o.f), o.f - 1)"),
            Arguments.of("array assign", "a[i] = 1;", "assign(array(a, i), 1)"),
            Arguments.of("array compound", "a[0] += 1;", "assign(array(a, 0), a[0] + 1)"),
            Arguments.of("array index expression", "a[i + 1] = 2;", "assign(array(a, i + 1), 2)"),
            Arguments.of("array of this field", "this.a[0] = 1;", "assign(array(this.a, 0), 1)"),
            Arguments.of("array of receiver field", "b.a[i] = 1;", "assign(array(b.a, i), 1)"),
            Arguments.of("two-dimensional array", "a[i][j] = 1;", "assign(array(a[i], j), 1)"),
            Arguments.of("array postfix increment", "a[0]++;", "assign(array(a, 0), a[0] + 1)"),
            Arguments.of("array prefix decrement", "--a[i];", "assign(array(a, i), a[i] - 1)"),
            Arguments.of("array of method result", "f()[0] = 1;", "assign(array(f(), 0), 1)"),
            Arguments.of("cast rhs", "double d = (double) x;", "assign(local:d:double, (double) x)"),
            Arguments.of("call rhs", "int x = obj.toString();", "assign(local:x:int, obj.toString())"),

            // ---------------------------------------------------------------- declarations
            Arguments.of("decl with init", "int x = 1;", "assign(local:x:int, 1)"),
            Arguments.of("decl without init", "int x;", "havoc(local:x:int)"),
            Arguments.of("decl multi declarator", "int x = 1, y = 2;", "assign(local:x:int, 1);\nassign(local:y:int, 2)"),
            Arguments.of("decl mixed init", "int x = 1, y;", "assign(local:x:int, 1);\nhavoc(local:y:int)"),
            Arguments.of("decl array initializer", "int[] a = null;", "assign(local:a:int[], null)"),
            Arguments.of("decl new array", "int a = new int[5];", "assign(local:a:int, new int[5])"),
            Arguments.of("decl new object", "Foo f = new Foo();", "assign(local:f:Foo, new Foo())"),
            Arguments.of("decl string", "String s = \"hi\";", "assign(local:s:String, \"hi\")"),
            Arguments.of("decl boolean", "boolean b = c && d;", "assign(local:b:boolean, c && d)"),
            Arguments.of("decl two arrays", "int[] a; int[] b;", "havoc(local:a:int[]);\nhavoc(local:b:int[])"),
            Arguments.of("decl long", "long l = 42L;", "assign(local:l:long, 42L)"),
            Arguments.of("decl method result", "int n = size();", "assign(local:n:int, size())"),

            // ---------------------------------------------------------------- calls
            Arguments.of("call no args", "foo();", "call(foo())"),
            Arguments.of("call with args", "foo(a, b);", "call(foo(a, b))"),
            Arguments.of("call this", "this.foo();", "call(this.foo())"),
            Arguments.of("call receiver", "obj.method(x);", "call(obj.method(x))"),
            Arguments.of("call chained", "a.b.method();", "call(a.b.method())"),
            Arguments.of("call string literal", "System.out.println(\"x\");", "call(System.out.println(\"x\"))"),
            Arguments.of("call static", "Math.max(a, b);", "call(Math.max(a, b))"),
            Arguments.of("call new object arg", "use(new Foo());", "call(use(new Foo()))"),
            Arguments.of("call nested args", "foo(bar(), baz());", "call(foo(bar(), baz()))"),

            // ---------------------------------------------------------------- blocks and empty statements
            Arguments.of("empty block", "{}", ""),
            Arguments.of("nested empty blocks", "{ { } }", ""),
            Arguments.of("empty statement", ";", ""),
            Arguments.of("two empty statements", ";;", ""),
            Arguments.of("block flattening", "{ x = 1; { y = 2; } }", "assign(local:x, 1);\nassign(local:y, 2)"),
            Arguments.of("block with empties", "; x = 1; ;", "assign(local:x, 1)"),
            Arguments.of("deeply nested block", "{ { { x = 1; } } }", "assign(local:x, 1)"),

            // ---------------------------------------------------------------- if / else
            Arguments.of("if then only", "if (c) x = 1;", "if(c, [assign(local:x, 1)], [])"),
            Arguments.of("if else", "if (c) x = 1; else y = 2;", "if(c, [assign(local:x, 1)], [assign(local:y, 2)])"),
            Arguments.of("if block", "if (c) { x = 1; y = 2; }", "if(c, [assign(local:x, 1); assign(local:y, 2)], [])"),
            Arguments.of("if else-if", "if (c) { x = 1; } else if (d) { y = 2; }",
                "if(c, [assign(local:x, 1)], [if(d, [assign(local:y, 2)], [])])"),
            Arguments.of("empty if body", "if (c) { }", "if(c, [], [])"),
            Arguments.of("empty then non-empty else", "if (c) ; else x = 1;", "if(c, [], [assign(local:x, 1)])"),
            Arguments.of("if else block", "if (c) x = 1; else { y = 2; z = 3; }",
                "if(c, [assign(local:x, 1)], [assign(local:y, 2); assign(local:z, 3)])"),
            Arguments.of("if complex condition", "if (a && b || !c) x = 1;", "if(a && b || !c, [assign(local:x, 1)], [])"),
            Arguments.of("nested if", "if (a) if (b) x = 1;", "if(a, [if(b, [assign(local:x, 1)], [])], [])"),

            // ---------------------------------------------------------------- while loops
            Arguments.of("while single stmt", "while (c) x = 1;", "loop(c, [assign(local:x, 1)])"),
            Arguments.of("while block", "while (c) { x = 1; y = 2; }", "loop(c, [assign(local:x, 1); assign(local:y, 2)])"),
            Arguments.of("while empty body", "while (c);", "loop(c, [])"),
            Arguments.of("nested while", "while (a) while (b) x = 1;", "loop(a, [loop(b, [assign(local:x, 1)])])"),
            Arguments.of("while negated condition", "while (!c) x = 1;", "loop(!c, [assign(local:x, 1)])"),
            Arguments.of("while numeric condition", "while (i < n) { i = i + 1; }", "loop(i < n, [assign(local:i, i + 1)])"),

            // ---------------------------------------------------------------- for loops (desugared)
            Arguments.of("for all parts", "for (int i = 0; i < n; i++) x = x + i;",
                "assign(local:i:int, 0);\nloop(i < n, [assign(local:x, x + i); assign(local:i, i + 1)])"),
            Arguments.of("for without init", "for (; c;) x = 1;", "loop(c, [assign(local:x, 1)])"),
            Arguments.of("for without condition", "for (int i = 0; ; i++) x = 1;",
                "assign(local:i:int, 0);\nloop(true, [assign(local:x, 1); assign(local:i, i + 1)])"),
            Arguments.of("for empty header", "for (;;) x = 1;", "loop(true, [assign(local:x, 1)])"),
            Arguments.of("for multi declarators", "for (int i = 0, j = 1; i < j; i++, j--) { }",
                "assign(local:i:int, 0);\nassign(local:j:int, 1);\nloop(i < j, [assign(local:i, i + 1); assign(local:j, j - 1)])"),
            Arguments.of("for empty body", "for (int i = 0; i < n; i++);",
                "assign(local:i:int, 0);\nloop(i < n, [assign(local:i, i + 1)])"),
            Arguments.of("for with call body", "for (int i = 0; i < 10; i++) { use(i); }",
                "assign(local:i:int, 0);\nloop(i < 10, [call(use(i)); assign(local:i, i + 1)])"),
            Arguments.of("for compound init assign", "for (i = 0; i < n; i += 1) x = 1;",
                "assign(local:i, 0);\nloop(i < n, [assign(local:x, 1); assign(local:i, i + 1)])"),
            Arguments.of("for called in condition", "for (int i = 0; i < size(); i++) ;",
                "assign(local:i:int, 0);\nloop(i < size(), [assign(local:i, i + 1)])"),

            // ---------------------------------------------------------------- do-while loops (unrolled)
            Arguments.of("do while single", "do x = 1; while (c);",
                "assign(local:x, 1);\nloop(c, [assign(local:x, 1)])"),
            Arguments.of("do while block", "do { x = 1; } while (c);",
                "assign(local:x, 1);\nloop(c, [assign(local:x, 1)])"),
            Arguments.of("do while empty body", "do ; while (c);", "loop(c, [])"),
            Arguments.of("do while compound", "do { x += 1; } while (x < 10);",
                "assign(local:x, x + 1);\nloop(x < 10, [assign(local:x, x + 1)])"),
            Arguments.of("do while call", "do foo(); while (c);", "call(foo());\nloop(c, [call(foo())])"),

            // ---------------------------------------------------------------- foreach loops (desugared)
            Arguments.of("foreach basic", "for (int x : a) use(x);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("foreach this field", "for (int x : this.a) use(x);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < this.a.length, [assign(local:x:int, this.a[\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("foreach receiver field", "for (int x : b.a) use(x);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < b.a.length, [assign(local:x:int, b.a[\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("foreach empty body", "for (int x : a);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("foreach two sequential", "for (int x : a) use(x); for (int y : b) use(y);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)]);\n" +
                    "assign(local:\$idx1, 0);\nloop(\$idx1 < b.length, [assign(local:y:int, b[\$idx1]); call(use(y)); assign(local:\$idx1, \$idx1 + 1)])"),
            Arguments.of("foreach nested", "for (int x : a) for (int y : b) use(x, y);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); " +
                    "assign(local:\$idx1, 0); loop(\$idx1 < b.length, [assign(local:y:int, b[\$idx1]); call(use(x, y)); assign(local:\$idx1, \$idx1 + 1)]); " +
                    "assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("foreach over new array", "for (int x : new int[3]) use(x);",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < new int[3].length, [assign(local:x:int, new int[3][\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)])"),

            // ---------------------------------------------------------------- return / throw
            Arguments.of("return value", "return x;", "return(x)"),
            Arguments.of("return void", "return;", "return()"),
            Arguments.of("return expression", "return x + 1;", "return(x + 1)"),
            Arguments.of("return array element", "return a[0];", "return(a[0])"),
            Arguments.of("throw name", "throw e;", "throw(e)"),
            Arguments.of("throw new exception", "throw new Exception();", "throw(new Exception())"),
            Arguments.of("throw inside if", "if (c) throw e; else throw f;", "if(c, [throw(e)], [throw(f)])"),
            Arguments.of("throw after assign", "x = 1; throw e;", "assign(local:x, 1);\nthrow(e)"),

            // ---------------------------------------------------------------- break / continue (structured)
            Arguments.of("break outside loop", "break;", "break()"),
            Arguments.of("continue outside loop", "continue;", "continue()"),
            Arguments.of("break in while", "while (c) { break; }", "loop(c, [break()])"),
            Arguments.of("continue in while", "while (c) { continue; }", "loop(c, [continue()])"),
            Arguments.of("break in for", "for (;;) { break; }", "loop(true, [break()])"),
            Arguments.of("continue in for", "for (;;) { continue; }", "loop(true, [continue()])"),
            Arguments.of("break in foreach", "for (int x : a) { break; }",
                "assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); break(); assign(local:\$idx0, \$idx0 + 1)])"),
            Arguments.of("break in do while", "do { if (c) break; } while (d);",
                "if(c, [break()], []);\nloop(d, [if(c, [break()], [])])"),
            Arguments.of("break in nested loop", "while (a) { while (b) { break; } }",
                "loop(a, [loop(b, [break()])])"),
            Arguments.of("continue in nested loop", "while (a) { while (b) { continue; } }",
                "loop(a, [loop(b, [continue()])])"),
            Arguments.of("break then continue", "while (c) { break; continue; }", "loop(c, [break(); continue()])"),
            Arguments.of("continue in for inside while", "while (a) { for (int i = 0; i < n; i++) continue; }",
                "loop(a, [assign(local:i:int, 0); loop(i < n, [continue(); assign(local:i, i + 1)])])"),

            // ---------------------------------------------------------------- switch (desugared)
            Arguments.of("switch single case", "switch (x) { case 1: y = 1; }",
                "switch(x, [case(1, [assign(local:y, 1)])])"),
            Arguments.of("switch default only", "switch (x) { default: y = 2; }",
                "switch(x, [case(, [assign(local:y, 2)])])"),
            Arguments.of("switch case and default", "switch (x) { case 1: y = 1; default: y = 2; }",
                "switch(x, [case(1, [assign(local:y, 1)]); case(, [assign(local:y, 2)])])"),
            Arguments.of("switch multiple labels", "switch (x) { case 1: case 2: y = 3; }",
                "switch(x, [case(1, []); case(2, [assign(local:y, 3)])])"),
            Arguments.of("switch fall-through", "switch (x) { case 1: y = 1; case 2: y = 2; default: y = 3; }",
                "switch(x, [case(1, [assign(local:y, 1)]); case(2, [assign(local:y, 2)]); case(, [assign(local:y, 3)])])"),
            Arguments.of("switch with break", "switch (x) { case 1: y = 1; break; }",
                "switch(x, [case(1, [assign(local:y, 1); break()])])"),
            Arguments.of("switch empty case", "switch (x) { case 1: }", "switch(x, [case(1, [])])"),
            Arguments.of("switch string labels", "switch (s) { case \"a\": y = 1; }",
                "switch(s, [case(\"a\", [assign(local:y, 1)])])"),
            Arguments.of("switch multiple statements", "switch (x) { case 1: y = 1; z = 2; }",
                "switch(x, [case(1, [assign(local:y, 1); assign(local:z, 2)])])"),
            Arguments.of("switch in while", "while (c) { switch (x) { case 1: y = 1; } }",
                "loop(c, [switch(x, [case(1, [assign(local:y, 1)])])])"),

            // ---------------------------------------------------------------- try / catch / finally
            Arguments.of("try catch", "try { x = 1; } catch (E e) { y = 2; }",
                "try([assign(local:x, 1)], [catch(E, e, [assign(local:y, 2)])], [])"),
            Arguments.of("try finally", "try { x = 1; } finally { y = 2; }",
                "try([assign(local:x, 1)], [], [assign(local:y, 2)])"),
            Arguments.of("try catch finally", "try { x = 1; } catch (E e) { y = 2; } finally { z = 3; }",
                "try([assign(local:x, 1)], [catch(E, e, [assign(local:y, 2)])], [assign(local:z, 3)])"),
            Arguments.of("try multiple catches", "try { x = 1; } catch (E1 e) { } catch (E2 e) { }",
                "try([assign(local:x, 1)], [catch(E1, e, []); catch(E2, e, [])], [])"),
            Arguments.of("try union catch", "try { x = 1; } catch (A | B e) { y = 2; }",
                "try([assign(local:x, 1)], [catch(A | B, e, [assign(local:y, 2)])], [])"),
            Arguments.of("try empty both", "try { } finally { }", "try([], [], [])"),
            Arguments.of("try nested", "try { try { x = 1; } finally { y = 2; } } catch (E e) { z = 3; }",
                "try([try([assign(local:x, 1)], [], [assign(local:y, 2)])], [catch(E, e, [assign(local:z, 3)])], [])"),
            Arguments.of("try catch throws", "try { throw e; } catch (E e) { x = 1; }",
                "try([throw(e)], [catch(E, e, [assign(local:x, 1)])], [])"),
            Arguments.of("try with multiple statements", "try { x = 1; y = 2; } finally { x = 0; }",
                "try([assign(local:x, 1); assign(local:y, 2)], [], [assign(local:x, 0)])"),

            // ---------------------------------------------------------------- JML statements
            Arguments.of("jml assert", "//@ assert x > 0;", "assert(x > 0)"),
            Arguments.of("jml assert redundantly", "//@ assert_redundantly x > 0;", "assert(x > 0)"),
            Arguments.of("jml assume", "//@ assume y == 1;", "assume(y == 1)"),
            Arguments.of("jml assert conj", "//@ assert x >= 0 && x < 10;", "assert(x >= 0 && x < 10)"),
            Arguments.of("jml assert complex", "//@ assert a == b || b == c;", "assert(a == b || b == c)"),
            Arguments.of("jml set skipped", "//@ set x = 1;", ""),
            Arguments.of("jml assert between statements", "x = 1;\n//@ assert x == 1;\ny = 2;",
                "assign(local:x, 1);\nassert(x == 1);\nassign(local:y, 2)"),
        )

        @JvmStatic
        fun compoundOperators(): Stream<Arguments> = Stream.of(
            Arguments.of("+=", "assign(local:x, x + 1)"),
            Arguments.of("-=", "assign(local:x, x - 1)"),
            Arguments.of("*=", "assign(local:x, x * 1)"),
            Arguments.of("/=", "assign(local:x, x / 1)"),
            Arguments.of("%=", "assign(local:x, x % 1)"),
            Arguments.of("&=", "assign(local:x, x & 1)"),
            Arguments.of("|=", "assign(local:x, x | 1)"),
            Arguments.of("^=", "assign(local:x, x ^ 1)"),
            Arguments.of("<<=", "assign(local:x, x << 1)"),
            Arguments.of(">>=", "assign(local:x, x >> 1)"),
            Arguments.of(">>>=", "assign(local:x, x >>> 1)"),
        )
    }
    //endregion

    //region unsupported statement / expression kinds
    @Test
    fun testUnsupportedLabeledStatement() {
        val e = assertThrows(UnsupportedOperationException::class.java) { normalize("foo: { x = 1; }") }
        assertTrue(e.message!!.contains("LabeledStmt"), "unexpected message: ${e.message}")
    }

    @Test
    fun testUnsupportedSynchronizedStatement() {
        assertThrows(UnsupportedOperationException::class.java) { normalize("synchronized (x) { x = 1; }") }
    }

    @Test
    fun testUnsupportedLocalClassStatement() {
        val e = assertThrows(UnsupportedOperationException::class.java) { normalize("class Local { }") }
        assertTrue(e.message!!.contains("LocalClassDeclarationStmt"), "unexpected message: ${e.message}")
    }

    @Test
    fun testUnsupportedJmlGhostStatement() {
        assertThrows(UnsupportedOperationException::class.java) { normalize("//@ ghost int g;") }
    }

    @Test
    fun testUnsupportedJmlUnreachableStatement() {
        assertThrows(UnsupportedOperationException::class.java) { normalize("//@ unreachable;") }
    }

    @Test
    fun testUnsupportedObjectCreationExpressionStatement() {
        val e = assertThrows(UnsupportedOperationException::class.java) { normalize("new Foo();") }
        assertTrue(e.message!!.contains("ObjectCreationExpr"), "unexpected message: ${e.message}")
    }

    @Test
    fun testUnsupportedAssignmentTarget() {
        val e = assertThrows(UnsupportedOperationException::class.java) { normalize("(x) = 1;") }
        assertTrue(e.message!!.contains("Assignment target"), "unexpected message: ${e.message}")
    }
    //endregion

    //region loop / switch break & continue targets
    @Test
    fun testBreakTargetsEnclosingWhile() {
        val src = "while (c) { if (d) break; }"
        val loop = blockOf(src).getStatement(0).asWhileStmt()
        val nf = normalize(src)
        val br = nf.firstOfType { it is NfBreak } as NfBreak
        assertEquals(loop.hashCode(), br.loopId, "break must target the enclosing while loop")
    }

    @Test
    fun testContinueTargetsEnclosingWhile() {
        val src = "while (c) { if (d) continue; }"
        val loop = blockOf(src).getStatement(0).asWhileStmt()
        val nf = normalize(src)
        val cont = nf.firstOfType { it is NfContinue } as NfContinue
        assertEquals(loop.hashCode(), cont.loopId, "continue must target the enclosing while loop")
    }

    @Test
    fun testBreakTargetsEnclosingFor() {
        val src = "for (int i = 0; i < n; i++) { break; }"
        val loop = blockOf(src).getStatement(0) as ForStmt
        val nf = normalize(src)
        val br = nf.firstOfType { it is NfBreak } as NfBreak
        assertEquals(loop.hashCode(), br.loopId, "break must target the enclosing for loop")
    }

    @Test
    fun testBreakTargetsEnclosingForeach() {
        val src = "for (int x : a) { break; }"
        val loop = blockOf(src).getStatement(0) as ForEachStmt
        val nf = normalize(src)
        val br = nf.firstOfType { it is NfBreak } as NfBreak
        assertEquals(loop.hashCode(), br.loopId, "break must target the enclosing foreach loop")
    }

    @Test
    fun testBreakTargetsEnclosingDoWhile() {
        val src = "do { break; } while (c);"
        val loop = blockOf(src).getStatement(0) as DoStmt
        val nf = normalize(src)
        val br = nf.firstOfType { it is NfBreak } as NfBreak
        assertEquals(loop.hashCode(), br.loopId, "break must target the enclosing do-while loop")
    }

    @Test
    fun testNestedBreakTargetsInnermostLoop() {
        val src = "while (a) { while (b) { break; } }"
        val outer = blockOf(src).getStatement(0).asWhileStmt()
        val inner = outer.body.asBlockStmt().getStatement(0).asWhileStmt()
        val nf = normalize(src)
        val br = nf.firstOfType { it is NfBreak } as NfBreak
        assertEquals(inner.hashCode(), br.loopId, "break must target the *innermost* loop")
        assertNotEquals(outer.hashCode(), br.loopId)
    }

    @Test
    fun testNestedContinueTargetsInnermostLoop() {
        val src = "while (a) { while (b) { continue; } }"
        val outer = blockOf(src).getStatement(0).asWhileStmt()
        val inner = outer.body.asBlockStmt().getStatement(0).asWhileStmt()
        val nf = normalize(src)
        val cont = nf.firstOfType { it is NfContinue } as NfContinue
        assertEquals(inner.hashCode(), cont.loopId, "continue must target the *innermost* loop")
        assertNotEquals(outer.hashCode(), cont.loopId)
    }

    @Test
    fun testBreakOutsideLoopHasMinusOneTarget() {
        val nf = normalize("break;")
        val br = nf[0] as NfBreak
        assertEquals(-1, br.loopId)
    }

    @Test
    fun testContinueOutsideLoopHasMinusOneTarget() {
        val nf = normalize("continue;")
        val cont = nf[0] as NfContinue
        assertEquals(-1, cont.loopId)
    }

    @Test
    fun testSwitchBreakTargetsSwitch() {
        val src = "switch (x) { case 1: y = 1; break; }"
        val sw = blockOf(src).getStatement(0) as SwitchStmt
        val nf = normalize(src)
        val swIn = nf[0] as NfSwitch
        assertEquals(sw.hashCode(), swIn.switchId, "switch id must be the switch node hash")
        val br = swIn.cases[0].body.first { it is NfBreak } as NfBreak
        assertEquals(sw.hashCode(), br.loopId, "a break in a case must target the switch")
    }

    @Test
    fun testBreakInSwitchInsideLoopTargetsSwitch() {
        val src = "while (c) { switch (x) { case 1: break; } }"
        val block = blockOf(src)
        val loop = block.getStatement(0).asWhileStmt()
        val sw = loop.body.asBlockStmt().getStatement(0) as SwitchStmt
        val nf = normalize(src)
        val swIn = (nf[0] as NfLoop).body.first { it is NfSwitch } as NfSwitch
        val br = swIn.cases[0].body.first { it is NfBreak } as NfBreak
        assertEquals(sw.hashCode(), swIn.switchId)
        assertEquals(sw.hashCode(), br.loopId, "break binds to the switch, shadowing the outer loop")
        assertNotEquals(loop.hashCode(), br.loopId)
    }

    @Test
    fun testBreakInsideNestedLoopInSwitchCaseTargetsLoop() {
        val src = "switch (x) { case 1: while (c) { break; } }"
        val block = blockOf(src)
        val sw = block.getStatement(0) as SwitchStmt
        val innerWhile = sw.entries[0].statements[0].asWhileStmt()
        val nf = normalize(src)
        val swIn = nf[0] as NfSwitch
        val br = swIn.cases[0].body.first { it is NfLoop }.let {
            (it as NfLoop).body.first { b -> b is NfBreak }
        } as NfBreak
        assertEquals(innerWhile.hashCode(), br.loopId, "a break in a nested loop inside a case targets the loop")
    }
    //endregion

    //region origin (provenance) tracking
    @Test
    fun testEveryStatementHasOrigin() {
        val cases = listOf(
            "x = 1;",
            "if (c) x = 1; else y = 2;",
            "while (c) { x = 1; }",
            "for (int i = 0; i < n; i++) x = x + i;",
            "do { x = 1; } while (c);",
            "for (int x : a) use(x);",
            "switch (x) { case 1: y = 1; break; default: y = 2; }",
            "try { x = 1; } catch (E e) { y = 2; } finally { z = 3; }",
            "//@ assert x > 0;",
            "return x;",
        )
        for (src in cases) {
            val nf = normalize(src)
            assertTrue(nf.isNotEmpty(), "no IR produced for: $src")
            nf.forEachStmt { s ->
                assertNotNull(s.origin, "origin must be set for $s (source: $src)")
            }
        }
    }

    @Test
    fun testOriginEqualsSourceStatement() {
        val block = blockOf("x = 1;")
        val stmt = block.getStatement(0)
        val nf = Normalizer().normalizeStmt(stmt)
        assertEquals(stmt, nf.single().origin, "a single statement's IR must reference the original statement")
    }

    @Test
    fun testOriginOfIfBranches() {
        val block = blockOf("if (c) x = 1; else y = 2;")
        val ifStmt = block.getStatement(0)
        val thenStmt = ifStmt.asIfStmt().thenStmt
        val elseStmt = ifStmt.asIfStmt().elseStmt.get()
        val nf = Normalizer().normalizeStmt(ifStmt)
        val nfIf = nf.single() as NfIf
        assertEquals(ifStmt, nfIf.origin)
        // single-statement branches are normalized in place: their origin is the
        // branch *statement* itself (not the if), mirroring normalizeExprStmt.
        assertEquals(thenStmt, nfIf.thenStmts.single().origin)
        assertEquals(elseStmt, nfIf.elseStmts.single().origin)
    }

    @Test
    fun testOriginOfLoopBodies() {
        val block = blockOf("while (c) { x = 1; }")
        val whileStmt = block.getStatement(0).asWhileStmt()
        val nf = Normalizer().normalizeStmt(whileStmt)
        val nfLoop = nf.single() as NfLoop
        assertEquals(whileStmt, nfLoop.origin)
        assertEquals(whileStmt, nfLoop.loopNode)
        // the body is a block: its inner statement carries the block as origin
        assertEquals(whileStmt.body, nfLoop.body.single().origin)
    }
    //endregion

    //region key(...) location mapping
    @Test
    fun testKeyOfName() {
        assertEquals("x", Normalizer().key(NameExpr("x")))
    }

    @Test
    fun testKeyOfThisExpr() {
        assertEquals("this", Normalizer().key(ThisExpr()))
    }

    @Test
    fun testKeyOfThisFieldAccess() {
        assertEquals("this.f", Normalizer().key(FieldAccessExpr(ThisExpr(), "f")))
    }

    @Test
    fun testKeyOfNameScopedFieldAccess() {
        assertEquals("o.f", Normalizer().key(FieldAccessExpr(NameExpr("o"), "f")))
    }

    @Test
    fun testKeyOfParenthesizedThisScope() {
        // `(this).f` is not a ThisExpr scope and must not collapse to `this.f`
        val scope = EnclosedExpr(ThisExpr())
        assertEquals("(this).f", Normalizer().key(FieldAccessExpr(scope, "f")))
    }

    @Test
    fun testKeyOfArrayAccess() {
        assertEquals("a[i]", Normalizer().key(ArrayAccessExpr(NameExpr("a"), NameExpr("i"))))
    }

    @Test
    fun testKeyOfArbitraryExpression() {
        assertEquals("5", Normalizer().key(IntegerLiteralExpr("5")))
    }

    @Test
    fun testKeyOfQualifiedCallScope() {
        assertEquals("System.out", Normalizer().key(FieldAccessExpr(NameExpr("System"), "out")))
    }
    //endregion

    //region JML statement kinds constructed directly (branches not reachable from source)
    @Test
    fun testJmlAssumedRedundantlyConstructedMapsToAssume() {
        val stmt = JmlExpressionStmt(
            NodeList<com.github.javaparser.ast.expr.SimpleName>(), JmlExpressionStmt.JmlStmtKind.ASSUME_REDUNDANTLY,
            com.github.javaparser.StaticJavaParser.parseExpression("y == 1")
        )
        val nf = Normalizer().normalizeStmt(stmt)
        assertEquals(1, nf.size)
        assertTrue(nf[0] is NfAssume, "ASSUME_REDUNDANTLY must normalize to NfAssume, got ${nf[0]}")
    }

    @Test
    fun testJmlSetConstructedIsSkipped() {
        val stmt = JmlExpressionStmt(
            NodeList<com.github.javaparser.ast.expr.SimpleName>(), JmlExpressionStmt.JmlStmtKind.SET,
            com.github.javaparser.StaticJavaParser.parseExpression("x = 1")
        )
        assertTrue(Normalizer().normalizeStmt(stmt).isEmpty(), "set statements produce no IR")
    }

    @Test
    fun testJmlHenceByConstructedIsSkipped() {
        val stmt = JmlExpressionStmt(
            NodeList<com.github.javaparser.ast.expr.SimpleName>(), JmlExpressionStmt.JmlStmtKind.HENCE_BY,
            com.github.javaparser.StaticJavaParser.parseExpression("x > 0")
        )
        assertTrue(Normalizer().normalizeStmt(stmt).isEmpty(), "hence_by statements produce no IR")
    }
    //endregion

    //region structural semantics
    @Test
    fun testDoWhileSecondBodyIsClone() {
        // the second body of a do-while is a *clone* of the first, not the same nodes
        val block = blockOf("do x = 1; while (c);")
        val doStmt = block.getStatement(0).asDoStmt()
        val nf = Normalizer().normalizeStmt(doStmt)
        val first = nf[0]
        val loop = nf[1] as NfLoop
        val second = loop.body.single()
        assertNotSame(first, second, "the unrolled body must be a fresh clone, not the original nodes")
    }

    @Test
    fun testForeachIndexCounterResetsPerNormalizerInstance() {
        val src = "for (int x : a) use(x);"
        val once = dump(src)
        // a *new* Normalizer starts its temp counter at 0 again
        assertEquals("assign(local:\$idx0, 0);\nloop(\$idx0 < a.length, [assign(local:x:int, a[\$idx0]); call(use(x)); assign(local:\$idx0, \$idx0 + 1)])", once)
        assertEquals(once, dump(src))
    }

    @Test
    fun testDoWhileBodyCountUnrolledOnce() {
        // a do-while with a block body unfolds to [body stmts..., loop(body stmts...)]
        val nf = normalize("do { x = 1; y = 2; } while (c);")
        assertEquals(3, nf.size, "two body statements followed by the loop")
        assertTrue(nf[0] is NfAssign && nf[1] is NfAssign)
        val loop = nf[2] as NfLoop
        assertEquals(2, loop.body.size, "the loop part re-executes the body")
    }

    @Test
    fun testDoWhileSingleStatementBodyUnfold() {
        // a do-while with a single-statement (non-block) body: [stmt, loop(stmt)]
        val nf = normalize("do x = 1; while (c);")
        assertEquals(2, nf.size)
        assertTrue(nf[0] is NfAssign)
        assertEquals(1, (nf[1] as NfLoop).body.size)
    }

    @Test
    fun testIfBothBranchesEmptyStaysIf() {
        val nf = normalize("if (c) { } else { }")
        assertEquals(1, nf.size)
        val f = nf[0] as NfIf
        assertTrue(f.thenStmts.isEmpty() && f.elseStmts.isEmpty())
    }

    @Test
    fun testBlockStmtInsideMethodFlattens() {
        // a block statement nested directly (not via { }) is still flattened
        val block = blockOf("x = 1;")
        val stmt = block.getStatement(0)
        val nf = Normalizer().normalizeStmt(stmt)
        assertEquals("assign(local:x, 1)", nf.joinToString(";\n") { it.dump() })
    }

    @Test
    fun testDeclaredTypeIsCarriedIntoNfLocal() {
        val nf = normalize("int[] arr = null;")
        val loc = (nf[0] as NfAssign).target as NfLocal
        assertEquals("arr", loc.key)
        assertNotNull(loc.declaredType, "the declared type must be preserved for the initializer target")
        assertEquals("int[]", loc.declaredType.toString())
    }

    @Test
    fun testHavocCarriesDeclaredType() {
        val nf = normalize("double d;")
        val loc = (nf[0] as NfHavoc).location as NfLocal
        assertEquals("double", loc.declaredType.toString())
    }

    @Test
    fun testUnderscoreInIdentifierPreserved() {
        val nf = normalize("my_var = 1;")
        assertEquals("assign(local:my_var, 1)", nf.joinToString(";\n") { it.dump() })
    }

    @Test
    fun testUnaryMethodCallStatement() {
        // `!foo();` is not an expression statement in Java, but a call *result* used in
        // a declaration is fine - verify a boolean call result expression
        val nf = normalize("boolean b = foo();")
        assertEquals("assign(local:b:boolean, foo())", nf.joinToString(";\n") { it.dump() })
    }
    //endregion

    //region assignment operator coverage
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("compoundOperators")
    fun testCompoundAssignmentOperators(operator: String, expected: String) {
        val actual = dump("x $operator 1;")
        assertEquals(expected, actual, "operator '$operator'")
    }
    //endregion
}
