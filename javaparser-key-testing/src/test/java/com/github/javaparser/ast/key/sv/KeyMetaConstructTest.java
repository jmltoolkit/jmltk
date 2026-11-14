/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.key.sv;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KeyMetaConstructTest {

    private KeyContextStatementBlock parse(String schema) {
        ParserConfiguration config = new ParserConfiguration();
        config.setPreprocessUnicodeEscapes(true);
        return parseWithConfig(config, schema);
    }

    private KeyContextStatementBlock parseWithConfig(ParserConfiguration config, String schema) {
        ParseResult<KeyContextStatementBlock> result = new JavaParser(config).parseSchemaBlock(schema);
        assertThat(result.isSuccessful())
                .as("Parsing of '%s' failed: %s", schema, result.getProblems())
                .isTrue();
        return result.getResult().get();
    }

    @Test
    void parsesBareIdentifierArgumentsInOrder() {
        // "#forToWhile" (unlike most constructs) takes no trailing semicolon
        KeyContextStatementBlock block = parse("{.. #forToWhile(#innerLabel, #outerLabel, #forloop) ...}");

        KeyMetaConstruct kmc = block.getStatements().get(0).asKeyMetaConstruct();
        assertThat(kmc.getKind()).isEqualTo("#forToWhile");
        assertThat(kmc.getChildren()).hasSize(3);
        assertThat(((KeyExpressionSV) kmc.getChildren().get(0)).getText()).isEqualTo("#innerLabel");
        assertThat(((KeyExpressionSV) kmc.getChildren().get(1)).getText()).isEqualTo("#outerLabel");
        assertThat(((KeyExpressionSV) kmc.getChildren().get(2)).getText()).isEqualTo("#forloop");
    }

    @Test
    void parsesMixedArgumentShapesInOrder() {
        KeyContextStatementBlock block = parse("{.. #methodCall(#ex, #mn(#elist), #forloop); ...}");

        KeyMetaConstruct kmc = block.getStatements().get(0).asKeyMetaConstruct();
        assertThat(kmc.getKind()).isEqualTo("#methodCall");
        assertThat(kmc.getChildren()).hasSize(3);

        // bare "#ex" -> KeyExpressionSV
        assertThat(((KeyExpressionSV) kmc.getChildren().get(0)).getText()).isEqualTo("#ex");
        // "#mn(#elist)" -> method call expression
        MethodCallExpr call = (MethodCallExpr) kmc.getChildren().get(1);
        assertThat(call.getName().toString()).isEqualTo("#mn");
        assertThat(call.getArgument(0).toString()).isEqualTo("#elist");
        // bare "#forloop" as last argument (before the closing parenthesis)
        assertThat(((KeyExpressionSV) kmc.getChildren().get(2)).getText()).isEqualTo("#forloop");
    }

    @Test
    void parsesLabeledStatementAsArgument() {
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
    void nonReservedNameRemainsAnExpressionStatement() {
        // "#custom" is not a reserved meta-construct name, so it stays an expression statement
        KeyContextStatementBlock block = parse("{.. #custom(#a, #b); ...}");

        ExpressionStmt stmt = (ExpressionStmt) block.getStatements().get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
        assertThat(call.getName().toString()).isEqualTo("#custom");
        assertThat(call.getArguments()).hasSize(2);
    }

    @Test
    void renamedExpressionConstructsFallBackToMethodCalls() {
        // "#create-object" / "#length-reference" / "#static-evaluate" lost their dedicated
        // productions and now parse like any non-reserved "#name(...)"
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
    void isstaticFallsBackToMethodCall() {
        // "#isstatic" lost its dedicated production: it parses like any other "#name(...)"
        ExpressionStmt stmt = (ExpressionStmt)
                parse("{.. #isstatic(#a, #b); ...}").getStatements().get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
        assertThat(call.getName().toString()).isEqualTo("#isstatic");
        assertThat(call.getArguments()).hasSize(2);
        assertThat(call.getArgument(0).toString()).isEqualTo("#a");
        assertThat(call.getArgument(1).toString()).isEqualTo("#b");

        // expression position as well
        ExpressionStmt assignStmt = (ExpressionStmt)
                parse("{.. #v0 = #isstatic(#a); ...}").getStatements().get(0);
        MethodCallExpr value = (MethodCallExpr) ((AssignExpr) assignStmt.getExpression()).getValue();
        assertThat(value.getName().toString()).isEqualTo("#isstatic");
        assertThat(value.getArguments()).hasSize(1);

        // print -> re-parse is stable
        String printed = stmt.toString();
        assertThat(parse("{" + printed + "}").getStatements().get(0).toString()).isEqualTo(printed);
    }

    @Test
    void printReparsePrintRoundTripPreservesArguments() {
        String[] schemas = {
            "{.. #methodCall(#ex, #mn(#elist), #forloop); ...}",
            "{.. #forToWhile(#innerLabel, #outerLabel, #forloop) ...}",
            "{.. #switchToIf(#sw) ...}",
            "{.. #doBreak(#lb0: break #lb1;); ...}",
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

    @Test
    void defaultReservedNamesCoverTheStandardConstructs() {
        assertThat(ParserConfiguration.DEFAULT_KEY_META_CONSTRUCT_NAMES)
                .hasSize(23)
                .contains("#arrayPostDeclaration", "#forToWhile", "#methodCall", "#switchToIf", "#unwindLoop");
    }

    @Test
    void configuredNamesFullyReplaceTheDefaults() {
        ParserConfiguration config = new ParserConfiguration();
        config.setPreprocessUnicodeEscapes(true);
        config.setKeyMetaConstructNames(Set.of("#custom"));

        // the configured name now parses as a meta-construct
        KeyMetaConstruct kmc = parseWithConfig(config, "{.. #custom(#a, #b); ...}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        assertThat(kmc.getKind()).isEqualTo("#custom");
        assertThat(kmc.getChildren()).hasSize(2);

        // the default names are no longer reserved: the call falls through to an expression statement
        ExpressionStmt stmt =
                (ExpressionStmt) parseWithConfig(config, "{.. #methodCall(#ex, #mn(#elist), #forloop); ...}")
                        .getStatements()
                        .get(0);
        MethodCallExpr call = (MethodCallExpr) stmt.getExpression();
        assertThat(call.getName().toString()).isEqualTo("#methodCall");
        assertThat(call.getArguments()).hasSize(3);
    }

    @Test
    void emptySetDisablesAllMetaConstructs() {
        ParserConfiguration config = new ParserConfiguration();
        config.setPreprocessUnicodeEscapes(true);
        config.setKeyMetaConstructNames(Set.of());

        // with a trailing ";" the call still parses, but as an expression statement
        ExpressionStmt stmt =
                (ExpressionStmt) parseWithConfig(config, "{.. #methodCall(#ex, #mn(#elist), #forloop); ...}")
                        .getStatements()
                        .get(0);
        assertThat(((MethodCallExpr) stmt.getExpression()).getName().toString()).isEqualTo("#methodCall");

        // without the trailing ";" no statement production matches at all
        ParseResult<KeyContextStatementBlock> failed =
                new JavaParser(config).parseSchemaBlock("{.. #forToWhile(#innerLabel, #outerLabel, #forloop) ...}");
        assertThat(failed.isSuccessful()).isFalse();
    }

    @Test
    void configuredCustomConstructRoundTrips() {
        ParserConfiguration config = new ParserConfiguration();
        config.setPreprocessUnicodeEscapes(true);
        config.setKeyMetaConstructNames(Set.of("#myConstruct"));

        KeyMetaConstruct first = parseWithConfig(config, "{.. #myConstruct(#x, #y(#z)); ...}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        String printed = first.toString();

        KeyMetaConstruct reparsed = parseWithConfig(config, "{" + printed + "}")
                .getStatements()
                .get(0)
                .asKeyMetaConstruct();
        assertThat(reparsed.toString()).isEqualTo(printed);
        assertThat(reparsed.getKind()).isEqualTo(first.getKind());
        assertThat(reparsed.getChildren()).hasSameSizeAs(first.getChildren());
    }
}
