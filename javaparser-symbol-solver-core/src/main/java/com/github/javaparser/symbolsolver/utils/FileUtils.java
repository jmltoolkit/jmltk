/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.utils;

import com.github.javaparser.utils.Utils;
import java.io.File;

public class FileUtils {

    /*
     * returns true if the filename exists otherwise return false
     */
    public static boolean isValidPath(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    /*
     * returns the parent path from the filename as string
     */
    public static String getParentPath(String filename) {
        Utils.assertNotNull(filename);
        int lastIndex = filename.lastIndexOf(File.separator);
        return filename.substring(0, lastIndex);
    }
}
