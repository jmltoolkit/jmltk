/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern

object JbmcFacade {
    fun verifyJBMCVersion(jbmcBin: String?, isWindows: Boolean): Boolean {
        try {
            var commands = arrayOf<String?>(jbmcBin)
            if (isWindows) {
                commands = arrayOf<String?>("cmd.exe", "/c", jbmcBin)
            }

            val rt = Runtime.getRuntime()

            val process = rt.exec(commands)

            val stdInput = BufferedReader(InputStreamReader(process.inputStream))

            val stdError = BufferedReader(InputStreamReader(process.errorStream))

            val sb = StringBuilder()
            var line = stdInput.readLine()
            while (line != null) {
                sb.append(line)
                sb.append(System.lineSeparator())
                line = stdInput.readLine()
            }

            val sb2 = ""
            val line2 = stdInput.readLine()
            while (line2 != null) {
                sb.append(line)
                sb.append(System.lineSeparator())
                line = stdError.readLine()
            }

            // Has to stay down here otherwise not reading the output may block the process
            process.waitFor()

            val output = sb.toString()
            val error = sb2
            if (output.lowercase(Locale.getDefault()).contains("jbmc version")) {
                ErrorLogger.debug("Found valid jbmc version: " + output)
                val pattern =
                    Pattern.compile("jbmc version (\\d*)\\.(\\d*)\\.(\\d*)? \\(", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(output)
                val matchFound = matcher.find()
                if (matcher.group(1).toInt() < JJBMCOptions.jbmcMajorVer) {
                    ErrorLogger.error("Error validating jbmc binary \"" + jbmcBin + "\"")
                    ErrorLogger.error("Found version: " + output)
                    ErrorLogger.error(
                        (
                            "but at least version " + JJBMCOptions.jbmcMajorVer + "." + JJBMCOptions.jbmcMinorVer +
                                " is required."
                        )
                    )
                    ErrorLogger.error(
                        "Either install jbmc and make sure it is included in the path or provide " +
                                "a jbmc binary manually with the -jbmcBinary option"
                    )
                    ErrorLogger.error("To install jbmc (as part of cbmc) head to https://github.com/diffblue/cbmc/releases/ ")
                    return false
                } else if (matcher.group(2).toInt() < JJBMCOptions.jbmcMinorVer) {
                    ErrorLogger.error("Error validating jbmc binary \"" + jbmcBin + "\"")
                    ErrorLogger.error("Found version: " + output)
                    ErrorLogger.error(
                        (
                            "but at least version " + JJBMCOptions.jbmcMajorVer + "." + JJBMCOptions.jbmcMinorVer +
                                " is required."
                        )
                    )
                    ErrorLogger.error(
                        "Either install jbmc and make sure it is included in the path or provide " +
                                "a jbmc binary manually with the -jbmcBinary option"
                    )
                    ErrorLogger.error("To install jbmc (as part of cbmc) head to https://github.com/diffblue/cbmc/releases/ ")
                    return false
                }
                return true
            }
        } catch (e: IOException) {
            ErrorLogger.error("Error validating jbmc binary \"" + jbmcBin + "\" (" + e.message + ")")
            ErrorLogger.error(
                "Either install jbmc and make sure it is included in the path or provide a jbmc binary manually with the -jbmcBinary option"
            )
            ErrorLogger.error("To install jbmc (as part of cbmc) head to https://github.com/diffblue/cbmc/releases/ ")
            // e.printStackTrace();
            return false
        } catch (e: InterruptedException) {
            ErrorLogger.error("Error validating jbmc binary \"" + jbmcBin + "\" (" + e.message + ")")
            ErrorLogger.error(
                "Either install jbmc and make sure it is included in the path or provide a jbmc binary manually with the -jbmcBinary option"
            )
            ErrorLogger.error("To install jbmc (as part of cbmc) head to https://github.com/diffblue/cbmc/releases/ ")
            return false
        }
        ErrorLogger.error("Error validating jbmc binary \"" + jbmcBin + "\"")
        ErrorLogger.error(
            "Either install jbmc and make sure it is included in the path or provide a jbmc binary manually with the -jbmcBinary option"
        )
        ErrorLogger.error("To install jbmc (as part of cbmc) head to https://github.com/diffblue/cbmc/releases/ ")
        return true
    }
}
