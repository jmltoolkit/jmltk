/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc;

import jjbmc.trace.TraceInformation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Assignment {
    private String parameterName;
    private int lineNumber;
    private String value;

    private @Nullable Object guessedValue;
    private @Nullable String guess;
    private String jbmcVarname;

    public Assignment(int line, String jbmcVarname, String value, String guess, String parameterName) {
        this(parameterName, line, value, null, guess, jbmcVarname);
    }

    @Override
    public String toString() {
        String val = guessedValue == null ? value : guessedValue.toString();
        String lhs = TraceInformation.applyExpressionMap(this.guess);
        return "in line " + lineNumber + ": " + lhs + " (" + jbmcVarname + ") = " + val;
    }
}
