/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.observer.AstObserverAdapter;
import com.github.javaparser.ast.observer.ObservableProperty;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.*;
import static com.github.javaparser.ast.Modifier.createModifierList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeWithModifiersTest {

    @Test
    void addModifierWorks() {
        ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        decl.addModifier(PUBLIC);
        assertEquals(createModifierList(PUBLIC), decl.getModifiers());
    }

    @Test
    void addModifierTriggerNotification() {
        List<String> changes = new LinkedList<>();
        ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        decl.register(new AstObserverAdapter() {
            @Override
            public void propertyChange(
                    Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                changes.add("property " + property.name() + " is changed to " + newValue);
            }
        });
        decl.addModifier(PUBLIC);
        assertEquals(1, changes.size());
        assertEquals("property MODIFIERS is changed to [public ]", changes.get(0));
    }

    @Test
    void removeExistingModifier() {
        NodeWithModifiers node = anythingWithModifiers(PUBLIC);
        node.removeModifier(PUBLIC);
        assertEquals(0, node.getModifiers().size());
    }

    @Test
    void ignoreNotExistingModifiersOnRemove() {
        NodeWithModifiers node = anythingWithModifiers(PUBLIC);
        node.removeModifier(PRIVATE);

        assertEquals(createModifierList(PUBLIC), node.getModifiers());
    }

    @Test
    void keepModifiersThatShouldNotBeRemoved() {
        NodeWithModifiers node = anythingWithModifiers(PUBLIC, STATIC, SYNCHRONIZED);
        node.removeModifier(PUBLIC, PRIVATE, STATIC);

        assertEquals(createModifierList(SYNCHRONIZED), node.getModifiers());
    }

    private NodeWithModifiers anythingWithModifiers(Modifier.DefaultKeyword... keywords) {
        ClassOrInterfaceDeclaration foo = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        foo.addModifier(keywords);
        return foo;
    }
}
