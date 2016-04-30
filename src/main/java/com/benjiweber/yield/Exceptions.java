/*
 * Copyright (c) 2015 Benji Weber
 * Licensed under the MIT License
 */
package com.benjiweber.yield;

public class Exceptions {
    public interface ExceptionalSupplier<R, E extends Exception> {
        R get() throws E;
    }

    public interface ExceptionalVoid<E extends Exception> {
        void apply() throws E;
    }

    public static <T, E extends Exception> T unchecked(ExceptionalSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (Error | RuntimeException rex) {
            throw rex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void unchecked(ExceptionalVoid method) {
        try {
            method.apply();
        } catch (Error | RuntimeException rex) {
            throw rex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
