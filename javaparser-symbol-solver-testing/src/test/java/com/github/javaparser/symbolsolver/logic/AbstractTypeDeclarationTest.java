/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.logic;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclarationTest;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractTypeDeclarationTest extends AbstractSymbolResolutionTest
        implements ResolvedReferenceTypeDeclarationTest {

    @Override
    public abstract AbstractTypeDeclaration createValue();

    /**
     * Should say if an {@link AbstractTypeDeclaration} is functional interface.
     *
     * @param typeDeclaration The type declaration to check.
     *
     * @return {@code True}
     */
    public abstract boolean isFunctionalInterface(AbstractTypeDeclaration typeDeclaration);

    // TODO: Fix implementation
    @Disabled(value = "JavaParserTypeDeclaration doesn't implement the getAncestors method.")
    @Test
    void checkIfMatchedExpectedFunctionalInterface() {
        AbstractTypeDeclaration abstractTypeDeclaration = createValue();
        assertEquals(isFunctionalInterface(abstractTypeDeclaration), abstractTypeDeclaration.isFunctionalInterface());
    }
}
