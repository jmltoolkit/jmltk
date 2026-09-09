/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.utils;

import jjbmc.FunctionNameVisitor;

public record TestOptions(FunctionNameVisitor.TestBehaviour behaviour, int unwinds, String functionName) {}
