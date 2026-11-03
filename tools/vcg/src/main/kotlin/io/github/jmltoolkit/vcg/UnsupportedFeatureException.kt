/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import com.github.javaparser.Range

/**
 * Raised when verification-condition generation encounters a construct the VCG
 * cannot model (multi-dimensional array creation, switch expressions, lambdas,
 * anonymous classes, unsupported JML keywords such as `\fresh`, …).
 *
 * Generation aborts on the first such construct. The message names the
 * construct; when the offending node is attached to a parsed compilation unit,
 * [range] carries its exact source location so tools (CLI, LSP) can surface a
 * precise diagnostic instead of a generic failure.
 *
 * Extends [UnsupportedOperationException] (a [RuntimeException]) so existing
 * `assertThrows(UnsupportedOperationException::class.java)` guards keep
 * working; callers that want the structured form catch [UnsupportedFeatureException]
 * specifically.
 */
class UnsupportedFeatureException(
    message: String,
    val range: Range? = null,
    cause: Throwable? = null,
) : UnsupportedOperationException(message, cause)
