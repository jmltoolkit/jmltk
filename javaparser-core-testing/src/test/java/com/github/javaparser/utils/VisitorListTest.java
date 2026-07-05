/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ObjectIdentityEqualsVisitor;
import com.github.javaparser.ast.visitor.ObjectIdentityHashCodeVisitor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisitorListTest {

    @Test
    void visitorAddAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        VisitorList<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.addAll(list);
        for (int i = 0; i < list.size(); i++) assertEquals(list.get(i), vList.get(i));
    }

    @Test
    void visitorAddAllAtIndex() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class Y{}"));
        VisitorList<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.add(parse("class A{}"));
        vList.add(parse("class B{}"));
        vList.addAll(2, list);
        vList.add(parse("class C{}"));
        for (int i = 0; i < list.size(); i++) assertEquals(list.get(i), vList.get(2 + i));
    }

    @Test
    void visitorListContains() {
        CompilationUnit x1 = parse("class X{}");
        VisitorList<CompilationUnit> list =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        list.add(x1);
        assertTrue(list.contains(x1));
    }

    @Test
    void visitorListContainsAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        VisitorList<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.addAll(list);
        assertTrue(vList.size() == 2 && vList.containsAll(list));
    }

    @Test
    void visitorListIterator() {
        VisitorList<CompilationUnit> list =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        CompilationUnit x1 = parse("class X{}");
        list.add(x1);
        CompilationUnit x2 = parse("class X{}");
        list.add(x2);
        Iterator<CompilationUnit> itr = list.iterator();
        assertEquals(x1, itr.next());
        itr.remove();
        assertEquals(1, list.size());
        assertEquals(x2, itr.next());
        itr.remove();
        assertEquals(0, list.size());
    }

    @Test
    void visitorListListIterator() {
        VisitorList<CompilationUnit> list =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        CompilationUnit x1 = parse("class X{}");
        list.add(x1);
        CompilationUnit x2 = parse("class X{}");
        list.add(x2);
        Iterator<CompilationUnit> itr = list.listIterator(2);
        assertEquals(x1, itr.next());
        itr.remove();
        assertEquals(3, list.size());
        assertEquals(x2, itr.next());
        itr.remove();
        assertEquals(2, list.size());
    }

    @Test
    void visitorListRemove() {
        CompilationUnit x1 = parse("class X{}");
        VisitorList<CompilationUnit> list =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        list.add(x1);
        assertTrue(list.remove(x1));
    }

    @Test
    void visitorListRemoveAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        VisitorList<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.addAll(list);
        vList.removeAll(list);
        assertEquals(0, vList.size());
    }

    @Test
    void visitorListRetainAll() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        VisitorList<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.addAll(list);
        vList.add(parse("class X{}"));
        vList.retainAll(list);
        assertEquals(2, vList.size());
    }

    @Test
    void visitorListSubList() {
        VisitorList<CompilationUnit> list =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        assertEquals(4, list.size());
        List<CompilationUnit> subLst = list.subList(1, 3);
        assertEquals(2, subLst.size());
        subLst.add(parse("class X{}"));
        assertEquals(3, subLst.size());
        assertEquals(5, list.size());
    }

    @Test
    void visitorListToArray() {
        List<CompilationUnit> list = new ArrayList<>();
        list.add(parse("class X{}"));
        list.add(parse("class X{}"));
        List<CompilationUnit> vList =
                new VisitorList<>(new ObjectIdentityHashCodeVisitor(), new ObjectIdentityEqualsVisitor());
        vList.addAll(list);
        for (CompilationUnit u : vList.toArray(new CompilationUnit[2])) assertTrue(vList.contains(u));
    }
}
