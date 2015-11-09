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

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.terminal.TerminalConnection;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TestTerminalConnection {


    @Test
    public void simpleTest() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder().logging(true).create();

        TerminalConnection connection = new TerminalConnection(pipedInputStream, byteArrayOutputStream);

        Readline readline = new Readline();

        readline.readline(connection, "> ", line -> {

            if(line == null) {
                System.out.println("we got null");
            }
            else {
                System.out.println("we got: "+ line);
                connection.write("FOOO");
            }
        });

        outputStream.write(("testing....\n").getBytes());
        outputStream.flush();

        //readline.schedulePendingEvent();

        System.out.println("before sleep");
        Thread.sleep(1000);
        System.out.println("after sleep");
        System.out.println("buffer: " + byteArrayOutputStream.toString());
        System.out.println("after buffer");

    }
}
