/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.quality;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.utils.SourceRoot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class NotNullGeneratorTest {

    @Test
    void testExecutionOfGenerator() throws Exception {

        // Setup the
        String resourcesFolderPath = getClass().getCanonicalName().replace(".", File.separator);

        String basePath = Paths.get("src", "test", "resources").toString();
        Path originalFile = Paths.get(basePath, resourcesFolderPath, "original");
        Path expectedFile = Paths.get(basePath, resourcesFolderPath, "expected");

        SourceRoot originalSources = new SourceRoot(originalFile);
        SourceRoot expectedSources = new SourceRoot(expectedFile);
        expectedSources.tryToParse();

        // Generate the information
        new NotNullGenerator(originalSources).generate();

        List<CompilationUnit> editedSourceCus = originalSources.getCompilationUnits();
        List<CompilationUnit> expectedSourcesCus = expectedSources.getCompilationUnits();
        assertEquals(expectedSourcesCus.size(), editedSourceCus.size());

        // Check if all the files match the expected result
        for (int i = 0; i < editedSourceCus.size(); i++) {

            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            String expectedCode = printer.print(expectedSourcesCus.get(i));
            String editedCode = printer.print(editedSourceCus.get(i));

            Assertions.assertEquals(expectedCode, editedCode);

            if (!expectedCode.equals(editedCode)) {
                System.out.println("Expected:");
                System.out.println("####");
                System.out.println(expectedSourcesCus.get(i));
                System.out.println("####");
                System.out.println("Actual:");
                System.out.println("####");
                System.out.println(editedSourceCus.get(i));
                System.out.println("####");
                fail("Actual code doesn't match with the expected code.");
            }
        }
    }
}
