/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest;

import java.io.InputStream;

/**
 * @author Federico Tomassetti
 */
public abstract class AbstractResolutionTest extends AbstractSymbolResolutionTest {

    protected CompilationUnit parseSampleWithStandardExtension(String sampleName) {
        return parseSample(sampleName, "java");
    }

    protected CompilationUnit parseSample(String sampleName) {
        return parseSample(sampleName, "java.txt");
    }

    private CompilationUnit parseSample(String sampleName, String extension) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(sampleName + "." + extension);
        if (is == null) {
            throw new RuntimeException("Unable to find sample " + sampleName);
        }
        return StaticJavaParser.parse(is);
    }

    protected CompilationUnit parseSampleWithStandardExtension(String sampleName, TypeSolver typeSolver) {
        return parseSample(sampleName, "java", typeSolver);
    }

    protected CompilationUnit parseSample(String sampleName, TypeSolver typeSolver) {
        return parseSample(sampleName, "java.txt", typeSolver);
    }

    private CompilationUnit parseSample(String sampleName, String extension, TypeSolver typeSolver) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(sampleName + "." + extension);
        if (is == null) {
            throw new RuntimeException("Unable to find sample " + sampleName);
        }
        JavaParser javaParser = createParserWithResolver(typeSolver);
        return javaParser
                .parse(is)
                .getResult()
                .orElseThrow(() -> new IllegalArgumentException("Sample does not parse: " + sampleName));
    }

    protected JavaParser createParserWithResolver(TypeSolver typeSolver) {
        return new JavaParser(new ParserConfiguration().setSymbolResolver(symbolResolver(typeSolver)));
    }
}
