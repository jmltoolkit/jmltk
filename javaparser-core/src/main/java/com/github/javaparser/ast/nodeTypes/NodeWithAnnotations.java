/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.StaticJavaParser.parseName;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * A node that can be annotated.
 *
 * @author Federico Tomassetti
 * @since July 2014
 */
public interface NodeWithAnnotations<N extends Node> {

    NodeList<AnnotationExpr> getAnnotations();

    N setAnnotations(NodeList<AnnotationExpr> annotations);

    void tryAddImportToParentCompilationUnit(Class<?> clazz);

    default AnnotationExpr getAnnotation(int i) {
        return getAnnotations().get(i);
    }

    @SuppressWarnings("unchecked")
    default N setAnnotation(int i, AnnotationExpr element) {
        getAnnotations().set(i, element);
        return (N) this;
    }

    @SuppressWarnings("unchecked")
    default N addAnnotation(AnnotationExpr element) {
        getAnnotations().add(element);
        return (N) this;
    }

    /**
     * Annotates this
     *
     * @param name the name of the annotation
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addAnnotation(String name) {
        NormalAnnotationExpr annotation = new NormalAnnotationExpr(parseName(name), new NodeList<>());
        addAnnotation(annotation);
        return (N) this;
    }

    /**
     * Annotates this
     *
     * @param name the name of the annotation
     * @return the {@link NormalAnnotationExpr} added
     */
    @SuppressWarnings("unchecked")
    default NormalAnnotationExpr addAndGetAnnotation(String name) {
        NormalAnnotationExpr annotation = new NormalAnnotationExpr(parseName(name), new NodeList<>());
        addAnnotation(annotation);
        return annotation;
    }

    /**
     * Annotates this node and automatically add the import
     *
     * @param clazz the class of the annotation
     * @return this
     */
    default N addAnnotation(Class<? extends Annotation> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addAnnotation(clazz.getSimpleName());
    }

    /**
     * Annotates this node and automatically add the import
     *
     * @param clazz the class of the annotation
     * @return the {@link NormalAnnotationExpr} added
     */
    default NormalAnnotationExpr addAndGetAnnotation(Class<? extends Annotation> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addAndGetAnnotation(clazz.getSimpleName());
    }

    /**
     * Annotates this with a marker annotation
     *
     * @param name the name of the annotation
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addMarkerAnnotation(String name) {
        MarkerAnnotationExpr markerAnnotationExpr = new MarkerAnnotationExpr(parseName(name));
        addAnnotation(markerAnnotationExpr);
        return (N) this;
    }

    /**
     * Annotates this with a marker annotation and automatically add the import
     *
     * @param clazz the class of the annotation
     * @return this
     */
    default N addMarkerAnnotation(Class<? extends Annotation> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addMarkerAnnotation(clazz.getSimpleName());
    }

    /**
     * Annotates this with a single member annotation
     *
     * @param name the name of the annotation
     * @param expression the part between ()
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addSingleMemberAnnotation(String name, Expression expression) {
        SingleMemberAnnotationExpr singleMemberAnnotationExpr =
                new SingleMemberAnnotationExpr(parseName(name), expression);
        return addAnnotation(singleMemberAnnotationExpr);
    }

    /**
     * Annotates this with a single member annotation
     *
     * @param clazz the class of the annotation
     * @param expression the part between ()
     * @return this
     */
    default N addSingleMemberAnnotation(Class<? extends Annotation> clazz, Expression expression) {
        tryAddImportToParentCompilationUnit(clazz);
        return addSingleMemberAnnotation(clazz.getSimpleName(), expression);
    }

    /**
     * Annotates this with a single member annotation
     *
     * @param name the name of the annotation
     * @param value the value, don't forget to add \"\" for a string value
     * @return this
     */
    default N addSingleMemberAnnotation(String name, String value) {
        return addSingleMemberAnnotation(name, parseExpression(value));
    }

    /**
     * Annotates this with a single member annotation and automatically add the import
     *
     * @param clazz the class of the annotation
     * @param value the value, don't forget to add \"\" for a string value
     * @return this
     */
    default N addSingleMemberAnnotation(Class<? extends Annotation> clazz, String value) {
        tryAddImportToParentCompilationUnit(clazz);
        return addSingleMemberAnnotation(clazz.getSimpleName(), value);
    }

    /**
     * Check whether an annotation with this name is present on this element
     *
     * @param annotationName the name of the annotation
     * @return true if found, false if not
     */
    default boolean isAnnotationPresent(String annotationName) {
        return getAnnotations().stream()
                .anyMatch(a -> a.getName().getIdentifier().equals(annotationName));
    }

    /**
     * Check whether an annotation with this class is present on this element
     *
     * @param annotationClass the class of the annotation
     * @return true if found, false if not
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return isAnnotationPresent(annotationClass.getSimpleName());
    }

    /**
     * Try to find an annotation by its name
     *
     * @param annotationName the name of the annotation
     */
    default Optional<AnnotationExpr> getAnnotationByName(String annotationName) {
        return getAnnotations().stream()
                .filter(a -> a.getName().getIdentifier().equals(annotationName))
                .findFirst();
    }

    /**
     * Try to find an annotation by its class
     *
     * @param annotationClass the class of the annotation
     */
    default Optional<AnnotationExpr> getAnnotationByClass(Class<? extends Annotation> annotationClass) {
        return getAnnotationByName(annotationClass.getSimpleName());
    }
}
