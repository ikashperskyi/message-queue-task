package com.kashperskyi.queue.service.impl;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.kashperskyi.queue.buffer.CircularBuffer;
import com.kashperskyi.queue.service.MessageQueue;
import com.kashperskyi.queue.service.Snapshot;
import com.kashperskyi.queue.buffer.impl.ConcurrentCircularBuffer;
import com.kashperskyi.queue.buffer.impl.ReadingIterator;
import com.kashperskyi.queue.service.Message;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class ExpiringBufferMessageQueue implements MessageQueue {

    private static final Range<Integer> ERROR_RANGE = Range.closed(400, 599);

    private final CircularBuffer<TimedMessage> circularBuffer;
    private final Clock clock;

    public ExpiringBufferMessageQueue(int capacity, Duration expiration, Clock clock) {
        this.circularBuffer = new ConcurrentCircularBuffer<>(capacity);
        this.clock = Clock.offset(clock, expiration.negated());
    }

    @Override
    public void add(Message message) {
        circularBuffer.add(new TimedMessage(message, clock.instant()));
    }

    @Override
    public Snapshot snapshot() {
        Instant expirationInstant = clock.instant();
        return new Snapshot(Streams.stream(new ReadingIterator<>(circularBuffer.read()))
                .filter(Objects::nonNull)
                .filter(timedMessage -> timedMessage.instant().isAfter(expirationInstant))
                .map(TimedMessage::message));
    }

    @Override
    public long numberOfErrorMessages() {
        return snapshot()
                .stream()
                .map(Message::getErrorCode)
                .filter(ERROR_RANGE::contains)
                .count();
    }

    private static class TimedMessage {

        private final Message message;
        private final Instant instant;

        public TimedMessage(Message message, Instant instant) {
            this.message = message;
            this.instant = instant;
        }

        public Message message() {
            return message;
        }

        public Instant instant() {
            return instant;
        }
    }
}
