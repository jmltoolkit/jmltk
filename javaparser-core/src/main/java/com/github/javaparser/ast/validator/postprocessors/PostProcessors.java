/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.postprocessors;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Processor;
import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A post processor that will call a collection of post processors.
 */
public class PostProcessors {

    private final List<Processor> postProcessors = new ArrayList<>();

    public PostProcessors(Processor... postProcessors) {
        this.postProcessors.addAll(Arrays.asList(postProcessors));
    }

    public List<Processor> getPostProcessors() {
        return postProcessors;
    }

    public PostProcessors remove(Processor postProcessor) {
        if (!postProcessors.remove(postProcessor)) {
            throw new AssertionError("Trying to remove a post processor that isn't there.");
        }
        return this;
    }

    public PostProcessors replace(Processor oldProcessor, Processor newProcessor) {
        remove(oldProcessor);
        add(newProcessor);
        return this;
    }

    public PostProcessors add(Processor newProcessor) {
        postProcessors.add(newProcessor);
        return this;
    }

    public void postProcess(ParseResult<? extends Node> result, ParserConfiguration configuration) {
        postProcessors.forEach(pp -> pp.postProcess(result, configuration));
    }
}
