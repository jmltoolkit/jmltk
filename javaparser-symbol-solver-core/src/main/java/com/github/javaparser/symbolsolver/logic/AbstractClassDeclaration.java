/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.logic;

import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.logic.MethodResolutionCapability;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import java.util.ArrayList;
import java.util.List;

/**
 * A common ancestor for all ClassDeclarations.
 *
 * @author Federico Tomassetti
 */
public abstract class AbstractClassDeclaration extends AbstractTypeDeclaration
        implements ResolvedClassDeclaration, MethodResolutionCapability {

    ///
    /// Public
    ///

    @Override
    public boolean hasName() {
        return getQualifiedName() != null;
    }

    @Override
    public final List<ResolvedReferenceType> getAllSuperClasses() {
        List<ResolvedReferenceType> superclasses = new ArrayList<>();

        getSuperClass().ifPresent(superClass -> {
            superclasses.add(superClass);
            superclasses.addAll(superClass.getAllClassesAncestors());
        });

        if (superclasses.removeIf(ResolvedReferenceType::isJavaLangObject)) {
            superclasses.add(object());
        }
        return superclasses;
    }

    @Override
    public final List<ResolvedReferenceType> getAllInterfaces() {
        List<ResolvedReferenceType> interfaces = new ArrayList<>();
        for (ResolvedReferenceType interfaceDeclaration : getInterfaces()) {
            interfaces.add(interfaceDeclaration);
            interfaces.addAll(interfaceDeclaration.getAllInterfacesAncestors());
        }
        getSuperClass().ifPresent(superClass -> {
            interfaces.addAll(superClass.getAllInterfacesAncestors());
        });
        return interfaces;
    }

    @Override
    public final ResolvedClassDeclaration asClass() {
        return this;
    }

    ///
    /// Protected
    ///

    /**
     * An implementation of the Object class.
     */
    protected abstract ResolvedReferenceType object();
}
