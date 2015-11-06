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

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.terminal.TerminalConnection;
import org.jboss.aesh.tty.TtyConnection;
import org.jboss.aesh.tty.TtyEvent;
import org.jboss.aesh.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <ul>
 *   <li>{@link Readline} usage</li>
 *   <li>{@link TtyConnection} usage</li>
 * </ul>
 */
public class TestShell implements Consumer<TtyConnection> {

  private static final Pattern splitter = Pattern.compile("\\w+");

  private static final Settings settings = new SettingsBuilder().logging(true).create();

  private static final Logger LOGGER = LoggerUtil.getLogger(TestShell.class.getName());
  private volatile boolean running = true;

  public static void main(String[] args) {

    LOGGER.warning("starting...");
    TerminalConnection connection = new TerminalConnection();
    connection.startReading();
    LOGGER.info("starting shell");

    TestShell shell = new TestShell();
    shell.accept(connection);
  }

  public void accept(final TtyConnection conn) {
    Readline readline = new Readline();
    conn.write("Welcome to shell example\n\n");
    read(conn, readline);

    try {
      while(running) {
        Thread.sleep(500);
      }
    }
    catch(InterruptedException e) {
      LOGGER.warning("INTERRUPTED... ignoring..");
    }

  }

  /**
   * Use {@link Readline} to read a user input and then process it
   *
   * @param conn the tty connection
   * @param readline the readline object
   */
  public void read(final TtyConnection conn, final Readline readline) {

    // Just call readline and get a callback when line is read
    readline.readline(conn, "% ", line -> {

      // Ctrl-D
      if (line == null) {
        conn.write("logout\n").close();
        running = false;
        return;
      }

      LOGGER.info("got: " + line);

      Matcher matcher = splitter.matcher(line);
      if (matcher.find()) {
        String cmd = matcher.group();

        // Gather args
        List<String> args = new ArrayList<>();
        while (matcher.find()) {
          args.add(matcher.group());
        }

        try {
          new Task(conn, readline, Command.valueOf(cmd), args).start();
          return;
        } catch (IllegalArgumentException e) {
          conn.write(cmd + ": command not found\n");
        }
      }
      read(conn, readline);
    });
  }

  /**
   * A blocking interruptible task.
   */
  class Task extends Thread implements BiConsumer<TtyEvent, Integer> {

    final TtyConnection conn;
    final Readline readline;
    final Command command;
    final List<String> args;
    volatile boolean running;

    public Task(TtyConnection conn, Readline readline, Command command, List<String> args) {
      this.conn = conn;
      this.readline = readline;
      this.command = command;
      this.args = args;
    }

    @Override
    public void accept(TtyEvent event, Integer cp) {
      switch (event) {
        case INTR:
          if (running) {
            // Ctrl-C interrupt : we use Thread interrupts to signal the command to stop
            LOGGER.info("got interrupted in Task");
            interrupt();
          }
      }
    }

    @Override
    public void run() {

      // Subscribe to events, in particular Ctrl-C
      conn.setEventHandler(this);
      running = true;
      try {
        command.execute(conn, args);
      }
      catch (InterruptedException e) {
        // Ctlr-C interrupt
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        running = false;
        conn.setEventHandler(null);

        LOGGER.info("trying to read again.");
        // Readline again
        read(conn, readline);
      }
    }
  }

  /**
   * The shell app commands.
   */
  enum Command {

    sleep() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {
        if (args.isEmpty()) {
          conn.write("usage: sleep seconds\n");
          return;
        }
        int time = -1;
        try {
          time = Integer.parseInt(args.get(0));
        } catch (NumberFormatException ignore) {
        }
        if (time > 0) {
          // Sleep until timeout or Ctrl-C interrupted
          Thread.sleep(time * 1000);
        }
      }
    },

    echo() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {
        for (int i = 0;i < args.size();i++) {
          if (i > 0) {
            conn.write(" ");
          }
          conn.write(args.get(i));
        }
        conn.write("\n");
      }
    },

    window() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {
        conn.write("Current window size " + conn.size() + ", try resize it\n");

        // Refresh the screen with the new size
        conn.setSizeHandler(size -> {
          conn.write("Window resized " + size + "\n");
        });

        try {
          // Wait until interrupted
          new CountDownLatch(1).await();
        } finally {
          conn.setSizeHandler(null);
        }
      }
    },

    help() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {
        StringBuilder msg = new StringBuilder("Demo term, try commands: ");
        Command[] commands = Command.values();
        for (int i = 0;i < commands.length;i++) {
          if (i > 0) {
            msg.append(",");
          }
          msg.append(" ").append(commands[i].name());
        }
        msg.append("...\n");
        conn.write(msg.toString());
      }
    },

    keyscan() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {

        // Subscribe to key events and print them
        conn.setStdinHandler(keys -> {
          for (int key : keys) {
            conn.write(key + " pressed\n");
          }
        });

        try {
          // Wait until interrupted
          new CountDownLatch(1).await();
        }
        finally {
          conn.setStdinHandler(null);
        }
      }
    },

    top() {
      @Override
      public void execute(TtyConnection conn, List<String> args) throws Exception {
        while (true) {

          StringBuilder buf = new StringBuilder();
          Formatter formatter = new Formatter(buf);

          List<Thread> threads = new ArrayList<>(Thread.getAllStackTraces().keySet());
          for (int i = 1;i <= conn.size().getHeight();i++) {

            // Change cursor position and erase line with ANSI escape code magic
            buf.append("\033[").append(i).append(";1H\033[K");

            //
            String format = "  %1$-5s %2$-10s %3$-50s %4$s";
            if (i == 1) {
              formatter.format(format,
                  "ID",
                  "STATE",
                  "NAME",
                  "GROUP");
            } else {
              int index = i - 2;
              if (index < threads.size()) {
                Thread thread = threads.get(index);
                formatter.format(format,
                    thread.getId(),
                    thread.getState().name(),
                    thread.getName(),
                    thread.getThreadGroup().getName());
              }
            }
          }

          conn.write(buf.toString());

          // Sleep until we refresh the list of interrupted
          Thread.sleep(1000);
        }
      }
    };

    abstract void execute(TtyConnection conn, List<String> args) throws Exception;
  }
}
