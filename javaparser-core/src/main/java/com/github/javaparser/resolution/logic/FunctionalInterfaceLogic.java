/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.logic;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Federico Tomassetti
 */
public final class FunctionalInterfaceLogic {

    private static String JAVA_LANG_FUNCTIONAL_INTERFACE = FunctionalInterface.class.getCanonicalName();

    private FunctionalInterfaceLogic() {
        // prevent instantiation
    }

    /**
     * Get the functional method defined by the type, if any.
     */
    public static Optional<MethodUsage> getFunctionalMethod(ResolvedType type) {
        Optional<ResolvedReferenceTypeDeclaration> optionalTypeDeclaration =
                type.asReferenceType().getTypeDeclaration();
        if (!optionalTypeDeclaration.isPresent()) {
            return Optional.empty();
        }
        ResolvedReferenceTypeDeclaration typeDeclaration = optionalTypeDeclaration.get();
        if (type.isReferenceType() && typeDeclaration.isInterface()) {
            return getFunctionalMethod(typeDeclaration);
        }
        return Optional.empty();
    }

    /**
     * Get the functional method defined by the type, if any.
     */
    public static Optional<MethodUsage> getFunctionalMethod(ResolvedReferenceTypeDeclaration typeDeclaration) {
        // We need to find all abstract methods
        // Remove methods inherited by Object:
        Set<MethodUsage> // Remove methods inherited by Object:
                // Consider the case of Comparator which define equals. It would be considered a functional method.
                methods = typeDeclaration.getAllMethods().stream()
                        .filter(m -> m.getDeclaration().isAbstract())
                        .filter(m -> !isPublicMemberOfObject(m))
                        .collect(Collectors.toSet());
        // TODO a functional interface can have multiple subsignature method with a return-type-substitutable
        // see https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.8
        if (methods.size() == 0) {
            return Optional.empty();
        }
        Iterator<MethodUsage> iterator = methods.iterator();
        MethodUsage methodUsage = iterator.next();
        while (iterator.hasNext()) {
            MethodUsage otherMethodUsage = iterator.next();
            if (!(methodUsage.isSameSignature(otherMethodUsage)
                    || methodUsage.isSubSignature(otherMethodUsage)
                    || otherMethodUsage.isSubSignature(methodUsage))) {
                methodUsage = null;
                break;
            }
            if (!(methodUsage.isReturnTypeSubstituable(otherMethodUsage))) {
                methodUsage = null;
                break;
            }
        }
        return Optional.ofNullable(methodUsage);
    }

    public static boolean isFunctionalInterfaceType(ResolvedType type) {
        if (type.isReferenceType()) {
            Optional<ResolvedReferenceTypeDeclaration> optionalTypeDeclaration =
                    type.asReferenceType().getTypeDeclaration();
            if (optionalTypeDeclaration.isPresent()
                    && optionalTypeDeclaration.get().hasAnnotation(JAVA_LANG_FUNCTIONAL_INTERFACE)) {
                return true;
            }
        }
        return getFunctionalMethod(type).isPresent();
    }

    private static String getSignature(Method m) {
        return String.format(
                "%s(%s)",
                m.getName(),
                String.join(
                        ", ",
                        Arrays.stream(m.getParameters())
                                .map(p -> toSignature(p))
                                .collect(Collectors.toList())));
    }

    private static String toSignature(Parameter p) {
        return p.getType().getCanonicalName();
    }

    private static List<String> OBJECT_PUBLIC_METHODS_SIGNATURES = Arrays.stream(Object.class.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .map(method -> getSignature(method))
            .collect(Collectors.toList());

    private static boolean isPublicMemberOfObject(MethodUsage m) {
        return OBJECT_PUBLIC_METHODS_SIGNATURES.contains(m.getDeclaration().getSignature());
    }
}
