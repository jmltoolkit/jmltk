/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.Optional;
import java.util.Set;

/**
 * A declaration of a type. It could be a primitive type, an enum, a class, an interface
 * or a type variable.
 * It cannot be an annotation or an array.
 *
 * @author Federico Tomassetti
 */
public interface ResolvedTypeDeclaration extends ResolvedDeclaration {

    // /
    // / Containment
    // /
    /**
     * Get the list of types defined inside the current type.
     */
    default Set<ResolvedReferenceTypeDeclaration> internalTypes() {
        throw new UnsupportedOperationException(
                "InternalTypes not available for " + this.getClass().getCanonicalName());
    }

    /**
     * Returns a type declaration for the internal type based on name.
     * (Does not include internal types inside internal types).
     */
    default ResolvedReferenceTypeDeclaration getInternalType(String name) {
        Optional<ResolvedReferenceTypeDeclaration> type = this.internalTypes().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
        return type.orElseThrow(() -> new UnsolvedSymbolException("Internal type not found: " + name));
    }

    /**
     * Does this type contain an internal type with the given name?
     * (Does not include internal types inside internal types).
     */
    default boolean hasInternalType(String name) {
        return this.internalTypes().stream().anyMatch(f -> f.getName().equals(name));
    }

    /**
     * Get the ReferenceTypeDeclaration enclosing this declaration.
     */
    Optional<ResolvedReferenceTypeDeclaration> containerType();

    // /
    // / Misc
    // /
    /**
     * Is this the declaration of a class?
     * Note that an Enum is not considered a Class in this case.
     */
    default boolean isClass() {
        return false;
    }

    /**
     * Is this the declaration of an interface?
     */
    default boolean isInterface() {
        return false;
    }

    /**
     * Is this the declaration of an enum?
     */
    default boolean isEnum() {
        return false;
    }

    /**
     * Is this the declaration of a record class?
     */
    default boolean isRecord() {
        return false;
    }

    /**
     * Is this the declaration of an annotation?
     */
    default boolean isAnnotation() {
        return false;
    }

    /**
     * Is this the declaration of a reference type?
     */
    default boolean isReferenceType() {
        return false;
    }

    /**
     * Is this the declaration of a type parameter?
     */
    default boolean isTypeParameter() {
        return false;
    }

    @Override
    default boolean isType() {
        return true;
    }

    /**
     * Is this type declaration corresponding to an anonymous class?
     * <p>
     * This is an example of anonymous class:
     * <pre>
     * HelloWorld frenchGreeting = new HelloWorld() {
     *     String name = "tout le monde";
     *
     *     public void greet() {
     *         greetSomeone("tout le monde");
     *     }
     *
     *     public void greetSomeone(String someone) {
     *         name = someone;
     *         System.out.println("Salut " + name);
     *     }
     * };
     * </pre>
     */
    default boolean isAnonymousClass() {
        return false;
    }

    @Override
    default ResolvedTypeDeclaration asType() {
        return this;
    }

    /**
     * Return this as a ClassDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedClassDeclaration asClass() {
        throw new UnsupportedOperationException(String.format("%s is not a class", this));
    }

    /**
     * Return this as a InterfaceDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedInterfaceDeclaration asInterface() {
        throw new UnsupportedOperationException(String.format("%s is not an interface", this));
    }

    /**
     * Return this as a EnumDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedEnumDeclaration asEnum() {
        throw new UnsupportedOperationException(String.format("%s is not an enum", this));
    }

    /**
     * Return this as a RecordDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedRecordDeclaration asRecord() {
        throw new UnsupportedOperationException(String.format("%s is not a record", this));
    }

    /**
     * Return this as a AnnotationDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedAnnotationDeclaration asAnnotation() {
        throw new UnsupportedOperationException(String.format("%s is not an annotation", this));
    }

    /**
     * Return this as a TypeParameterDeclaration or throw UnsupportedOperationException.
     */
    default ResolvedTypeParameterDeclaration asTypeParameter() {
        throw new UnsupportedOperationException(String.format("%s is not a type parameter", this));
    }

    default ResolvedReferenceTypeDeclaration asReferenceType() {
        throw new UnsupportedOperationException(String.format("%s is not a reference type", this));
    }

    /**
     * The package name of the type.
     */
    String getPackageName();

    /**
     * The class(es) wrapping this type.
     */
    String getClassName();

    /**
     * The fully qualified name of the type declared.
     */
    String getQualifiedName();

    /**
     * The ID corresponds most of the type to the qualified name. It differs only for local
     * classes which do not have a qualified name but have an ID.
     */
    default String getId() {
        String qname = getQualifiedName();
        if (qname == null) {
            return String.format("<localClass>:%s", getName());
        }
        return qname;
    }
}
