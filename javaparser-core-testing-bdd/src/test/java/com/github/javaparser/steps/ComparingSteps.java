/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.steps;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;

public class ComparingSteps {

    private CompilationUnit first;
    private CompilationUnit second;

    /*
     * Given steps
     */

    @Given("the first class:$classSrc")
    public void givenTheFirstClass(String classSrc) {
        this.first = parse(classSrc.trim());
    }

    @Given("the second class:$classSrc")
    public void givenTheSecondClass(String classSrc) {
        this.second = parse(classSrc.trim());
    }

    /*
     * Then steps
     */

    @Then("they are equals")
    public void thenTheyAreEquals() {
        assertThat(first, is(equalTo(second)));
    }
}
