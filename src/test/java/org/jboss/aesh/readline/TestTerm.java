/*
 * Copyright 2015 Julien Viet
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

package org.jboss.aesh.readline;


import org.jboss.aesh.console.CompletionHandler;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.BeginningOfLine;
import org.jboss.aesh.readline.actions.Complete;
import org.jboss.aesh.readline.actions.DeleteChar;
import org.jboss.aesh.readline.actions.DeleteEndOfLine;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.tty.TtyConnection;
import org.jboss.aesh.tty.TtyEvent;
import org.jboss.aesh.tty.TtyOutputMode;
import org.jboss.aesh.util.TestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
class TestTerm {

  private TestBase readlineTest;
  private int[][] buffer = new int[10][];
  private int row;
  private int cursor;
  private int status = 0;
  private int acc = -1;
  private int bell;
  private int width = 40;
  Consumer<int[]> writeHandler = new Consumer<int[]>() {
    @Override
    public void accept(int[] event) {
      for (int i : event) {
        if (buffer[row] == null) {
          buffer[row] = new int[100];
        }
        switch (status) {
          case 0:
            if (i >= 32) {
              buffer[row][cursor] = i;
              forward();
            } else {
              switch (i) {
                case 7:
                  bell++;
                  break;
                case '\r':
                  cursor = 0;
                  break;
                case '\n':
                  row++;
                  break;
                case 27:
                  status = 1;
                  break;
                case '\b':
                  backward();
                  break;
              }
            }
            break;
          case 1:
            if (i == '[') {
              status = 2;
            } else {
              throw new UnsupportedOperationException();
            }
            break;
          case 2:
            if (i >= '0' && i <= '9') {
              if (acc == -1) {
                acc = i - '0';
              } else {
                acc = acc * 10 + (i - '0');
              }
            } else {
              switch (i) {
                case 'A':
                  while (acc-- > 0 && row > 0) {
                    row--;
                  }
                  break;
                case 'B':
                  while (acc-- > 0 && row < buffer.length) {
                    row++;
                  }
                  break;
                case 'C':
                  while (acc-- > 0) {
                    forward();
                  }
                  break;
                case 'D':
                  while (acc-- > 0) {
                    backward();
                  }
                  break;
                case 'K': {
                  if (acc != -1) {
                    throw new UnsupportedOperationException("Not yet implemented");
                  } else {
                    for (int j = cursor;j < buffer[row].length;j++) {
                      buffer[row][j] = 0;
                    }
                  }
                  break;
                }
                default:
                  throw new UnsupportedOperationException("Implement escape sequence " + i);
              }
              acc = -1;
              status = 0;
            }
            break;
          default:
            throw new UnsupportedOperationException("Unsupported cp=" + i + " with status=" + status);
        }
      }
    }

    private void backward() {
      if (cursor > 0) {
        cursor--;
      } else {
        throw new UnsupportedOperationException();
      }
    }

    private void forward() {
      cursor++;
      if (cursor == width) {
        cursor = 0;
        row++;
      }
    }
  };
  final Consumer<int[]> stdout = new TtyOutputMode(writeHandler);
  final Readline readline;

  Consumer<int[]> readHandler;
  Consumer<Size> sizeHandler;
  BiConsumer<TtyEvent, Integer> eventHandler;
  private LinkedList<Runnable> tasks = new LinkedList<>();

  TtyConnection conn = new TtyConnection() {

    @Override
    public String terminalType() {
      return "xterm";
    }

    @Override
    public Size size() {
      return new Size(width, 20);
    }

    @Override
    public Consumer<String> getTerminalTypeHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setTerminalTypeHandler(Consumer<String> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Size> getSizeHandler() {
      return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
      sizeHandler = handler;
      if (handler != null) {
        handler.accept(new Size(40, 20));
      }
    }

    @Override
    public BiConsumer<TtyEvent, Integer> getEventHandler() {
      return eventHandler;
    }

    @Override
    public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
      eventHandler = handler;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
      return readHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
      readHandler = handler;
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
      return stdout;
    }

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Void> getCloseHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException();
    }
  };

  public TestTerm(TestBase test) {
    this.readlineTest = test;
    EditMode editMode = EditModeBuilder.builder().createSimple();

    editMode.addAction(Key.BACKSPACE, new DeletePrevChar());
    editMode.addAction(Key.LEFT, new BackwardChar());
    editMode.addAction(Key.RIGHT,  new ForwardChar());
    editMode.addAction(Key.UP, new PrevHistory());
    editMode.addAction(Key.DOWN,  new NextHistory());
    editMode.addAction(Key.CTRL_A, new BeginningOfLine());
    editMode.addAction(Key.CTRL_E, new EndOfLine());
    editMode.addAction(Key.CTRL_D, new DeleteChar());
    editMode.addAction(Key.CTRL_I, new Complete());
    editMode.addAction(Key.CTRL_K, new DeleteEndOfLine());

    readline = new Readline(editMode);
  }

  public void readlineFail() {
    readline(event -> readlineTest.fail("Was not accepting a call"));
  }

  public Supplier<String> readlineComplete() {
    final AtomicReference<String> queue = new AtomicReference<>();
    readline(event -> queue.compareAndSet(null, event));
    return () -> queue.get();
  }

  public void readline(Consumer<String> readlineHandler) {
    readline(readlineHandler, null);
  }

  public Supplier<String> readlineComplete(CompletionHandler completionHandler) {
    final AtomicReference<String> queue = new AtomicReference<>();
    readline(event -> queue.compareAndSet(null, event), completionHandler);
    return () -> queue.get();
  }

  public void readline(Consumer<String> readlineHandler, CompletionHandler completionHandler) {
    readline.readline(conn, "% ", readlineHandler, completionHandler);
  }

  public void executeTasks() {
    while (!tasks.isEmpty()) {
      Runnable task = tasks.removeFirst();
      task.run();
    }
  }

  public int getBellCount() {
    return bell;
  }

  public void resetBellCount() {
    bell = 0;
  }

  private List<String> render() {
    List<String> lines = new ArrayList<>();
    for (int[] row : buffer) {
      if (row == null) {
        break;
      }
      StringBuilder line = new StringBuilder();
      for (int codePoint : row) {
        if (codePoint < 32) {
          break;
        }
        line.appendCodePoint(codePoint);
      }
      lines.add(line.toString());
    }
    return lines;
  }

  void assertScreen(String... expected) {
    List<String> lines = render();
    readlineTest.assertEquals(Arrays.asList(expected), lines);
  }

  void assertAt(int row, int cursor) {
    readlineTest.assertEquals(row, this.row);
    readlineTest.assertEquals(cursor, this.cursor);
  }

  void read(int... data) {
    readHandler.accept(data);
  }

  TestTerm setWidth(int width) {
    this.width = width;
    if (sizeHandler != null) {
      sizeHandler.accept(conn.size());
    }
    return this;
  }

}
