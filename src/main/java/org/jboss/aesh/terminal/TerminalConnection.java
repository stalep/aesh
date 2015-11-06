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

package org.jboss.aesh.terminal;

import org.jboss.aesh.io.BinaryDecoder;
import org.jboss.aesh.io.BinaryEncoder;
import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.api.Terminal;
import org.jboss.aesh.terminal.api.TerminalBuilder;
import org.jboss.aesh.tty.TtyConnection;
import org.jboss.aesh.tty.TtyEvent;
import org.jboss.aesh.tty.TtyEventDecoder;
import org.jboss.aesh.tty.TtyOutputMode;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalConnection implements TtyConnection {

    private Terminal terminal;

    private static final Logger LOGGER = LoggerUtil.getLogger(TerminalConnection.class.getName());

    private Consumer<Size> sizeHandler;
    private Consumer<String> termHandler;

    private TtyEventDecoder eventDecoder;
    private BinaryDecoder decoder;
    private Consumer<int[]> stdout;
    private ExecutorService executorService;
    private Attributes attributes;

    public TerminalConnection(InputStream inputStream, OutputStream outputStream) {
        try {
            terminal = TerminalBuilder.builder()
            .streams(inputStream, outputStream)
                    .nativeSignals(false)
            .name("Aesh console")
            .build();

            eventDecoder = new TtyEventDecoder(3, 26, 4);
            decoder = new BinaryDecoder(1024, StandardCharsets.UTF_8, eventDecoder);
            stdout = new TtyOutputMode(new BinaryEncoder(StandardCharsets.UTF_8, this::write));

            terminal.handle(Terminal.Signal.INT, s -> LOGGER.info("GOT INTERRUPTED::::"+s));

            LOGGER.info("started connection, trying to start reader:");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
         startReading();
        LOGGER.info("reader started...");
    }

    public TerminalConnection() {
        this(System.in, System.out);
    }

    public void startReading() {
        executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread inputThread = Executors.defaultThreadFactory().newThread(runnable);
                inputThread.setName("Aesh InputStream Reader");
                inputThread.setDaemon(true);
                return inputThread;
            }
        });

        executorService.execute(new Runnable() {
            byte[] bBuf = new byte[1024];
            @Override
            public void run() {
                try {
                    attributes = terminal.enterRawMode();
                    LOGGER.info("reading data");
                    while(true) {
                        int read = terminal.input().read(bBuf);
                        if(read > 0) {
                            LOGGER.info("got data, pushing to decoder.");
                            decoder.write(bBuf, 0, read);
                        }
                        else if(read < 0) {
                            close();
                            return;
                        }

                    }
                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
    }

    private void write(byte[] data) {
        try {
            terminal.output().write(data);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Size size() {
        LOGGER.info("getting size: "+terminal.getSize());
        return terminal.getSize();
    }

    @Override
    public String term() {
        return terminal.getName();
    }

    @Override
    public Consumer<String> getTermHandler() {
        return termHandler;
    }

    @Override
    public void setTermHandler(Consumer<String> handler) {
        termHandler = handler;
    }

    @Override
    public Consumer<Size> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        sizeHandler = handler;

    }

    @Override
    public BiConsumer<TtyEvent, Integer> getEventHandler() {
        return eventDecoder.getEventHandler();
    }

    @Override
    public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
        eventDecoder.setEventHandler(handler);
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return eventDecoder.getReadHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        eventDecoder.setReadHandler(handler);
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return stdout;
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {

    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return null;
    }

    @Override
    public void close() {
        try {
            if (attributes != null && terminal != null) {
                terminal.setAttributes(attributes);
                terminal.close();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(Runnable task) {

    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {

    }
}
