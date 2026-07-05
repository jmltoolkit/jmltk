/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import java.util.List;

public abstract class CompilationUnitGenerator extends Generator {

    protected CompilationUnitGenerator(SourceRoot sourceRoot) {
        super(sourceRoot);
    }

    @Override
    public void generate() throws Exception {
        List<ParseResult<CompilationUnit>> parsedCus = sourceRoot.tryToParse();
        for (ParseResult<CompilationUnit> cu : parsedCus) {
            cu.ifSuccessful(this::generateCompilationUnit);
        }
    }

    protected abstract void generateCompilationUnit(CompilationUnit compilationUnit);
}
