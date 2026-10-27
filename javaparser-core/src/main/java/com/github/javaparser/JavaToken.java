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
        ASSIGNABLE_REDUNDANTLY(18),
        ASSUME(19),
        ASSUME_REDUNDANTLY(20),
        AXIOM(21),
        LEMMA(22),
        BEHAVIOR(23),
        BEHAVIOUR(24),
        BIGINT(25),
        BIGINT_MATH(26),
        BREAKS(27),
        BREAKS_REDUNDANTLY(28),
        BREAK_BEHAVIOR(29),
        BREAK_BEHAVIOUR(30),
        CALLABLE(31),
        CALLABLE_REDUNDANTLY(32),
        CAPTURES(33),
        CAPTURES_REDUNDANTLY(34),
        CHOOSE(35),
        CHOOSE_IF(36),
        CODE(37),
        CODE_BIGINT_MATH(38),
        CODE_JAVA_MATH(39),
        CODE_SAFE_MATH(40),
        IMMUTABLE(41),
        CONSTRAINT(42),
        CONSTRAINT_REDUNDANTLY(43),
        CONSTRUCTOR(44),
        CONTINUES(45),
        CONTINUES_REDUNDANTLY(46),
        CONTINUE_BEHAVIOR(47),
        CONTINUE_BEHAVIOUR(48),
        DECLASSIFIES(49),
        DECREASES(50),
        DECREASES_REDUNDANTLY(51),
        DECREASING(52),
        DECREASING_REDUNDANTLY(53),
        DETERMINES(54),
        LOOP_DETERMINES(55),
        SEPARATES(56),
        LOOP_SEPARATES(57),
        NEW_OBJECTS(58),
        BY(59),
        DIVERGES(60),
        DIVERGES_REDUNDANTLY(61),
        DURATION(62),
        DURATION_REDUNDANTLY(63),
        ENSURES(64),
        ENSURES_REDUNDANTLY(65),
        ENSURES_FREE(66),
        REQUIRES_FREE(67),
        EQUIVALENCE(68),
        IMPLICATION(69),
        IMPLICATION_BACKWARD(70),
        ERASES(71),
        EXAMPLE(72),
        EXCEPTIONAL_BEHAVIOR(73),
        EXCEPTIONAL_BEHAVIOUR(74),
        EXCEPTIONAL_EXAMPLE(75),
        EXISTS(76),
        EXSURES(77),
        EXSURES_REDUNDANTLY(78),
        EXTRACT(79),
        FIELD(80),
        FORALLQ(81),
        LET(82),
        FORALL(83),
        FOR_EXAMPLE(84),
        PEER(85),
        REP(86),
        READ_ONLY(87),
        BEGIN(88),
        END(89),
        HELPER(90),
        HENCE_BY(91),
        HENCE_BY_REDUNDANTLY(92),
        IMPLIES_THAT(93),
        IN(94),
        INITIALIZER(95),
        INITIALLY(96),
        INSTANCE(97),
        TWO_STATE(98),
        NO_STATE(99),
        NON_NULL_BY_DEFAULT(100),
        INVARIANT_REDUNDANTLY(101),
        IN_REDUNDANTLY(102),
        JAVA_MATH(103),
        LBLNEG(104),
        LBLPOS(105),
        LBL(106),
        LOOP_CONTRACT(107),
        LOOP_INVARIANT(108),
        LOOP_INVARIANT_FREE(109),
        LOOP_INVARIANT_REDUNDANTLY(110),
        MAINTAINING(111),
        MAINTAINING_REDUNDANTLY(112),
        MAPS(113),
        MAPS_REDUNDANTLY(114),
        MAX(115),
        MEASURED_BY(116),
        ESC_MEASURED_BY(117),
        MEASURED_BY_REDUNDANTLY(118),
        METHOD(119),
        MIN(120),
        MODEL_PROGRAM(121),
        MODIFIABLE(122),
        MODIFIABLE_REDUNDANTLY(123),
        LOOP_MODIFIES(124),
        MODIFIES(125),
        MODIFIES_REDUNDANTLY(126),
        MONITORED(127),
        MONITORS_FOR(128),
        NESTED_CONTRACT_END(129),
        NESTED_CONTRACT_START(130),
        NONNULLELEMENTS(131),
        NON_NULL(132),
        NORMAL_BEHAVIOR(133),
        NORMAL_BEHAVIOUR(134),
        FEASIBLE_BEHAVIOR(135),
        FEASIBLE_BEHAVIOUR(136),
        NORMAL_EXAMPLE(137),
        NOWARN(138),
        NOWARN_OP(139),
        NULLABLE(140),
        NULLABLE_BY_DEFAULT(141),
        NUM_OF(142),
        OLD(143),
        OR(144),
        POST(145),
        POST_REDUNDANTLY(146),
        PRE_ESC(147),
        PRE(148),
        PRE_REDUNDANTLY(149),
        PRODUCT(150),
        PURE(151),
        READABLE(152),
        REFINING(153),
        REPRESENTS(154),
        REPRESENTS_REDUNDANTLY(155),
        REQUIRES_REDUNDANTLY(156),
        RESULT(157),
        RETURNS(158),
        RETURNS_REDUNDANTLY(159),
        RETURN_BEHAVIOR(160),
        BACKARROW(161),
        RETURN_BEHAVIOUR(162),
        SAFE_MATH(163),
        SET(164),
        SIGNALS(165),
        SIGNALS_ONLY(166),
        SIGNALS_ONLY_REDUNDANTLY(167),
        SIGNALS_REDUNDANTLY(168),
        SPEC_BIGINT_MATH(169),
        SPEC_JAVA_MATH(170),
        SPEC_PACKAGE(171),
        SPEC_PRIVATE(172),
        SPEC_PROTECTED(173),
        SPEC_PUBLIC(174),
        SPEC_SAFE_MATH(175),
        STATIC_INITIALIZER(176),
        STRICTLY_PURE(177),
        SUBTYPE(178),
        SUCH_THAT(179),
        FROM_GOAL(180),
        SUM(181),
        TYPE(182),
        UNINITIALIZED(183),
        UNKNOWN_OP(184),
        UNKNOWN_OP_EQ(185),
        UNREACHABLE(186),
        WARN(187),
        WARN_OP(188),
        WHEN_REDUNDANTLY(189),
        WORKING_SPACE_ESC(190),
        WORKING_SPACE(191),
        WORKING_SPACE_REDUNDANTLY(192),
        WRITABLE(193),
        JML_LINE_COMMENT(194),
        SINGLE_LINE_COMMENT(195),
        JML_ENTER_MULTILINE_COMMENT(196),
        ENTER_JAVADOC_COMMENT(197),
        ENTER_JML_BLOCK_COMMENT(198),
        ENTER_MULTILINE_COMMENT(199),
        JML_BLOCK_COMMENT(200),
        JAVADOC_COMMENT(201),
        MULTI_LINE_COMMENT(202),
        JML_MULTI_LINE_COMMENT(203),
        COMMENT_CONTENT(204),
        ASSERT(205),
        ABSTRACT(206),
        BOOLEAN(207),
        BREAK(208),
        BYTE(209),
        CASE(210),
        CATCH(211),
        CHAR(212),
        CLASS(213),
        CONST(214),
        CONTINUE(215),
        _DEFAULT(216),
        DO(217),
        DOUBLE(218),
        ELSE(219),
        ENUM(220),
        EXTENDS(221),
        FALSE(222),
        FINAL(223),
        FINALLY(224),
        FLOAT(225),
        FOR(226),
        GOTO(227),
        IF(228),
        IMPLEMENTS(229),
        IMPORT(230),
        INSTANCEOF(231),
        INT(232),
        INTERFACE(233),
        LONG(234),
        NATIVE(235),
        NEW(236),
        NON_SEALED(237),
        NULL(238),
        PACKAGE(239),
        PERMITS(240),
        PRIVATE(241),
        PROTECTED(242),
        PUBLIC(243),
        RECORD(244),
        RETURN(245),
        SEALED(246),
        SHORT(247),
        STATIC(248),
        STRICTFP(249),
        SUPER(250),
        SWITCH(251),
        SYNCHRONIZED(252),
        THIS(253),
        THROW(254),
        THROWS(255),
        TRANSIENT(256),
        TRUE(257),
        TRY(258),
        VOID(259),
        VOLATILE(260),
        WHILE(261),
        YIELD(262),
        REQUIRES(263),
        TO(264),
        WITH(265),
        OPEN(266),
        OPENS(267),
        USES(268),
        MODULE(269),
        EXPORTS(270),
        PROVIDES(271),
        TRANSITIVE(272),
        WHEN(273),
        MODEL(274),
        GHOST(275),
        SOURCE(276),
        TRANSACTIONBEGIN(277),
        TRANSACTIONCOMMIT(278),
        TRANSACTIONFINISH(279),
        TRANSACTIONABORT(280),
        RETURNTYPE(281),
        LOOPSCOPE(282),
        MERGE_POINT(283),
        METHODFRAME(284),
        EXEC(285),
        CONTINUETYPE(286),
        CCATCH(287),
        CCAT(288),
        BREAKTYPE(289),
        TYPEOF(290),
        SWITCHTOIF(291),
        UNPACK(292),
        REATTACHLOOPINVARIANT(293),
        FORINITUNFOLDTRANSFORMER(294),
        LOOPSCOPEINVARIANTTRANSFORMER(295),
        SETSV(296),
        ISSTATIC(297),
        EVALARGS(298),
        REPLACEARGS(299),
        UNWINDLOOP(300),
        CATCHALL(301),
        COMMIT(302),
        FINISH(303),
        ABORT(304),
        UNWIND_LOOP_BOUNDED(305),
        FORTOWHILE(306),
        DOBREAK(307),
        METHODCALL(308),
        EXPANDMETHODBODY(309),
        CONSTRUCTORCALL(310),
        SPECIALCONSTRUCTORECALL(311),
        POSTWORK(312),
        STATICINITIALIZATION(313),
        RESOLVE_MULTIPLE_VAR_DECL(314),
        ARRAY_POST_DECL(315),
        ARRAY_INIT_CREATION(316),
        ARRAY_INIT_CREATION_TRANSIENT(317),
        ARRAY_INIT_CREATION_ASSIGNMENTS(318),
        ENHANCEDFOR_ELIM(319),
        STATIC_EVALUATE(320),
        CREATE_OBJECT(321),
        LENGTHREF(322),
        RESULTARROW(323),
        LONG_LITERAL(324),
        INTEGER_LITERAL(325),
        DECIMAL_LITERAL(326),
        HEX_LITERAL(327),
        OCTAL_LITERAL(328),
        BINARY_LITERAL(329),
        FLOATING_POINT_LITERAL(330),
        DECIMAL_FLOATING_POINT_LITERAL(331),
        DECIMAL_EXPONENT(332),
        HEXADECIMAL_FLOATING_POINT_LITERAL(333),
        HEXADECIMAL_EXPONENT(334),
        HEX_DIGITS(335),
        UNICODE_ESCAPE(336),
        CHARACTER_LITERAL(337),
        STRING_LITERAL(338),
        ENTER_TEXT_BLOCK(339),
        TEXT_BLOCK_LITERAL(340),
        TEXT_BLOCK_CONTENT(341),
        IDENTIFIER(342),
        JML_IDENTIFIER(343),
        SVIDENTIFIER(344),
        KEYIDENTIFIER(345),
        NON_UNDERSCORE_LETTER(346),
        PART_LETTER(347),
        LPAREN(348),
        RPAREN(349),
        LBRACE(350),
        RBRACE(351),
        LBRACKET(352),
        RBRACKET(353),
        SEMICOLON(354),
        COMMA(355),
        DOTDOT(356),
        ELLIPSIS(357),
        DOT(358),
        AT(359),
        DOUBLECOLON(360),
        ASSIGN(361),
        LT(362),
        BANG(363),
        TILDE(364),
        HOOK(365),
        COLON(366),
        ARROW(367),
        EQ(368),
        GE(369),
        LE(370),
        NE(371),
        SC_AND(372),
        SC_OR(373),
        INCR(374),
        DECR(375),
        PLUS(376),
        MINUS(377),
        STAR(378),
        SLASH(379),
        BIT_AND(380),
        BIT_OR(381),
        XOR(382),
        REM(383),
        LSHIFT(384),
        SHARP(385),
        PLUSASSIGN(386),
        MINUSASSIGN(387),
        STARASSIGN(388),
        SLASHASSIGN(389),
        ANDASSIGN(390),
        ORASSIGN(391),
        XORASSIGN(392),
        REMASSIGN(393),
        LSHIFTASSIGN(394),
        RSIGNEDSHIFTASSIGN(395),
        RUNSIGNEDSHIFTASSIGN(396),
        RUNSIGNEDSHIFT(397),
        RSIGNEDSHIFT(398),
        GT(399),
        CTRL_Z(400),
        UNNAMED_PLACEHOLDER(401);

        private final int kind;

        Kind(int kind) {
            this.kind = kind;
        }

        public static Kind valueOf(int kind) {
            switch (kind) {
                case 401:
                    return UNNAMED_PLACEHOLDER;
                case 400:
                    return CTRL_Z;
                case 399:
                    return GT;
                case 398:
                    return RSIGNEDSHIFT;
                case 397:
                    return RUNSIGNEDSHIFT;
                case 396:
                    return RUNSIGNEDSHIFTASSIGN;
                case 395:
                    return RSIGNEDSHIFTASSIGN;
                case 394:
                    return LSHIFTASSIGN;
                case 393:
                    return REMASSIGN;
                case 392:
                    return XORASSIGN;
                case 391:
                    return ORASSIGN;
                case 390:
                    return ANDASSIGN;
                case 389:
                    return SLASHASSIGN;
                case 388:
                    return STARASSIGN;
                case 387:
                    return MINUSASSIGN;
                case 386:
                    return PLUSASSIGN;
                case 385:
                    return SHARP;
                case 384:
                    return LSHIFT;
                case 383:
                    return REM;
                case 382:
                    return XOR;
                case 381:
                    return BIT_OR;
                case 380:
                    return BIT_AND;
                case 379:
                    return SLASH;
                case 378:
                    return STAR;
                case 377:
                    return MINUS;
                case 376:
                    return PLUS;
                case 375:
                    return DECR;
                case 374:
                    return INCR;
                case 373:
                    return SC_OR;
                case 372:
                    return SC_AND;
                case 371:
                    return NE;
                case 370:
                    return LE;
                case 369:
                    return GE;
                case 368:
                    return EQ;
                case 367:
                    return ARROW;
                case 366:
                    return COLON;
                case 365:
                    return HOOK;
                case 364:
                    return TILDE;
                case 363:
                    return BANG;
                case 362:
                    return LT;
                case 361:
                    return ASSIGN;
                case 360:
                    return DOUBLECOLON;
                case 359:
                    return AT;
                case 358:
                    return DOT;
                case 357:
                    return ELLIPSIS;
                case 356:
                    return DOTDOT;
                case 355:
                    return COMMA;
                case 354:
                    return SEMICOLON;
                case 353:
                    return RBRACKET;
                case 352:
                    return LBRACKET;
                case 351:
                    return RBRACE;
                case 350:
                    return LBRACE;
                case 349:
                    return RPAREN;
                case 348:
                    return LPAREN;
                case 347:
                    return PART_LETTER;
                case 346:
                    return NON_UNDERSCORE_LETTER;
                case 345:
                    return KEYIDENTIFIER;
                case 344:
                    return SVIDENTIFIER;
                case 343:
                    return JML_IDENTIFIER;
                case 342:
                    return IDENTIFIER;
                case 341:
                    return TEXT_BLOCK_CONTENT;
                case 340:
                    return TEXT_BLOCK_LITERAL;
                case 339:
                    return ENTER_TEXT_BLOCK;
                case 338:
                    return STRING_LITERAL;
                case 337:
                    return CHARACTER_LITERAL;
                case 336:
                    return UNICODE_ESCAPE;
                case 335:
                    return HEX_DIGITS;
                case 334:
                    return HEXADECIMAL_EXPONENT;
                case 333:
                    return HEXADECIMAL_FLOATING_POINT_LITERAL;
                case 332:
                    return DECIMAL_EXPONENT;
                case 331:
                    return DECIMAL_FLOATING_POINT_LITERAL;
                case 330:
                    return FLOATING_POINT_LITERAL;
                case 329:
                    return BINARY_LITERAL;
                case 328:
                    return OCTAL_LITERAL;
                case 327:
                    return HEX_LITERAL;
                case 326:
                    return DECIMAL_LITERAL;
                case 325:
                    return INTEGER_LITERAL;
                case 324:
                    return LONG_LITERAL;
                case 323:
                    return RESULTARROW;
                case 322:
                    return LENGTHREF;
                case 321:
                    return CREATE_OBJECT;
                case 320:
                    return STATIC_EVALUATE;
                case 319:
                    return ENHANCEDFOR_ELIM;
                case 318:
                    return ARRAY_INIT_CREATION_ASSIGNMENTS;
                case 317:
                    return ARRAY_INIT_CREATION_TRANSIENT;
                case 316:
                    return ARRAY_INIT_CREATION;
                case 315:
                    return ARRAY_POST_DECL;
                case 314:
                    return RESOLVE_MULTIPLE_VAR_DECL;
                case 313:
                    return STATICINITIALIZATION;
                case 312:
                    return POSTWORK;
                case 311:
                    return SPECIALCONSTRUCTORECALL;
                case 310:
                    return CONSTRUCTORCALL;
                case 309:
                    return EXPANDMETHODBODY;
                case 308:
                    return METHODCALL;
                case 307:
                    return DOBREAK;
                case 306:
                    return FORTOWHILE;
                case 305:
                    return UNWIND_LOOP_BOUNDED;
                case 304:
                    return ABORT;
                case 303:
                    return FINISH;
                case 302:
                    return COMMIT;
                case 301:
                    return CATCHALL;
                case 300:
                    return UNWINDLOOP;
                case 299:
                    return REPLACEARGS;
                case 298:
                    return EVALARGS;
                case 297:
                    return ISSTATIC;
                case 296:
                    return SETSV;
                case 295:
                    return LOOPSCOPEINVARIANTTRANSFORMER;
                case 294:
                    return FORINITUNFOLDTRANSFORMER;
                case 293:
                    return REATTACHLOOPINVARIANT;
                case 292:
                    return UNPACK;
                case 291:
                    return SWITCHTOIF;
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
                    return MERGE_POINT;
                case 282:
                    return LOOPSCOPE;
                case 281:
                    return RETURNTYPE;
                case 280:
                    return TRANSACTIONABORT;
                case 279:
                    return TRANSACTIONFINISH;
                case 278:
                    return TRANSACTIONCOMMIT;
                case 277:
                    return TRANSACTIONBEGIN;
                case 276:
                    return SOURCE;
                case 275:
                    return GHOST;
                case 274:
                    return MODEL;
                case 273:
                    return WHEN;
                case 272:
                    return TRANSITIVE;
                case 271:
                    return PROVIDES;
                case 270:
                    return EXPORTS;
                case 269:
                    return MODULE;
                case 268:
                    return USES;
                case 267:
                    return OPENS;
                case 266:
                    return OPEN;
                case 265:
                    return WITH;
                case 264:
                    return TO;
                case 263:
                    return REQUIRES;
                case 262:
                    return YIELD;
                case 261:
                    return WHILE;
                case 260:
                    return VOLATILE;
                case 259:
                    return VOID;
                case 258:
                    return TRY;
                case 257:
                    return TRUE;
                case 256:
                    return TRANSIENT;
                case 255:
                    return THROWS;
                case 254:
                    return THROW;
                case 253:
                    return THIS;
                case 252:
                    return SYNCHRONIZED;
                case 251:
                    return SWITCH;
                case 250:
                    return SUPER;
                case 249:
                    return STRICTFP;
                case 248:
                    return STATIC;
                case 247:
                    return SHORT;
                case 246:
                    return SEALED;
                case 245:
                    return RETURN;
                case 244:
                    return RECORD;
                case 243:
                    return PUBLIC;
                case 242:
                    return PROTECTED;
                case 241:
                    return PRIVATE;
                case 240:
                    return PERMITS;
                case 239:
                    return PACKAGE;
                case 238:
                    return NULL;
                case 237:
                    return NON_SEALED;
                case 236:
                    return NEW;
                case 235:
                    return NATIVE;
                case 234:
                    return LONG;
                case 233:
                    return INTERFACE;
                case 232:
                    return INT;
                case 231:
                    return INSTANCEOF;
                case 230:
                    return IMPORT;
                case 229:
                    return IMPLEMENTS;
                case 228:
                    return IF;
                case 227:
                    return GOTO;
                case 226:
                    return FOR;
                case 225:
                    return FLOAT;
                case 224:
                    return FINALLY;
                case 223:
                    return FINAL;
                case 222:
                    return FALSE;
                case 221:
                    return EXTENDS;
                case 220:
                    return ENUM;
                case 219:
                    return ELSE;
                case 218:
                    return DOUBLE;
                case 217:
                    return DO;
                case 216:
                    return _DEFAULT;
                case 215:
                    return CONTINUE;
                case 214:
                    return CONST;
                case 213:
                    return CLASS;
                case 212:
                    return CHAR;
                case 211:
                    return CATCH;
                case 210:
                    return CASE;
                case 209:
                    return BYTE;
                case 208:
                    return BREAK;
                case 207:
                    return BOOLEAN;
                case 206:
                    return ABSTRACT;
                case 205:
                    return ASSERT;
                case 204:
                    return COMMENT_CONTENT;
                case 203:
                    return JML_MULTI_LINE_COMMENT;
                case 202:
                    return MULTI_LINE_COMMENT;
                case 201:
                    return JAVADOC_COMMENT;
                case 200:
                    return JML_BLOCK_COMMENT;
                case 199:
                    return ENTER_MULTILINE_COMMENT;
                case 198:
                    return ENTER_JML_BLOCK_COMMENT;
                case 197:
                    return ENTER_JAVADOC_COMMENT;
                case 196:
                    return JML_ENTER_MULTILINE_COMMENT;
                case 195:
                    return SINGLE_LINE_COMMENT;
                case 194:
                    return JML_LINE_COMMENT;
                case 193:
                    return WRITABLE;
                case 192:
                    return WORKING_SPACE_REDUNDANTLY;
                case 191:
                    return WORKING_SPACE;
                case 190:
                    return WORKING_SPACE_ESC;
                case 189:
                    return WHEN_REDUNDANTLY;
                case 188:
                    return WARN_OP;
                case 187:
                    return WARN;
                case 186:
                    return UNREACHABLE;
                case 185:
                    return UNKNOWN_OP_EQ;
                case 184:
                    return UNKNOWN_OP;
                case 183:
                    return UNINITIALIZED;
                case 182:
                    return TYPE;
                case 181:
                    return SUM;
                case 180:
                    return FROM_GOAL;
                case 179:
                    return SUCH_THAT;
                case 178:
                    return SUBTYPE;
                case 177:
                    return STRICTLY_PURE;
                case 176:
                    return STATIC_INITIALIZER;
                case 175:
                    return SPEC_SAFE_MATH;
                case 174:
                    return SPEC_PUBLIC;
                case 173:
                    return SPEC_PROTECTED;
                case 172:
                    return SPEC_PRIVATE;
                case 171:
                    return SPEC_PACKAGE;
                case 170:
                    return SPEC_JAVA_MATH;
                case 169:
                    return SPEC_BIGINT_MATH;
                case 168:
                    return SIGNALS_REDUNDANTLY;
                case 167:
                    return SIGNALS_ONLY_REDUNDANTLY;
                case 166:
                    return SIGNALS_ONLY;
                case 165:
                    return SIGNALS;
                case 164:
                    return SET;
                case 163:
                    return SAFE_MATH;
                case 162:
                    return RETURN_BEHAVIOUR;
                case 161:
                    return BACKARROW;
                case 160:
                    return RETURN_BEHAVIOR;
                case 159:
                    return RETURNS_REDUNDANTLY;
                case 158:
                    return RETURNS;
                case 157:
                    return RESULT;
                case 156:
                    return REQUIRES_REDUNDANTLY;
                case 155:
                    return REPRESENTS_REDUNDANTLY;
                case 154:
                    return REPRESENTS;
                case 153:
                    return REFINING;
                case 152:
                    return READABLE;
                case 151:
                    return PURE;
                case 150:
                    return PRODUCT;
                case 149:
                    return PRE_REDUNDANTLY;
                case 148:
                    return PRE;
                case 147:
                    return PRE_ESC;
                case 146:
                    return POST_REDUNDANTLY;
                case 145:
                    return POST;
                case 144:
                    return OR;
                case 143:
                    return OLD;
                case 142:
                    return NUM_OF;
                case 141:
                    return NULLABLE_BY_DEFAULT;
                case 140:
                    return NULLABLE;
                case 139:
                    return NOWARN_OP;
                case 138:
                    return NOWARN;
                case 137:
                    return NORMAL_EXAMPLE;
                case 136:
                    return FEASIBLE_BEHAVIOUR;
                case 135:
                    return FEASIBLE_BEHAVIOR;
                case 134:
                    return NORMAL_BEHAVIOUR;
                case 133:
                    return NORMAL_BEHAVIOR;
                case 132:
                    return NON_NULL;
                case 131:
                    return NONNULLELEMENTS;
                case 130:
                    return NESTED_CONTRACT_START;
                case 129:
                    return NESTED_CONTRACT_END;
                case 128:
                    return MONITORS_FOR;
                case 127:
                    return MONITORED;
                case 126:
                    return MODIFIES_REDUNDANTLY;
                case 125:
                    return MODIFIES;
                case 124:
                    return LOOP_MODIFIES;
                case 123:
                    return MODIFIABLE_REDUNDANTLY;
                case 122:
                    return MODIFIABLE;
                case 121:
                    return MODEL_PROGRAM;
                case 120:
                    return MIN;
                case 119:
                    return METHOD;
                case 118:
                    return MEASURED_BY_REDUNDANTLY;
                case 117:
                    return ESC_MEASURED_BY;
                case 116:
                    return MEASURED_BY;
                case 115:
                    return MAX;
                case 114:
                    return MAPS_REDUNDANTLY;
                case 113:
                    return MAPS;
                case 112:
                    return MAINTAINING_REDUNDANTLY;
                case 111:
                    return MAINTAINING;
                case 110:
                    return LOOP_INVARIANT_REDUNDANTLY;
                case 109:
                    return LOOP_INVARIANT_FREE;
                case 108:
                    return LOOP_INVARIANT;
                case 107:
                    return LOOP_CONTRACT;
                case 106:
                    return LBL;
                case 105:
                    return LBLPOS;
                case 104:
                    return LBLNEG;
                case 103:
                    return JAVA_MATH;
                case 102:
                    return IN_REDUNDANTLY;
                case 101:
                    return INVARIANT_REDUNDANTLY;
                case 100:
                    return NON_NULL_BY_DEFAULT;
                case 99:
                    return NO_STATE;
                case 98:
                    return TWO_STATE;
                case 97:
                    return INSTANCE;
                case 96:
                    return INITIALLY;
                case 95:
                    return INITIALIZER;
                case 94:
                    return IN;
                case 93:
                    return IMPLIES_THAT;
                case 92:
                    return HENCE_BY_REDUNDANTLY;
                case 91:
                    return HENCE_BY;
                case 90:
                    return HELPER;
                case 89:
                    return END;
                case 88:
                    return BEGIN;
                case 87:
                    return READ_ONLY;
                case 86:
                    return REP;
                case 85:
                    return PEER;
                case 84:
                    return FOR_EXAMPLE;
                case 83:
                    return FORALL;
                case 82:
                    return LET;
                case 81:
                    return FORALLQ;
                case 80:
                    return FIELD;
                case 79:
                    return EXTRACT;
                case 78:
                    return EXSURES_REDUNDANTLY;
                case 77:
                    return EXSURES;
                case 76:
                    return EXISTS;
                case 75:
                    return EXCEPTIONAL_EXAMPLE;
                case 74:
                    return EXCEPTIONAL_BEHAVIOUR;
                case 73:
                    return EXCEPTIONAL_BEHAVIOR;
                case 72:
                    return EXAMPLE;
                case 71:
                    return ERASES;
                case 70:
                    return IMPLICATION_BACKWARD;
                case 69:
                    return IMPLICATION;
                case 68:
                    return EQUIVALENCE;
                case 67:
                    return REQUIRES_FREE;
                case 66:
                    return ENSURES_FREE;
                case 65:
                    return ENSURES_REDUNDANTLY;
                case 64:
                    return ENSURES;
                case 63:
                    return DURATION_REDUNDANTLY;
                case 62:
                    return DURATION;
                case 61:
                    return DIVERGES_REDUNDANTLY;
                case 60:
                    return DIVERGES;
                case 59:
                    return BY;
                case 58:
                    return NEW_OBJECTS;
                case 57:
                    return LOOP_SEPARATES;
                case 56:
                    return SEPARATES;
                case 55:
                    return LOOP_DETERMINES;
                case 54:
                    return DETERMINES;
                case 53:
                    return DECREASING_REDUNDANTLY;
                case 52:
                    return DECREASING;
                case 51:
                    return DECREASES_REDUNDANTLY;
                case 50:
                    return DECREASES;
                case 49:
                    return DECLASSIFIES;
                case 48:
                    return CONTINUE_BEHAVIOUR;
                case 47:
                    return CONTINUE_BEHAVIOR;
                case 46:
                    return CONTINUES_REDUNDANTLY;
                case 45:
                    return CONTINUES;
                case 44:
                    return CONSTRUCTOR;
                case 43:
                    return CONSTRAINT_REDUNDANTLY;
                case 42:
                    return CONSTRAINT;
                case 41:
                    return IMMUTABLE;
                case 40:
                    return CODE_SAFE_MATH;
                case 39:
                    return CODE_JAVA_MATH;
                case 38:
                    return CODE_BIGINT_MATH;
                case 37:
                    return CODE;
                case 36:
                    return CHOOSE_IF;
                case 35:
                    return CHOOSE;
                case 34:
                    return CAPTURES_REDUNDANTLY;
                case 33:
                    return CAPTURES;
                case 32:
                    return CALLABLE_REDUNDANTLY;
                case 31:
                    return CALLABLE;
                case 30:
                    return BREAK_BEHAVIOUR;
                case 29:
                    return BREAK_BEHAVIOR;
                case 28:
                    return BREAKS_REDUNDANTLY;
                case 27:
                    return BREAKS;
                case 26:
                    return BIGINT_MATH;
                case 25:
                    return BIGINT;
                case 24:
                    return BEHAVIOUR;
                case 23:
                    return BEHAVIOR;
                case 22:
                    return LEMMA;
                case 21:
                    return AXIOM;
                case 20:
                    return ASSUME_REDUNDANTLY;
                case 19:
                    return ASSUME;
                case 18:
                    return ASSIGNABLE_REDUNDANTLY;
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
