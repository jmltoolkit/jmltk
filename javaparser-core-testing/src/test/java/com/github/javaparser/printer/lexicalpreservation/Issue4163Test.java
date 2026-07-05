/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

public class Issue4163Test extends AbstractLexicalPreservingTest {

    @Test
    void test() {
        VoidVisitorAdapter<Object> visitor = new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(MethodDeclaration n, Object arg) {
                System.out.println(n.getDeclarationAsString(true, true, true));
                System.out.println(n.getComment());
            }
        };
        String code = "class Foo {\n" + "		/*\n" + "      * comment\n" + "      */\n" + "		void m() {}\n" + "	}";

        // setup pretty printer to print comments
        PrinterConfiguration config = new DefaultPrinterConfiguration()
                .addOption(new DefaultConfigurationOption(ConfigOption.PRINT_COMMENTS));
        Printer printer = new DefaultPrettyPrinter(config);
        CompilationUnit cu = StaticJavaParser.parse(code);
        MethodDeclaration md = cu.findFirst(MethodDeclaration.class).get();

        // expected result is
        String expected = md.getComment().get().asString() + "\n";

        // set the new pretty printer in the compilation unit
        cu.printer(printer);
        // visit the MethodDeclaration node
        visitor.visit(cu, null);
        // checks that the comment is printed after executing the getDeclarationAsString method
        assertEqualsStringIgnoringEol(expected, md.getComment().get().toString());
    }
}
