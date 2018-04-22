package com.kashperskyi.queue.buffer;

public interface CircularBuffer<T> {

    /**
     * Inserts a new element to the front the com.kashprskyi.queue.buffer overriding the tail element.
     */
    void add(T item);

    /**
     * Creates a reader that goes through all element of the com.kashprskyi.queue.buffer.
     * Implementations may hold resource and should be traversed fully.
     */
    Reader<T> read();
}
