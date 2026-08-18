/**
 * A simple bounded stack implementation with JML specifications.
 * This file demonstrates JML annotations for verification.
 */
public class Stack {

    //@ public model int size;
    private int[] elements;
    private int top;

    /**
     * Creates an empty stack with the specified capacity.
     * @param capacity the maximum number of elements
     */
    //@ ensures capacity > 0;
    //@ ensures size == 0;
    //@ ensures elements.length == capacity;
    public Stack(int capacity) {
        //@ assert capacity > 0;

        //@ assert true;
        //@ assume true;
        elements = new int[capacity];
        top = 0;
    }

    /**
     * Pushes an element onto the stack.
     * @param value the value to push
     */
    //@ requires !isFull();
    //@ ensures size == \old(size) + 1;
    //@ ensures elements[\old(size)] == value;
    //@ assignable elements, top;
    public void push(int value) {
        elements[top] = value;
        top++;
    }

    /**
     * Pops an element from the stack.
     * @return the top element
     */
    //@ requires !isEmpty();
    //@ ensures \result == elements[\old(size) - 1];
    //@ ensures size == \old(size) - 1;
    //@ assignable top;
    public /*@ pure @*/ int pop() {
        top--;
        return elements[top];
    }

    /**
     * Returns the top element without removing it.
     * @return the top element
     */
    //@ requires !isEmpty();
    //@ ensures \result == elements[size - 1];
    //@ ensures size == \old(size);
    public /*@ pure @*/ int peek() {
        return elements[top - 1];
    }

    /**
     * Checks if the stack is empty.
     * @return true if empty
     */
    //@ ensures \result == (size == 0);
    public /*@ pure @*/ boolean isEmpty() {
        return top == 0;
    }

    /**
     * Checks if the stack is full.
     * @return true if full
     */
    //@ ensures \result == (size == elements.length);
    public /*@ pure @*/ boolean isFull() {
        return top == elements.length;
    }

    /**
     * Returns the current size of the stack.
     * @return the number of elements
     */
    //@ ensures \result == top;
    public /*@ pure @*/ int getSize() {
        return top;
    }

    //@ invariant 0 <= top && top <= elements.length;
    //@ invariant elements != null;
}
