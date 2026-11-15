/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import jjbmc.utils.Utils
import jjbmc.utils.Utils.prepareParameters
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestFactory
import java.util.function.Consumer

@Order(value = 1)
class IntegrationTests {
    @TestFactory
    fun assignableTests() = getTestStream("AssignableTests.java")

    @TestFactory
    fun assignableTests2() = getTestStream("AssignableTests2.java")

    @TestFactory
    fun fiTests() = getTestStream("FITests.java")
    // JJBMCOptions.forceInliningMethods = false;

    @TestFactory
    fun ppTests() = getTestStream("PPTests.java") { it: JJBMCOptions? -> it!!.proofPreconditions = true }

    @TestFactory
    fun runTestSuite() = getTestStream("TestSuite.java")

    companion object {
        @Throws(Exception::class)
        private fun getTestStream(filename: String, configure: Consumer<JJBMCOptions> = {}): Sequence<DynamicTest> {
            val fileName = Utils.SRC_TEST_RESOURCES.resolve("tests").resolve(filename)
            return prepareParameters(fileName).map {
                val o = it.op.options
                val displayName = o.getFileName().fileName.toString() + "::" + o.functionName
                DynamicTest.dynamicTest(displayName) {
                    configure.accept(o)
                    Utils.runTests(it)
                }
            }
        }
    }
}
