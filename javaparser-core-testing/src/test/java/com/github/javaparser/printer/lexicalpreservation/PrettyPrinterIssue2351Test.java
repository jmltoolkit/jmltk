/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.body.EnumDeclaration;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

// manage java.lang.UnsupportedOperationException: Csm token token(}) NodeText TOKEN ";"   <102>   (line 1,col 39)-(line
// 1,col 39)
class PrettyPrinterIssue2351Test extends AbstractLexicalPreservingTest {

    @Test
    void printingEnum2() {
        String def2 = "public enum RandomEnum {" + " TYPE_1;" + "}";
        considerCode(def2);
        Optional<EnumDeclaration> decl = cu.findFirst(EnumDeclaration.class);
        if (decl.isPresent()) decl.get().addEnumConstant("SOMETHING");
        assertTrue(decl.get().getEntries().size() == 2);
    }
}
