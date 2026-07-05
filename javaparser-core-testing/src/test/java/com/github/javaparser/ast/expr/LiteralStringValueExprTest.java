/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.observer.AstObserver;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("OctalInteger")
class LiteralStringValueExprTest {

    @Test
    void trivialLiteralsAreConverted() {
        assertThat(new CharLiteralExpr('\t').getValue()).isEqualTo("\\t");
        assertThat(new CharLiteralExpr('\b').getValue()).isEqualTo("\\b");
        assertThat(new CharLiteralExpr('\f').getValue()).isEqualTo("\\f");
        assertThat(new CharLiteralExpr('\r').getValue()).isEqualTo("\\r");
        assertThat(new CharLiteralExpr('\n').getValue()).isEqualTo("\\n");
        assertThat(new CharLiteralExpr('\\').getValue()).isEqualTo("\\\\");
        assertThat(new CharLiteralExpr('\"').getValue()).isEqualTo("\\\"");

        assertThat(new IntegerLiteralExpr("0B0").asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr("0b0").asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr("0X0").asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr("0x0").asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(0).asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(00).asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(0B0).asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(0b0).asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(0X0).asInt()).isEqualTo(0);
        assertThat(new IntegerLiteralExpr(0x0).asInt()).isEqualTo(0);

        assertThat(new LongLiteralExpr("0B0L").asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr("0b0L").asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr("0X0L").asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr("0x0L").asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(0L).asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(00L).asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(0B0L).asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(0b0L).asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(0X0L).asLong()).isEqualTo(0);
        assertThat(new LongLiteralExpr(0x0L).asLong()).isEqualTo(0);

        assertThat(new DoubleLiteralExpr("0.0f").asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr("0.0F").asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr("0.0d").asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr("0.0D").asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr(0.0F).asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr(0.0f).asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr(0.0D).asDouble()).isEqualTo(0.0);
        assertThat(new DoubleLiteralExpr(0.0d).asDouble()).isEqualTo(0.0);
    }

    @Test
    void lowerAndUpperBoundIntegersAreConverted() {
        IntegerLiteralExpr dec = parseExpression("2147483647");
        IntegerLiteralExpr posOct = parseExpression("0177_7777_7777");
        IntegerLiteralExpr negOct = parseExpression("0377_7777_7777");
        IntegerLiteralExpr posHex = parseExpression("0x7fff_ffff");
        IntegerLiteralExpr negHex = parseExpression("0xffff_ffff");
        IntegerLiteralExpr posBin = parseExpression("0b0111_1111_1111_1111_1111_1111_1111_1111");
        IntegerLiteralExpr negBin = parseExpression("0b1000_0000_0000_0000_0000_0000_0000_0000");

        assertThat(dec.asInt()).isEqualTo(2147483647);
        assertThat(posOct.asInt()).isEqualTo(2147483647); // 0177_7777_7777
        assertThat(negOct.asInt()).isEqualTo(-1); // 0377_7777_7777
        assertThat(posHex.asInt()).isEqualTo(0x7fff_ffff);
        assertThat(negHex.asInt()).isEqualTo(0xffff_ffff);
        assertThat(posBin.asInt()).isEqualTo(0b0111_1111_1111_1111_1111_1111_1111_1111);
        assertThat(negBin.asInt()).isEqualTo(0b1000_0000_0000_0000_0000_0000_0000_0000);
    }

    @Test
    void negativeLiteralValues() {
        UnaryExpr unaryIntExpr = parseExpression("-2147483648"); // valid, Integer.MIN_VALUE
        IntegerLiteralExpr literalIntExpr = (IntegerLiteralExpr) unaryIntExpr.getExpression();
        IntegerLiteralExpr notValidIntExpr = parseExpression("2147483648"); // not valid

        UnaryExpr unaryLongExpr = parseExpression("-9223372036854775808L"); // valid, Long.MIN_VALUE
        LongLiteralExpr literalLongExpr = (LongLiteralExpr) unaryLongExpr.getExpression();
        LongLiteralExpr notValidLongExpr = parseExpression("9223372036854775808L"); // not valid

        assertThat(literalIntExpr.asNumber()).isEqualTo(2147483648L);
        assertThat(literalLongExpr.asNumber()).isEqualTo(new BigInteger("9223372036854775808"));

        assertThatThrownBy(notValidIntExpr::asNumber).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(notValidLongExpr::asNumber).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void lowerAndUpperBoundLongsAreConverted() {
        LongLiteralExpr dec = parseExpression("9223372036854775807L");
        LongLiteralExpr posOct = parseExpression("07_7777_7777_7777_7777_7777L");
        LongLiteralExpr negOct = parseExpression("010_0000_0000_0000_0000_0000L");
        LongLiteralExpr posHex = parseExpression("0x7fff_ffff_ffff_ffffL");
        LongLiteralExpr negHex = parseExpression("0xffff_ffff_ffff_ffffL");
        LongLiteralExpr posBin =
                parseExpression("0b0111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111L");
        LongLiteralExpr negBin =
                parseExpression("0b1000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L");

        assertThat(dec.asLong()).isEqualTo(9223372036854775807L);
        assertThat(posOct.asLong()).isEqualTo(9223372036854775807L); // 07_7777_7777_7777_7777_7777L
        assertThat(negOct.asLong()).isEqualTo(-9223372036854775808L); // 010_0000_0000_0000_0000_0000L
        assertThat(posHex.asLong()).isEqualTo(0x7fff_ffff_ffff_ffffL);
        assertThat(negHex.asLong()).isEqualTo(0xffff_ffff_ffff_ffffL);
        assertThat(posBin.asLong())
                .isEqualTo(0b0111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111L);
        assertThat(negBin.asLong())
                .isEqualTo(0b1000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L);
    }

    @Test
    void charLiteralsAreConverted() {
        CharLiteralExpr a = parseExpression("'a'");
        CharLiteralExpr percent = parseExpression("'%'");
        CharLiteralExpr tab = parseExpression("'\\t'");
        CharLiteralExpr newLine = parseExpression("'\\n'");
        CharLiteralExpr slash = parseExpression("'\\\\'");
        CharLiteralExpr quote = parseExpression("'\\''");
        CharLiteralExpr omega = parseExpression("'\\u03a9'");
        CharLiteralExpr unicode = parseExpression("'\\uFFFF'");
        CharLiteralExpr ascii = parseExpression("'\\177'");
        CharLiteralExpr trademark = parseExpression("'™'");

        assertThat(a.asChar()).isEqualTo('a');
        assertThat(percent.asChar()).isEqualTo('%');
        assertThat(tab.asChar()).isEqualTo('\t');
        assertThat(newLine.asChar()).isEqualTo('\n');
        assertThat(slash.asChar()).isEqualTo('\\');
        assertThat(quote.asChar()).isEqualTo('\'');
        assertThat(omega.asChar()).isEqualTo('\u03a9');
        assertThat(unicode.asChar()).isEqualTo('\uFFFF');
        assertThat(ascii.asChar()).isEqualTo('\177');
        assertThat(trademark.asChar()).isEqualTo('™');
    }

    @Test
    void lowerAndUpperBoundDoublesAreConverted() {
        DoubleLiteralExpr posFloat = parseExpression("3.4028235e38f");
        DoubleLiteralExpr negFloat = parseExpression("1.40e-45f");
        DoubleLiteralExpr posDouble = parseExpression("1.7976931348623157e308");
        DoubleLiteralExpr negDouble = parseExpression("4.9e-324");
        DoubleLiteralExpr posHexFloat = parseExpression("0x1.fffffffffffffp1023");
        DoubleLiteralExpr negHexFloat = parseExpression("0x0.0000000000001P-1022");

        assertThat(posFloat.asDouble()).isCloseTo(3.4028235e38f, Percentage.withPercentage(1));
        assertThat(negFloat.asDouble()).isCloseTo(1.40e-45f, Percentage.withPercentage(1));
        assertThat(posDouble.asDouble()).isEqualTo(1.7976931348623157e308);
        assertThat(negDouble.asDouble()).isEqualTo(4.9e-324);
        assertThat(posHexFloat.asDouble()).isEqualTo(0x1.fffffffffffffp1023);
        assertThat(negHexFloat.asDouble()).isEqualTo(0x0.0000000000001P-1022);
    }

    @Test
    void specialCharactersInStringsAreEscaped() {
        assertThat(new StringLiteralExpr("\n").getValue()).isEqualTo("\\n");
        assertThat(new StringLiteralExpr("\r").getValue()).isEqualTo("\\r");
        assertThat(new StringLiteralExpr("").setEscapedValue("\n").getValue()).isEqualTo("\\n");
        assertThat(new StringLiteralExpr("").setEscapedValue("\r").getValue()).isEqualTo("\\r");
        assertThat(new StringLiteralExpr("").setEscapedValue("\n").asString()).isEqualTo("\n");
        assertThat(new StringLiteralExpr("").setEscapedValue("\r").asString()).isEqualTo("\r");
        assertThat(new StringLiteralExpr("Hello\nWorld\rHello\"World\'").asString())
                .isEqualTo("Hello\nWorld\rHello\"World\'");
    }

    @Test
    void issue4791Test() {
        String a = new String("Hello World");
        String b = new String("Hello World");
        StringLiteralExpr expression = new StringLiteralExpr(a);

        AstObserver observer = mock(AstObserver.class);
        expression.register(observer);

        expression.setValue(b);

        verifyNoInteractions(observer);
    }
}
