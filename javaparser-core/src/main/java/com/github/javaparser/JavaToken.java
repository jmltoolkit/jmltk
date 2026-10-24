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
        BEHAVIOR(22),
        BEHAVIOUR(23),
        BIGINT(24),
        BIGINT_MATH(25),
        BREAKS(26),
        BREAKS_REDUNDANTLY(27),
        BREAK_BEHAVIOR(28),
        BREAK_BEHAVIOUR(29),
        CALLABLE(30),
        CALLABLE_REDUNDANTLY(31),
        CAPTURES(32),
        CAPTURES_REDUNDANTLY(33),
        CHOOSE(34),
        CHOOSE_IF(35),
        CODE(36),
        CODE_BIGINT_MATH(37),
        CODE_JAVA_MATH(38),
        CODE_SAFE_MATH(39),
        IMMUTABLE(40),
        CONSTRAINT(41),
        CONSTRAINT_REDUNDANTLY(42),
        CONSTRUCTOR(43),
        CONTINUES(44),
        CONTINUES_REDUNDANTLY(45),
        CONTINUE_BEHAVIOR(46),
        CONTINUE_BEHAVIOUR(47),
        DECLASSIFIES(48),
        DECREASES(49),
        DECREASES_REDUNDANTLY(50),
        DECREASING(51),
        DECREASING_REDUNDANTLY(52),
        DETERMINES(53),
        LOOP_DETERMINES(54),
        SEPARATES(55),
        LOOP_SEPARATES(56),
        NEW_OBJECTS(57),
        BY(58),
        DIVERGES(59),
        DIVERGES_REDUNDANTLY(60),
        DURATION(61),
        DURATION_REDUNDANTLY(62),
        ENSURES(63),
        ENSURES_REDUNDANTLY(64),
        ENSURES_FREE(65),
        REQUIRES_FREE(66),
        EQUIVALENCE(67),
        IMPLICATION(68),
        IMPLICATION_BACKWARD(69),
        ERASES(70),
        EXAMPLE(71),
        EXCEPTIONAL_BEHAVIOR(72),
        EXCEPTIONAL_BEHAVIOUR(73),
        EXCEPTIONAL_EXAMPLE(74),
        EXISTS(75),
        EXSURES(76),
        EXSURES_REDUNDANTLY(77),
        EXTRACT(78),
        FIELD(79),
        FORALLQ(80),
        LET(81),
        FORALL(82),
        FOR_EXAMPLE(83),
        PEER(84),
        REP(85),
        READ_ONLY(86),
        BEGIN(87),
        END(88),
        HELPER(89),
        HENCE_BY(90),
        HENCE_BY_REDUNDANTLY(91),
        IMPLIES_THAT(92),
        IN(93),
        INITIALIZER(94),
        INITIALLY(95),
        INSTANCE(96),
        TWO_STATE(97),
        NO_STATE(98),
        NON_NULL_BY_DEFAULT(99),
        INVARIANT_REDUNDANTLY(100),
        IN_REDUNDANTLY(101),
        JAVA_MATH(102),
        LBLNEG(103),
        LBLPOS(104),
        LBL(105),
        LOOP_CONTRACT(106),
        LOOP_INVARIANT(107),
        LOOP_INVARIANT_FREE(108),
        LOOP_INVARIANT_REDUNDANTLY(109),
        MAINTAINING(110),
        MAINTAINING_REDUNDANTLY(111),
        MAPS(112),
        MAPS_REDUNDANTLY(113),
        MAX(114),
        MEASURED_BY(115),
        ESC_MEASURED_BY(116),
        MEASURED_BY_REDUNDANTLY(117),
        METHOD(118),
        MIN(119),
        MODEL_PROGRAM(120),
        MODIFIABLE(121),
        MODIFIABLE_REDUNDANTLY(122),
        LOOP_MODIFIES(123),
        MODIFIES(124),
        MODIFIES_REDUNDANTLY(125),
        MONITORED(126),
        MONITORS_FOR(127),
        NESTED_CONTRACT_END(128),
        NESTED_CONTRACT_START(129),
        NONNULLELEMENTS(130),
        NON_NULL(131),
        NORMAL_BEHAVIOR(132),
        NORMAL_BEHAVIOUR(133),
        FEASIBLE_BEHAVIOR(134),
        FEASIBLE_BEHAVIOUR(135),
        NORMAL_EXAMPLE(136),
        NOWARN(137),
        NOWARN_OP(138),
        NULLABLE(139),
        NULLABLE_BY_DEFAULT(140),
        NUM_OF(141),
        OLD(142),
        OR(143),
        POST(144),
        POST_REDUNDANTLY(145),
        PRE_ESC(146),
        PRE(147),
        PRE_REDUNDANTLY(148),
        PRODUCT(149),
        PURE(150),
        READABLE(151),
        REFINING(152),
        REPRESENTS(153),
        REPRESENTS_REDUNDANTLY(154),
        REQUIRES_REDUNDANTLY(155),
        RESULT(156),
        RETURNS(157),
        RETURNS_REDUNDANTLY(158),
        RETURN_BEHAVIOR(159),
        BACKARROW(160),
        RETURN_BEHAVIOUR(161),
        SAFE_MATH(162),
        SET(163),
        SIGNALS(164),
        SIGNALS_ONLY(165),
        SIGNALS_ONLY_REDUNDANTLY(166),
        SIGNALS_REDUNDANTLY(167),
        SPEC_BIGINT_MATH(168),
        SPEC_JAVA_MATH(169),
        SPEC_PACKAGE(170),
        SPEC_PRIVATE(171),
        SPEC_PROTECTED(172),
        SPEC_PUBLIC(173),
        SPEC_SAFE_MATH(174),
        STATIC_INITIALIZER(175),
        STRICTLY_PURE(176),
        SUBTYPE(177),
        SUCH_THAT(178),
        FROM_GOAL(179),
        SUM(180),
        TYPE(181),
        UNINITIALIZED(182),
        UNKNOWN_OP(183),
        UNKNOWN_OP_EQ(184),
        UNREACHABLE(185),
        WARN(186),
        WARN_OP(187),
        WHEN_REDUNDANTLY(188),
        WORKING_SPACE_ESC(189),
        WORKING_SPACE(190),
        WORKING_SPACE_REDUNDANTLY(191),
        WRITABLE(192),
        JML_LINE_COMMENT(193),
        SINGLE_LINE_COMMENT(194),
        JML_ENTER_MULTILINE_COMMENT(195),
        ENTER_JAVADOC_COMMENT(196),
        ENTER_JML_BLOCK_COMMENT(197),
        ENTER_MULTILINE_COMMENT(198),
        JML_BLOCK_COMMENT(199),
        JAVADOC_COMMENT(200),
        MULTI_LINE_COMMENT(201),
        JML_MULTI_LINE_COMMENT(202),
        COMMENT_CONTENT(203),
        ASSERT(204),
        ABSTRACT(205),
        BOOLEAN(206),
        BREAK(207),
        BYTE(208),
        CASE(209),
        CATCH(210),
        CHAR(211),
        CLASS(212),
        CONST(213),
        CONTINUE(214),
        _DEFAULT(215),
        DO(216),
        DOUBLE(217),
        ELSE(218),
        ENUM(219),
        EXTENDS(220),
        FALSE(221),
        FINAL(222),
        FINALLY(223),
        FLOAT(224),
        FOR(225),
        GOTO(226),
        IF(227),
        IMPLEMENTS(228),
        IMPORT(229),
        INSTANCEOF(230),
        INT(231),
        INTERFACE(232),
        LONG(233),
        NATIVE(234),
        NEW(235),
        NON_SEALED(236),
        NULL(237),
        PACKAGE(238),
        PERMITS(239),
        PRIVATE(240),
        PROTECTED(241),
        PUBLIC(242),
        RECORD(243),
        RETURN(244),
        SEALED(245),
        SHORT(246),
        STATIC(247),
        STRICTFP(248),
        SUPER(249),
        SWITCH(250),
        SYNCHRONIZED(251),
        THIS(252),
        THROW(253),
        THROWS(254),
        TRANSIENT(255),
        TRUE(256),
        TRY(257),
        VOID(258),
        VOLATILE(259),
        WHILE(260),
        YIELD(261),
        REQUIRES(262),
        TO(263),
        WITH(264),
        OPEN(265),
        OPENS(266),
        USES(267),
        MODULE(268),
        EXPORTS(269),
        PROVIDES(270),
        TRANSITIVE(271),
        WHEN(272),
        MODEL(273),
        GHOST(274),
        SOURCE(275),
        TRANSACTIONBEGIN(276),
        TRANSACTIONCOMMIT(277),
        TRANSACTIONFINISH(278),
        TRANSACTIONABORT(279),
        RETURNTYPE(280),
        LOOPSCOPE(281),
        MERGE_POINT(282),
        METHODFRAME(283),
        EXEC(284),
        CONTINUETYPE(285),
        CCATCH(286),
        CCAT(287),
        BREAKTYPE(288),
        TYPEOF(289),
        SWITCHTOIF(290),
        UNPACK(291),
        REATTACHLOOPINVARIANT(292),
        FORINITUNFOLDTRANSFORMER(293),
        LOOPSCOPEINVARIANTTRANSFORMER(294),
        SETSV(295),
        ISSTATIC(296),
        EVALARGS(297),
        REPLACEARGS(298),
        UNWINDLOOP(299),
        CATCHALL(300),
        COMMIT(301),
        FINISH(302),
        ABORT(303),
        UNWIND_LOOP_BOUNDED(304),
        FORTOWHILE(305),
        DOBREAK(306),
        METHODCALL(307),
        EXPANDMETHODBODY(308),
        CONSTRUCTORCALL(309),
        SPECIALCONSTRUCTORECALL(310),
        POSTWORK(311),
        STATICINITIALIZATION(312),
        RESOLVE_MULTIPLE_VAR_DECL(313),
        ARRAY_POST_DECL(314),
        ARRAY_INIT_CREATION(315),
        ARRAY_INIT_CREATION_TRANSIENT(316),
        ARRAY_INIT_CREATION_ASSIGNMENTS(317),
        ENHANCEDFOR_ELIM(318),
        STATIC_EVALUATE(319),
        CREATE_OBJECT(320),
        LENGTHREF(321),
        RESULTARROW(322),
        LONG_LITERAL(323),
        INTEGER_LITERAL(324),
        DECIMAL_LITERAL(325),
        HEX_LITERAL(326),
        OCTAL_LITERAL(327),
        BINARY_LITERAL(328),
        FLOATING_POINT_LITERAL(329),
        DECIMAL_FLOATING_POINT_LITERAL(330),
        DECIMAL_EXPONENT(331),
        HEXADECIMAL_FLOATING_POINT_LITERAL(332),
        HEXADECIMAL_EXPONENT(333),
        HEX_DIGITS(334),
        UNICODE_ESCAPE(335),
        CHARACTER_LITERAL(336),
        STRING_LITERAL(337),
        ENTER_TEXT_BLOCK(338),
        TEXT_BLOCK_LITERAL(339),
        TEXT_BLOCK_CONTENT(340),
        IDENTIFIER(341),
        JML_IDENTIFIER(342),
        SVIDENTIFIER(343),
        KEYIDENTIFIER(344),
        NON_UNDERSCORE_LETTER(345),
        PART_LETTER(346),
        LPAREN(347),
        RPAREN(348),
        LBRACE(349),
        RBRACE(350),
        LBRACKET(351),
        RBRACKET(352),
        SEMICOLON(353),
        COMMA(354),
        DOTDOT(355),
        ELLIPSIS(356),
        DOT(357),
        AT(358),
        DOUBLECOLON(359),
        ASSIGN(360),
        LT(361),
        BANG(362),
        TILDE(363),
        HOOK(364),
        COLON(365),
        ARROW(366),
        EQ(367),
        GE(368),
        LE(369),
        NE(370),
        SC_AND(371),
        SC_OR(372),
        INCR(373),
        DECR(374),
        PLUS(375),
        MINUS(376),
        STAR(377),
        SLASH(378),
        BIT_AND(379),
        BIT_OR(380),
        XOR(381),
        REM(382),
        LSHIFT(383),
        SHARP(384),
        PLUSASSIGN(385),
        MINUSASSIGN(386),
        STARASSIGN(387),
        SLASHASSIGN(388),
        ANDASSIGN(389),
        ORASSIGN(390),
        XORASSIGN(391),
        REMASSIGN(392),
        LSHIFTASSIGN(393),
        RSIGNEDSHIFTASSIGN(394),
        RUNSIGNEDSHIFTASSIGN(395),
        RUNSIGNEDSHIFT(396),
        RSIGNEDSHIFT(397),
        GT(398),
        CTRL_Z(399),
        UNNAMED_PLACEHOLDER(400);

        private final int kind;

        Kind(int kind) {
            this.kind = kind;
        }

        public static Kind valueOf(int kind) {
            switch (kind) {
                case 400:
                    return UNNAMED_PLACEHOLDER;
                case 399:
                    return CTRL_Z;
                case 398:
                    return GT;
                case 397:
                    return RSIGNEDSHIFT;
                case 396:
                    return RUNSIGNEDSHIFT;
                case 395:
                    return RUNSIGNEDSHIFTASSIGN;
                case 394:
                    return RSIGNEDSHIFTASSIGN;
                case 393:
                    return LSHIFTASSIGN;
                case 392:
                    return REMASSIGN;
                case 391:
                    return XORASSIGN;
                case 390:
                    return ORASSIGN;
                case 389:
                    return ANDASSIGN;
                case 388:
                    return SLASHASSIGN;
                case 387:
                    return STARASSIGN;
                case 386:
                    return MINUSASSIGN;
                case 385:
                    return PLUSASSIGN;
                case 384:
                    return SHARP;
                case 383:
                    return LSHIFT;
                case 382:
                    return REM;
                case 381:
                    return XOR;
                case 380:
                    return BIT_OR;
                case 379:
                    return BIT_AND;
                case 378:
                    return SLASH;
                case 377:
                    return STAR;
                case 376:
                    return MINUS;
                case 375:
                    return PLUS;
                case 374:
                    return DECR;
                case 373:
                    return INCR;
                case 372:
                    return SC_OR;
                case 371:
                    return SC_AND;
                case 370:
                    return NE;
                case 369:
                    return LE;
                case 368:
                    return GE;
                case 367:
                    return EQ;
                case 366:
                    return ARROW;
                case 365:
                    return COLON;
                case 364:
                    return HOOK;
                case 363:
                    return TILDE;
                case 362:
                    return BANG;
                case 361:
                    return LT;
                case 360:
                    return ASSIGN;
                case 359:
                    return DOUBLECOLON;
                case 358:
                    return AT;
                case 357:
                    return DOT;
                case 356:
                    return ELLIPSIS;
                case 355:
                    return DOTDOT;
                case 354:
                    return COMMA;
                case 353:
                    return SEMICOLON;
                case 352:
                    return RBRACKET;
                case 351:
                    return LBRACKET;
                case 350:
                    return RBRACE;
                case 349:
                    return LBRACE;
                case 348:
                    return RPAREN;
                case 347:
                    return LPAREN;
                case 346:
                    return PART_LETTER;
                case 345:
                    return NON_UNDERSCORE_LETTER;
                case 344:
                    return KEYIDENTIFIER;
                case 343:
                    return SVIDENTIFIER;
                case 342:
                    return JML_IDENTIFIER;
                case 341:
                    return IDENTIFIER;
                case 340:
                    return TEXT_BLOCK_CONTENT;
                case 339:
                    return TEXT_BLOCK_LITERAL;
                case 338:
                    return ENTER_TEXT_BLOCK;
                case 337:
                    return STRING_LITERAL;
                case 336:
                    return CHARACTER_LITERAL;
                case 335:
                    return UNICODE_ESCAPE;
                case 334:
                    return HEX_DIGITS;
                case 333:
                    return HEXADECIMAL_EXPONENT;
                case 332:
                    return HEXADECIMAL_FLOATING_POINT_LITERAL;
                case 331:
                    return DECIMAL_EXPONENT;
                case 330:
                    return DECIMAL_FLOATING_POINT_LITERAL;
                case 329:
                    return FLOATING_POINT_LITERAL;
                case 328:
                    return BINARY_LITERAL;
                case 327:
                    return OCTAL_LITERAL;
                case 326:
                    return HEX_LITERAL;
                case 325:
                    return DECIMAL_LITERAL;
                case 324:
                    return INTEGER_LITERAL;
                case 323:
                    return LONG_LITERAL;
                case 322:
                    return RESULTARROW;
                case 321:
                    return LENGTHREF;
                case 320:
                    return CREATE_OBJECT;
                case 319:
                    return STATIC_EVALUATE;
                case 318:
                    return ENHANCEDFOR_ELIM;
                case 317:
                    return ARRAY_INIT_CREATION_ASSIGNMENTS;
                case 316:
                    return ARRAY_INIT_CREATION_TRANSIENT;
                case 315:
                    return ARRAY_INIT_CREATION;
                case 314:
                    return ARRAY_POST_DECL;
                case 313:
                    return RESOLVE_MULTIPLE_VAR_DECL;
                case 312:
                    return STATICINITIALIZATION;
                case 311:
                    return POSTWORK;
                case 310:
                    return SPECIALCONSTRUCTORECALL;
                case 309:
                    return CONSTRUCTORCALL;
                case 308:
                    return EXPANDMETHODBODY;
                case 307:
                    return METHODCALL;
                case 306:
                    return DOBREAK;
                case 305:
                    return FORTOWHILE;
                case 304:
                    return UNWIND_LOOP_BOUNDED;
                case 303:
                    return ABORT;
                case 302:
                    return FINISH;
                case 301:
                    return COMMIT;
                case 300:
                    return CATCHALL;
                case 299:
                    return UNWINDLOOP;
                case 298:
                    return REPLACEARGS;
                case 297:
                    return EVALARGS;
                case 296:
                    return ISSTATIC;
                case 295:
                    return SETSV;
                case 294:
                    return LOOPSCOPEINVARIANTTRANSFORMER;
                case 293:
                    return FORINITUNFOLDTRANSFORMER;
                case 292:
                    return REATTACHLOOPINVARIANT;
                case 291:
                    return UNPACK;
                case 290:
                    return SWITCHTOIF;
                case 289:
                    return TYPEOF;
                case 288:
                    return BREAKTYPE;
                case 287:
                    return CCAT;
                case 286:
                    return CCATCH;
                case 285:
                    return CONTINUETYPE;
                case 284:
                    return EXEC;
                case 283:
                    return METHODFRAME;
                case 282:
                    return MERGE_POINT;
                case 281:
                    return LOOPSCOPE;
                case 280:
                    return RETURNTYPE;
                case 279:
                    return TRANSACTIONABORT;
                case 278:
                    return TRANSACTIONFINISH;
                case 277:
                    return TRANSACTIONCOMMIT;
                case 276:
                    return TRANSACTIONBEGIN;
                case 275:
                    return SOURCE;
                case 274:
                    return GHOST;
                case 273:
                    return MODEL;
                case 272:
                    return WHEN;
                case 271:
                    return TRANSITIVE;
                case 270:
                    return PROVIDES;
                case 269:
                    return EXPORTS;
                case 268:
                    return MODULE;
                case 267:
                    return USES;
                case 266:
                    return OPENS;
                case 265:
                    return OPEN;
                case 264:
                    return WITH;
                case 263:
                    return TO;
                case 262:
                    return REQUIRES;
                case 261:
                    return YIELD;
                case 260:
                    return WHILE;
                case 259:
                    return VOLATILE;
                case 258:
                    return VOID;
                case 257:
                    return TRY;
                case 256:
                    return TRUE;
                case 255:
                    return TRANSIENT;
                case 254:
                    return THROWS;
                case 253:
                    return THROW;
                case 252:
                    return THIS;
                case 251:
                    return SYNCHRONIZED;
                case 250:
                    return SWITCH;
                case 249:
                    return SUPER;
                case 248:
                    return STRICTFP;
                case 247:
                    return STATIC;
                case 246:
                    return SHORT;
                case 245:
                    return SEALED;
                case 244:
                    return RETURN;
                case 243:
                    return RECORD;
                case 242:
                    return PUBLIC;
                case 241:
                    return PROTECTED;
                case 240:
                    return PRIVATE;
                case 239:
                    return PERMITS;
                case 238:
                    return PACKAGE;
                case 237:
                    return NULL;
                case 236:
                    return NON_SEALED;
                case 235:
                    return NEW;
                case 234:
                    return NATIVE;
                case 233:
                    return LONG;
                case 232:
                    return INTERFACE;
                case 231:
                    return INT;
                case 230:
                    return INSTANCEOF;
                case 229:
                    return IMPORT;
                case 228:
                    return IMPLEMENTS;
                case 227:
                    return IF;
                case 226:
                    return GOTO;
                case 225:
                    return FOR;
                case 224:
                    return FLOAT;
                case 223:
                    return FINALLY;
                case 222:
                    return FINAL;
                case 221:
                    return FALSE;
                case 220:
                    return EXTENDS;
                case 219:
                    return ENUM;
                case 218:
                    return ELSE;
                case 217:
                    return DOUBLE;
                case 216:
                    return DO;
                case 215:
                    return _DEFAULT;
                case 214:
                    return CONTINUE;
                case 213:
                    return CONST;
                case 212:
                    return CLASS;
                case 211:
                    return CHAR;
                case 210:
                    return CATCH;
                case 209:
                    return CASE;
                case 208:
                    return BYTE;
                case 207:
                    return BREAK;
                case 206:
                    return BOOLEAN;
                case 205:
                    return ABSTRACT;
                case 204:
                    return ASSERT;
                case 203:
                    return COMMENT_CONTENT;
                case 202:
                    return JML_MULTI_LINE_COMMENT;
                case 201:
                    return MULTI_LINE_COMMENT;
                case 200:
                    return JAVADOC_COMMENT;
                case 199:
                    return JML_BLOCK_COMMENT;
                case 198:
                    return ENTER_MULTILINE_COMMENT;
                case 197:
                    return ENTER_JML_BLOCK_COMMENT;
                case 196:
                    return ENTER_JAVADOC_COMMENT;
                case 195:
                    return JML_ENTER_MULTILINE_COMMENT;
                case 194:
                    return SINGLE_LINE_COMMENT;
                case 193:
                    return JML_LINE_COMMENT;
                case 192:
                    return WRITABLE;
                case 191:
                    return WORKING_SPACE_REDUNDANTLY;
                case 190:
                    return WORKING_SPACE;
                case 189:
                    return WORKING_SPACE_ESC;
                case 188:
                    return WHEN_REDUNDANTLY;
                case 187:
                    return WARN_OP;
                case 186:
                    return WARN;
                case 185:
                    return UNREACHABLE;
                case 184:
                    return UNKNOWN_OP_EQ;
                case 183:
                    return UNKNOWN_OP;
                case 182:
                    return UNINITIALIZED;
                case 181:
                    return TYPE;
                case 180:
                    return SUM;
                case 179:
                    return FROM_GOAL;
                case 178:
                    return SUCH_THAT;
                case 177:
                    return SUBTYPE;
                case 176:
                    return STRICTLY_PURE;
                case 175:
                    return STATIC_INITIALIZER;
                case 174:
                    return SPEC_SAFE_MATH;
                case 173:
                    return SPEC_PUBLIC;
                case 172:
                    return SPEC_PROTECTED;
                case 171:
                    return SPEC_PRIVATE;
                case 170:
                    return SPEC_PACKAGE;
                case 169:
                    return SPEC_JAVA_MATH;
                case 168:
                    return SPEC_BIGINT_MATH;
                case 167:
                    return SIGNALS_REDUNDANTLY;
                case 166:
                    return SIGNALS_ONLY_REDUNDANTLY;
                case 165:
                    return SIGNALS_ONLY;
                case 164:
                    return SIGNALS;
                case 163:
                    return SET;
                case 162:
                    return SAFE_MATH;
                case 161:
                    return RETURN_BEHAVIOUR;
                case 160:
                    return BACKARROW;
                case 159:
                    return RETURN_BEHAVIOR;
                case 158:
                    return RETURNS_REDUNDANTLY;
                case 157:
                    return RETURNS;
                case 156:
                    return RESULT;
                case 155:
                    return REQUIRES_REDUNDANTLY;
                case 154:
                    return REPRESENTS_REDUNDANTLY;
                case 153:
                    return REPRESENTS;
                case 152:
                    return REFINING;
                case 151:
                    return READABLE;
                case 150:
                    return PURE;
                case 149:
                    return PRODUCT;
                case 148:
                    return PRE_REDUNDANTLY;
                case 147:
                    return PRE;
                case 146:
                    return PRE_ESC;
                case 145:
                    return POST_REDUNDANTLY;
                case 144:
                    return POST;
                case 143:
                    return OR;
                case 142:
                    return OLD;
                case 141:
                    return NUM_OF;
                case 140:
                    return NULLABLE_BY_DEFAULT;
                case 139:
                    return NULLABLE;
                case 138:
                    return NOWARN_OP;
                case 137:
                    return NOWARN;
                case 136:
                    return NORMAL_EXAMPLE;
                case 135:
                    return FEASIBLE_BEHAVIOUR;
                case 134:
                    return FEASIBLE_BEHAVIOR;
                case 133:
                    return NORMAL_BEHAVIOUR;
                case 132:
                    return NORMAL_BEHAVIOR;
                case 131:
                    return NON_NULL;
                case 130:
                    return NONNULLELEMENTS;
                case 129:
                    return NESTED_CONTRACT_START;
                case 128:
                    return NESTED_CONTRACT_END;
                case 127:
                    return MONITORS_FOR;
                case 126:
                    return MONITORED;
                case 125:
                    return MODIFIES_REDUNDANTLY;
                case 124:
                    return MODIFIES;
                case 123:
                    return LOOP_MODIFIES;
                case 122:
                    return MODIFIABLE_REDUNDANTLY;
                case 121:
                    return MODIFIABLE;
                case 120:
                    return MODEL_PROGRAM;
                case 119:
                    return MIN;
                case 118:
                    return METHOD;
                case 117:
                    return MEASURED_BY_REDUNDANTLY;
                case 116:
                    return ESC_MEASURED_BY;
                case 115:
                    return MEASURED_BY;
                case 114:
                    return MAX;
                case 113:
                    return MAPS_REDUNDANTLY;
                case 112:
                    return MAPS;
                case 111:
                    return MAINTAINING_REDUNDANTLY;
                case 110:
                    return MAINTAINING;
                case 109:
                    return LOOP_INVARIANT_REDUNDANTLY;
                case 108:
                    return LOOP_INVARIANT_FREE;
                case 107:
                    return LOOP_INVARIANT;
                case 106:
                    return LOOP_CONTRACT;
                case 105:
                    return LBL;
                case 104:
                    return LBLPOS;
                case 103:
                    return LBLNEG;
                case 102:
                    return JAVA_MATH;
                case 101:
                    return IN_REDUNDANTLY;
                case 100:
                    return INVARIANT_REDUNDANTLY;
                case 99:
                    return NON_NULL_BY_DEFAULT;
                case 98:
                    return NO_STATE;
                case 97:
                    return TWO_STATE;
                case 96:
                    return INSTANCE;
                case 95:
                    return INITIALLY;
                case 94:
                    return INITIALIZER;
                case 93:
                    return IN;
                case 92:
                    return IMPLIES_THAT;
                case 91:
                    return HENCE_BY_REDUNDANTLY;
                case 90:
                    return HENCE_BY;
                case 89:
                    return HELPER;
                case 88:
                    return END;
                case 87:
                    return BEGIN;
                case 86:
                    return READ_ONLY;
                case 85:
                    return REP;
                case 84:
                    return PEER;
                case 83:
                    return FOR_EXAMPLE;
                case 82:
                    return FORALL;
                case 81:
                    return LET;
                case 80:
                    return FORALLQ;
                case 79:
                    return FIELD;
                case 78:
                    return EXTRACT;
                case 77:
                    return EXSURES_REDUNDANTLY;
                case 76:
                    return EXSURES;
                case 75:
                    return EXISTS;
                case 74:
                    return EXCEPTIONAL_EXAMPLE;
                case 73:
                    return EXCEPTIONAL_BEHAVIOUR;
                case 72:
                    return EXCEPTIONAL_BEHAVIOR;
                case 71:
                    return EXAMPLE;
                case 70:
                    return ERASES;
                case 69:
                    return IMPLICATION_BACKWARD;
                case 68:
                    return IMPLICATION;
                case 67:
                    return EQUIVALENCE;
                case 66:
                    return REQUIRES_FREE;
                case 65:
                    return ENSURES_FREE;
                case 64:
                    return ENSURES_REDUNDANTLY;
                case 63:
                    return ENSURES;
                case 62:
                    return DURATION_REDUNDANTLY;
                case 61:
                    return DURATION;
                case 60:
                    return DIVERGES_REDUNDANTLY;
                case 59:
                    return DIVERGES;
                case 58:
                    return BY;
                case 57:
                    return NEW_OBJECTS;
                case 56:
                    return LOOP_SEPARATES;
                case 55:
                    return SEPARATES;
                case 54:
                    return LOOP_DETERMINES;
                case 53:
                    return DETERMINES;
                case 52:
                    return DECREASING_REDUNDANTLY;
                case 51:
                    return DECREASING;
                case 50:
                    return DECREASES_REDUNDANTLY;
                case 49:
                    return DECREASES;
                case 48:
                    return DECLASSIFIES;
                case 47:
                    return CONTINUE_BEHAVIOUR;
                case 46:
                    return CONTINUE_BEHAVIOR;
                case 45:
                    return CONTINUES_REDUNDANTLY;
                case 44:
                    return CONTINUES;
                case 43:
                    return CONSTRUCTOR;
                case 42:
                    return CONSTRAINT_REDUNDANTLY;
                case 41:
                    return CONSTRAINT;
                case 40:
                    return IMMUTABLE;
                case 39:
                    return CODE_SAFE_MATH;
                case 38:
                    return CODE_JAVA_MATH;
                case 37:
                    return CODE_BIGINT_MATH;
                case 36:
                    return CODE;
                case 35:
                    return CHOOSE_IF;
                case 34:
                    return CHOOSE;
                case 33:
                    return CAPTURES_REDUNDANTLY;
                case 32:
                    return CAPTURES;
                case 31:
                    return CALLABLE_REDUNDANTLY;
                case 30:
                    return CALLABLE;
                case 29:
                    return BREAK_BEHAVIOUR;
                case 28:
                    return BREAK_BEHAVIOR;
                case 27:
                    return BREAKS_REDUNDANTLY;
                case 26:
                    return BREAKS;
                case 25:
                    return BIGINT_MATH;
                case 24:
                    return BIGINT;
                case 23:
                    return BEHAVIOUR;
                case 22:
                    return BEHAVIOR;
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
