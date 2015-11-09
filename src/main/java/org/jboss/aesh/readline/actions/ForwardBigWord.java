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

import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.readline.editing.EditMode;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class ForwardBigWord extends ChangeAction {

    ForwardBigWord(EditMode.Status status) {
        super(status);
    }

    ForwardBigWord(boolean viMode, EditMode.Status status) {
        super(viMode, status);
    }

    @Override
    public void apply(Readline.Interaction interaction) {
        int cursor = interaction.buffer().getCursor();
        //if cursor stand on a delimiter, move till its no more delimiters
        if(cursor < interaction.buffer().size() &&
                (isDelimiter(interaction.buffer().getAt(cursor))))
            while(cursor < interaction.buffer().size() && (isDelimiter(interaction.buffer().getAt(cursor))))
                cursor++;
            //if we stand on a non-delimiter
        else {
            while(cursor < interaction.buffer().size() && !isSpace(interaction.buffer().getAt(cursor)))
                cursor++;

            //if we end up on a space we move past that too
            if(cursor < interaction.buffer().size() && isSpace(interaction.buffer().getAt(cursor)))
                while(cursor < interaction.buffer().size() && isSpace(interaction.buffer().getAt(cursor)))
                    cursor++;

            apply(cursor, interaction);
        }
    }

}
