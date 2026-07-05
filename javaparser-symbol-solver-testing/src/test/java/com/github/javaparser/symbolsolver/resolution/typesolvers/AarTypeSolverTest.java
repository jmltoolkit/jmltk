/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typesolvers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AarTypeSolverTest extends AbstractTypeSolverTest<AarTypeSolver> {

    private static final Supplier<AarTypeSolver> AAR_SUPLIER = () -> {
        try {
            Path pathToJar = adaptPath("src/test/resources/aars/support-compat-24.2.0.aar");
            return new AarTypeSolver(pathToJar);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create a AarTypeSolver.", e);
        }
    };

    public AarTypeSolverTest() {
        super(AAR_SUPLIER);
    }

    @Test
    void initial() throws IOException {
        Path pathToJar = adaptPath("src/test/resources/aars/support-compat-24.2.0.aar");
        AarTypeSolver aarTypeSolver = new AarTypeSolver(pathToJar);
        assertEquals(
                true,
                aarTypeSolver
                        .tryToSolveType("android.support.v4.app.ActivityCompat")
                        .isSolved());
        assertEquals(
                true,
                aarTypeSolver
                        .tryToSolveType("android.support.v4.app.ActivityManagerCompat")
                        .isSolved());
        assertEquals(
                true,
                aarTypeSolver
                        .tryToSolveType("android.support.v4.app.NotificationCompat")
                        .isSolved());
        assertEquals(
                true,
                aarTypeSolver
                        .tryToSolveType("android.support.v4.app.NotificationCompat.Action")
                        .isSolved());
        assertEquals(
                true,
                aarTypeSolver
                        .tryToSolveType("android.support.v4.app.NotificationCompat.Action.Builder")
                        .isSolved());
        assertEquals(
                false,
                aarTypeSolver
                        .tryToSolveType("com.github.javaparser.ASTParser.Foo")
                        .isSolved());
        assertEquals(
                false, aarTypeSolver.tryToSolveType("com.github.javaparser.Foo").isSolved());
        assertEquals(false, aarTypeSolver.tryToSolveType("Foo").isSolved());
    }
}
