/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.type;

import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Convert a {@link Type} into a {@link ResolvedType}.
 */
public interface ConvertibleToUsage {

    ResolvedType convertToUsage(Context context);
}
