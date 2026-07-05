/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.steps.CommentParsingSteps;
import org.jbehave.core.junit.JUnit4StoryRunner;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.runner.RunWith;

@RunWith(JUnit4StoryRunner.class)
public class CommentParsingTest extends BasicJBehaveTest {

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new InstanceStepsFactory(configuration(), new CommentParsingSteps());
    }

    public CommentParsingTest() {
        super("**/comment*.story");
    }
}
