/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.*;
import java.util.stream.Collectors;
import javassist.CtBehavior;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.SignatureAttribute;

public class JavassistMethodLikeDeclarationAdapter {

    private CtBehavior ctBehavior;
    private TypeSolver typeSolver;
    private ResolvedMethodLikeDeclaration declaration;

    private SignatureAttribute.MethodSignature methodSignature;

    public JavassistMethodLikeDeclarationAdapter(
            CtBehavior ctBehavior, TypeSolver typeSolver, ResolvedMethodLikeDeclaration declaration) {
        this.ctBehavior = ctBehavior;
        this.typeSolver = typeSolver;
        this.declaration = declaration;

        try {
            String signature = ctBehavior.getGenericSignature();
            if (signature == null) {
                signature = ctBehavior.getSignature();
            }
            methodSignature = SignatureAttribute.toMethodSignature(signature);
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }

    public int getNumberOfParams() {
        return methodSignature.getParameterTypes().length;
    }

    public ResolvedParameterDeclaration getParam(int i) {
        boolean variadic = false;
        if ((ctBehavior.getModifiers() & javassist.Modifier.VARARGS) > 0) {
            variadic = i == (methodSignature.getParameterTypes().length - 1);
        }

        Optional<String> paramName = JavassistUtils.extractParameterName(ctBehavior, i);
        ResolvedType type =
                JavassistUtils.signatureTypeToType(methodSignature.getParameterTypes()[i], typeSolver, declaration);
        return new JavassistParameterDeclaration(type, typeSolver, variadic, paramName.orElse(null));
    }

    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
        if (ctBehavior.getGenericSignature() == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(methodSignature.getTypeParameters())
                .map(jasTp -> new JavassistTypeParameter(jasTp, declaration, typeSolver))
                .collect(Collectors.toList());
    }

    public int getNumberOfSpecifiedExceptions() {
        ExceptionsAttribute exceptionsAttribute = ctBehavior.getMethodInfo().getExceptionsAttribute();
        if (exceptionsAttribute == null) {
            return 0;
        }

        String[] exceptions = exceptionsAttribute.getExceptions();
        return exceptions == null ? 0 : exceptions.length;
    }

    public ResolvedType getSpecifiedException(int index) {
        if (index < 0) {
            throw new IllegalArgumentException(String.format("index < 0: %d", index));
        }

        ExceptionsAttribute exceptionsAttribute = ctBehavior.getMethodInfo().getExceptionsAttribute();
        if (exceptionsAttribute == null) {
            throw new IllegalArgumentException(
                    String.format("No exception with index %d. Number of exceptions: 0", index));
        }

        String[] exceptions = exceptionsAttribute.getExceptions();
        if (exceptions == null || index >= exceptions.length) {
            throw new IllegalArgumentException(String.format(
                    "No exception with index %d. Number of exceptions: %d", index, getNumberOfSpecifiedExceptions()));
        }

        ResolvedReferenceTypeDeclaration typeDeclaration = typeSolver.solveType(exceptions[index]);
        return new ReferenceTypeImpl(typeDeclaration, Collections.emptyList());
    }

    public ResolvedType getReturnType() {
        return JavassistUtils.signatureTypeToType(methodSignature.getReturnType(), typeSolver, declaration);
    }
}
