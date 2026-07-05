/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.steps;

import static com.github.javaparser.StaticJavaParser.*;
import static com.github.javaparser.utils.Utils.normalizeEolInTextBlock;
import static com.github.javaparser.utils.Utils.readerToString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.utils.LineSeparator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class PrettyPrintingSteps {

    private Node resultNode;
    private String sourceUnderTest;

    @Given(
            "the {class|compilation unit|expression|block|statement|import|annotation|body|class body|interface body}:$classSrc")
    public void givenTheClass(String classSrc) {
        this.sourceUnderTest = classSrc.trim();
    }

    @Given(
            "the {class|compilation unit|expression|block|statement|import|annotation|body|class body|interface body} in the file \"$classFile\"")
    public void givenTheClassInTheFile(String classFile) throws URISyntaxException, IOException {
        URL url = getClass().getResource("../samples/" + classFile);
        sourceUnderTest = readerToString(new FileReader(new File(url.toURI()))).trim();
    }

    @When("the {class|compilation unit} is parsed by the Java parser")
    public void whenTheClassIsParsedByTheJavaParser() {
        resultNode = parse(sourceUnderTest);
    }

    @When("the expression is parsed by the Java parser")
    public void whenTheExpressionIsParsedByTheJavaParser() {
        resultNode = parseExpression(sourceUnderTest);
    }

    @When("the block is parsed by the Java parser")
    public void whenTheBlockIsParsedByTheJavaParser() {
        resultNode = parseBlock(sourceUnderTest);
    }

    @When("the statement is parsed by the Java parser")
    public void whenTheStatementIsParsedByTheJavaParser() {
        resultNode = parseStatement(sourceUnderTest);
    }

    @When("the import is parsed by the Java parser")
    public void whenTheImportIsParsedByTheJavaParser() {
        resultNode = parseImport(sourceUnderTest);
    }

    @When("the annotation is parsed by the Java parser")
    public void whenTheAnnotationIsParsedByTheJavaParser() {
        resultNode = parseAnnotation(sourceUnderTest);
    }

    @When("the annotation body declaration is parsed by the Java parser")
    public void whenTheBodyDeclarationIsParsedByTheJavaParser() {
        resultNode = parseAnnotationBodyDeclaration(sourceUnderTest);
    }

    @When("the class body declaration is parsed by the Java parser")
    public void whenTheClassBodyDeclarationIsParsedByTheJavaParser() {
        resultNode = parseBodyDeclaration(sourceUnderTest);
    }

    @When("the interface body declaration is parsed by the Java parser")
    public void whenTheInterfaceBodyDeclarationIsParsedByTheJavaParser() {
        resultNode = parseBodyDeclaration(sourceUnderTest);
    }

    @When("the class is visited by an empty ModifierVisitorAdapter")
    public void whenTheClassIsVisitedByAnEmptyModifierVisitorAdapter() {
        (new ModifierVisitor<Void>() {}).visit((CompilationUnit) resultNode, null);
    }

    @Then("it is printed as:$src")
    public void isPrintedAs(String src) {
        assertEquals(
                normalizeEolInTextBlock(src.trim(), LineSeparator.ARBITRARY),
                normalizeEolInTextBlock(resultNode.toString().trim(), LineSeparator.ARBITRARY));
    }
}
