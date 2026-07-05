/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.javadoc;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.TraditionalJavadocComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static com.github.javaparser.StaticJavaParser.parse;

@Disabled
class JavadocExtractorTest {

    @Test
    void canParseAllJavadocsInJavaParser() throws FileNotFoundException {
        processDir(new File(".."));
    }

    private void processFile(File file) throws FileNotFoundException {
        try {
            CompilationUnit cu = parse(file);
            new VoidVisitorAdapter<Object>() {
                @Override
                public void visit(TraditionalJavadocComment n, Object arg) {
                    super.visit(n, arg);
                    n.parse();
                }
            }.visit(cu, null);
        } catch (ParseProblemException e) {
            System.err.println("ERROR PROCESSING " + file);
        }
    }

    private void processDir(File dir) throws FileNotFoundException {
        for (File child : dir.listFiles()) {
            if (child.isFile() && child.getName().endsWith(".java")) {
                processFile(child);
            } else if (child.isDirectory()) {
                processDir(child);
            }
        }
    }
}
