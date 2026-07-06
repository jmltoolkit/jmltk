/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import java.util.function.Function;

/**
 * Pretty printer for AST nodes.
 */
public class DefaultPrettyPrinter implements ConfigurablePrinter {

    private PrinterConfiguration configuration;

    // visitor factory
    Function<PrinterConfiguration, VoidVisitor<Void>> visitorFactory;

    // static methods
    private static Function<PrinterConfiguration, VoidVisitor<Void>> createDefaultVisitor() {
        return (config) -> new DefaultPrettyPrinterVisitor(config, new SourcePrinter(config));
    }

    private static PrinterConfiguration createDefaultConfiguration() {
        return new DefaultPrinterConfiguration();
    }

    // Constructors
    /**
     * Build a new DefaultPrettyPrinter with a default configuration and a default factory
     */
    public DefaultPrettyPrinter() {
        this(createDefaultConfiguration());
    }

    /**
     * Build a new DefaultPrettyPrinter with a configuration and a default factory
     *
     * @param configuration
     */
    public DefaultPrettyPrinter(PrinterConfiguration configuration) {
        this(createDefaultVisitor(), configuration);
    }

    /**
     * Build a new DefaultPrettyPrinter with a configuration and a factory to create a visitor to browse the nodes of the AST
     *
     * @param visitorFactory
     * @param configuration  Configuration to apply
     */
    public DefaultPrettyPrinter(Function<PrinterConfiguration, VoidVisitor<Void>> visitorFactory, PrinterConfiguration configuration) {
        this.configuration = configuration;
        this.visitorFactory = visitorFactory;
    }

    // Methods
    /*
     * Returns the Printer configuration
     */
    public PrinterConfiguration getConfiguration() {
        return configuration;
    }

    /*
     * set or update the PrettyPrinter configuration
     */
    public Printer setConfiguration(PrinterConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public String print(Node node) {
        // lazy initialization of visitor which can have a state (like a buffer)
        VoidVisitor<Void> visitor = visitorFactory.apply(configuration);
        node.accept(visitor, null);
        return visitor.toString();
    }
}
