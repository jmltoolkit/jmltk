/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.clauses;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.jml.JmlKeyword;

/**
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
public enum JmlBehaviorKind implements Jmlish, JmlKeyword {
    NONE(""),
    BEHAVIOR("behavior"),
    NORMAL("normal_behavior"),
    ABRUPT("abrupt_behavior"),
    EXCEPTIONAL("exceptional_behavior"),
    MODEL("model_behavior"),
    BREAK("break_behavior"),
    CONTINUE("continue_behavior"),
    RETURN("return_behavior");

    private final String symbol;

    JmlBehaviorKind(String symbol) {
        this.symbol = symbol;
    }

    public static JmlBehaviorKind getByToken(JavaToken token) {
        return null;
    }

    @Override
    public String jmlSymbol() {
        return symbol;
    }
}
