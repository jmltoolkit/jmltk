/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.serialization;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaParserJsonSerializerTest {
    @Test
    @Disabled("weigl: This is test so stupid over-tight.")
    void test() {
        CompilationUnit cu = parse("class X{java.util.Y y;}");

        String serialized = serialize(cu, false);

        assertEquals(
                "{\"!\":\"com.github.javaparser.ast.CompilationUnit\",\"range\":{\"beginLine\":1,\"beginColumn\":1,\"endLine\":1,\"endColumn\":23},\"tokenRange\":{\"beginToken\":{\"kind\":19,\"text\":\"class\"},\"endToken\":{\"kind\":0,\"text\":\"\"}},\"imports\":[],\"types\":[{\"!\":\"com.github.javaparser.ast.body.ClassOrInterfaceDeclaration\",\"range\":{\"beginLine\":1,\"beginColumn\":1,\"endLine\":1,\"endColumn\":23},\"tokenRange\":{\"beginToken\":{\"kind\":19,\"text\":\"class\"},\"endToken\":{\"kind\":164,\"text\":\"}\"}},\"extendedTypes\":[],\"implementedTypes\":[],\"isCompact\":\"false\",\"isInterface\":\"false\",\"typeParameters\":[],\"members\":[{\"!\":\"com.github.javaparser.ast.body.FieldDeclaration\",\"range\":{\"beginLine\":1,\"beginColumn\":9,\"endLine\":1,\"endColumn\":22},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"java\"},\"endToken\":{\"kind\":167,\"text\":\";\"}},\"modifiers\":[],\"variables\":[{\"!\":\"com.github.javaparser.ast.body.VariableDeclarator\",\"range\":{\"beginLine\":1,\"beginColumn\":21,\"endLine\":1,\"endColumn\":21},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"y\"},\"endToken\":{\"kind\":155,\"text\":\"y\"}},\"name\":{\"!\":\"com.github.javaparser.ast.expr.SimpleName\",\"range\":{\"beginLine\":1,\"beginColumn\":21,\"endLine\":1,\"endColumn\":21},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"y\"},\"endToken\":{\"kind\":155,\"text\":\"y\"}},\"identifier\":\"y\"},\"type\":{\"!\":\"com.github.javaparser.ast.type.ClassOrInterfaceType\",\"range\":{\"beginLine\":1,\"beginColumn\":9,\"endLine\":1,\"endColumn\":19},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"java\"},\"endToken\":{\"kind\":155,\"text\":\"Y\"}},\"name\":{\"!\":\"com.github.javaparser.ast.expr.SimpleName\",\"range\":{\"beginLine\":1,\"beginColumn\":19,\"endLine\":1,\"endColumn\":19},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"Y\"},\"endToken\":{\"kind\":155,\"text\":\"Y\"}},\"identifier\":\"Y\"},\"scope\":{\"!\":\"com.github.javaparser.ast.type.ClassOrInterfaceType\",\"range\":{\"beginLine\":1,\"beginColumn\":9,\"endLine\":1,\"endColumn\":17},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"java\"},\"endToken\":{\"kind\":155,\"text\":\"util\"}},\"name\":{\"!\":\"com.github.javaparser.ast.expr.SimpleName\",\"range\":{\"beginLine\":1,\"beginColumn\":14,\"endLine\":1,\"endColumn\":17},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"util\"},\"endToken\":{\"kind\":155,\"text\":\"util\"}},\"identifier\":\"util\"},\"scope\":{\"!\":\"com.github.javaparser.ast.type.ClassOrInterfaceType\",\"range\":{\"beginLine\":1,\"beginColumn\":9,\"endLine\":1,\"endColumn\":12},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"java\"},\"endToken\":{\"kind\":155,\"text\":\"java\"}},\"name\":{\"!\":\"com.github.javaparser.ast.expr.SimpleName\",\"range\":{\"beginLine\":1,\"beginColumn\":9,\"endLine\":1,\"endColumn\":12},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"java\"},\"endToken\":{\"kind\":155,\"text\":\"java\"}},\"identifier\":\"java\"},\"annotations\":[]},\"annotations\":[]},\"annotations\":[]}}],\"annotations\":[]}],\"modifiers\":[],\"name\":{\"!\":\"com.github.javaparser.ast.expr.SimpleName\",\"range\":{\"beginLine\":1,\"beginColumn\":7,\"endLine\":1,\"endColumn\":7},\"tokenRange\":{\"beginToken\":{\"kind\":155,\"text\":\"X\"},\"endToken\":{\"kind\":155,\"text\":\"X\"}},\"identifier\":\"X\"},\"annotations\":[]}]}",
                serialized);
    }

    static String serialize(Node node, boolean prettyPrint) {
        Map<String, ?> config = new HashMap<>();
        if (prettyPrint) {
            config.put(JsonGenerator.PRETTY_PRINTING, null);
        }
        JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(config);
        JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
        StringWriter jsonWriter = new StringWriter();
        try (JsonGenerator generator = generatorFactory.createGenerator(jsonWriter)) {
            serializer.serialize(node, generator);
        }
        return jsonWriter.toString();
    }
}
