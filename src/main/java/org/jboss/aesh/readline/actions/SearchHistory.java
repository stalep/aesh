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

import org.jboss.aesh.history.SearchDirection;
import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.LineBuffer;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.readline.SearchAction;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class SearchHistory implements SearchAction {

    private static final Logger LOGGER = LoggerUtil.getLogger(SearchHistory.class.getName());

    private SearchAction.Status status = Status.SEARCH_NOT_STARTED;
    private StringBuilder searchArgument;
    private String searchResult;
    private SearchAction.Status defaultAction;

    SearchHistory(SearchAction.Status defaultAction) {
        this.defaultAction = defaultAction;
    }

     @Override
    public void input(Action action, KeyEvent key) {
         if(action == null && Key.isPrintable(key.buffer().array())) {
             if(searchArgument == null)
                 searchArgument = new StringBuilder();
             status = defaultAction;
             searchArgument.append((char) key.buffer().array()[0]);
         }
         else if(action instanceof Interrupt) {
             status = Status.SEARCH_INTERRUPT;
         }
         else if(action != null &&
                 action.name().equals("accept-line")) {
             status = Status.SEARCH_END;
         }
         else if(action instanceof ReverseSearchHistory) {
             status = Status.SEARCH_PREV;
         }
         else if(action instanceof ForwardSearchHistory) {
             status = Status.SEARCH_NEXT;
         }
         else if(action instanceof DeletePrevChar) {
             status = Status.SEARCH_DELETE;
         }
         else if(action instanceof PrevHistory)
             status = Status.SEARCH_MOVE_PREV;
         else if(action instanceof NextHistory)
             status = Status.SEARCH_MOVE_NEXT;
         else if(action instanceof ForwardChar)
             status = Status.SEARCH_MOVE_RIGHT;
         else if(action instanceof BackwardChar)
             status = Status.SEARCH_EXIT;
         else {
             if(key == Key.ESC) {
                 status = Status.SEARCH_EXIT;
             }
             if(Key.isPrintable(key.buffer().array())) {
                 if(searchArgument == null)
                     searchArgument = new StringBuilder();
                 status = defaultAction;
                 searchArgument.append((char) key.buffer().array()[0]);
             }
         }
    }

    @Override
    public boolean keepFocus() {
        return (status == Status.SEARCH_INPUT || status == Status.SEARCH_PREV ||
                status == Status.SEARCH_NEXT || status == Status.SEARCH_DELETE );
    }

   @Override
    public void apply(Readline.Interaction interaction) {

       if(status == Status.SEARCH_INTERRUPT) {
           interaction.refresh(new LineBuffer());
           /*
           inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
           inputProcessor.getBuffer().setBufferLine("");
           inputProcessor.getBuffer().drawLine();
           inputProcessor.getBuffer().out().print(Buffer.printAnsi((inputProcessor.getBuffer().getBuffer().getPrompt().getLength() + 1) + "G"));
           inputProcessor.getBuffer().out().flush();
           */
           searchArgument = null;
           searchResult = null;
       }
       else {
           switch(status) {
               case SEARCH_PREV:
                   if(interaction.getHistory().getSearchDirection() != SearchDirection.REVERSE)
                       interaction.getHistory().setSearchDirection(SearchDirection.REVERSE);
                   if(searchArgument != null && searchArgument.length() > 0) {
                       String tmpResult = interaction.getHistory().search(searchArgument.toString());
                       if(tmpResult == null)
                           searchArgument.deleteCharAt(searchArgument.length()-1);
                       else
                           searchResult = tmpResult;
                   }
                   break;
               case SEARCH_NEXT:
                   if(interaction.getHistory().getSearchDirection() != SearchDirection.FORWARD)
                       interaction.getHistory().setSearchDirection(SearchDirection.FORWARD);
                   if(searchArgument != null && searchArgument.length() > 0) {
                       String tmpResult = interaction.getHistory().search(searchArgument.toString());
                       if(tmpResult == null)
                           searchArgument.deleteCharAt(searchArgument.length()-1);
                       else
                           searchResult = tmpResult;
                   }
                   break;
               case SEARCH_NOT_STARTED:
                   status = Status.SEARCH_PREV;
                   interaction.getHistory().setSearchDirection(SearchDirection.REVERSE);
                   if(interaction.buffer().size() > 0) {
                       searchArgument = new StringBuilder( interaction.buffer().toString());
                       searchResult = interaction.getHistory().search(searchArgument.toString());
                   }
                   break;
               case SEARCH_DELETE:
                   if(searchArgument != null && searchArgument.length() > 0) {
                       searchArgument.deleteCharAt(searchArgument.length() - 1);
                       searchResult = interaction.getHistory().search(searchArgument.toString());
                   }
                   break;
               case SEARCH_END:
                   if(searchResult != null) {
                       LineBuffer buf = new LineBuffer().insert(searchResult);
                       interaction.refresh(buf);
                       LOGGER.info("sending new-line to queueEvent");
                       interaction.queueEvent(new int[]{'\n'});
                       /*
                       inputProcessor.getBuffer().moveCursor(-interaction.buffer().getCursor());
                       inputProcessor.getBuffer().setBufferLine( searchResult);
                       inputProcessor.getBuffer().drawLine();
                       inputProcessor.getBuffer().out().println();
                       inputProcessor.getHistory().push(inputProcessor.getBuffer().getBuffer().getLineNoMask());
                       //search.setResult( inputProcessor.getBuffer().getBuffer().getLineNoMask());
                       //search.setFinished(true);
                       inputProcessor.getBuffer().getBuffer().reset();
                       inputProcessor.setReturnValue(searchResult);
                       */
                       break;
                   }
                   else {
                       interaction.refresh(new LineBuffer());
                       /*
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine("");
                       inputProcessor.getBuffer().drawLine();
                       */
                   }
                   break;
               case SEARCH_EXIT:
                   if(searchResult != null) {
                       LineBuffer buf = new LineBuffer().insert(searchResult);
                       interaction.refresh(buf);
                       /*
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine(searchResult);
                       */
                   }
                   else {
                       interaction.refresh(new LineBuffer());
                       /*
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine("");
                       */
                   }
                   break;
               case SEARCH_MOVE_NEXT:
                   LineBuffer buf = new LineBuffer().insert(interaction.getHistory().getNextFetch());
                   interaction.refresh(buf);
                   /*
                   searchResult = inputProcessor.getHistory().getNextFetch();
                   inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                   inputProcessor.getBuffer().setBufferLine(searchResult);
                   */
                   break;
               case SEARCH_MOVE_PREV:
                   //LineBuffer buf = new LineBuffer().insert(interaction.getHistory().getNextFetch());
                   interaction.refresh(new LineBuffer().insert(interaction.getHistory().getNextFetch()));
                   /*
                   searchResult = inputProcessor.getHistory().getPreviousFetch();
                   inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                   inputProcessor.getBuffer().setBufferLine(searchResult);
                   */
                   break;
               case SEARCH_MOVE_RIGHT:
                   interaction.refresh(new LineBuffer().insert(interaction.getHistory().getNextFetch()));
                   /*
                   inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                   inputProcessor.getBuffer().setBufferLine(searchResult);
                   */
           }

           if(!keepFocus()) {
               searchArgument = null;
               searchResult = null;
               /*
               inputProcessor.getBuffer().drawLine();
               inputProcessor.getBuffer().out().print(Buffer.printAnsi((inputProcessor.getBuffer().getBuffer().getPrompt().getLength() + 1) + "G"));
               if(status == Status.SEARCH_MOVE_RIGHT)
                   inputProcessor.getBuffer().moveCursor(inputProcessor.getBuffer().getBuffer().getLine().length());
               inputProcessor.getBuffer().out().flush();
               */
           }
           else {
               if(searchArgument == null || searchArgument.length() == 0) {
                   if(searchResult != null)
                       printSearch("", searchResult, interaction);
                   else
                       printSearch("", "", interaction);
               }
               else {
                   if(searchResult != null && searchResult.length() > 0)
                       printSearch(searchArgument.toString(), searchResult, interaction);
               }
           }
       }
       interaction.resume();
    }

    private void printSearch(String searchTerm, String result, Readline.Interaction interaction) {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder builder;
        if(interaction.getHistory().getSearchDirection() == SearchDirection.REVERSE)
            builder = new StringBuilder("(reverse-i-search) `");
        else
            builder = new StringBuilder("(forward-i-search) `");
        builder.append(searchTerm).append("': ");
        cursor += builder.length();
        builder.append(result);
        interaction.disablePrompt();
        if(searchTerm.length() < 1) {
            interaction.connection().write(ANSI.CURSOR_START);
            interaction.connection().write(ANSI.START + "2K");
        }
        LOGGER.info("printing: "+builder.toString());
        LineBuffer buf = new LineBuffer().insert(builder.toString());
        interaction.refresh(buf);
        interaction.enablePrompt();
        //interaction.resume();
        /*
        inputProcessor.getBuffer().getBuffer().disablePrompt(true);
        inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
        inputProcessor.getBuffer().out().print(ANSI.CURSOR_START);
        inputProcessor.getBuffer().out().print(ANSI.START + "2K");
        inputProcessor.getBuffer().setBufferLine(builder.toString());
        inputProcessor.getBuffer().moveCursor(cursor);
        inputProcessor.getBuffer().drawLine(true, false);
        inputProcessor.getBuffer().getBuffer().disablePrompt(false);
        inputProcessor.getBuffer().out().flush();
        */
    }
}
