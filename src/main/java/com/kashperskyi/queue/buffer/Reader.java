package com.kashperskyi.queue.buffer;

import javax.annotation.Nullable;

public interface Reader<T> {

    /**
     * Advances the readers position to the next element.
     * Returns true if the reader was advanced and false if the end was reached.
     */
    boolean advance();

    /**
     * Returns the current element.
     */
    @Nullable T current();
}
