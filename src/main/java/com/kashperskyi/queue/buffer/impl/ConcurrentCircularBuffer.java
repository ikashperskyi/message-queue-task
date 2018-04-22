package com.kashperskyi.queue.buffer.impl;

import com.kashperskyi.queue.buffer.CircularBuffer;
import com.kashperskyi.queue.buffer.Reader;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentCircularBuffer<T> implements CircularBuffer<T> {

    private static final int WRITE_PROMISE = -1;
    private static final int WRITE_UNLOCKED = 0;

    private final AtomicReferenceArray<T> buffer;
    private final AtomicIntegerArray bufferReferenceCounts;
    private final AtomicInteger cursor;
    private final Lock readLock;
    private final Lock writeLock;

    private final int capacity;

    public ConcurrentCircularBuffer(int capacity) {
        this.buffer = new AtomicReferenceArray<>(capacity);
        this.bufferReferenceCounts = new AtomicIntegerArray(capacity);
        this.cursor = new AtomicInteger(0);
        this.capacity = capacity;

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    public void add(T item) {
        writeLock.lock();

        try {
            int slot = cursor.get();
            cursor.set((slot + 1) % capacity);

            boolean reserved;
            do {
                reserved = bufferReferenceCounts.compareAndSet(slot, WRITE_UNLOCKED, WRITE_PROMISE);
            } while (!reserved);

            buffer.set(slot, item);

            if (!bufferReferenceCounts.compareAndSet(slot, WRITE_PROMISE, WRITE_UNLOCKED)) {
                throw new IllegalStateException("WRITE_PROMISE was reset."); // should never happen
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Reader<T> read() {
        // reserving the 1st element is blocking
        readLock.lock();
        try {
            return new ReferenceTrackingReader(cursor.get(), capacity);
        } finally {
            readLock.unlock();
        }
    }

    //TODO: make closable and wrap in buffering to prevent unfinished readers from permanently blocking the queue
    private class ReferenceTrackingReader implements Reader<T> {

        private int cursor;
        private int remaining;
        private T item;

        public ReferenceTrackingReader(int cursor, int remaining) {
            this.cursor = cursor;
            this.remaining = remaining;

            bufferReferenceCounts.incrementAndGet(cursor);
        }

        @Override
        public boolean advance() {
            if (remaining == 0) {
                return false;
            }

            item = buffer.get(cursor);
            cursor = (cursor + 1) % capacity;
            remaining--;

            if (remaining > 0) {
                bufferReferenceCounts.incrementAndGet(cursor);
            }
            int prev = cursor - 1;
            bufferReferenceCounts.decrementAndGet(prev < 0 ? capacity + prev : prev);

            return true;
        }

        @Override
        public T current() {
            return item;
        }
    }
}
