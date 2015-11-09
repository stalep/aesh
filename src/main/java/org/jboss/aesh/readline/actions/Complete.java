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

import org.jboss.aesh.console.Config;
import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.ActionEvent;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.terminal.Key;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Complete implements ActionEvent {

    private boolean askForCompletion = false;
    private KeyEvent key;

    @Override
    public String name() {
        return "complete";
    }

    @Override
    public void apply(Readline.Interaction interaction) {
        if(askForCompletion) {
            askForCompletion = false;
            if(key == Key.y) {
                //inputProcessor.getCompleter().setAskDisplayCompletion(false);
                interaction.completionHandler().complete(interaction);
            }
            else {
                interaction.completionHandler().setAskDisplayCompletion(false);
                interaction.getUndoManager().clear();
                interaction.conn.write(Config.getLineSeparator());
                interaction.buffer().clear();
                interaction.resume();
                /*
                inputProcessor.getBuffer().out().print(Config.getLineSeparator());
                inputProcessor.clearBufferAndDisplayPrompt();
                inputProcessor.getBuffer().drawLine();
                inputProcessor.getBuffer().moveCursor(inputProcessor.getBuffer().getBuffer().getMultiCursor());
                */
            }
        }
        else {
            if(interaction.completionHandler() != null) {
                interaction.completionHandler().complete( interaction);
                if(interaction.completionHandler().doAskDisplayCompletion()) {
                    askForCompletion = true;
                }
            }
        }
    }

    @Override
    public void input(Action action, KeyEvent key) {
        if(askForCompletion) {
            this.key = key;
        }
    }

    @Override
    public boolean keepFocus() {
        return askForCompletion;
    }
}
