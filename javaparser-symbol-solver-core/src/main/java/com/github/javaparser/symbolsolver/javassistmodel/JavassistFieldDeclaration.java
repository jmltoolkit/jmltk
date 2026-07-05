/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParametrizable;
import com.github.javaparser.resolution.types.ResolvedType;

import java.lang.reflect.Modifier;
import javassist.CtField;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;

/**
 * @author Federico Tomassetti
 */
public class JavassistFieldDeclaration implements ResolvedFieldDeclaration {
    private CtField ctField;
    private TypeSolver typeSolver;

    public JavassistFieldDeclaration(CtField ctField, TypeSolver typeSolver) {
        this.ctField = ctField;
        this.typeSolver = typeSolver;
    }

    @Override
    public ResolvedType getType() {
        try {
            String signature = ctField.getGenericSignature();
            if (signature == null) {
                signature = ctField.getSignature();
            }
            SignatureAttribute.Type genericSignatureType = SignatureAttribute.toTypeSignature(signature);
            return JavassistUtils.signatureTypeToType(
                    genericSignatureType, typeSolver, (ResolvedTypeParametrizable) declaringType());
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(ctField.getModifiers());
    }

    @Override
    public boolean isVolatile() {
        return Modifier.isVolatile(ctField.getModifiers());
    }

    @Override
    public String getName() {
        return ctField.getName();
    }

    @Override
    public boolean isField() {
        return true;
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
    public AccessSpecifier accessSpecifier() {
        return JavassistFactory.modifiersToAccessLevel(ctField.getModifiers());
    }

    @Override
    public ResolvedTypeDeclaration declaringType() {
        return JavassistFactory.toTypeDeclaration(ctField.getDeclaringClass(), typeSolver);
    }
}
