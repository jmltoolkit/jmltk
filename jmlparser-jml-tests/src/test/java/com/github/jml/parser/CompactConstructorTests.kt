package com.github.jml.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.RecordDeclaration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

val config = ParserConfiguration().also {
    it.isProcessJml = true
    it.languageLevel = ParserConfiguration.LanguageLevel.JAVA_25
}
private val javaParser = JavaParser(config)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompactConstructorTests {
    @Test
    fun parsing() {
        val s = """
            public record Point2dU(int x, int y) {
                //@ requires x > Integer.MIN_VALUE;
                //@ requires y > Integer.MIN_VALUE;
                //@ ensures \old(x) >= 0 ==> this.x == x;
                //@ ensures \old(x) <  0 ==> this.x == -x;
                public Point2dU {
                    if(x<0) x *= -1;
                    if(y<0) y *= -1;
                }
            }
        """
        val r = javaParser.parse(s)
        r.problems.forEach { println(it) }
        assertTrue(r.isSuccessful)

        val cu = r.result.get()
        val cc = (cu.types.first() as RecordDeclaration).compactConstructors.first()

        assertTrue(cc.contracts.isNotEmpty())
    }
}