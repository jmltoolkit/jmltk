/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jjbmc.ErrorLogger.info
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk

class CaseStudies {
    private var configString: String? = null
    private val configFilePath: Path = File("testRes" + File.separator + "CaseStudyConfig.json").toPath()
    private var configs: JsonObject? = null

    @Throws(Exception::class)
    fun runCaseStudies() {
        Paths.get("testRes", "CaseStudy").walk()
            .forEach { walk ->
                walk.filter { it.isRegularFile() }
                    .forEach { f ->
                        for (l in getConfigsForFile(f.fileName.toString())) {
                            l.addFirst(f.toAbsolutePath().toString())
                            l.addFirst("-c")
                            val args = l.toTypedArray()
                            info("Running Casestudy: %s", f.fileName)
                            info("with params: " + args.contentToString())
                            Main.main(args)
                        }
                    }
            }
    }

    fun getConfigsForFile(file: String): MutableList<MutableList<String>> {
        if (configString == null) {
            readConfigString()
            configs = JsonParser.parseString(configString) as JsonObject?
        }
        val config = configs!!.get(file) as JsonArray?
        if (config == null) {
            val innerList = ArrayList<String>()
            val outerList = ArrayList<MutableList<String>>()
            outerList.add(innerList)
            return outerList
        }
        return jsonToList(config)
    }

    private fun jsonToList(arr: JsonArray): MutableList<MutableList<String>> {
        val configs = ArrayList<MutableList<String>>()
        for (i in 0..<arr.size()) {
            val config = ArrayList<String>(16)
            val jsonConfig = arr.get(i) as JsonArray
            for (j in 0..<jsonConfig.size()) {
                config.add(jsonConfig.get(j).asString)
            }
            configs.add(config)
        }
        return configs
    }

    private fun readConfigString() {
        try {
            configString = configFilePath.readText()
        } catch (_: IOException) {
            assert(false)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                CaseStudies().runCaseStudies()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
