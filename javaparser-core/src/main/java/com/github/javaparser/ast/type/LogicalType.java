/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.type;

import static com.github.javaparser.utils.Utils.assertNotNull;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.OptionalProperty;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A class or an interface type.
 * <br>{@code Object}
 * <br>{@code HashMap<String, String>}
 * <br>{@code java.util.Punchcard}
 * <p>
 * <p>Note that the syntax is ambiguous here, and JavaParser does not know what is to the left of the class. It assumes
 * cases like {@code Map.Entry} where Map is the scope of Entry. In {@code java.util.Punchcard}, it will not
 * recognize that java and util are parts of the package name. Instead, it will set util as the scope of Punchcard, as a
 * ClassOrInterfaceType (which it is not). In turn, util will have java as its scope, also as a
 * ClassOrInterfaceType</p>
 *
 * @author Julio Vilmar Gesser
 */
public class LogicalType extends ReferenceType
        implements NodeWithSimpleName<LogicalType>,
                NodeWithAnnotations<LogicalType>,
                NodeWithTypeArguments<LogicalType> {

    private SimpleName name;

    @OptionalProperty
    private NodeList<Type> typeArguments;

    public LogicalType() {
        this(null, new SimpleName(), null, new NodeList<>());
    }

    /**
     * @deprecated use JavaParser.parseClassOrInterfaceType instead. This constructor does not understand generics.
     */
    public LogicalType(final String name) {
        this(null, new SimpleName(name), null, new NodeList<>());
    }

    public LogicalType(final SimpleName name, final NodeList<Type> typeArguments) {
        this(null, name, typeArguments, new NodeList<>());
    }

    @AllFieldsConstructor
    public LogicalType(
            final SimpleName name, final NodeList<Type> typeArguments, final NodeList<AnnotationExpr> annotations) {
        this(null, name, typeArguments, annotations);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public LogicalType(
            TokenRange tokenRange,
            SimpleName name,
            NodeList<Type> typeArguments,
            NodeList<AnnotationExpr> annotations) {
        super(tokenRange, annotations);
        setName(name);
        setTypeArguments(typeArguments);
        customInitialization();
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
    public SimpleName getName() {
        return name;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public LogicalType setName(final SimpleName name) {
        assertNotNull(name);
        if (name == this.name) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null) this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<NodeList<Type>> getTypeArguments() {
        return Optional.ofNullable(typeArguments);
    }

    /**
     * Sets the typeArguments
     *
     * @param typeArguments the typeArguments, can be null
     * @return this, the ClassOrInterfaceType
     */
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public LogicalType setTypeArguments(final NodeList<Type> typeArguments) {
        if (typeArguments == this.typeArguments) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.TYPE_ARGUMENTS, this.typeArguments, typeArguments);
        if (this.typeArguments != null) this.typeArguments.setParentNode(null);
        this.typeArguments = typeArguments;
        setAsParentNodeOf(typeArguments);
        return this;
    }

    @Override
    public LogicalType setAnnotations(NodeList<AnnotationExpr> annotations) {
        return (LogicalType) super.setAnnotations(annotations);
    }

    /*
     * Note that the internal forms of the binary names of object are used.
     * for example java/lang/Object
     */
    @Override
    public String toDescriptor() {
        return String.format(
                "S%s;", resolve().asReferenceType().getQualifiedName().replace(".", "/"));
    }

    @Override
    public String asString() {
        return name.toString();
    }

    @Override
    public ResolvedType resolve() {
        return null;
    }

    /**
     * Convert a {@link LogicalType} into a {@link ResolvedType}.
     *
     * @param context               The current context.
     *
     * @return The type resolved.
     */
    @Override
    public ResolvedType convertToUsage(Context context) {
        String name = getNameAsString();
        SymbolReference<ResolvedTypeDeclaration> ref = context.solveType(name);
        if (!ref.isSolved()) {
            throw new UnsolvedSymbolException(name);
        }
        ResolvedTypeDeclaration typeDeclaration = ref.getCorrespondingDeclaration();
        List<ResolvedType> typeParameters = Collections.emptyList();
        if (getTypeArguments().isPresent()) {
            typeParameters = getTypeArguments().get().stream()
                    .map((pt) -> pt.convertToUsage(context))
                    .collect(Collectors.toList());
        }
        if (typeDeclaration.isTypeParameter()) {
            return new ResolvedTypeVariable(typeDeclaration.asTypeParameter());
        }
        return new ReferenceTypeImpl((ResolvedReferenceTypeDeclaration) typeDeclaration, typeParameters);
    }
}
