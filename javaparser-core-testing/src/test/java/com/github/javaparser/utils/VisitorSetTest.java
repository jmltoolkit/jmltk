/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ObjectIdentityEqualsVisitor;
import com.github.javaparser.ast.visitor.ObjectIdentityHashCodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseMethodDeclaration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitorSetTest {

    @Test
    void normalEqualsDoesDeepCompare() {
        Set<CompilationUnit> set = new HashSet<>();
        set.add(parse("class X{}"));
        set.add(parse("class X{}"));
        assertEquals(1, set.size());
    }

    @Test
    void objectIdentityEqualsDoesShallowCompare() {
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.add(parse("class X{}"));
        set.add(parse("class X{}"));
        assertEquals(2, set.size());
    }

    @Test
    void visitorSetContains() {
        CompilationUnit x1 = parse("class X{}");
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.add(x1);
        assertTrue(set.contains(x1));
    }

    @Test
    void visitorSetContainsAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(list);
        assertTrue(set.size() == 2 && set.containsAll(list));
    }

    @Test
    void visitorSetIterator() {
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        CompilationUnit x1 = parse("class X{}");
        set.add(x1);
        CompilationUnit x2 = parse("class X{}");
        set.add(x2);
        Iterator<CompilationUnit> itr = set.iterator();
        assertEquals(x1, itr.next());
        itr.remove();
        assertEquals(1, set.size());
        assertEquals(x2, itr.next());
        itr.remove();
        assertEquals(0, set.size());
    }

    @Test
    void visitorSetRemove() {
        CompilationUnit x1 = parse("class X{}");
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.add(x1);
        assertTrue(set.remove(x1));
    }

    @Test
    void visitorSetRemoveAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(list);
        set.removeAll(list);
        assertEquals(0, set.size());
    }

    @Test
    void visitorSetRetainAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(list);
        set.add(parse("class X{}"));
        set.retainAll(list);
        assertEquals(2, set.size());
    }

    @Test
    void visitorSetToArray() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        Set<CompilationUnit> set =
                new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(list);
        for (CompilationUnit u : set.toArray(new CompilationUnit[2])) assertTrue(set.contains(u));
    }

    @Test
    void visitSetWithEmpty() {
        Set<Type> set = new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        assertEquals("[]", set.toString());
    }

    @Test
    void visitSetWithOneElement() {
        Set<Type> set = new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(parseMethodDeclaration("public void main() {}").findAll(Type.class));
        assertEquals("[void]", set.toString());
    }

    @Test
    void visitSetWithMultiElements() {
        Set<Type> set = new VisitorSet<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        set.addAll(parseMethodDeclaration("public void main(String arg1, Integer arg2) {}")
                .findAll(Type.class));
        assertEquals("[void,Integer,String]", set.toString());
    }
}
