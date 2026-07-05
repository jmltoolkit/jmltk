/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.other;

import com.github.javaparser.generator.Generator;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Generates the bnd.bnd file in javaparser-core.
 */
public class BndGenerator extends Generator {

    public BndGenerator(SourceRoot sourceRoot) {
        super(sourceRoot);
    }

    @Override
    public void generate() throws IOException {
        Log.info("Running %s", () -> getClass().getSimpleName());
        Path root = sourceRoot.getRoot();
        Path projectRoot = root.getParent().getParent().getParent();
        String lineSeparator = System.getProperty("line.separator");
        try (Stream<Path> stream = Files.walk(root)) {
            String packagesList = stream.filter(Files::isRegularFile)
                    .map(path -> getPackageName(root, path))
                    .distinct()
                    .sorted()
                    .reduce(
                            null,
                            (packageList, packageName) -> concatPackageName(packageName, packageList, lineSeparator));
            Path output = projectRoot.resolve("bnd.bnd");
            try (Writer writer = Files.newBufferedWriter(output)) {
                Path templateFile = projectRoot.resolve("bnd.bnd.template");
                String template = new String(Files.readAllBytes(templateFile), StandardCharsets.UTF_8);
                writer.write(template.replace("{exportedPackages}", packagesList));
            }
            Log.info("Written " + output);
        }
    }

    private String concatPackageName(String packageName, String packageList, String lineSeparator) {
        return (packageList == null ? ("\\" + lineSeparator) : (packageList + ", \\" + lineSeparator)) + "    "
                + packageName;
    }

    private static String getPackageName(Path root, Path path) {
        return root.relativize(path.getParent()).toString().replace(File.separatorChar, '.');
    }
}
