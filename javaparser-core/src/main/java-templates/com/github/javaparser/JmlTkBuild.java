/*
 * Copyright (C) 2013-2026 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser;

/**
 * Core information about this library.
 * Generated during build - do not edit directly.
 */
public class JmlTkBuild {
    public static final String PROJECT_VERSION = "${version}";
    public static final String PROJECT_NAME = "${name}";
    public static final String GROUP_ID = "${groupId}";
    public static final String ARTIFACT_ID = "${artifactId}";
    
    /**
     * Returns the full Maven/Gradle coordinate string for this artifact.
     * Format: groupId:artifactId:version
     */
    public static String getArtifactCoordinate() {
        return GROUP_ID + ":" + ARTIFACT_ID + ":" + PROJECT_VERSION;
    }
}
