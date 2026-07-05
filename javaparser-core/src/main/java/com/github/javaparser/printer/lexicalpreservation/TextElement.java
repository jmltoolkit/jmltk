/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import java.util.Optional;

public abstract class TextElement implements TextElementMatcher, PrintableTextElement {

    abstract String expand();

    abstract boolean isToken(int tokenKind);

    final boolean isCommentToken() {
        return isToken(GeneratedJavaParserConstants.JAVADOC_COMMENT)
                || isToken(GeneratedJavaParserConstants.SINGLE_LINE_COMMENT)
                || isToken(GeneratedJavaParserConstants.MULTI_LINE_COMMENT);
    }

    @Override
    public boolean match(TextElement textElement) {
        return this.equals(textElement);
    }

    abstract boolean isNode(Node node);

    public abstract boolean isLiteral();

    public abstract boolean isWhiteSpace();

    public abstract boolean isSpaceOrTab();

    public abstract boolean isNewline();

    public abstract boolean isComment();

    public abstract boolean isSeparator();

    public abstract boolean isIdentifier();

    public abstract boolean isKeyword();

    public abstract boolean isPrimitive();

    public final boolean isWhiteSpaceOrComment() {
        return isWhiteSpace() || isComment();
    }

    /**
     * Is this TextElement representing a child of the given class?
     */
    public abstract boolean isChildOfClass(Class<? extends Node> nodeClass);

    public boolean isChild() {
        return isChildOfClass(Node.class);
    }

    abstract Optional<Range> getRange();

    /**
     * Creates a {@link TextElementMatcher} that matches any TextElement with the same range as this TextElement.<br>
     * This can be used to curry another TextElementMatcher.<br>
     * e.g. {@code someTextElementMatcher.and(textElement.matchByRange());}
     *
     * @return TextElementMatcher that matches any TextElement with the same Range
     */
    TextElementMatcher matchByRange() {
        return ( // We're missing range information. This may happen when a node is manually instantiated. Don't be too
                // harsh on that:
                TextElement textElement) ->
                getRange().flatMap(r1 -> textElement.getRange().map(r1::equals)).orElse(true);
    }
}
