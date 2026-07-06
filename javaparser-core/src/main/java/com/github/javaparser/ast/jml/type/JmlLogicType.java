/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.type;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.jml.JmlKeyword;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * A primitive type in JML.
 */
public class JmlLogicType extends Type implements Jmlish {

    public enum Primitive implements JmlKeyword {
        SET("\\set"),
        SEQ("\\seq"),
        MAP("\\map"),
        BIGINT("\\bigint"),
        BIGFLOAT("\\bigfloat");

        final String symbol;

        public String jmlSymbol() {
            return symbol;
        }

        Primitive(String symbol) {
            this.symbol = symbol;
        }
    }

    private final Primitive type;

    public JmlLogicType() {
        this(null, Primitive.BIGINT);
    }

    @AllFieldsConstructor
    public JmlLogicType(Primitive type) {
        this(null, type);
    }

    public JmlLogicType(TokenRange range, final Primitive type) {
        super(range);
        this.type = type;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return null;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <A> void accept(final VoidVisitor<A> v, final A arg) {}

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Primitive getType() {
        return type;
    }

    @Override
    public String toDescriptor() {
        return type.symbol;
    }

    @Override
    public String asString() {
        return toDescriptor();
    }

    @Override
    public ResolvedPrimitiveType resolve() {
        return getSymbolResolver().toResolvedType(this, ResolvedPrimitiveType.class);
    }

    @Override
    public ResolvedType convertToUsage(Context context) {
        return resolve();
    }
}
