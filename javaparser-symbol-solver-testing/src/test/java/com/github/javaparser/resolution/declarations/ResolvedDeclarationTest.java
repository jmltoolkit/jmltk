/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public interface ResolvedDeclarationTest extends AssociableToASTTest {

    ResolvedDeclaration createValue();

    @Test
    default void whenNameIsPresentACallForMethodGetNameShouldNotBeNull() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.hasName()) assertNotNull(resolvedDeclaration.getName());
        else assertNull(resolvedDeclaration.getName());
    }

    @Test
    default void whenDeclarationIsAFieldTheCallToTheMethodAsFieldShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isField()) assertDoesNotThrow(resolvedDeclaration::asField);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asField);
    }

    @Test
    default void whenDeclarationIsAMethodTheCallToTheMethodAsMethodShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isMethod()) assertDoesNotThrow(resolvedDeclaration::asMethod);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asMethod);
    }

    @Test
    default void whenDeclarationIsAParameterTheCallToTheMethodAsParameterShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isParameter()) assertDoesNotThrow(resolvedDeclaration::asParameter);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asParameter);
    }

    @Test
    default void whenDeclarationIsAPatternTheCallToTheMethodAsPatternShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isTypePattern()) assertDoesNotThrow(resolvedDeclaration::asTypePattern);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asTypePattern);
    }

    @Test
    default void whenDeclarationIsAEnumConstantTheCallToTheMethodAsEnumConstantShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isEnumConstant()) assertDoesNotThrow(resolvedDeclaration::asEnumConstant);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asEnumConstant);
    }

    @Test
    default void whenDeclarationIsATypeTheCallToTheMethodAsTypeShouldNotThrow() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        if (resolvedDeclaration.isType()) assertDoesNotThrow(resolvedDeclaration::asType);
        else assertThrows(UnsupportedOperationException.class, resolvedDeclaration::asType);
    }

    /**
     * According to the documentation in {@link AssociableToAST#toAst()}
     * all the Resolved declaration most be associable to a AST.
     *
     * @see AssociableToAST#toAst()
     */
    @Test
    default void declarationMostBeAssociableToAST() {
        ResolvedDeclaration resolvedDeclaration = createValue();
        assertTrue(resolvedDeclaration instanceof AssociableToAST);
    }
}
