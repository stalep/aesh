/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.tty;

import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.api.Size;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A connection to a tty.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TtyConnection {

    String terminalType();

    Size size();

    Consumer<String> getTerminalTypeHandler();

    void setTerminalTypeHandler(Consumer<String> handler);

    Consumer<Size> getSizeHandler();

    void setSizeHandler(Consumer<Size> handler);

    BiConsumer<TtyEvent, Integer> getEventHandler();

    void setEventHandler(BiConsumer<TtyEvent, Integer> handler);

    Consumer<int[]> getStdinHandler();

    /**
     * Set the read handler on this connection.
     *
     * @param handler the event handler
     */
    void setStdinHandler(Consumer<int[]> handler);

    /**
     * @return the stdout handler of this connection
     */
    Consumer<int[]> stdoutHandler();

    void setCloseHandler(Consumer<Void> closeHandler);

    Consumer<Void> getCloseHandler();

    void close();

    /**
     * Write a string to the client.
     *
     * @param s the string to write
     */
    default TtyConnection write(String s) {
        int[] codePoints = Parser.toCodePoints(s);
        stdoutHandler().accept(codePoints);
        return this;
    }

    /**
     * Schedule a task for execution.
     *
     * @param task the task to schedule
     */
    void execute(Runnable task);

    /**
     * Schedule a task for execution.
     *
     * @param task the task to schedule
     * @param delay the delay
     * @param unit the time unit
     */
    void schedule(Runnable task, long delay, TimeUnit unit);

}
