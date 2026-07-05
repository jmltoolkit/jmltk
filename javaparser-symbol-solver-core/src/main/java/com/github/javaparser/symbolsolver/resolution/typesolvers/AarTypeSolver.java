/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Will let the symbol solver look inside an Android aar file while solving types.
 * (It will look inside the contained classes.jar)
 *
 * @author Federico Tomassetti
 */
public class AarTypeSolver implements TypeSolver {

    private JarTypeSolver delegate;

    public AarTypeSolver(String aarFile) throws IOException {
        this(new File(aarFile));
    }

    public AarTypeSolver(Path aarFile) throws IOException {
        this(aarFile.toFile());
    }

    public AarTypeSolver(File aarFile) throws IOException {
        JarFile jarFile = new JarFile(aarFile);
        ZipEntry classesJarEntry = jarFile.getEntry("classes.jar");
        if (classesJarEntry == null) {
            throw new IllegalArgumentException(String.format(
                    "The given file (%s) is malformed: entry classes.jar was not found", aarFile.getAbsolutePath()));
        }
        delegate = new JarTypeSolver(jarFile.getInputStream(classesJarEntry));
    }

    @Override
    public TypeSolver getParent() {
        return delegate.getParent();
    }

    @Override
    public void setParent(TypeSolver parent) {
        if (parent == this) throw new IllegalStateException("The parent of this TypeSolver cannot be itself.");

        delegate.setParent(parent);
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
        return delegate.tryToSolveType(name);
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeInModule(
            String qualifiedModuleName, String simpleTypeName) {
        return delegate.tryToSolveTypeInModule(qualifiedModuleName, simpleTypeName);
    }
}
