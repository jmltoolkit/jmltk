package com.github.javaparser.ast.jml;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.Node;

/**
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
public abstract class JmlKeywordNode<T extends JmlKeyword>
    extends Node implements Jmlish {

    private T value;

    public JmlKeywordNode(T value) {
        this(null, value);
    }

    public JmlKeywordNode(TokenRange tokenRange, T kind) {
        super(tokenRange);
        this.value = kind;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getKind() {
        return value;
    }

    public void setKind(T kind) {
        this.value = kind;
    }
}