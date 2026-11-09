/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.key.sv;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyMetaConstructTest {

    private KeyContextStatementBlock parse(String schema) {
        ParserConfiguration config = new ParserConfiguration();
        config.setPreprocessUnicodeEscapes(true);
        JavaParser parser = new JavaParser(config);
        ParseResult<KeyContextStatementBlock> result = parser.parseSchemaBlock(schema);
        assertThat(result.isSuccessful())
                .as("Parsing of '%s' failed: %s", schema, result.getProblems())
                .isTrue();
        return result.getResult().get();
    }

    @Test
    void statementArgumentIsParsedAsMetaConstruct() {
        // "#doBreak" takes a labeled statement; a MethodCallExpr cannot carry a statement
        // argument, so the construct is materialized as a KeyMetaConstruct
        KeyContextStatementBlock block = parse("{.. #doBreak(#lb0: break #lb1;); ...}");

        KeyMetaConstruct kmc = block.getStatements().get(0).asKeyMetaConstruct();
        assertThat(kmc.getKind()).isEqualTo("#doBreak");
        assertThat(kmc.getChildren()).hasSize(1);

        LabeledStmt labeled = (LabeledStmt) kmc.getChildren().get(0);
        assertThat(labeled.getLabel().toString()).isEqualTo("#lb0");
        BreakStmt breakStmt = (BreakStmt) labeled.getStatement();
        assertThat(breakStmt.getLabel().map(Object::toString).orElse(null)).isEqualTo("#lb1");
    }

    @Test
    void controlStatementArgumentsAreParsedAsMetaConstructs() {
        // as in the KeY corpus: the loop statement is a real statement, not a schema variable
        KeyMetaConstruct enhancedforElim = parse("{.. #enhancedforElim(for (#ty #id : #e) #stm); ...}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        assertThat(enhancedforElim.getKind()).isEqualTo("#enhancedforElim");
        assertThat(enhancedforElim.getChildren()).hasSize(1);
        assertThat((Node) enhancedforElim.getChildren().get(0)).isInstanceOf(ForEachStmt.class);

        KeyMetaConstruct reattachLoopInvariant = parse("{.. #reattachLoopInvariant(while (#nse) #body); ...}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        assertThat(reattachLoopInvariant.getKind()).isEqualTo("#reattachLoopInvariant");
        assertThat(reattachLoopInvariant.getChildren()).hasSize(1);
        assertThat((Node) reattachLoopInvariant.getChildren().get(0)).isInstanceOf(WhileStmt.class);
    }

    @Test
    void expressionOnlyArgumentsRemainAnExpressionStatement() {
        // all arguments are expressions or bare schema variables, so a MethodCallExpr is
        // sufficient and the statement is a plain method-call statement, reserved name or not
        for (String[] c : new String[][] {
            {"{.. #custom(#a, #b); ...}", "#custom", "2"},
            {"{.. #switchToIf(#sw); ...}", "#switchToIf", "1"},
        }) {
            ExpressionStmt stmt = (ExpressionStmt) parse(c[0]).getStatements().get(0);
            MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
            assertThat(call.getName().toString()).as(c[0]).isEqualTo(c[1]);
            assertThat(call.getArguments()).as(c[0]).hasSize(Integer.parseInt(c[2]));
            // bare schema variables are represented the same way as in expression position
            for (Expression arg : call.getArguments()) {
                assertThat(arg).as(c[0]).isInstanceOf(NameExpr.class);
            }
        }
    }

    @Test
    void formsWithoutSemicolonAreParsedAsMetaConstructs() {
        // "#forToWhile" and "#switchToIf" are written without a trailing ";", so they do not
        // form a method-call statement and are materialized as KeyMetaConstructs
        KeyMetaConstruct forToWhile = parse("{.. #forToWhile(#innerLabel, #outerLabel, #forloop) ...}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        assertThat(forToWhile.getKind()).isEqualTo("#forToWhile");
        assertThat(forToWhile.getChildren()).hasSize(3);
        for (Node child : forToWhile.getChildren()) {
            assertThat(child).isInstanceOf(KeyExpressionSV.class);
        }

        KeyMetaConstruct switchToIf =
                parse("{.. #switchToIf(#sw) ...}").getStatements().get(0).asKeyMetaConstruct();
        assertThat(switchToIf.getKind()).isEqualTo("#switchToIf");
        assertThat(switchToIf.getChildren()).hasSize(1);
        assertThat(switchToIf.getChildren().get(0)).isInstanceOf(KeyExpressionSV.class);
    }

    @Test
    void nestedExpressionArgumentsStayExpressions() {
        // "#mn(#elist)" is a method-call expression argument, not a bare schema variable
        ExpressionStmt stmt = (ExpressionStmt) parse("{.. #methodCall(#ex, #mn(#elist), #forloop); ...}")
                .getStatements()
                .get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
        assertThat(call.getArguments()).hasSize(3);
        assertThat(call.getArgument(0).toString()).isEqualTo("#ex");
        MethodCallExpr nested = call.getArgument(1).asMethodCallExpr();
        assertThat(nested.getName().toString()).isEqualTo("#mn");
        assertThat(nested.getArgument(0).toString()).isEqualTo("#elist");
        assertThat(call.getArgument(2).toString()).isEqualTo("#forloop");
    }

    @Test
    void renamedExpressionConstructsFallBackToMethodCalls() {
        // "#create-object" / "#length-reference" / "#static-evaluate" lost their dedicated
        // productions; their renamed forms take expression arguments only, so in statement
        // position they parse as a plain method-call statement
        String[][] cases = {
            {"{.. #createObject(#n); ...}", "#createObject", "#n"},
            {"{.. #lengthReference(#a); ...}", "#lengthReference", "#a"},
            {"{.. #staticEvaluate(#e); ...}", "#staticEvaluate", "#e"},
        };
        for (String[] c : cases) {
            ExpressionStmt stmt = (ExpressionStmt) parse(c[0]).getStatements().get(0);
            MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
            assertThat(call.getName().toString()).as(c[0]).isEqualTo(c[1]);
            assertThat(call.getArguments()).as(c[0]).hasSize(1);
            assertThat(call.getArgument(0).toString()).as(c[0]).isEqualTo(c[2]);

            // print -> re-parse -> print is stable
            String printed = stmt.toString();
            ExpressionStmt reparsed =
                    (ExpressionStmt) parse("{" + printed + "}").getStatements().get(0);
            assertThat(reparsed.toString()).as(c[0]).isEqualTo(printed);
        }
    }

    @Test
    void renamedExpressionConstructsWorkInAssignmentPosition() {
        // as in the KeY corpus: "#v0 = #createObject(#n)"
        ExpressionStmt stmt = (ExpressionStmt)
                parse("{.. #v0 = #createObject(#n); ...}").getStatements().get(0);
        AssignExpr assign = (AssignExpr) stmt.getExpression();
        assertThat(assign.getTarget().toString()).isEqualTo("#v0");
        MethodCallExpr call = (MethodCallExpr) assign.getValue();
        assertThat(call.getName().toString()).isEqualTo("#createObject");
    }

    @Test
    void printReparsePrintRoundTripPreservesArguments() {
        String[] schemas = {
            "{.. #doBreak(#lb0: break #lb1;); ...}",
            "{.. #enhancedforElim(for (#ty #id : #e) #stm); ...}",
            "{.. #reattachLoopInvariant(while (#nse) #body); ...}",
        };
        for (String schema : schemas) {
            KeyMetaConstruct first = parse(schema).getStatements().get(0).asKeyMetaConstruct();
            String printed = first.toString();
            KeyMetaConstruct reparsed =
                    parse("{" + printed + "}").getStatements().get(0).asKeyMetaConstruct();

            assertThat(reparsed.toString()).as("printed form of '%s'", schema).isEqualTo(printed);
            assertThat(reparsed.getKind()).isEqualTo(first.getKind());
            assertThat(reparsed.getChildren()).hasSameSizeAs(first.getChildren());
            for (int i = 0; i < first.getChildren().size(); i++) {
                assertThat(reparsed.getChildren().get(i).getClass())
                        .as("child %d of '%s'", i, schema)
                        .isEqualTo(first.getChildren().get(i).getClass());
                assertThat(reparsed.getChildren().get(i).toString())
                        .as("child %d of '%s'", i, schema)
                        .isEqualTo(first.getChildren().get(i).toString());
            }
        }
    }
}
