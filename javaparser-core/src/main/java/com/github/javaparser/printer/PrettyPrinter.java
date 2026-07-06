/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.key.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import java.util.function.Function;

/**
 * Pretty printer for AST nodes.
 * This class is no longer acceptable to use because it is not sufficiently configurable and it is too tied to a specific implementation
 * <p> Use {@link Printer interface or DefaultPrettyPrinter default implementation } instead.
 *
 * @deprecated This class could be removed in a future version. Use defaultPrettyPrinter.
 */
@Deprecated
public class PrettyPrinter implements ConfigurablePrinter {

    private PrinterConfiguration configuration;

    private Function<PrettyPrinterConfiguration, VoidVisitor<Void>> visitorFactory;

    public PrettyPrinter() {
        this(new PrettyPrinterConfiguration());
    }

    public PrettyPrinter(PrettyPrinterConfiguration configuration) {
        this(configuration, PrettyPrintVisitor::new);
    }

    public PrettyPrinter(PrettyPrinterConfiguration configuration, Function<PrettyPrinterConfiguration, VoidVisitor<Void>> visitorFactory) {
        this.configuration = configuration;
        this.visitorFactory = visitorFactory;
    }

    /*
     * Returns the PrettyPrinter configuration
     */
    public PrinterConfiguration getConfiguration() {
        return configuration;
    }

    /*
     * set or update the PrettyPrinter configuration
     */
    public Printer setConfiguration(PrinterConfiguration configuration) {
        if (!(configuration instanceof PrettyPrinterConfiguration))
            throw new IllegalArgumentException("PrettyPrinter must be configured with a PrettyPrinterConfiguration class");
        this.configuration = configuration;
        return this;
    }

    @Override
    public String print(Node node) {
        final VoidVisitor<Void> visitor = visitorFactory.apply((PrettyPrinterConfiguration) configuration);
        node.accept(visitor, null);
        return visitor.toString();
    }
}
