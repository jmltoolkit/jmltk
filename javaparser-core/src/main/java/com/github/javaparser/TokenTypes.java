/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.utils.LineSeparator;

import static com.github.javaparser.GeneratedJavaParserConstants.*;

/**
 * Complements GeneratedJavaParserConstants
 */
public class TokenTypes {

    public static boolean isWhitespace(int kind) {
        return getCategory(kind).isWhitespace();
    }

    public static boolean isEndOfLineToken(int kind) {
        return getCategory(kind).isEndOfLine();
    }

    public static boolean isWhitespaceOrComment(int kind) {
        return getCategory(kind).isWhitespaceOrComment();
    }

    /**
     * @deprecated Use {@link #isWhitespaceButNotEndOfLine(int)} which more explicitly reflects that this also includes
     * other whitespace e.g. {@code EOF} and {@code CTRL_Z} and a large number of other characters.
     * See the grammar for details of exactly which characters are included as a "space" (.
     * <pre>{@code
     *   <SPACE: [" ", "\t", "\f", "\u0085", "\u00A0", "\u1680", "\u180e", "\u2000", "\u2001", "\u2002", "\u2003", "\u2004", "\u2005",
     *       "\u2006", "\u2007", "\u2008", "\u2009", "\u200a", "\u200b", "\u200c", "\u200d", "\u2028", "\u2029", "\u202f", "\u205f", "\u2060", "\u3000", "\ufeff"]>
     *  }</pre>
     */
    @Deprecated
    public static boolean isSpaceOrTab(int kind) {
        return isWhitespaceButNotEndOfLine(kind);
    }

    public static boolean isWhitespaceButNotEndOfLine(int kind) {
        return getCategory(kind).isWhitespaceButNotEndOfLine();
    }

    public static boolean isComment(int kind) {
        return getCategory(kind).isComment();
    }

    private static final boolean[] IDENTIFIER_KINDS = new boolean[GeneratedJavaParserConstants.tokenImage.length];

    static {
        IDENTIFIER_KINDS[ABRUPT_BEHAVIOR] = true;
        IDENTIFIER_KINDS[ABRUPT_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[ACCESSIBLE] = true;
        IDENTIFIER_KINDS[ACCESSIBLE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[ALSO] = true;
        IDENTIFIER_KINDS[ASSERT] = true;
        IDENTIFIER_KINDS[ASSERT_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[ASSIGNABLE] = true;
        IDENTIFIER_KINDS[ASSIGNABLE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[ASSUME] = true;
        IDENTIFIER_KINDS[ASSUME_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[AXIOM] = true;
        IDENTIFIER_KINDS[BEGIN] = true;
        IDENTIFIER_KINDS[BEHAVIOR] = true;
        IDENTIFIER_KINDS[BEHAVIOUR] = true;
        IDENTIFIER_KINDS[BREAKS] = true;
        IDENTIFIER_KINDS[BREAKS_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[BREAK_BEHAVIOR] = true;
        IDENTIFIER_KINDS[BREAK_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[BY] = true;
        IDENTIFIER_KINDS[CALLABLE] = true;
        IDENTIFIER_KINDS[CALLABLE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[CAPTURES] = true;
        IDENTIFIER_KINDS[CAPTURES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[CCAT] = true;
        IDENTIFIER_KINDS[CCATCH] = true;
        IDENTIFIER_KINDS[CHOOSE] = true;
        IDENTIFIER_KINDS[CHOOSE_IF] = true;
        IDENTIFIER_KINDS[CODE] = true;
        IDENTIFIER_KINDS[CODE_BIGINT_MATH] = true;
        IDENTIFIER_KINDS[CODE_JAVA_MATH] = true;
        IDENTIFIER_KINDS[CODE_SAFE_MATH] = true;
        IDENTIFIER_KINDS[CONSTRAINT] = true;
        IDENTIFIER_KINDS[CONSTRAINT_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[CONSTRUCTOR] = true;
        IDENTIFIER_KINDS[CONTINUES] = true;
        IDENTIFIER_KINDS[CONTINUES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[CONTINUE_BEHAVIOR] = true;
        IDENTIFIER_KINDS[CONTINUE_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[DECREASES] = true;
        IDENTIFIER_KINDS[DECREASES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[DECREASING] = true;
        IDENTIFIER_KINDS[DECREASING_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[DIVERGES] = true;
        IDENTIFIER_KINDS[DIVERGES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[DURATION_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[END] = true;
        IDENTIFIER_KINDS[ENSURES] = true;
        IDENTIFIER_KINDS[ENSURES_FREE] = true;
        IDENTIFIER_KINDS[ENSURES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[ENUM] = true;
        IDENTIFIER_KINDS[ERASES] = true;
        IDENTIFIER_KINDS[EXAMPLE] = true;
        IDENTIFIER_KINDS[EXCEPTIONAL_BEHAVIOR] = true;
        IDENTIFIER_KINDS[EXCEPTIONAL_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[EXCEPTIONAL_EXAMPLE] = true;
        IDENTIFIER_KINDS[EXEC] = true;
        IDENTIFIER_KINDS[EXPORTS] = true;
        IDENTIFIER_KINDS[EXSURES] = true;
        IDENTIFIER_KINDS[EXSURES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[EXTRACT] = true;
        IDENTIFIER_KINDS[FIELD] = true;
        IDENTIFIER_KINDS[FORALL] = true;
        IDENTIFIER_KINDS[FOR_EXAMPLE] = true;
        IDENTIFIER_KINDS[GHOST] = true;
        IDENTIFIER_KINDS[HELPER] = true;
        IDENTIFIER_KINDS[HENCE_BY] = true;
        IDENTIFIER_KINDS[HENCE_BY_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[IDENTIFIER] = true;
        IDENTIFIER_KINDS[IMMUTABLE] = true;
        IDENTIFIER_KINDS[IMPLIES_THAT] = true;
        IDENTIFIER_KINDS[IN] = true;
        IDENTIFIER_KINDS[INITIALIZER] = true;
        IDENTIFIER_KINDS[INITIALLY] = true;
        IDENTIFIER_KINDS[INSTANCE] = true;
        IDENTIFIER_KINDS[INVARIANT] = true;
        IDENTIFIER_KINDS[INVARIANT_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[JML_IDENTIFIER] = true;
        IDENTIFIER_KINDS[KEYIDENTIFIER] = true;
        IDENTIFIER_KINDS[LEMMA] = true;
        IDENTIFIER_KINDS[LOOP_CONTRACT] = true;
        IDENTIFIER_KINDS[LOOP_DETERMINES] = true;
        IDENTIFIER_KINDS[LOOP_INVARIANT] = true;
        IDENTIFIER_KINDS[LOOP_INVARIANT_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[LOOP_SEPARATES] = true;
        IDENTIFIER_KINDS[MAINTAINING] = true;
        IDENTIFIER_KINDS[MAINTAINING_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[MAPS] = true;
        IDENTIFIER_KINDS[MAPS_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[MEASURED_BY] = true;
        IDENTIFIER_KINDS[MEASURED_BY_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[METHOD] = true;
        IDENTIFIER_KINDS[MODEL] = true;
        IDENTIFIER_KINDS[MODEL_BEHAVIOR] = true;
        IDENTIFIER_KINDS[MODEL_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[MODEL_PROGRAM] = true;
        IDENTIFIER_KINDS[MODIFIABLE] = true;
        IDENTIFIER_KINDS[MODIFIABLE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[MODIFIES] = true;
        IDENTIFIER_KINDS[MODIFIES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[MODULE] = true;
        IDENTIFIER_KINDS[MONITORED] = true;
        IDENTIFIER_KINDS[MONITORS_FOR] = true;
        IDENTIFIER_KINDS[NEW_OBJECTS] = true;
        IDENTIFIER_KINDS[NON_NULL] = true;
        IDENTIFIER_KINDS[NORMAL_BEHAVIOR] = true;
        IDENTIFIER_KINDS[NORMAL_BEHAVIOUR] = true;
        IDENTIFIER_KINDS[NORMAL_EXAMPLE] = true;
        IDENTIFIER_KINDS[NULLABLE] = true;
        IDENTIFIER_KINDS[NULLABLE_BY_DEFAULT] = true;
        IDENTIFIER_KINDS[OLD] = true;
        IDENTIFIER_KINDS[OPEN] = true;
        IDENTIFIER_KINDS[OPENS] = true;
        IDENTIFIER_KINDS[OR] = true;
        IDENTIFIER_KINDS[PEER] = true;
        IDENTIFIER_KINDS[PERMITS] = true;
        IDENTIFIER_KINDS[POST] = true;
        IDENTIFIER_KINDS[POST_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[PRE] = true;
        IDENTIFIER_KINDS[PRE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[PROVIDES] = true;
        IDENTIFIER_KINDS[PURE] = true;
        IDENTIFIER_KINDS[READABLE] = true;
        IDENTIFIER_KINDS[READ_ONLY] = true;
        IDENTIFIER_KINDS[RECORD] = true;
        IDENTIFIER_KINDS[REFINING] = true;
        IDENTIFIER_KINDS[REP] = true;
        IDENTIFIER_KINDS[REPRESENTS] = true;
        IDENTIFIER_KINDS[REPRESENTS_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[REQUIRES] = true;
        IDENTIFIER_KINDS[REQUIRES_FREE] = true;
        IDENTIFIER_KINDS[REQUIRES_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[RESULT] = true;
        IDENTIFIER_KINDS[RETURNS] = true;
        IDENTIFIER_KINDS[RETURNS_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[SEALED] = true;
        IDENTIFIER_KINDS[SEPARATES] = true;
        IDENTIFIER_KINDS[SET] = true;
        IDENTIFIER_KINDS[SIGNALS] = true;
        IDENTIFIER_KINDS[SIGNALS_ONLY] = true;
        IDENTIFIER_KINDS[SIGNALS_ONLY_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[SIGNALS_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[SOURCE] = true;
        IDENTIFIER_KINDS[SPEC_BIGINT_MATH] = true;
        IDENTIFIER_KINDS[SPEC_JAVA_MATH] = true;
        IDENTIFIER_KINDS[SPEC_PROTECTED] = true;
        IDENTIFIER_KINDS[SPEC_PUBLIC] = true;
        IDENTIFIER_KINDS[SPEC_SAFE_MATH] = true;
        IDENTIFIER_KINDS[STATIC_INITIALIZER] = true;
        IDENTIFIER_KINDS[STRICTFP] = true;
        IDENTIFIER_KINDS[SVIDENTIFIER] = true;
        IDENTIFIER_KINDS[TO] = true;
        IDENTIFIER_KINDS[TRANSITIVE] = true;
        IDENTIFIER_KINDS[TYPE] = true;
        IDENTIFIER_KINDS[UNINITIALIZED] = true;
        IDENTIFIER_KINDS[UNNAMED_PLACEHOLDER] = true;
        IDENTIFIER_KINDS[UNREACHABLE] = true;
        IDENTIFIER_KINDS[USES] = true;
        IDENTIFIER_KINDS[WHEN] = true;
        IDENTIFIER_KINDS[WHEN_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[WITH] = true;
        IDENTIFIER_KINDS[WORKING_SPACE] = true;
        IDENTIFIER_KINDS[WORKING_SPACE_REDUNDANTLY] = true;
        IDENTIFIER_KINDS[WRITABLE] = true;
        IDENTIFIER_KINDS[YIELD] = true;
    }

    /**
     * @return {@code true} if a token of this kind may appear where the grammar's {@code Identifier()} is expected.
     */
    public static boolean isIdentifierKind(int kind) {
        return kind >= 0 && kind < IDENTIFIER_KINDS.length && IDENTIFIER_KINDS[kind];
    }

    /**
     * @return the kind of EOL token to use on the platform you're running on.
     */
    public static int eolTokenKind(LineSeparator lineSeparator) {
        if (lineSeparator.equalsString(LineSeparator.LF)) {
            return UNIX_EOL;
        }
        if (lineSeparator.equalsString(LineSeparator.CRLF)) {
            return WINDOWS_EOL;
        }
        if (lineSeparator.equalsString(LineSeparator.CR)) {
            return OLD_MAC_EOL;
        }
        throw new AssertionError("Unknown EOL character sequence");
    }

    public static int eolTokenKind() {
        return eolTokenKind(LineSeparator.SYSTEM);
    }

    /**
     * @return the token kind for a single space.
     */
    public static int spaceTokenKind() {
        return SPACE;
    }

    /**
     * Category of a token, a little more detailed than
     * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.5">The JLS</a>.
     */
    public static JavaToken.Category getCategory(int kind) {
        switch (kind) {
            case WINDOWS_EOL:
            case UNIX_EOL:
            case OLD_MAC_EOL:
                return JavaToken.Category.EOL;
            case EOF:
            case SPACE:
            case CTRL_Z:
                return JavaToken.Category.WHITESPACE_NO_EOL;
            case SINGLE_LINE_COMMENT:
            case JAVADOC_COMMENT:
            case MULTI_LINE_COMMENT:
                return JavaToken.Category.COMMENT;
            case JML_BLOCK_COMMENT:
            case JML_LINE_COMMENT:
            case ABSTRACT:
            case ASSERT:
            case GHOST:
            case BOOLEAN:
            case BREAK:
            case BYTE:
            case CASE:
            case CATCH:
            case CHAR:
            case CLASS:
            case CONST:
            case CONTINUE:
            case LOOP_DETERMINES:
            case LOOP_SEPARATES:
            case SEPARATES:
            case _DEFAULT:
            case DO:
            case DOUBLE:
            case ELSE:
            case ENUM:
            case EXTENDS:
            case FINAL:
            case FINALLY:
            case FLOAT:
            case FOR:
            case GOTO:
            case IF:
            case IMPLEMENTS:
            case IMPORT:
            case INSTANCEOF:
            case INT:
            case INTERFACE:
            case LONG:
            case NATIVE:
            case NEW:
            case PACKAGE:
            case PRIVATE:
            case PROTECTED:
            case PUBLIC:
            case RECORD:
            case RETURN:
            case SHORT:
            case STATIC:
            case STRICTFP:
            case SUPER:
            case SWITCH:
            case SYNCHRONIZED:
            case THIS:
            case THROW:
            case THROWS:
            case TRANSIENT:
            case TRY:
            case VOID:
            case VOLATILE:
            case WHILE:
            case YIELD:
            case REQUIRES:
            case TO:
            case WITH:
            case OPEN:
            case OPENS:
            case USES:
            case MODULE:
            case EXPORTS:
            case PROVIDES:
            case TRANSITIVE:
            case PERMITS:
            case SEALED:
            case NON_SEALED:
            case WHEN:
            case INVARIANT:
            case INVARIANT_FREE:
            case ABRUPT_BEHAVIOR:
            case ABRUPT_BEHAVIOUR:
            case MODEL_BEHAVIOR:
            case MODEL_BEHAVIOUR:
            case ACCESSIBLE:
            case ACCESSIBLE_REDUNDANTLY:
            case ALSO:
            case ANTIVALENCE:
            case ASSERT_REDUNDANTLY:
            case JML_ASSERT:
            case ASSIGNABLE:
            case ASSIGNABLE_FREE:
            case ASSIGNABLE_REDUNDANTLY:
            case ASSUME:
            case ASSUME_REDUNDANTLY:
            case AXIOM:
            case LEMMA:
            case BEHAVIOR:
            case BEHAVIOUR:
            case BIGINT:
            case BIGINT_MATH:
            case BREAKS:
            case BREAKS_REDUNDANTLY:
            case BREAK_BEHAVIOR:
            case BREAK_BEHAVIOUR:
            case CALLABLE:
            case CALLABLE_REDUNDANTLY:
            case CAPTURES:
            case CAPTURES_REDUNDANTLY:
            case CHOOSE:
            case CHOOSE_IF:
            case CODE:
            case CODE_BIGINT_MATH:
            case CODE_JAVA_MATH:
            case CODE_SAFE_MATH:
            case IMMUTABLE:
            case CONSTRAINT:
            case CONSTRAINT_REDUNDANTLY:
            case CONSTRUCTOR:
            case CONTINUES:
            case CONTINUES_REDUNDANTLY:
            case CONTINUE_BEHAVIOR:
            case CONTINUE_BEHAVIOUR:
            case DECLASSIFIES:
            case DECREASES:
            case DECREASES_REDUNDANTLY:
            case DECREASING:
            case DECREASING_REDUNDANTLY:
            case DETERMINES:
            case DIVERGES:
            case DIVERGES_REDUNDANTLY:
            case DURATION:
            case DURATION_REDUNDANTLY:
            case ENSURES:
            case ENSURES_REDUNDANTLY:
            case ENSURES_FREE:
            case REQUIRES_FREE:
            case EQUIVALENCE:
            case IMPLICATION:
            case IMPLICATION_BACKWARD:
            case ERASES:
            case EXAMPLE:
            case EXCEPTIONAL_BEHAVIOR:
            case EXCEPTIONAL_BEHAVIOUR:
            case EXCEPTIONAL_EXAMPLE:
            case EXISTS:
            case EXSURES:
            case EXSURES_REDUNDANTLY:
            case EXTRACT:
            case FIELD:
            case FORALLQ:
            case LET:
            case FORALL:
            case FOR_EXAMPLE:
            case FROM_GOAL:
            case PEER:
            case REP:
            case READ_ONLY:
            case END:
            case HELPER:
            case HENCE_BY:
            case HENCE_BY_REDUNDANTLY:
            case IMPLIES_THAT:
            case IN:
            case INITIALIZER:
            case INITIALLY:
            case INSTANCE:
            case TWO_STATE:
            case NO_STATE:
            case NON_NULL_BY_DEFAULT:
            case INVARIANT_REDUNDANTLY:
            case IN_REDUNDANTLY:
            case JAVA_MATH:
            case LBLNEG:
            case LBLPOS:
            case LBL:
            case LOOP_CONTRACT:
            case LOOP_INVARIANT:
            case LOOP_INVARIANT_FREE:
            case LOOP_INVARIANT_REDUNDANTLY:
            case MAINTAINING:
            case MAINTAINING_REDUNDANTLY:
            case MAPS:
            case MAPS_REDUNDANTLY:
            case MAX:
            case MEASURED_BY:
            case ESC_MEASURED_BY:
            case MEASURED_BY_REDUNDANTLY:
            case METHOD:
            case MIN:
            case MODEL:
            case MODEL_PROGRAM:
            case MODIFIABLE:
            case MODIFIABLE_REDUNDANTLY:
            case LOOP_MODIFIES:
            case MODIFIES:
            case MODIFIES_REDUNDANTLY:
            case MONITORED:
            case MONITORS_FOR:
            case NESTED_CONTRACT_END:
            case NESTED_CONTRACT_START:
            case NEW_OBJECTS:
            case BY:
            case NONNULLELEMENTS:
            case NON_NULL:
            case NORMAL_BEHAVIOR:
            case NORMAL_BEHAVIOUR:
            case FEASIBLE_BEHAVIOR:
            case FEASIBLE_BEHAVIOUR:
            case NORMAL_EXAMPLE:
            case NOWARN:
            case NOWARN_OP:
            case NULLABLE:
            case NULLABLE_BY_DEFAULT:
            case NUM_OF:
            case OLD:
            case OR:
            case POST:
            case POST_REDUNDANTLY:
            case PRE_ESC:
            case PRE:
            case PRE_REDUNDANTLY:
            case PRODUCT:
            case PURE:
            case READABLE:
            case REFINING:
            case REPRESENTS:
            case REPRESENTS_REDUNDANTLY:
            case REQUIRES_REDUNDANTLY:
            case RESULT:
            case RETURNS:
            case RETURNS_REDUNDANTLY:
            case RETURN_BEHAVIOR:
            case BACKARROW:
            case RETURN_BEHAVIOUR:
            case SAFE_MATH:
            case SET:
            case SIGNALS:
            case SIGNALS_ONLY:
            case SIGNALS_ONLY_REDUNDANTLY:
            case SIGNALS_REDUNDANTLY:
            case SPEC_BIGINT_MATH:
            case SPEC_JAVA_MATH:
            case SPEC_PACKAGE:
            case SPEC_PRIVATE:
            case SPEC_PROTECTED:
            case SPEC_PUBLIC:
            case SPEC_SAFE_MATH:
            case STATIC_INITIALIZER:
            case STRICTLY_PURE:
            case SUBTYPE:
            case SUCH_THAT:
            case SUM:
            case TYPE:
            case UNINITIALIZED:
            case UNKNOWN_OP:
            case UNKNOWN_OP_EQ:
            case UNREACHABLE:
            case WARN:
            case WARN_OP:
            case WHEN_REDUNDANTLY:
            case WORKING_SPACE_ESC:
            case WORKING_SPACE:
            case WORKING_SPACE_REDUNDANTLY:
            case WRITABLE:
            case DOTDOT:
            case JML_MULTI_LINE_COMMENT:
            case TRANSACTIONBEGIN:
            case TRANSACTIONCOMMIT:
            case TRANSACTIONFINISH:
            case TRANSACTIONABORT:
            case RETURNTYPE:
            case LOOPSCOPE:
            case METHODFRAME:
            case SHARP:
            case EXEC:
            case CONTINUETYPE:
            case CCATCH:
            case CCAT:
            case BREAKTYPE:
            case TYPEOF:
            case SETSV:
            case CATCHALL:
            case BEGIN:
            case COMMIT:
            case FINISH:
            case ABORT:
            case KEYIDENTIFIER:
            case SOURCE:
            case RESULTARROW:
                return JavaToken.Category.KEYWORD;
            case LONG_LITERAL:
            case INTEGER_LITERAL:
            case DECIMAL_LITERAL:
            case HEX_LITERAL:
            case OCTAL_LITERAL:
            case BINARY_LITERAL:
            case FLOATING_POINT_LITERAL:
            case DECIMAL_FLOATING_POINT_LITERAL:
            case DECIMAL_EXPONENT:
            case HEXADECIMAL_FLOATING_POINT_LITERAL:
            case HEXADECIMAL_EXPONENT:
            case CHARACTER_LITERAL:
            case STRING_LITERAL:
            case TEXT_BLOCK_LITERAL:
            case TRUE:
            case FALSE:
            case NULL:
            case UNNAMED_PLACEHOLDER:
                return JavaToken.Category.LITERAL;
            case SVIDENTIFIER:
            case IDENTIFIER:
            case JML_IDENTIFIER:
                return JavaToken.Category.IDENTIFIER;
            case LPAREN:
            case RPAREN:
            case LBRACE:
            case RBRACE:
            case LBRACKET:
            case RBRACKET:
            case SEMICOLON:
            case COMMA:
            case DOT:
            case ELLIPSIS:
            case AT:
            case DOUBLECOLON:
                return JavaToken.Category.SEPARATOR;
            case ASSIGN:
            case LT:
            case BANG:
            case TILDE:
            case HOOK:
            case COLON:
            case EQ:
            case LE:
            case GE:
            case NE:
            case SC_OR:
            case SC_AND:
            case INCR:
            case DECR:
            case PLUS:
            case MINUS:
            case STAR:
            case SLASH:
            case BIT_AND:
            case BIT_OR:
            case XOR:
            case REM:
            case LSHIFT:
            case PLUSASSIGN:
            case MINUSASSIGN:
            case STARASSIGN:
            case SLASHASSIGN:
            case ANDASSIGN:
            case ORASSIGN:
            case XORASSIGN:
            case REMASSIGN:
            case LSHIFTASSIGN:
            case RSIGNEDSHIFTASSIGN:
            case RUNSIGNEDSHIFTASSIGN:
            case ARROW:
            case RUNSIGNEDSHIFT:
            case RSIGNEDSHIFT:
            case GT:
                return JavaToken.Category.OPERATOR;
            // The following are tokens that are only used internally by the lexer
            case ENTER_JAVADOC_COMMENT:
            case ENTER_JML_BLOCK_COMMENT:
            case JML_ENTER_MULTILINE_COMMENT:
            case ENTER_MULTILINE_COMMENT:
            case COMMENT_CONTENT:
            case HEX_DIGITS:
            case NON_UNDERSCORE_LETTER:
            case UNICODE_ESCAPE:
            case PART_LETTER:
            case TEXT_BLOCK_CONTENT:
            case ENTER_TEXT_BLOCK:
                return JavaToken.Category.EOL;
            default:
                throw new AssertionError(
                        "Unable to categorise token kind " + kind
                                + " -- has it recently been added to the grammar but not classified within TokenTypes.java, perhaps?");
        }
    }
}
