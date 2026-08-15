/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Alexander Weigl
 * @version 1 (15.08.26)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Issues {
    private JavaParser javaParser = createJavaParser();

    private JavaParser createJavaParser() {
        var config = new ParserConfiguration();
        config.setProcessJml(true);
        return new JavaParser(config);
    }

    private Object parse(String source) {
        var r = javaParser.parse(source);
        r.getProblems().forEach(System.out::println);
        assert r.isSuccessful();
        return r.getResult().get();
    }

    @Test
    void issue10() {
        var source = """
                public class PolishFlagSort {
                    /*@
                      @ public normal_behavior
                      @    ensures (\\forall int I, J; 0 <= I && I < J && J < ar.length; ar[I] <= ar[J]);
                      @*/
                    public static void sort ( int[] ar ) {
                	return;
                    }
                }
            """;
        var cu = parse(source);
        assertThat(cu.toString()).isEqualTo("""
                public class PolishFlagSort {

                    /*@ public normal_behavior
                        ensures (\\forall int I, J; 0 <= I && I < J && J < ar.length; ar[I] <= ar[J]);
                       \s
                    */
                    public static void sort(int[] ar) {
                        return;
                    }
                }
                """);
    }
}
