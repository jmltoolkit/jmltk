/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

/**
 * The start production for JavaParser.
 * Tells JavaParser what piece of Java code it can expect.
 * For example,
 * COMPILATION_UNIT indicates a complete Java file,
 * and CLASS_BODY would indicate the part of a class that is within { and }.
 *
 * @see JavaParser#parse(ParseStart, Provider)
 */
@FunctionalInterface
public interface ParseStart<R> {

    ParseStart<CompilationUnit> COMPILATION_UNIT = GeneratedJavaParser::CompilationUnit;

    ParseStart<BlockStmt> BLOCK = GeneratedJavaParser::BlockParseStart;

    ParseStart<Statement> STATEMENT = GeneratedJavaParser::BlockStatementParseStart;

    ParseStart<ImportDeclaration> IMPORT_DECLARATION = GeneratedJavaParser::ImportDeclarationParseStart;

    ParseStart<Expression> EXPRESSION = GeneratedJavaParser::ExpressionParseStart;

    ParseStart<AnnotationExpr> ANNOTATION = GeneratedJavaParser::AnnotationParseStart;

    ParseStart<BodyDeclaration<?>> ANNOTATION_BODY = GeneratedJavaParser::AnnotationBodyDeclarationParseStart;

    ParseStart<BodyDeclaration<?>> CLASS_BODY = GeneratedJavaParser::ClassOrInterfaceBodyDeclarationParseStart;

    ParseStart<ClassOrInterfaceType> CLASS_OR_INTERFACE_TYPE = GeneratedJavaParser::ClassOrInterfaceTypeParseStart;

    ParseStart<Type> TYPE = GeneratedJavaParser::ResultTypeParseStart;

    ParseStart<TypeParameter> TYPE_PARAMETER = GeneratedJavaParser::TypeParameterParseStart;

    ParseStart<VariableDeclarationExpr> VARIABLE_DECLARATION_EXPR = GeneratedJavaParser::VariableDeclarationExpressionParseStart;

    ParseStart<ExplicitConstructorInvocationStmt> EXPLICIT_CONSTRUCTOR_INVOCATION_STMT = GeneratedJavaParser::ExplicitConstructorInvocationParseStart;

    ParseStart<Name> NAME = GeneratedJavaParser::NameParseStart;

    ParseStart<SimpleName> SIMPLE_NAME = GeneratedJavaParser::SimpleNameParseStart;

    ParseStart<Parameter> PARAMETER = GeneratedJavaParser::ParameterParseStart;

    ParseStart<PackageDeclaration> PACKAGE_DECLARATION = GeneratedJavaParser::PackageDeclarationParseStart;

    ParseStart<TypeDeclaration<?>> TYPE_DECLARATION = GeneratedJavaParser::TypeDeclarationParseStart;

    ParseStart<ModuleDeclaration> MODULE_DECLARATION = GeneratedJavaParser::ModuleDeclarationParseStart;

    ParseStart<ModuleDirective> MODULE_DIRECTIVE = GeneratedJavaParser::ModuleDirectiveParseStart;

    ParseStart<MethodDeclaration> METHOD_DECLARATION = GeneratedJavaParser::MethodDeclarationParseStart;

    ParseStart<ArrayInitializerExpr> ARRAY_INITIALIZER_EXPR = GeneratedJavaParser::ArrayInitializer;

    R parse(GeneratedJavaParser parser) throws ParseException;
}
