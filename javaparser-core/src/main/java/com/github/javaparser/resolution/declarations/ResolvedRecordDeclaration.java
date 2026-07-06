/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import com.github.javaparser.resolution.types.ResolvedReferenceType;

import java.util.List;
import java.util.Optional;

/**
 * Declaration of a Record (not an interface or an enum).
 *
 * Note that it can be associated to a Node AST because anonymous class declarations return an incompatible
 * node type, compared to classic class declarations.
 *
 * @author Federico Tomassetti
 * @author Johannes Coetzee
 */
public interface ResolvedRecordDeclaration
        extends ResolvedReferenceTypeDeclaration, ResolvedTypeParametrizable, HasAccessSpecifier {

    @Override
    default boolean isRecord() {
        return true;
    }

    @Override
    default ResolvedRecordDeclaration asRecord() {
        return this;
    }

    /**
     * This is a ReferenceTypeUsage because it could contain type typeParametersValues.
     * For example: {@code class A extends B<Integer, String>}.
     * <p>
     * Note that only the Object class should not have a superclass and therefore
     * return empty.
     */
    Optional<ResolvedReferenceType> getSuperClass();

    /**
     * Return all the interfaces implemented directly by this class.
     * It does not include the interfaces implemented by superclasses or extended
     * by the interfaces implemented.
     */
    List<ResolvedReferenceType> getInterfaces();

    /**
     * Get all superclasses, with all the type typeParametersValues expressed as functions of the type
     * typeParametersValues of this declaration.
     */
    List<ResolvedReferenceType> getAllSuperClasses();

    /**
     * Return all the interfaces implemented by this class, either directly or indirectly, including the interfaces
     * extended by interfaces it implements.
     * <p>
     * Get all interfaces, with all the type typeParametersValues expressed as functions of the type
     * typeParametersValues of this declaration.
     */
    List<ResolvedReferenceType> getAllInterfaces();

    // /
    // / Constructors
    // /
    /**
     * List of constructors available for the class.
     * This list should also include the default constructor.
     */
    @Override
    List<ResolvedConstructorDeclaration> getConstructors();
}
