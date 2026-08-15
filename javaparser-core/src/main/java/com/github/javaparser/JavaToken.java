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
        GHOST(87),
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
        MODEL(121),
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
        SOURCE(274),
        TRANSACTIONBEGIN(275),
        TRANSACTIONCOMMIT(276),
        TRANSACTIONFINISH(277),
        TRANSACTIONABORT(278),
        RETURNTYPE(279),
        LOOPSCOPE(280),
        MERGE_POINT(281),
        METHODFRAME(282),
        EXEC(283),
        CONTINUETYPE(284),
        CCATCH(285),
        CCAT(286),
        BREAKTYPE(287),
        TYPEOF(288),
        SWITCHTOIF(289),
        UNPACK(290),
        REATTACHLOOPINVARIANT(291),
        FORINITUNFOLDTRANSFORMER(292),
        LOOPSCOPEINVARIANTTRANSFORMER(293),
        SETSV(294),
        ISSTATIC(295),
        EVALARGS(296),
        REPLACEARGS(297),
        UNWINDLOOP(298),
        CATCHALL(299),
        COMMIT(300),
        FINISH(301),
        ABORT(302),
        UNWIND_LOOP_BOUNDED(303),
        FORTOWHILE(304),
        DOBREAK(305),
        METHODCALL(306),
        EXPANDMETHODBODY(307),
        CONSTRUCTORCALL(308),
        SPECIALCONSTRUCTORECALL(309),
        POSTWORK(310),
        STATICINITIALIZATION(311),
        RESOLVE_MULTIPLE_VAR_DECL(312),
        ARRAY_POST_DECL(313),
        ARRAY_INIT_CREATION(314),
        ARRAY_INIT_CREATION_TRANSIENT(315),
        ARRAY_INIT_CREATION_ASSIGNMENTS(316),
        ENHANCEDFOR_ELIM(317),
        STATIC_EVALUATE(318),
        CREATE_OBJECT(319),
        LENGTHREF(320),
        RESULTARROW(321),
        LONG_LITERAL(322),
        INTEGER_LITERAL(323),
        DECIMAL_LITERAL(324),
        HEX_LITERAL(325),
        OCTAL_LITERAL(326),
        BINARY_LITERAL(327),
        FLOATING_POINT_LITERAL(328),
        DECIMAL_FLOATING_POINT_LITERAL(329),
        DECIMAL_EXPONENT(330),
        HEXADECIMAL_FLOATING_POINT_LITERAL(331),
        HEXADECIMAL_EXPONENT(332),
        HEX_DIGITS(333),
        UNICODE_ESCAPE(334),
        CHARACTER_LITERAL(335),
        STRING_LITERAL(336),
        ENTER_TEXT_BLOCK(337),
        TEXT_BLOCK_LITERAL(338),
        TEXT_BLOCK_CONTENT(339),
        IDENTIFIER(340),
        JML_IDENTIFIER(341),
        SVIDENTIFIER(342),
        KEYIDENTIFIER(343),
        NON_UNDERSCORE_LETTER(344),
        PART_LETTER(345),
        LPAREN(346),
        RPAREN(347),
        LBRACE(348),
        RBRACE(349),
        LBRACKET(350),
        RBRACKET(351),
        SEMICOLON(352),
        COMMA(353),
        DOTDOT(354),
        ELLIPSIS(355),
        DOT(356),
        AT(357),
        DOUBLECOLON(358),
        ASSIGN(359),
        LT(360),
        BANG(361),
        TILDE(362),
        HOOK(363),
        COLON(364),
        ARROW(365),
        EQ(366),
        GE(367),
        LE(368),
        NE(369),
        SC_AND(370),
        SC_OR(371),
        INCR(372),
        DECR(373),
        PLUS(374),
        MINUS(375),
        STAR(376),
        SLASH(377),
        BIT_AND(378),
        BIT_OR(379),
        XOR(380),
        REM(381),
        LSHIFT(382),
        SHARP(383),
        PLUSASSIGN(384),
        MINUSASSIGN(385),
        STARASSIGN(386),
        SLASHASSIGN(387),
        ANDASSIGN(388),
        ORASSIGN(389),
        XORASSIGN(390),
        REMASSIGN(391),
        LSHIFTASSIGN(392),
        RSIGNEDSHIFTASSIGN(393),
        RUNSIGNEDSHIFTASSIGN(394),
        RUNSIGNEDSHIFT(395),
        RSIGNEDSHIFT(396),
        GT(397),
        CTRL_Z(398),
        UNNAMED_PLACEHOLDER(399);

        private final int kind;

        Kind(int kind) {
            this.kind = kind;
        }

        public static Kind valueOf(int kind) {
            switch (kind) {
                case 399:
                    return UNNAMED_PLACEHOLDER;
                case 398:
                    return CTRL_Z;
                case 397:
                    return GT;
                case 396:
                    return RSIGNEDSHIFT;
                case 395:
                    return RUNSIGNEDSHIFT;
                case 394:
                    return RUNSIGNEDSHIFTASSIGN;
                case 393:
                    return RSIGNEDSHIFTASSIGN;
                case 392:
                    return LSHIFTASSIGN;
                case 391:
                    return REMASSIGN;
                case 390:
                    return XORASSIGN;
                case 389:
                    return ORASSIGN;
                case 388:
                    return ANDASSIGN;
                case 387:
                    return SLASHASSIGN;
                case 386:
                    return STARASSIGN;
                case 385:
                    return MINUSASSIGN;
                case 384:
                    return PLUSASSIGN;
                case 383:
                    return SHARP;
                case 382:
                    return LSHIFT;
                case 381:
                    return REM;
                case 380:
                    return XOR;
                case 379:
                    return BIT_OR;
                case 378:
                    return BIT_AND;
                case 377:
                    return SLASH;
                case 376:
                    return STAR;
                case 375:
                    return MINUS;
                case 374:
                    return PLUS;
                case 373:
                    return DECR;
                case 372:
                    return INCR;
                case 371:
                    return SC_OR;
                case 370:
                    return SC_AND;
                case 369:
                    return NE;
                case 368:
                    return LE;
                case 367:
                    return GE;
                case 366:
                    return EQ;
                case 365:
                    return ARROW;
                case 364:
                    return COLON;
                case 363:
                    return HOOK;
                case 362:
                    return TILDE;
                case 361:
                    return BANG;
                case 360:
                    return LT;
                case 359:
                    return ASSIGN;
                case 358:
                    return DOUBLECOLON;
                case 357:
                    return AT;
                case 356:
                    return DOT;
                case 355:
                    return ELLIPSIS;
                case 354:
                    return DOTDOT;
                case 353:
                    return COMMA;
                case 352:
                    return SEMICOLON;
                case 351:
                    return RBRACKET;
                case 350:
                    return LBRACKET;
                case 349:
                    return RBRACE;
                case 348:
                    return LBRACE;
                case 347:
                    return RPAREN;
                case 346:
                    return LPAREN;
                case 345:
                    return PART_LETTER;
                case 344:
                    return NON_UNDERSCORE_LETTER;
                case 343:
                    return KEYIDENTIFIER;
                case 342:
                    return SVIDENTIFIER;
                case 341:
                    return JML_IDENTIFIER;
                case 340:
                    return IDENTIFIER;
                case 339:
                    return TEXT_BLOCK_CONTENT;
                case 338:
                    return TEXT_BLOCK_LITERAL;
                case 337:
                    return ENTER_TEXT_BLOCK;
                case 336:
                    return STRING_LITERAL;
                case 335:
                    return CHARACTER_LITERAL;
                case 334:
                    return UNICODE_ESCAPE;
                case 333:
                    return HEX_DIGITS;
                case 332:
                    return HEXADECIMAL_EXPONENT;
                case 331:
                    return HEXADECIMAL_FLOATING_POINT_LITERAL;
                case 330:
                    return DECIMAL_EXPONENT;
                case 329:
                    return DECIMAL_FLOATING_POINT_LITERAL;
                case 328:
                    return FLOATING_POINT_LITERAL;
                case 327:
                    return BINARY_LITERAL;
                case 326:
                    return OCTAL_LITERAL;
                case 325:
                    return HEX_LITERAL;
                case 324:
                    return DECIMAL_LITERAL;
                case 323:
                    return INTEGER_LITERAL;
                case 322:
                    return LONG_LITERAL;
                case 321:
                    return RESULTARROW;
                case 320:
                    return LENGTHREF;
                case 319:
                    return CREATE_OBJECT;
                case 318:
                    return STATIC_EVALUATE;
                case 317:
                    return ENHANCEDFOR_ELIM;
                case 316:
                    return ARRAY_INIT_CREATION_ASSIGNMENTS;
                case 315:
                    return ARRAY_INIT_CREATION_TRANSIENT;
                case 314:
                    return ARRAY_INIT_CREATION;
                case 313:
                    return ARRAY_POST_DECL;
                case 312:
                    return RESOLVE_MULTIPLE_VAR_DECL;
                case 311:
                    return STATICINITIALIZATION;
                case 310:
                    return POSTWORK;
                case 309:
                    return SPECIALCONSTRUCTORECALL;
                case 308:
                    return CONSTRUCTORCALL;
                case 307:
                    return EXPANDMETHODBODY;
                case 306:
                    return METHODCALL;
                case 305:
                    return DOBREAK;
                case 304:
                    return FORTOWHILE;
                case 303:
                    return UNWIND_LOOP_BOUNDED;
                case 302:
                    return ABORT;
                case 301:
                    return FINISH;
                case 300:
                    return COMMIT;
                case 299:
                    return CATCHALL;
                case 298:
                    return UNWINDLOOP;
                case 297:
                    return REPLACEARGS;
                case 296:
                    return EVALARGS;
                case 295:
                    return ISSTATIC;
                case 294:
                    return SETSV;
                case 293:
                    return LOOPSCOPEINVARIANTTRANSFORMER;
                case 292:
                    return FORINITUNFOLDTRANSFORMER;
                case 291:
                    return REATTACHLOOPINVARIANT;
                case 290:
                    return UNPACK;
                case 289:
                    return SWITCHTOIF;
                case 288:
                    return TYPEOF;
                case 287:
                    return BREAKTYPE;
                case 286:
                    return CCAT;
                case 285:
                    return CCATCH;
                case 284:
                    return CONTINUETYPE;
                case 283:
                    return EXEC;
                case 282:
                    return METHODFRAME;
                case 281:
                    return MERGE_POINT;
                case 280:
                    return LOOPSCOPE;
                case 279:
                    return RETURNTYPE;
                case 278:
                    return TRANSACTIONABORT;
                case 277:
                    return TRANSACTIONFINISH;
                case 276:
                    return TRANSACTIONCOMMIT;
                case 275:
                    return TRANSACTIONBEGIN;
                case 274:
                    return SOURCE;
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
                    return MODEL;
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
                    return GHOST;
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
