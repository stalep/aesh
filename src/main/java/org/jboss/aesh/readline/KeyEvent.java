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

import java.nio.IntBuffer;

/**
 * A key event.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface KeyEvent {

  default IntBuffer buffer() {
    int length = length();
    IntBuffer buf = IntBuffer.allocate(length);
    for (int i = 0;i < length;i++) {
      buf.put(getCodePointAt(i));
    }
    buf.flip();
    return buf;
  }

  int getCodePointAt(int index) throws IndexOutOfBoundsException;

  int length();
}
