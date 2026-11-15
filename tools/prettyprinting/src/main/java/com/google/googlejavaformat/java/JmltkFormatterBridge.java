/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.google.googlejavaformat.java;

import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Doc;
import com.google.googlejavaformat.DocBuilder;
import com.google.googlejavaformat.OpsBuilder;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Calls the package-private google-java-format pieces needed to run the formatter with a custom
 * {@link CommentsHelper}. This shim lives in google-java-format's package only because {@code
 * Trees.parse} is package-private; it is not a fork of the formatter.
 */
public final class JmltkFormatterBridge {
    private JmltkFormatterBridge() {}

    public static String format(String text, CommentsHelper helper, int maxWidth) throws FormatterException {
        JavaInput input = new JavaInput(text);
        Context context = new Context();
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        JCCompilationUnit unit = Trees.parse(context, errors, false, input.getText());
        input.setCompilationUnit(unit);
        if (!errors.isEmpty()) {
            throw FormatterException.fromJavacDiagnostics(errors);
        }

        JavaOutput output = new JavaOutput("\n", input, helper);
        OpsBuilder builder = new OpsBuilder(input, output);
        JavaInputAstVisitor visitor =
                new JavaInputAstVisitor(builder, JavaFormatterOptions.defaultOptions().indentationMultiplier());
        visitor.scan(unit, null);
        builder.sync(input.getText().length());
        builder.drain();

        Doc doc = new DocBuilder().withOps(builder.build()).build();
        doc.computeBreaks(output.getCommentsHelper(), maxWidth, new Doc.State(+0, 0));
        doc.write(output);
        output.flush();

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < output.getLineCount(); i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(output.getLine(i));
        }
        return result.append('\n').toString();
    }
}
