package com.kashperskyi.queue.buffer.impl;

import com.google.common.collect.AbstractIterator;
import com.kashperskyi.queue.buffer.Reader;

public class ReadingIterator<T> extends AbstractIterator<T> {

    private final Reader<T> reader;

    public ReadingIterator(Reader<T> reader) {
        this.reader = reader;
    }

    @Override
    protected T computeNext() {
        if (!reader.advance()) {
            return endOfData();
        }

        return reader.current();
    }
}
