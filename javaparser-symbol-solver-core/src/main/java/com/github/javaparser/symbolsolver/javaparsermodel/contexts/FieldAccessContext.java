/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import static com.github.javaparser.resolution.Navigator.demandParentNode;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.Value;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Federico Tomassetti
 */
public class FieldAccessContext extends ExpressionContext<FieldAccessExpr> {

    private static final String ARRAY_LENGTH_FIELD_NAME = "length";

    public FieldAccessContext(FieldAccessExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        if (wrappedNode.getName().toString().equals(name)) {
            if (wrappedNode.getScope() instanceof ThisExpr) {
                ResolvedType typeOfThis = JavaParserFacade.get(typeSolver).getTypeOfThisIn(wrappedNode);
                if (typeOfThis.asReferenceType().getTypeDeclaration().isPresent()) {
                    return new SymbolSolver(typeSolver)
                            .solveSymbolInType(
                                    typeOfThis
                                            .asReferenceType()
                                            .getTypeDeclaration()
                                            .get(),
                                    name);
                }
            }
        }
        return super.solveSymbol(name);
    }

    @Override
    public SymbolReference<ResolvedTypeDeclaration> solveType(String name, List<ResolvedType> typeArguments) {
        return solveTypeInParentContext(name, typeArguments);
    }

    @Override
    public SymbolReference<ResolvedMethodDeclaration> solveMethod(
            String name,
            List<ResolvedType> parameterTypes,
            boolean staticOnly,
            ResolvedReferenceTypeDeclaration invocationContext) {
        return JavaParserFactory.getContext(demandParentNode(wrappedNode), typeSolver)
                .solveMethod(name, parameterTypes, false, invocationContext);
    }

    @Override
    public Optional<Value> solveSymbolAsValue(String name) {
        Expression scope = wrappedNode.getScope();
        if (wrappedNode.getName().toString().equals(name)) {
            ResolvedType typeOfScope = JavaParserFacade.get(typeSolver).getType(scope);
            if (typeOfScope.isArray() && ARRAY_LENGTH_FIELD_NAME.equals(name)) {
                return Optional.of(new Value(ResolvedPrimitiveType.INT, ARRAY_LENGTH_FIELD_NAME));
            }
            if (typeOfScope.isReferenceType()) {
                return solveSymbolAsValue(name, typeOfScope.asReferenceType());
            }
            if (typeOfScope.isConstraint()) {
                return solveSymbolAsValue(
                        name, typeOfScope.asConstraintType().getBound().asReferenceType());
            }
            return Optional.empty();
        }
        return super.solveSymbolAsValue(name);
    }

    /*
     * Try to resolve the name parameter as a field of the reference type
     */
    private Optional<Value> solveSymbolAsValue(String name, ResolvedReferenceType type) {
        Optional<ResolvedReferenceTypeDeclaration> optionalTypeDeclaration = type.getTypeDeclaration();
        if (optionalTypeDeclaration.isPresent()) {
            ResolvedReferenceTypeDeclaration typeDeclaration = optionalTypeDeclaration.get();
            if (typeDeclaration.isEnum()) {
                ResolvedEnumDeclaration enumDeclaration = (ResolvedEnumDeclaration) typeDeclaration;
                if (enumDeclaration.hasEnumConstant(name)) {
                    return Optional.of(
                            new Value(enumDeclaration.getEnumConstant(name).getType(), name));
                }
            }
        }
        Optional<ResolvedType> typeUsage = type.getFieldType(name);
        return typeUsage.map(resolvedType -> new Value(resolvedType, name));
    }

    public SymbolReference<ResolvedValueDeclaration> solveField(String name) {
        Collection<ResolvedReferenceTypeDeclaration> rrtds = findTypeDeclarations(Optional.of(wrappedNode.getScope()));
        for (ResolvedReferenceTypeDeclaration rrtd : rrtds) {
            if (rrtd.isEnum()) {
                Optional<ResolvedEnumConstantDeclaration> enumConstant = rrtd.asEnum().getEnumConstants().stream()
                        .filter(c -> c.getName().equals(name))
                        .findFirst();
                if (enumConstant.isPresent()) {
                    return SymbolReference.solved(enumConstant.get());
                }
            }
            try {
                return SymbolReference.solved(
                        rrtd.getField(wrappedNode.getName().getId()));
            } catch (Throwable t) {
            }
        }
        return SymbolReference.unsolved();
    }
}
