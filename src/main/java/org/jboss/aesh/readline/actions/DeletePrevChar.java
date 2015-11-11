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
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.LineBuffer;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.undo.UndoAction;

import java.util.Arrays;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DeletePrevChar implements Action {
    @Override
    public String name() {
        return "backward-delete-char";
    }

    @Override
    public void apply(Readline.Interaction interaction) {
        /*
        if(inputProcessor.getBuffer().getBuffer().isMasking()) {
            if(inputProcessor.getBuffer().getBuffer().getPrompt().getMask() == 0) {
                deleteWithMaskEnabled(inputProcessor.getBuffer());
                return;
            }
        }
        */
        deleteNoMasking(interaction);
    }

    private void deleteNoMasking(Readline.Interaction interaction) {
        int cursor = interaction.buffer().getCursor();
        if(cursor > 0) {
            int lineSize = interaction.buffer().size();
            if(cursor > lineSize)
                cursor = lineSize;

        interaction.getUndoManager().addUndo(new UndoAction(
                interaction.buffer().getCursor(),
                interaction.buffer().toArray()));

            interaction.getPasteManager().addText(
                    Arrays.copyOfRange(interaction.buffer().toArray(), cursor - 1, cursor));

            LineBuffer buf = interaction.buffer().copy();
            buf.delete(-1);
            //buf.moveCursor(-1);
            interaction.refresh(buf);
        }
        interaction.resume();
    }

    private void deleteWithMaskEnabled(ConsoleBuffer consoleBuffer) {
        if(consoleBuffer.getBuffer().getLineNoMask().length() > 0) {
            consoleBuffer.getBuffer().delete(consoleBuffer.getBuffer().getLineNoMask().length() - 1,
                    consoleBuffer.getBuffer().getLineNoMask().length());
            consoleBuffer.moveCursor(consoleBuffer.getBuffer().getLineNoMask().length());
            consoleBuffer.drawLine();
        }
    }
}
