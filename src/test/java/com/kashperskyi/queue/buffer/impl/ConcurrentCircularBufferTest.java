package com.kashperskyi.queue.buffer.impl;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.kashperskyi.queue.buffer.Reader;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class ConcurrentCircularBufferTest {

    private ExecutorService executorService;
    private ConcurrentCircularBuffer<Integer> buffer;

    @Before
    public void setup() {
        executorService = Executors.newFixedThreadPool(32);
        buffer = new ConcurrentCircularBuffer<>(100);
    }

    @After
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void readGuaranteesAllDataPresentAtReaderCreation() {

        IntStream.range(0, 100).forEach(val -> executorService.submit(() -> buffer.add(val)));

        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch writeLatch = new CountDownLatch(100);
        Set<Integer> expected = Sets.newConcurrentHashSet();

        executorService.submit(() -> {
            Reader<Integer> read = buffer.read();
            readLatch.countDown();

            // starting reader
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            IntStream.range(0, 50).forEach(val -> {
                read.advance();
                expected.add(read.current());
            });

            // pausing reader
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            // finish reading
            while (read.advance()) {
                expected.add(read.current());
            }
        });

        Uninterruptibles.awaitUninterruptibly(readLatch);

        IntStream.range(100, 200).forEach(val -> executorService.submit(() -> {
            buffer.add(val);
            writeLatch.countDown();
        }));

        Uninterruptibles.awaitUninterruptibly(writeLatch);
        List<Integer> missing = IntStream.range(0, 100).boxed().filter(Predicates.not(expected::contains)).collect(toList());
        Assert.assertThat(missing.isEmpty(), CoreMatchers.is(true));
    }
}