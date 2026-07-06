/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.printer.SourcePrinter;
import static com.github.javaparser.utils.CodeGenerationUtils.f;

public class CsmAttribute implements CsmElement {

    public ObservableProperty getProperty() {
        return property;
    }

    private final ObservableProperty property;

    public CsmAttribute(ObservableProperty property) {
        this.property = property;
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        Object value = property.getRawValue(node);
        printer.print(PrintingHelper.printToString(value));
    }

    /**
     * Obtain the token type corresponding to the specific value of the attribute.
     * For example, to the attribute "Operator" different token could correspond like PLUS or MINUS.
     *
     * @param tokenText Operator's token text
     */
    public int getTokenType(Node node, String text, String tokenText) {
        switch(property) {
            case IDENTIFIER:
                return GeneratedJavaParserConstants.IDENTIFIER;
            case TYPE:
                {
                    String expectedImage = "\"" + text.toLowerCase() + "\"";
                    for (int i = 0; i < GeneratedJavaParserConstants.tokenImage.length; i++) {
                        if (GeneratedJavaParserConstants.tokenImage[i].equals(expectedImage)) {
                            return i;
                        }
                    }
                    throw new RuntimeException(f("Attribute '%s' does not corresponding to any expected value. Text: %s", property.camelCaseName(), text));
                }
            case KEYWORD:
            case OPERATOR:
                {
                    String expectedImage = "\"" + tokenText.toLowerCase() + "\"";
                    for (int i = 0; i < GeneratedJavaParserConstants.tokenImage.length; i++) {
                        if (GeneratedJavaParserConstants.tokenImage[i].equals(expectedImage)) {
                            return i;
                        }
                    }
                    throw new RuntimeException(f("Attribute '%s' does not corresponding to any expected value. Text: %s", property.camelCaseName(), tokenText));
                }
            case VALUE:
                if (node instanceof IntegerLiteralExpr) {
                    return GeneratedJavaParserConstants.INTEGER_LITERAL;
                }
            case NAME:
                return GeneratedJavaParserConstants.IDENTIFIER;
        }
        throw new UnsupportedOperationException("getTokenType does not know how to handle property " + property + " with text: " + text);
    }

    @Override
    public String toString() {
        return String.format("%s(property:%s)", this.getClass().getSimpleName(), getProperty());
    }
}
