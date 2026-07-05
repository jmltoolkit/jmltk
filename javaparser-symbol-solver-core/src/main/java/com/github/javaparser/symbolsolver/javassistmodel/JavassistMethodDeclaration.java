/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.TypeVariableResolutionCapability;
import com.github.javaparser.symbolsolver.declarations.common.MethodDeclarationCommonLogic;
import java.lang.reflect.Modifier;
import java.util.List;
import javassist.CtMethod;

/**
 * @author Federico Tomassetti
 */
public class JavassistMethodDeclaration implements ResolvedMethodDeclaration, TypeVariableResolutionCapability {
    private CtMethod ctMethod;
    private TypeSolver typeSolver;
    private final JavassistMethodLikeDeclarationAdapter methodLikeAdaper;

    public JavassistMethodDeclaration(CtMethod ctMethod, TypeSolver typeSolver) {
        this.ctMethod = ctMethod;
        this.typeSolver = typeSolver;
        this.methodLikeAdaper = new JavassistMethodLikeDeclarationAdapter(ctMethod, typeSolver, this);
    }

    @Override
    public boolean isDefaultMethod() {
        return ctMethod.getDeclaringClass().isInterface() && !isAbstract();
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(ctMethod.getModifiers());
    }

    @Override
    public String toString() {
        return "JavassistMethodDeclaration{" + "ctMethod=" + ctMethod + '}';
    }

    @Override
    public String getName() {
        return ctMethod.getName();
    }

    @Override
    public boolean isField() {
        return false;
    }

    @Override
    public boolean isParameter() {
        return false;
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public ResolvedReferenceTypeDeclaration declaringType() {
        return JavassistFactory.toTypeDeclaration(ctMethod.getDeclaringClass(), typeSolver);
    }

    @Override
    public ResolvedType getReturnType() {
        return methodLikeAdaper.getReturnType();
    }

    @Override
    public int getNumberOfParams() {
        return methodLikeAdaper.getNumberOfParams();
    }

    @Override
    public ResolvedParameterDeclaration getParam(int i) {
        return methodLikeAdaper.getParam(i);
    }

    public MethodUsage getUsage(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodUsage resolveTypeVariables(Context context, List<ResolvedType> parameterTypes) {
        return new MethodDeclarationCommonLogic(this, typeSolver).resolveTypeVariables(context, parameterTypes);
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(ctMethod.getModifiers());
    }

    @Override
    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
        return methodLikeAdaper.getTypeParameters();
    }

    @Override
    public AccessSpecifier accessSpecifier() {
        return JavassistFactory.modifiersToAccessLevel(ctMethod.getModifiers());
    }

    @Override
    public int getNumberOfSpecifiedExceptions() {
        return methodLikeAdaper.getNumberOfSpecifiedExceptions();
    }

    @Override
    public ResolvedType getSpecifiedException(int index) {
        return methodLikeAdaper.getSpecifiedException(index);
    }

    @Override
    public String toDescriptor() {
        return ctMethod.getMethodInfo().getDescriptor();
    }
}
