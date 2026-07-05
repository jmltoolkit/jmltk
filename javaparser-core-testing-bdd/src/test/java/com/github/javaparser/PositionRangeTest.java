/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.steps.PositionRangeSteps;
import com.github.javaparser.steps.SharedSteps;
import org.jbehave.core.junit.JUnit4StoryRunner;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4StoryRunner.class)
public class PositionRangeTest extends BasicJBehaveTest {

    @Override
    public InjectableStepsFactory stepsFactory() {
        Map<String, Object> state = new HashMap<>();

        return new InstanceStepsFactory(configuration(), new SharedSteps(state), new PositionRangeSteps());
    }

    public PositionRangeTest() {
        super("**/position_range*.story");
    }
}
