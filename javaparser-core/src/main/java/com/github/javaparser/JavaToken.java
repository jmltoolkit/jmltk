/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.Generated;
import com.github.javaparser.utils.LineSeparator;

import java.util.List;
import java.util.Optional;

import static com.github.javaparser.utils.CodeGenerationUtils.f;
import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A token from a parsed source file.
 * (Awkwardly named "Java"Token since JavaCC already generates an internal class Token.)
 * It is a node in a double linked list called token list.
 */
public class JavaToken {

    public static final JavaToken INVALID = new JavaToken();

    private Range range;

    private int kind;

    private String text;

    private JavaToken previousToken = null;

    private JavaToken nextToken = null;

    private JavaToken() {
        this(null, 0, "INVALID", null, null);
    }

    public JavaToken(int kind, String text) {
        this(null, kind, text, null, null);
    }

    JavaToken(Token token, List<JavaToken> tokens) {
        // You could be puzzled by the following lines
        //
        // The reason why these lines are necessary is the fact that Java is ambiguous. There are cases where the
        // sequence of characters ">>>" and ">>" should be recognized as the single tokens ">>>" and ">>". In other
        // cases however we want to split those characters in single GT tokens (">").
        //
        // For example, in expressions ">>" and ">>>" are valid, while when defining types we could have this:
        //
        // List<List<Set<String>>>>
        //
        // You can see that the sequence ">>>>" should be interpreted as four consecutive ">" tokens closing a type
        // parameter list.
        //
        // The JavaCC handle this case by first recognizing always the longest token, and then depending on the context
        // putting back the unused chars in the stream. However in those cases the token provided is invalid: it has an
        // image corresponding to the text originally recognized, without considering that after some characters could
        // have been put back into the stream.
        //
        // So in the case of:
        //
        // List<List<Set<String>>>>
        // ___   -> recognized as ">>>", then ">>" put back in the stream but Token(type=GT, image=">>>") passed to this
        // class
        // ___  -> recognized as ">>>", then ">>" put back in the stream but Token(type=GT, image=">>>") passed to this
        // class
        // __  -> recognized as ">>", then ">" put back in the stream but Token(type=GT, image=">>") passed to this
        // class
        // _  -> Token(type=GT, image=">") good!
        //
        // So given the image could be wrong but the type is correct, we look at the type of the token and we fix
        // the image. Everybody is happy and we can keep this horrible thing as our little secret.
        Range range = Range.range(token.beginLine, token.beginColumn, token.endLine, token.endColumn);
        String text = token.image;
        if (token.kind == GeneratedJavaParserConstants.GT) {
            range = Range.range(token.beginLine, token.beginColumn, token.endLine, token.beginColumn);
            text = ">";
        } else if (token.kind == GeneratedJavaParserConstants.RSIGNEDSHIFT) {
            range = Range.range(token.beginLine, token.beginColumn, token.endLine, token.beginColumn + 1);
            text = ">>";
        }
        this.range = range;
        this.kind = token.kind;
        this.text = text;
        if (!tokens.isEmpty()) {
            final JavaToken previousToken = tokens.get(tokens.size() - 1);
            this.previousToken = previousToken;
            previousToken.nextToken = this;
        } else {
            previousToken = null;
        }
    }

    /**
     * Create a token of a certain kind.
     */
    public JavaToken(int kind) {
        String content = GeneratedJavaParserConstants.tokenImage[kind];
        if (content.startsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        if (TokenTypes.isEndOfLineToken(kind)) {
            content = LineSeparator.SYSTEM.asRawString();
        } else if (TokenTypes.isWhitespace(kind)) {
            content = " ";
        }
        this.kind = kind;
        this.text = content;
    }

    public JavaToken(Range range, int kind, String text, JavaToken previousToken, JavaToken nextToken) {
        assertNotNull(text);
        this.range = range;
        this.kind = kind;
        this.text = text;
        this.previousToken = previousToken;
        this.nextToken = nextToken;
    }

    public Optional<Range> getRange() {
        return Optional.ofNullable(range);
    }

    /*
     * Returns true if the token has a range
     */
    public boolean hasRange() {
        return getRange().isPresent();
    }

    public int getKind() {
        return kind;
    }

    void setKind(int kind) {
        this.kind = kind;
    }

    public String getText() {
        return text;
    }

    public Optional<JavaToken> getNextToken() {
        return Optional.ofNullable(nextToken);
    }

    public Optional<JavaToken> getPreviousToken() {
        return Optional.ofNullable(previousToken);
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String asString() {
        return text;
    }

    /**
     * @return the token range that goes from the beginning to the end of the token list this token is a part of.
     */
    public TokenRange toTokenRange() {
        return new TokenRange(findFirstToken(), findLastToken());
    }

    @Override
    public String toString() {
        String text = getText()
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\r\n", "\\r\\n")
                .replace("\t", "\\t");
        return f(
                "\"%s\"   <%s>   %s",
                text, getKind(), getRange().map(Range::toString).orElse("(?)-(?)"));
    }

    /**
     * Used by the parser while constructing nodes. No tokens should be invalid when the parser is done.
     */
    public boolean valid() {
        return !invalid();
    }

    /**
     * Used by the parser while constructing nodes. No tokens should be invalid when the parser is done.
     */
    public boolean invalid() {
        return this == INVALID;
    }

    public enum Category {
        WHITESPACE_NO_EOL,
        EOL,
        COMMENT,
        IDENTIFIER,
        KEYWORD,
        LITERAL,
        SEPARATOR,
        OPERATOR;

        public boolean isWhitespaceOrComment() {
            return isWhitespace() || this == COMMENT;
        }

        public boolean isWhitespace() {
            return this == WHITESPACE_NO_EOL || this == EOL;
        }

        public boolean isEndOfLine() {
            return this == EOL;
        }

        public boolean isComment() {
            return this == COMMENT;
        }

        public boolean isWhitespaceButNotEndOfLine() {
            return this == WHITESPACE_NO_EOL;
        }

        public boolean isIdentifier() {
            return this == IDENTIFIER;
        }

        public boolean isKeyword() {
            return this == KEYWORD;
        }

        public boolean isLiteral() {
            return this == LITERAL;
        }

        public boolean isSeparator() {
            return this == SEPARATOR;
        }

        public boolean isOperator() {
            return this == OPERATOR;
        }
    }

    @Generated("com.github.javaparser.generator.core.other.TokenKindGenerator")
    public enum Kind {
        EOF(0),
        SPACE(1),
        WINDOWS_EOL(2),
        UNIX_EOL(3),
        OLD_MAC_EOL(4),
        INVARIANT(5),
        INVARIANT_FREE(6),
        ABRUPT_BEHAVIOR(7),
        ABRUPT_BEHAVIOUR(8),
        MODEL_BEHAVIOR(9),
        MODEL_BEHAVIOUR(10),
        ACCESSIBLE(11),
        ACCESSIBLE_REDUNDANTLY(12),
        ALSO(13),
        ANTIVALENCE(14),
        JML_ASSERT(15),
        ASSERT_REDUNDANTLY(16),
        ASSIGNABLE(17),
        ASSIGNABLE_FREE(18),
        ASSIGNABLE_REDUNDANTLY(19),
        ASSUME(20),
        ASSUME_REDUNDANTLY(21),
        AXIOM(22),
        LEMMA(23),
        BEHAVIOR(24),
        BEHAVIOUR(25),
        BIGINT(26),
        BIGINT_MATH(27),
        BREAKS(28),
        BREAKS_REDUNDANTLY(29),
        BREAK_BEHAVIOR(30),
        BREAK_BEHAVIOUR(31),
        CALLABLE(32),
        CALLABLE_REDUNDANTLY(33),
        CAPTURES(34),
        CAPTURES_REDUNDANTLY(35),
        CHOOSE(36),
        CHOOSE_IF(37),
        CODE(38),
        CODE_BIGINT_MATH(39),
        CODE_JAVA_MATH(40),
        CODE_SAFE_MATH(41),
        IMMUTABLE(42),
        CONSTRAINT(43),
        CONSTRAINT_REDUNDANTLY(44),
        CONSTRUCTOR(45),
        CONTINUES(46),
        CONTINUES_REDUNDANTLY(47),
        CONTINUE_BEHAVIOR(48),
        CONTINUE_BEHAVIOUR(49),
        DECLASSIFIES(50),
        DECREASES(51),
        DECREASES_REDUNDANTLY(52),
        DECREASING(53),
        DECREASING_REDUNDANTLY(54),
        DETERMINES(55),
        LOOP_DETERMINES(56),
        SEPARATES(57),
        LOOP_SEPARATES(58),
        NEW_OBJECTS(59),
        BY(60),
        DIVERGES(61),
        DIVERGES_REDUNDANTLY(62),
        DURATION(63),
        DURATION_REDUNDANTLY(64),
        ENSURES(65),
        ENSURES_REDUNDANTLY(66),
        ENSURES_FREE(67),
        REQUIRES_FREE(68),
        EQUIVALENCE(69),
        IMPLICATION(70),
        IMPLICATION_BACKWARD(71),
        ERASES(72),
        EXAMPLE(73),
        EXCEPTIONAL_BEHAVIOR(74),
        EXCEPTIONAL_BEHAVIOUR(75),
        EXCEPTIONAL_EXAMPLE(76),
        EXISTS(77),
        EXSURES(78),
        EXSURES_REDUNDANTLY(79),
        EXTRACT(80),
        FIELD(81),
        FORALLQ(82),
        LET(83),
        FORALL(84),
        FOR_EXAMPLE(85),
        PEER(86),
        REP(87),
        READ_ONLY(88),
        BEGIN(89),
        END(90),
        HELPER(91),
        HENCE_BY(92),
        HENCE_BY_REDUNDANTLY(93),
        IMPLIES_THAT(94),
        IN(95),
        INITIALIZER(96),
        INITIALLY(97),
        INSTANCE(98),
        TWO_STATE(99),
        NO_STATE(100),
        NON_NULL_BY_DEFAULT(101),
        INVARIANT_REDUNDANTLY(102),
        IN_REDUNDANTLY(103),
        JAVA_MATH(104),
        LBLNEG(105),
        LBLPOS(106),
        LBL(107),
        LOOP_CONTRACT(108),
        LOOP_INVARIANT(109),
        LOOP_INVARIANT_FREE(110),
        LOOP_INVARIANT_REDUNDANTLY(111),
        MAINTAINING(112),
        MAINTAINING_REDUNDANTLY(113),
        MAPS(114),
        MAPS_REDUNDANTLY(115),
        MAX(116),
        MEASURED_BY(117),
        ESC_MEASURED_BY(118),
        MEASURED_BY_REDUNDANTLY(119),
        METHOD(120),
        MIN(121),
        MODEL_PROGRAM(122),
        MODIFIABLE(123),
        MODIFIABLE_REDUNDANTLY(124),
        LOOP_MODIFIES(125),
        MODIFIES(126),
        MODIFIES_REDUNDANTLY(127),
        MONITORED(128),
        MONITORS_FOR(129),
        NESTED_CONTRACT_END(130),
        NESTED_CONTRACT_START(131),
        NONNULLELEMENTS(132),
        NON_NULL(133),
        NORMAL_BEHAVIOR(134),
        NORMAL_BEHAVIOUR(135),
        FEASIBLE_BEHAVIOR(136),
        FEASIBLE_BEHAVIOUR(137),
        NORMAL_EXAMPLE(138),
        NOWARN(139),
        NOWARN_OP(140),
        NULLABLE(141),
        NULLABLE_BY_DEFAULT(142),
        NUM_OF(143),
        OLD(144),
        OR(145),
        POST(146),
        POST_REDUNDANTLY(147),
        PRE_ESC(148),
        PRE(149),
        PRE_REDUNDANTLY(150),
        PRODUCT(151),
        PURE(152),
        READABLE(153),
        REFINING(154),
        REPRESENTS(155),
        REPRESENTS_REDUNDANTLY(156),
        REQUIRES_REDUNDANTLY(157),
        RESULT(158),
        RETURNS(159),
        RETURNS_REDUNDANTLY(160),
        RETURN_BEHAVIOR(161),
        BACKARROW(162),
        RETURN_BEHAVIOUR(163),
        SAFE_MATH(164),
        SET(165),
        SIGNALS(166),
        SIGNALS_ONLY(167),
        SIGNALS_ONLY_REDUNDANTLY(168),
        SIGNALS_REDUNDANTLY(169),
        SPEC_BIGINT_MATH(170),
        SPEC_JAVA_MATH(171),
        SPEC_PACKAGE(172),
        SPEC_PRIVATE(173),
        SPEC_PROTECTED(174),
        SPEC_PUBLIC(175),
        SPEC_SAFE_MATH(176),
        STATIC_INITIALIZER(177),
        STRICTLY_PURE(178),
        SUBTYPE(179),
        SUCH_THAT(180),
        FROM_GOAL(181),
        SUM(182),
        TYPE(183),
        UNINITIALIZED(184),
        UNKNOWN_OP(185),
        UNKNOWN_OP_EQ(186),
        UNREACHABLE(187),
        WARN(188),
        WARN_OP(189),
        WHEN_REDUNDANTLY(190),
        WORKING_SPACE_ESC(191),
        WORKING_SPACE(192),
        WORKING_SPACE_REDUNDANTLY(193),
        WRITABLE(194),
        JML_LINE_COMMENT(195),
        SINGLE_LINE_COMMENT(196),
        JML_ENTER_MULTILINE_COMMENT(197),
        ENTER_JAVADOC_COMMENT(198),
        ENTER_JML_BLOCK_COMMENT(199),
        ENTER_MULTILINE_COMMENT(200),
        JML_BLOCK_COMMENT(201),
        JAVADOC_COMMENT(202),
        MULTI_LINE_COMMENT(203),
        JML_MULTI_LINE_COMMENT(204),
        COMMENT_CONTENT(205),
        ASSERT(206),
        ABSTRACT(207),
        BOOLEAN(208),
        BREAK(209),
        BYTE(210),
        CASE(211),
        CATCH(212),
        CHAR(213),
        CLASS(214),
        CONST(215),
        CONTINUE(216),
        _DEFAULT(217),
        DO(218),
        DOUBLE(219),
        ELSE(220),
        ENUM(221),
        EXTENDS(222),
        FALSE(223),
        FINAL(224),
        FINALLY(225),
        FLOAT(226),
        FOR(227),
        GOTO(228),
        IF(229),
        IMPLEMENTS(230),
        IMPORT(231),
        INSTANCEOF(232),
        INT(233),
        INTERFACE(234),
        LONG(235),
        NATIVE(236),
        NEW(237),
        NON_SEALED(238),
        NULL(239),
        PACKAGE(240),
        PERMITS(241),
        PRIVATE(242),
        PROTECTED(243),
        PUBLIC(244),
        RECORD(245),
        RETURN(246),
        SEALED(247),
        SHORT(248),
        STATIC(249),
        STRICTFP(250),
        SUPER(251),
        SWITCH(252),
        SYNCHRONIZED(253),
        THIS(254),
        THROW(255),
        THROWS(256),
        TRANSIENT(257),
        TRUE(258),
        TRY(259),
        VOID(260),
        VOLATILE(261),
        WHILE(262),
        YIELD(263),
        REQUIRES(264),
        TO(265),
        WITH(266),
        OPEN(267),
        OPENS(268),
        USES(269),
        MODULE(270),
        EXPORTS(271),
        PROVIDES(272),
        TRANSITIVE(273),
        WHEN(274),
        MODEL(275),
        GHOST(276),
        SOURCE(277),
        TRANSACTIONBEGIN(278),
        TRANSACTIONCOMMIT(279),
        TRANSACTIONFINISH(280),
        TRANSACTIONABORT(281),
        RETURNTYPE(282),
        LOOPSCOPE(283),
        METHODFRAME(284),
        EXEC(285),
        CONTINUETYPE(286),
        CCATCH(287),
        CCAT(288),
        BREAKTYPE(289),
        TYPEOF(290),
        SETSV(291),
        CATCHALL(292),
        COMMIT(293),
        FINISH(294),
        ABORT(295),
        RESULTARROW(296),
        LONG_LITERAL(297),
        INTEGER_LITERAL(298),
        DECIMAL_LITERAL(299),
        HEX_LITERAL(300),
        OCTAL_LITERAL(301),
        BINARY_LITERAL(302),
        FLOATING_POINT_LITERAL(303),
        DECIMAL_FLOATING_POINT_LITERAL(304),
        DECIMAL_EXPONENT(305),
        HEXADECIMAL_FLOATING_POINT_LITERAL(306),
        HEXADECIMAL_EXPONENT(307),
        HEX_DIGITS(308),
        UNICODE_ESCAPE(309),
        CHARACTER_LITERAL(310),
        STRING_LITERAL(311),
        ENTER_TEXT_BLOCK(312),
        TEXT_BLOCK_LITERAL(313),
        TEXT_BLOCK_CONTENT(314),
        IDENTIFIER(315),
        JML_IDENTIFIER(316),
        SVIDENTIFIER(317),
        KEYIDENTIFIER(318),
        NON_UNDERSCORE_LETTER(319),
        PART_LETTER(320),
        LPAREN(321),
        RPAREN(322),
        LBRACE(323),
        RBRACE(324),
        LBRACKET(325),
        RBRACKET(326),
        SEMICOLON(327),
        COMMA(328),
        DOTDOT(329),
        ELLIPSIS(330),
        DOT(331),
        AT(332),
        DOUBLECOLON(333),
        ASSIGN(334),
        LT(335),
        BANG(336),
        TILDE(337),
        HOOK(338),
        COLON(339),
        ARROW(340),
        EQ(341),
        GE(342),
        LE(343),
        NE(344),
        SC_AND(345),
        SC_OR(346),
        INCR(347),
        DECR(348),
        PLUS(349),
        MINUS(350),
        STAR(351),
        SLASH(352),
        BIT_AND(353),
        BIT_OR(354),
        XOR(355),
        REM(356),
        LSHIFT(357),
        SHARP(358),
        PLUSASSIGN(359),
        MINUSASSIGN(360),
        STARASSIGN(361),
        SLASHASSIGN(362),
        ANDASSIGN(363),
        ORASSIGN(364),
        XORASSIGN(365),
        REMASSIGN(366),
        LSHIFTASSIGN(367),
        RSIGNEDSHIFTASSIGN(368),
        RUNSIGNEDSHIFTASSIGN(369),
        RUNSIGNEDSHIFT(370),
        RSIGNEDSHIFT(371),
        GT(372),
        CTRL_Z(373),
        UNNAMED_PLACEHOLDER(374);

        private final int kind;

        Kind(int kind) {
            this.kind = kind;
        }

        public static Kind valueOf(int kind) {
            switch (kind) {
                case 374:
                    return UNNAMED_PLACEHOLDER;
                case 373:
                    return CTRL_Z;
                case 372:
                    return GT;
                case 371:
                    return RSIGNEDSHIFT;
                case 370:
                    return RUNSIGNEDSHIFT;
                case 369:
                    return RUNSIGNEDSHIFTASSIGN;
                case 368:
                    return RSIGNEDSHIFTASSIGN;
                case 367:
                    return LSHIFTASSIGN;
                case 366:
                    return REMASSIGN;
                case 365:
                    return XORASSIGN;
                case 364:
                    return ORASSIGN;
                case 363:
                    return ANDASSIGN;
                case 362:
                    return SLASHASSIGN;
                case 361:
                    return STARASSIGN;
                case 360:
                    return MINUSASSIGN;
                case 359:
                    return PLUSASSIGN;
                case 358:
                    return SHARP;
                case 357:
                    return LSHIFT;
                case 356:
                    return REM;
                case 355:
                    return XOR;
                case 354:
                    return BIT_OR;
                case 353:
                    return BIT_AND;
                case 352:
                    return SLASH;
                case 351:
                    return STAR;
                case 350:
                    return MINUS;
                case 349:
                    return PLUS;
                case 348:
                    return DECR;
                case 347:
                    return INCR;
                case 346:
                    return SC_OR;
                case 345:
                    return SC_AND;
                case 344:
                    return NE;
                case 343:
                    return LE;
                case 342:
                    return GE;
                case 341:
                    return EQ;
                case 340:
                    return ARROW;
                case 339:
                    return COLON;
                case 338:
                    return HOOK;
                case 337:
                    return TILDE;
                case 336:
                    return BANG;
                case 335:
                    return LT;
                case 334:
                    return ASSIGN;
                case 333:
                    return DOUBLECOLON;
                case 332:
                    return AT;
                case 331:
                    return DOT;
                case 330:
                    return ELLIPSIS;
                case 329:
                    return DOTDOT;
                case 328:
                    return COMMA;
                case 327:
                    return SEMICOLON;
                case 326:
                    return RBRACKET;
                case 325:
                    return LBRACKET;
                case 324:
                    return RBRACE;
                case 323:
                    return LBRACE;
                case 322:
                    return RPAREN;
                case 321:
                    return LPAREN;
                case 320:
                    return PART_LETTER;
                case 319:
                    return NON_UNDERSCORE_LETTER;
                case 318:
                    return KEYIDENTIFIER;
                case 317:
                    return SVIDENTIFIER;
                case 316:
                    return JML_IDENTIFIER;
                case 315:
                    return IDENTIFIER;
                case 314:
                    return TEXT_BLOCK_CONTENT;
                case 313:
                    return TEXT_BLOCK_LITERAL;
                case 312:
                    return ENTER_TEXT_BLOCK;
                case 311:
                    return STRING_LITERAL;
                case 310:
                    return CHARACTER_LITERAL;
                case 309:
                    return UNICODE_ESCAPE;
                case 308:
                    return HEX_DIGITS;
                case 307:
                    return HEXADECIMAL_EXPONENT;
                case 306:
                    return HEXADECIMAL_FLOATING_POINT_LITERAL;
                case 305:
                    return DECIMAL_EXPONENT;
                case 304:
                    return DECIMAL_FLOATING_POINT_LITERAL;
                case 303:
                    return FLOATING_POINT_LITERAL;
                case 302:
                    return BINARY_LITERAL;
                case 301:
                    return OCTAL_LITERAL;
                case 300:
                    return HEX_LITERAL;
                case 299:
                    return DECIMAL_LITERAL;
                case 298:
                    return INTEGER_LITERAL;
                case 297:
                    return LONG_LITERAL;
                case 296:
                    return RESULTARROW;
                case 295:
                    return ABORT;
                case 294:
                    return FINISH;
                case 293:
                    return COMMIT;
                case 292:
                    return CATCHALL;
                case 291:
                    return SETSV;
                case 290:
                    return TYPEOF;
                case 289:
                    return BREAKTYPE;
                case 288:
                    return CCAT;
                case 287:
                    return CCATCH;
                case 286:
                    return CONTINUETYPE;
                case 285:
                    return EXEC;
                case 284:
                    return METHODFRAME;
                case 283:
                    return LOOPSCOPE;
                case 282:
                    return RETURNTYPE;
                case 281:
                    return TRANSACTIONABORT;
                case 280:
                    return TRANSACTIONFINISH;
                case 279:
                    return TRANSACTIONCOMMIT;
                case 278:
                    return TRANSACTIONBEGIN;
                case 277:
                    return SOURCE;
                case 276:
                    return GHOST;
                case 275:
                    return MODEL;
                case 274:
                    return WHEN;
                case 273:
                    return TRANSITIVE;
                case 272:
                    return PROVIDES;
                case 271:
                    return EXPORTS;
                case 270:
                    return MODULE;
                case 269:
                    return USES;
                case 268:
                    return OPENS;
                case 267:
                    return OPEN;
                case 266:
                    return WITH;
                case 265:
                    return TO;
                case 264:
                    return REQUIRES;
                case 263:
                    return YIELD;
                case 262:
                    return WHILE;
                case 261:
                    return VOLATILE;
                case 260:
                    return VOID;
                case 259:
                    return TRY;
                case 258:
                    return TRUE;
                case 257:
                    return TRANSIENT;
                case 256:
                    return THROWS;
                case 255:
                    return THROW;
                case 254:
                    return THIS;
                case 253:
                    return SYNCHRONIZED;
                case 252:
                    return SWITCH;
                case 251:
                    return SUPER;
                case 250:
                    return STRICTFP;
                case 249:
                    return STATIC;
                case 248:
                    return SHORT;
                case 247:
                    return SEALED;
                case 246:
                    return RETURN;
                case 245:
                    return RECORD;
                case 244:
                    return PUBLIC;
                case 243:
                    return PROTECTED;
                case 242:
                    return PRIVATE;
                case 241:
                    return PERMITS;
                case 240:
                    return PACKAGE;
                case 239:
                    return NULL;
                case 238:
                    return NON_SEALED;
                case 237:
                    return NEW;
                case 236:
                    return NATIVE;
                case 235:
                    return LONG;
                case 234:
                    return INTERFACE;
                case 233:
                    return INT;
                case 232:
                    return INSTANCEOF;
                case 231:
                    return IMPORT;
                case 230:
                    return IMPLEMENTS;
                case 229:
                    return IF;
                case 228:
                    return GOTO;
                case 227:
                    return FOR;
                case 226:
                    return FLOAT;
                case 225:
                    return FINALLY;
                case 224:
                    return FINAL;
                case 223:
                    return FALSE;
                case 222:
                    return EXTENDS;
                case 221:
                    return ENUM;
                case 220:
                    return ELSE;
                case 219:
                    return DOUBLE;
                case 218:
                    return DO;
                case 217:
                    return _DEFAULT;
                case 216:
                    return CONTINUE;
                case 215:
                    return CONST;
                case 214:
                    return CLASS;
                case 213:
                    return CHAR;
                case 212:
                    return CATCH;
                case 211:
                    return CASE;
                case 210:
                    return BYTE;
                case 209:
                    return BREAK;
                case 208:
                    return BOOLEAN;
                case 207:
                    return ABSTRACT;
                case 206:
                    return ASSERT;
                case 205:
                    return COMMENT_CONTENT;
                case 204:
                    return JML_MULTI_LINE_COMMENT;
                case 203:
                    return MULTI_LINE_COMMENT;
                case 202:
                    return JAVADOC_COMMENT;
                case 201:
                    return JML_BLOCK_COMMENT;
                case 200:
                    return ENTER_MULTILINE_COMMENT;
                case 199:
                    return ENTER_JML_BLOCK_COMMENT;
                case 198:
                    return ENTER_JAVADOC_COMMENT;
                case 197:
                    return JML_ENTER_MULTILINE_COMMENT;
                case 196:
                    return SINGLE_LINE_COMMENT;
                case 195:
                    return JML_LINE_COMMENT;
                case 194:
                    return WRITABLE;
                case 193:
                    return WORKING_SPACE_REDUNDANTLY;
                case 192:
                    return WORKING_SPACE;
                case 191:
                    return WORKING_SPACE_ESC;
                case 190:
                    return WHEN_REDUNDANTLY;
                case 189:
                    return WARN_OP;
                case 188:
                    return WARN;
                case 187:
                    return UNREACHABLE;
                case 186:
                    return UNKNOWN_OP_EQ;
                case 185:
                    return UNKNOWN_OP;
                case 184:
                    return UNINITIALIZED;
                case 183:
                    return TYPE;
                case 182:
                    return SUM;
                case 181:
                    return FROM_GOAL;
                case 180:
                    return SUCH_THAT;
                case 179:
                    return SUBTYPE;
                case 178:
                    return STRICTLY_PURE;
                case 177:
                    return STATIC_INITIALIZER;
                case 176:
                    return SPEC_SAFE_MATH;
                case 175:
                    return SPEC_PUBLIC;
                case 174:
                    return SPEC_PROTECTED;
                case 173:
                    return SPEC_PRIVATE;
                case 172:
                    return SPEC_PACKAGE;
                case 171:
                    return SPEC_JAVA_MATH;
                case 170:
                    return SPEC_BIGINT_MATH;
                case 169:
                    return SIGNALS_REDUNDANTLY;
                case 168:
                    return SIGNALS_ONLY_REDUNDANTLY;
                case 167:
                    return SIGNALS_ONLY;
                case 166:
                    return SIGNALS;
                case 165:
                    return SET;
                case 164:
                    return SAFE_MATH;
                case 163:
                    return RETURN_BEHAVIOUR;
                case 162:
                    return BACKARROW;
                case 161:
                    return RETURN_BEHAVIOR;
                case 160:
                    return RETURNS_REDUNDANTLY;
                case 159:
                    return RETURNS;
                case 158:
                    return RESULT;
                case 157:
                    return REQUIRES_REDUNDANTLY;
                case 156:
                    return REPRESENTS_REDUNDANTLY;
                case 155:
                    return REPRESENTS;
                case 154:
                    return REFINING;
                case 153:
                    return READABLE;
                case 152:
                    return PURE;
                case 151:
                    return PRODUCT;
                case 150:
                    return PRE_REDUNDANTLY;
                case 149:
                    return PRE;
                case 148:
                    return PRE_ESC;
                case 147:
                    return POST_REDUNDANTLY;
                case 146:
                    return POST;
                case 145:
                    return OR;
                case 144:
                    return OLD;
                case 143:
                    return NUM_OF;
                case 142:
                    return NULLABLE_BY_DEFAULT;
                case 141:
                    return NULLABLE;
                case 140:
                    return NOWARN_OP;
                case 139:
                    return NOWARN;
                case 138:
                    return NORMAL_EXAMPLE;
                case 137:
                    return FEASIBLE_BEHAVIOUR;
                case 136:
                    return FEASIBLE_BEHAVIOR;
                case 135:
                    return NORMAL_BEHAVIOUR;
                case 134:
                    return NORMAL_BEHAVIOR;
                case 133:
                    return NON_NULL;
                case 132:
                    return NONNULLELEMENTS;
                case 131:
                    return NESTED_CONTRACT_START;
                case 130:
                    return NESTED_CONTRACT_END;
                case 129:
                    return MONITORS_FOR;
                case 128:
                    return MONITORED;
                case 127:
                    return MODIFIES_REDUNDANTLY;
                case 126:
                    return MODIFIES;
                case 125:
                    return LOOP_MODIFIES;
                case 124:
                    return MODIFIABLE_REDUNDANTLY;
                case 123:
                    return MODIFIABLE;
                case 122:
                    return MODEL_PROGRAM;
                case 121:
                    return MIN;
                case 120:
                    return METHOD;
                case 119:
                    return MEASURED_BY_REDUNDANTLY;
                case 118:
                    return ESC_MEASURED_BY;
                case 117:
                    return MEASURED_BY;
                case 116:
                    return MAX;
                case 115:
                    return MAPS_REDUNDANTLY;
                case 114:
                    return MAPS;
                case 113:
                    return MAINTAINING_REDUNDANTLY;
                case 112:
                    return MAINTAINING;
                case 111:
                    return LOOP_INVARIANT_REDUNDANTLY;
                case 110:
                    return LOOP_INVARIANT_FREE;
                case 109:
                    return LOOP_INVARIANT;
                case 108:
                    return LOOP_CONTRACT;
                case 107:
                    return LBL;
                case 106:
                    return LBLPOS;
                case 105:
                    return LBLNEG;
                case 104:
                    return JAVA_MATH;
                case 103:
                    return IN_REDUNDANTLY;
                case 102:
                    return INVARIANT_REDUNDANTLY;
                case 101:
                    return NON_NULL_BY_DEFAULT;
                case 100:
                    return NO_STATE;
                case 99:
                    return TWO_STATE;
                case 98:
                    return INSTANCE;
                case 97:
                    return INITIALLY;
                case 96:
                    return INITIALIZER;
                case 95:
                    return IN;
                case 94:
                    return IMPLIES_THAT;
                case 93:
                    return HENCE_BY_REDUNDANTLY;
                case 92:
                    return HENCE_BY;
                case 91:
                    return HELPER;
                case 90:
                    return END;
                case 89:
                    return BEGIN;
                case 88:
                    return READ_ONLY;
                case 87:
                    return REP;
                case 86:
                    return PEER;
                case 85:
                    return FOR_EXAMPLE;
                case 84:
                    return FORALL;
                case 83:
                    return LET;
                case 82:
                    return FORALLQ;
                case 81:
                    return FIELD;
                case 80:
                    return EXTRACT;
                case 79:
                    return EXSURES_REDUNDANTLY;
                case 78:
                    return EXSURES;
                case 77:
                    return EXISTS;
                case 76:
                    return EXCEPTIONAL_EXAMPLE;
                case 75:
                    return EXCEPTIONAL_BEHAVIOUR;
                case 74:
                    return EXCEPTIONAL_BEHAVIOR;
                case 73:
                    return EXAMPLE;
                case 72:
                    return ERASES;
                case 71:
                    return IMPLICATION_BACKWARD;
                case 70:
                    return IMPLICATION;
                case 69:
                    return EQUIVALENCE;
                case 68:
                    return REQUIRES_FREE;
                case 67:
                    return ENSURES_FREE;
                case 66:
                    return ENSURES_REDUNDANTLY;
                case 65:
                    return ENSURES;
                case 64:
                    return DURATION_REDUNDANTLY;
                case 63:
                    return DURATION;
                case 62:
                    return DIVERGES_REDUNDANTLY;
                case 61:
                    return DIVERGES;
                case 60:
                    return BY;
                case 59:
                    return NEW_OBJECTS;
                case 58:
                    return LOOP_SEPARATES;
                case 57:
                    return SEPARATES;
                case 56:
                    return LOOP_DETERMINES;
                case 55:
                    return DETERMINES;
                case 54:
                    return DECREASING_REDUNDANTLY;
                case 53:
                    return DECREASING;
                case 52:
                    return DECREASES_REDUNDANTLY;
                case 51:
                    return DECREASES;
                case 50:
                    return DECLASSIFIES;
                case 49:
                    return CONTINUE_BEHAVIOUR;
                case 48:
                    return CONTINUE_BEHAVIOR;
                case 47:
                    return CONTINUES_REDUNDANTLY;
                case 46:
                    return CONTINUES;
                case 45:
                    return CONSTRUCTOR;
                case 44:
                    return CONSTRAINT_REDUNDANTLY;
                case 43:
                    return CONSTRAINT;
                case 42:
                    return IMMUTABLE;
                case 41:
                    return CODE_SAFE_MATH;
                case 40:
                    return CODE_JAVA_MATH;
                case 39:
                    return CODE_BIGINT_MATH;
                case 38:
                    return CODE;
                case 37:
                    return CHOOSE_IF;
                case 36:
                    return CHOOSE;
                case 35:
                    return CAPTURES_REDUNDANTLY;
                case 34:
                    return CAPTURES;
                case 33:
                    return CALLABLE_REDUNDANTLY;
                case 32:
                    return CALLABLE;
                case 31:
                    return BREAK_BEHAVIOUR;
                case 30:
                    return BREAK_BEHAVIOR;
                case 29:
                    return BREAKS_REDUNDANTLY;
                case 28:
                    return BREAKS;
                case 27:
                    return BIGINT_MATH;
                case 26:
                    return BIGINT;
                case 25:
                    return BEHAVIOUR;
                case 24:
                    return BEHAVIOR;
                case 23:
                    return LEMMA;
                case 22:
                    return AXIOM;
                case 21:
                    return ASSUME_REDUNDANTLY;
                case 20:
                    return ASSUME;
                case 19:
                    return ASSIGNABLE_REDUNDANTLY;
                case 18:
                    return ASSIGNABLE_FREE;
                case 17:
                    return ASSIGNABLE;
                case 16:
                    return ASSERT_REDUNDANTLY;
                case 15:
                    return JML_ASSERT;
                case 14:
                    return ANTIVALENCE;
                case 13:
                    return ALSO;
                case 12:
                    return ACCESSIBLE_REDUNDANTLY;
                case 11:
                    return ACCESSIBLE;
                case 10:
                    return MODEL_BEHAVIOUR;
                case 9:
                    return MODEL_BEHAVIOR;
                case 8:
                    return ABRUPT_BEHAVIOUR;
                case 7:
                    return ABRUPT_BEHAVIOR;
                case 6:
                    return INVARIANT_FREE;
                case 5:
                    return INVARIANT;
                case 4:
                    return OLD_MAC_EOL;
                case 3:
                    return UNIX_EOL;
                case 2:
                    return WINDOWS_EOL;
                case 1:
                    return SPACE;
                case 0:
                    return EOF;
                default:
                    throw new IllegalArgumentException(f("Token kind %d is unknown.", kind));
            }
        }

        public boolean isPrimitive() {
            return this == BYTE
                    || this == CHAR
                    || this == SHORT
                    || this == INT
                    || this == LONG
                    || this == FLOAT
                    || this == DOUBLE;
        }

        public int getKind() {
            return kind;
        }
    }

    public JavaToken.Category getCategory() {
        return TokenTypes.getCategory(kind);
    }

    /**
     * Inserts newToken into the token list just before this token.
     */
    public void insert(JavaToken newToken) {
        assertNotNull(newToken);
        getPreviousToken().ifPresent(p -> {
            p.nextToken = newToken;
            newToken.previousToken = p;
        });
        previousToken = newToken;
        newToken.nextToken = this;
    }

    /**
     * Inserts newToken into the token list just after this token.
     */
    public void insertAfter(JavaToken newToken) {
        assertNotNull(newToken);
        getNextToken().ifPresent(n -> {
            n.previousToken = newToken;
            newToken.nextToken = n;
        });
        nextToken = newToken;
        newToken.previousToken = this;
    }

    /**
     * Links the tokens around the current token together, making the current token disappear from the list.
     */
    public void deleteToken() {
        final Optional<JavaToken> nextToken = getNextToken();
        final Optional<JavaToken> previousToken = getPreviousToken();
        previousToken.ifPresent(p -> p.nextToken = nextToken.orElse(null));
        nextToken.ifPresent(n -> n.previousToken = previousToken.orElse(null));
    }

    /**
     * Replaces the current token with newToken.
     */
    public void replaceToken(JavaToken newToken) {
        assertNotNull(newToken);
        getPreviousToken().ifPresent(p -> {
            p.nextToken = newToken;
            newToken.previousToken = p;
        });
        getNextToken().ifPresent(n -> {
            n.previousToken = newToken;
            newToken.nextToken = n;
        });
    }

    /**
     * @return the last token in the token list.
     */
    public JavaToken findLastToken() {
        JavaToken current = this;
        while (current.getNextToken().isPresent()) {
            current = current.getNextToken().get();
        }
        return current;
    }

    /**
     * @return the first token in the token list.
     */
    public JavaToken findFirstToken() {
        JavaToken current = this;
        while (current.getPreviousToken().isPresent()) {
            current = current.getPreviousToken().get();
        }
        return current;
    }

    @Override
    public int hashCode() {
        int result = kind;
        result = 31 * result + text.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaToken javaToken = (JavaToken) o;
        if (kind != javaToken.kind) return false;
        if (!text.equals(javaToken.text)) return false;
        return true;
    }
}
