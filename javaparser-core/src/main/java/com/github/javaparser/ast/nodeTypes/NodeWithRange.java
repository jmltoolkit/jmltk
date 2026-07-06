/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import java.util.Optional;

/**
 * A node that has a Range, which is every Node.
 */
public interface NodeWithRange<N> {

    Optional<Range> getRange();

    N setRange(Range range);

    /**
     * The begin position of this node in the source file.
     */
    default Optional<Position> getBegin() {
        return getRange().map(r -> r.begin);
    }

    /**
     * The end position of this node in the source file.
     */
    default Optional<Position> getEnd() {
        return getRange().map(r -> r.end);
    }

    /**
     * @deprecated use {@link #containsWithinRange(Node)} instead.
     */
    @Deprecated
    default boolean containsWithin(Node other) {
        return containsWithinRange(other);
    }

    /**
     * Checks whether the range of the given {@code Node} is contained within the range of this {@code NodeWithRange}.
     * Note that any range contains itself, i.e., for any node {@code n}, we have that
     * {@code n.containsWithinRange(n) == true}.
     *
     * <b>Notice:</b> This method compares two nodes based on their ranges <i>only</i>, but <i>not</i> based on the
     * storage unit of the two nodes. Therefore, this method may return {@code true} for a node that is contained in a
     * different file than this {@code NodeWithRange}. You may wish to use {@link Node#isAncestorOf(Node)} instead.
     *
     * @param other the node whose range should be compared with this node's range.
     * @return {@code true} if the given node's range is contained within this node's range, and {@code false}
     * otherwise.
     */
    default boolean containsWithinRange(Node other) {
        if (hasRange() && other.hasRange()) {
            return getRange().get().contains(other.getRange().get());
        }
        return false;
    }

    /*
     * Returns true if the node has a range
     */
    default boolean hasRange() {
        return getRange().isPresent();
    }
}
