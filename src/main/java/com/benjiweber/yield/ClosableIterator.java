/*
 * Copyright (c) 2015 Benji Weber
 * Licensed under the MIT License
 */
package com.benjiweber.yield;

import java.util.Iterator;

public interface ClosableIterator<T> extends Iterator<T>, AutoCloseable {
    void close();
}
