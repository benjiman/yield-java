/*
 * Copyright (c) 2015 Benji Weber
 * Licensed under the MIT License
 */
package com.benjiweber.yield;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static java.util.Arrays.spliterator;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class YieldTest {

    private SideEffects ignoreSideEffects = sideEffect -> {};

    public static Yielderable<String> fooBar(SideEffects sideEffects) {
        return yield -> {
            sideEffects.sideEffect(1);
            yield.returning("foo");
            sideEffects.sideEffect(2);
            yield.returning("bar");
            sideEffects.sideEffect(3);
        };
    }

    public static Yielderable<Integer> oneToFive(SideEffects sideEffects) {
        return yield -> {
            for (int i = 1; i < 10; i++) {
                sideEffects.sideEffect(i);
                if (i == 6) yield.breaking();
                yield.returning(i);
            }
        };
    }

    @Test public void should_have_expected_values() {
        ArrayList<String> results = new ArrayList<>();

        for (String result : fooBar(ignoreSideEffects)) {
            results.add(result);
        }

        assertEquals(asList("foo","bar"), results);
    }


    @Test public void should_perform_side_effects_in_expected_order() {
        SideEffects sideEffects = mock(SideEffects.class);

        Iterable<String> foos = fooBar(sideEffects);
        verifyZeroInteractions(sideEffects);

        int sideEffectNumber = 1;
        for  (String foo : foos) {
            verify(sideEffects).sideEffect(sideEffectNumber++);
            verifyNoMoreInteractions(sideEffects);
        }
        verify(sideEffects).sideEffect(sideEffectNumber++);
        verifyNoMoreInteractions(sideEffects);

    }

    @Test public void should_break_out() {
        ArrayList<Integer> results = new ArrayList<>();
        for (Integer number : oneToFive(ignoreSideEffects)) {
            results.add(number);
        }
        assertEquals(asList(1,2,3,4,5), results);
    }


    @Test public void should_perform_side_effects_in_expected_order_with_loop() {
        SideEffects sideEffects = mock(SideEffects.class);

        Iterable<Integer> numbers = oneToFive(sideEffects);
        verifyZeroInteractions(sideEffects);

        int sideEffectNumber = 1;
        for  (Integer i : numbers) {
            verify(sideEffects).sideEffect(sideEffectNumber++);
            verifyNoMoreInteractions(sideEffects);
        }
        verify(sideEffects).sideEffect(sideEffectNumber++);
        verifyNoMoreInteractions(sideEffects);
    }

    @Test public void iterator_should_not_require_call_to_hasNext() {
        Iterable<String> strings = fooBar(ignoreSideEffects);
        Iterator<String> iterator = strings.iterator();
        assertEquals("foo", iterator.next());
        assertEquals("bar", iterator.next());
    }

    @Test public void iterable_should_not_be_stateful() {
        Iterable<String> strings = fooBar(ignoreSideEffects);
        assertEquals("foo", strings.iterator().next());
        assertEquals("foo", strings.iterator().next());
    }


    @Test public void autoclose_infinite_iterator() {
        try (ClosableIterator<Integer> positiveIntegers = positiveIntegers().iterator()) {
            assertEquals(Integer.valueOf(1), positiveIntegers.next());
            assertEquals(Integer.valueOf(2), positiveIntegers.next());
            assertEquals(Integer.valueOf(3), positiveIntegers.next());
        }
    }

    public static Yielderable<Integer> positiveIntegers() {
        return yield -> {
            int i = 0;
            while (true) yield.returning(++i);
        };
    }

    interface SideEffects {
        void sideEffect(int sequence);
    }

}
