package com.kashperskyi.queue.service;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class Snapshot {

    private final List<Message> underlying;

    public Snapshot(Stream<Message> stream) {
        underlying = stream.collect(toImmutableList());
    }

    public List<Message> asList() {
        return underlying;
    }

    public Stream<Message> stream() {
        return underlying.stream();
    }
}
