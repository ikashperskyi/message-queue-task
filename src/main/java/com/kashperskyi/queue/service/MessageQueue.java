package com.kashperskyi.queue.service;

public interface MessageQueue {

    void add(Message message);

    Snapshot snapshot();

    long numberOfErrorMessages();
}
