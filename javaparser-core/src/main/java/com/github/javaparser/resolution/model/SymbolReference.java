/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.model;

import com.github.javaparser.quality.Nullable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;

import java.util.Optional;

/**
 * A reference to a symbol. It can solved or not solved. If solved the corresponding
 * declaration will be provided.
 *
 * @author Federico Tomassetti
 */
public class SymbolReference<S extends ResolvedDeclaration> {

    /**
     * Create a solve reference to the given symbol.
     */
    public static <S extends ResolvedDeclaration, S2 extends S> SymbolReference<S> solved(S2 symbolDeclaration) {
        return new SymbolReference<>(symbolDeclaration);
    }

    /**
     * Create a reference for an unsolved symbol.
     *
     * @return The created unsolved symbol reference.
     *
     * @param <S> The symbol reference type.
     */
    public static <S extends ResolvedDeclaration> SymbolReference<S> unsolved() {
        return new SymbolReference<>(null);
    }

    /**
     * Create an unsolved reference specifying the type of the value expected.
     *
     * @deprecated Consider using {@link #unsolved()} instead.
     */
    @Deprecated
    public static <S extends ResolvedDeclaration, S2 extends S> SymbolReference<S> unsolved(Class<S2> clazz) {
        return unsolved();
    }

    /**
     * Adapt a {@link SymbolReference} into another {@link SymbolReference}.
     *
     * @param ref   The reference to be adapted.
     * @param clazz The final type to be used.
     *
     * @return The adapted symbol reference.
     *
     * @param <I> The Symbol Reference before adapting.
     * @param <O> The Symbol Reference after adapting.
     */
    public static <I extends ResolvedDeclaration, O extends ResolvedDeclaration> SymbolReference<O> adapt(
            SymbolReference<I> ref, Class<O> clazz) {
        Optional<I> declaration = ref.getDeclaration();
        if (declaration.isPresent()) {
            I symbol = declaration.get();
            if (clazz.isInstance(symbol)) {
                return solved(clazz.cast(symbol));
            }
        }
        return unsolved();
    }

    private final S correspondingDeclaration;

    private SymbolReference(@Nullable S correspondingDeclaration) {
        this.correspondingDeclaration = correspondingDeclaration;
    }

    /**
     * Get the declaration associated with the Symbol.
     *
     * @return an {@link Optional} with a present value if the symbol is solved, otherwise an empty {@link Optional}.
     */
    public Optional<S> getDeclaration() {
        return Optional.ofNullable(correspondingDeclaration);
    }

    /**
     * The corresponding declaration. If not solve this throws UnsupportedOperationException.
     */
    public S getCorrespondingDeclaration() {
        return getDeclaration()
                .orElseThrow(() ->
                        new UnsolvedSymbolException("Corresponding declaration not available for unsolved symbol."));
    }

    /**
     * Is the reference solved?
     */
    public boolean isSolved() {
        return getDeclaration().isPresent();
    }

    @Override
    public String toString() {
        return "SymbolReference{" + correspondingDeclaration + "}";
    }
}
