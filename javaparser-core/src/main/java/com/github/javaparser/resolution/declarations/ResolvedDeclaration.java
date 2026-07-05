/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

/**
 * A generic declaration.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedDeclaration extends AssociableToAST {

    /**
     * Anonymous classes do not have a name, for example.
     */
    default boolean hasName() {
        return true;
    }

    /**
     * Should return the name or return null if the name is not available.
     */
    String getName();

    /**
     * Does this declaration represents a class field?
     */
    default boolean isField() {
        return false;
    }

    /**
     * Does this declaration represents a variable?
     */
    default boolean isVariable() {
        return false;
    }

    /**
     * Does this declaration represents an enum constant?
     */
    default boolean isEnumConstant() {
        return false;
    }

    /**
     * Does this declaration represents a pattern declaration?
     */
    default boolean isTypePattern() {
        return false;
    }

    /**
     * Does this declaration represents a method parameter?
     */
    default boolean isParameter() {
        return false;
    }

    /**
     * Does this declaration represents a type?
     */
    default boolean isType() {
        return false;
    }

    /**
     * Does this declaration represents a method?
     * // FIXME: This is never overridden.
     */
    default boolean isMethod() {
        return false;
    }

    /**
     * Does this declaration represent the array length pseudo-field?
     *
     * array.length is the only synthetic pseudo-field defined by the JLS (§10.7).
     * It is a singleton with a fixed name and a fixed type (int), so a dedicated interface would carry no meaningful contract beyond the flag itself.
     * The existing codebase already follows this lighter approach: isVariable() in ResolvedDeclaration has no companion ResolvedVariableDeclaration interface.
     * Introducing an empty marker interface just to be symmetric with isField() / isEnumConstant() would be over-engineering for a one-off case.
     *
     * The fix is therefore:
     * Add default boolean isArrayLength() { return false; } to ResolvedDeclaration
     * Override it to return true inside ArrayLengthValueDeclaration
     *
     * If more extensibility were needed in the future — for instance, to expose getArrayType() to retrieve the component type of the backing array — the right move would be to introduce a ResolvedArrayLengthDeclaration interface at that point.
     * Because all new methods on ResolvedDeclaration are default, and because ArrayLengthValueDeclaration already implements ResolvedValueDeclaration, that refactor could be done without any breaking change.
     */
    default boolean isArrayLength() {
        return false;
    }

    /**
     * Return this as a FieldDeclaration or throw an UnsupportedOperationException
     */
    default ResolvedFieldDeclaration asField() {
        throw new UnsupportedOperationException(String.format("%s is not a FieldDeclaration", this));
    }

    /**
     * Return this as a ParameterDeclaration or throw an UnsupportedOperationException
     */
    default ResolvedParameterDeclaration asParameter() {
        throw new UnsupportedOperationException(String.format("%s is not a ParameterDeclaration", this));
    }

    /**
     * Return this as a TypeDeclaration or throw an UnsupportedOperationException
     */
    default ResolvedTypeDeclaration asType() {
        throw new UnsupportedOperationException(String.format("%s is not a TypeDeclaration", this));
    }

    /**
     * Return this as a MethodDeclaration or throw an UnsupportedOperationException
     * // FIXME: This is never overridden.
     */
    default ResolvedMethodDeclaration asMethod() {
        throw new UnsupportedOperationException(String.format("%s is not a MethodDeclaration", this));
    }

    /**
     * Return this as a EnumConstantDeclaration or throw an UnsupportedOperationException
     */
    default ResolvedEnumConstantDeclaration asEnumConstant() {
        throw new UnsupportedOperationException(String.format("%s is not an EnumConstantDeclaration", this));
    }

    /**
     * Return this as a PatternDeclaration or throw an UnsupportedOperationException
     */
    default ResolvedTypePatternDeclaration asTypePattern() {
        throw new UnsupportedOperationException(String.format("%s is not a Pattern", this));
    }
}
