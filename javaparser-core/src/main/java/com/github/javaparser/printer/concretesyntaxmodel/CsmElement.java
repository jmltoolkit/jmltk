/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.printer.SourcePrinter;
import com.github.javaparser.printer.lexicalpreservation.TextElement;
import com.github.javaparser.utils.LineSeparator;

import java.util.Arrays;
import java.util.List;

import static com.github.javaparser.TokenTypes.eolTokenKind;
import static com.github.javaparser.TokenTypes.spaceTokenKind;

public interface CsmElement {

    void prettyPrint(Node node, SourcePrinter printer);

    static CsmElement child(ObservableProperty property) {
        return new CsmSingleReference(property);
    }

    static CsmElement attribute(ObservableProperty property) {
        return new CsmAttribute(property);
    }

    static CsmElement sequence(CsmElement... elements) {
        return new CsmSequence(Arrays.asList(elements));
    }

    static CsmElement specialJmlMultiCompareExpr() {
        return (node, printer) -> {
            JmlMultiCompareExpr e = (JmlMultiCompareExpr) node;
            for (int i = 0; i < e.getExpressions().size() - 1; i++) {
                var term = e.getExpressions().get(i);
                var op = e.getOperators().get(i);
                printer.print(PrintingHelper.printToString(term));
                space().prettyPrint(null, printer);
                printer.print(PrintingHelper.printToString(op));
                space().prettyPrint(null, printer);
            }
            var term = e.getExpressions().getLast();
            printer.print(PrintingHelper.printToString(term));
        };
    }

    static CsmElement string(int tokenType, String content) {
        return new CsmToken(tokenType, content);
    }

    static CsmElement string(int tokenType) {
        return new CsmToken(tokenType);
    }

    static CsmElement stringToken(ObservableProperty property) {
        return new CsmString(property);
    }

    static CsmElement textBlockToken(ObservableProperty property) {
        return new CsmString(property);
    }

    static CsmElement charToken(ObservableProperty property) {
        return new CsmChar(property);
    }

    static CsmElement token(int tokenType) {
        return new CsmToken(tokenType);
    }

    static CsmElement conditional(
            ObservableProperty property, CsmConditional.Condition condition, CsmElement thenElement) {
        return new CsmConditional(property, condition, thenElement);
    }

    static CsmElement conditional(
            ObservableProperty property,
            CsmConditional.Condition condition,
            CsmElement thenElement,
            CsmElement elseElement) {
        return new CsmConditional(property, condition, thenElement, elseElement);
    }

    static CsmElement conditional(
            List<ObservableProperty> properties,
            CsmConditional.Condition condition,
            CsmElement thenElement,
            CsmElement elseElement) {
        return new CsmConditional(properties, condition, thenElement, elseElement);
    }

    static CsmElement space() {
        return new CsmToken(spaceTokenKind(), " ");
    }

    static CsmElement semicolon() {
        return new CsmToken(GeneratedJavaParserConstants.SEMICOLON);
    }

    static CsmElement comment() {
        return new CsmComment();
    }

    static CsmElement newline() {
        return newline(LineSeparator.SYSTEM);
    }

    static CsmElement newline(LineSeparator lineSeparator) {
        return new CsmToken(eolTokenKind(lineSeparator), lineSeparator.asRawString());
    }

    static CsmElement none() {
        return new CsmNone();
    }

    static CsmElement comma() {
        return new CsmToken(GeneratedJavaParserConstants.COMMA);
    }

    static CsmElement list(ObservableProperty property) {
        return new CsmList(property);
    }

    static CsmElement list(ObservableProperty property, CsmElement separator) {
        return new CsmList(property, CsmElement.none(), separator, new CsmNone(), new CsmNone());
    }

    static CsmElement list(
            ObservableProperty property, CsmElement separator, CsmElement preceeding, CsmElement following) {
        return new CsmList(property, none(), separator, preceeding, following);
    }

    static CsmElement list(
            ObservableProperty property,
            CsmElement separatorPre,
            CsmElement separatorPost,
            CsmElement preceeding,
            CsmElement following) {
        return new CsmList(property, separatorPre, separatorPost, preceeding, following);
    }

    static CsmElement orphanCommentsEnding() {
        return new CsmOrphanCommentsEnding();
    }

    static CsmElement orphanCommentsBeforeThis() {
        // FIXME
        return new CsmNone();
    }

    static CsmElement indent() {
        return new CsmIndent();
    }

    static CsmElement unindent() {
        return new CsmUnindent();
    }

    static CsmElement block(CsmElement content) {
        return sequence(
                token(GeneratedJavaParserConstants.LBRACE),
                indent(),
                content,
                unindent(),
                token(GeneratedJavaParserConstants.RBRACE));
    }

    /*
     * Verifies if the content of the {@code CsmElement} is the same as the provided {@code TextElement}
     */
    default boolean isCorrespondingElement(TextElement textElement) {
        return false;
    }
}
