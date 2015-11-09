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
package org.jboss.aesh.console;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.alias.Alias;
import org.jboss.aesh.console.alias.AliasManager;
import org.jboss.aesh.console.operator.ControlOperatorParser;
import org.jboss.aesh.console.operator.RedirectionCompletion;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.LineBuffer;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.readline.actions.ActionMapper;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.util.LoggerUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCompletionHandler implements CompletionHandler {

    private volatile boolean enabled = true;

    private final AeshContext aeshContext;
    private boolean askDisplayCompletion = false;
    private int displayCompletionSize = 100;
    private final List<Completion> completionList;
    private AliasManager aliasManager;
    private final Shell shell;
    private final boolean doLogging;

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshCompletionHandler.class.getName());

    public AeshCompletionHandler(AeshContext aeshContext,
                                 Shell shell, boolean doLogging) {
        completionList = new ArrayList<>();
        this.aeshContext = aeshContext;
        this.shell = shell;
        this.doLogging = doLogging;
    }

    @Override
    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    @Override
    public void addCompletion(Completion completion) {
        completionList.add(completion);
    }

    @Override
    public void removeCompletion(Completion completion) {
        completionList.remove(completion);
    }

    @Override
    public boolean doAskDisplayCompletion() {
        return askDisplayCompletion;
    }

    @Override
    public void setAskDisplayCompletion(boolean askDisplayCompletion) {
        this.askDisplayCompletion = askDisplayCompletion;
    }

    @Override
    public void setAskCompletionSize(int size) {
        displayCompletionSize = size;
    }

    @Override
    public int getAskCompletionSize() {
        return displayCompletionSize;
    }

    @Override
    public void setAliasManager(AliasManager aliasManager) {
        this.aliasManager = aliasManager;
    }

     /**
     * Display possible completions.
     * 1. Find all possible completions
     * 2. If we find only one, display it.
     * 3. If we find more than one, display them,
     *    but not more than 100 at once
     */
    @Override
    public void complete(Readline.Interaction interaction) {
        LineBuffer buffer = interaction.buffer();
        PrintStream out = inputProcessor.getBuffer().out();
        if(!enabled)
            return;

        if(completionList.size() < 1)
            return;

        List<CompleteOperation> possibleCompletions = new ArrayList<>();
        int pipeLinePos = 0;
        boolean redirect = false;
        if(ControlOperatorParser.doStringContainPipelineOrEnd(buffer.getMultiLine())) {
            pipeLinePos =  ControlOperatorParser.findLastPipelineAndEndPositionBeforeCursor(buffer.getMultiLine(), buffer.getMultiCursor());
        }
        if(ControlOperatorParser.findLastRedirectionPositionBeforeCursor(buffer.getMultiLine(), buffer.getMultiCursor()) > pipeLinePos) {
            pipeLinePos = 0;
            redirect = true;
        }

        for(int i=0; i < completionList.size(); i++) {
            if(redirect && !completionList.get(i).getClass().equals(RedirectionCompletion.class)) {
                break;
            }
            CompleteOperation co;
            if(pipeLinePos > 0) {
                co = findAliases(buffer.getMultiLine().substring(pipeLinePos, buffer.getMultiCursor()), buffer.getMultiCursor() - pipeLinePos);
            }
            else {
                co = findAliases(buffer.getMultiLine(), buffer.getMultiCursor());
            }

            completionList.get(i).complete(co);

            if(co.getCompletionCandidates() != null && co.getCompletionCandidates().size() > 0)
                possibleCompletions.add(co);
        }

        if(doLogging)
            LOGGER.info("Found completions: "+possibleCompletions);

        if(possibleCompletions.size() == 0) {
            //do nothing
        }
        // only one hit, do a completion
        else if(possibleCompletions.size() == 1 &&
                possibleCompletions.get(0).getCompletionCandidates().size() == 1) {
            //some formatted completions might not be valid and shouldnt be displayed
            displayCompletion(
                    possibleCompletions.get(0).getFormattedCompletionCandidatesTerminalString().get(0),
                    buffer, out,
                    possibleCompletions.get(0).hasAppendSeparator(),
                    possibleCompletions.get(0).getSeparator(), inputProcessor);
        }
        // more than one hit...
        else {

            String startsWith = "";

            if(!possibleCompletions.get(0).isIgnoreStartsWith())
                startsWith = Parser.findStartsWithOperation(possibleCompletions);

            if(startsWith.length() > 0 ) {
                if(startsWith.contains(" ") && !possibleCompletions.get(0).doIgnoreNonEscapedSpace())
                    displayCompletion(new TerminalString(Parser.switchSpacesToEscapedSpacesInWord(startsWith), true),
                            buffer, out,
                            false, possibleCompletions.get(0).getSeparator(), inputProcessor);
                else
                    displayCompletion(new TerminalString(startsWith, true), buffer, out,
                            false, possibleCompletions.get(0).getSeparator(), inputProcessor);
            }
                // display all
                // check size
            else {
                List<TerminalString> completions = new ArrayList<>();
                for(int i=0; i < possibleCompletions.size(); i++)
                    completions.addAll(possibleCompletions.get(i).getCompletionCandidates());

                if(completions.size() > 100) {
                    //if(displayCompletion) {
                     if(askDisplayCompletion) {
                        displayCompletions(completions, buffer, out, inputProcessor);
                        //displayCompletion = false;
                         askDisplayCompletion = false;
                    }
                    else {
                        askDisplayCompletion = true;
                        out.print(Config.getLineSeparator() + "Display all " + completions.size() + " possibilities? (y or n)");
                    }
                }
                // display all
                else {
                    displayCompletions(completions, buffer, out, inputProcessor);
                }
            }
        }
    }
    /**
     * Display the completion string in the terminal.
     * If !completion.startsWith(buffer.getLine()) the completion will be added to the line,
     * else it will replace whats at the buffer line.
     *
     * @param completion partial completion
     * @param appendSpace if its an actual complete
     */
    private void displayCompletion(TerminalString completion, Buffer buffer, PrintStream out,
                                   boolean appendSpace, char separator, InputProcessor inputProcessor) {
        if(completion.getCharacters().startsWith(buffer.getMultiLine())) {
            ActionMapper.mapToAction("backward-kill-word").apply(inputProcessor);
            buffer.write(completion.getCharacters());
            out.print(completion);

            //only append space if its an actual complete, not a partial
        }
        else {
            buffer.write(completion.getCharacters());
            out.print(completion);
        }
        if(appendSpace) { // && fullCompletion.startsWith(buffer.getLine())) {
            buffer.write(separator);
            out.print(separator);
        }

        inputProcessor.getBuffer().drawLine();
    }

    /**
     * Display all possible completions
     *
     * @param completions all completion items
     */
    private void displayCompletions(List<TerminalString> completions, Buffer buffer,
                                    PrintStream out, InputProcessor inputProcessor) {
        Collections.sort(completions);
        //printNewline reset cursor pos, so we need to store it
        int oldCursorPos = buffer.getCursor();
        out.print(Config.getLineSeparator());
        buffer.setCursor(oldCursorPos);
        out.print(Parser.formatDisplayListTerminalString(completions,
                shell.getSize().getHeight(), shell.getSize().getWidth()));
        inputProcessor.getBuffer().displayPrompt();
        out.print(buffer.getLine());
        //if we do a complete and the cursor is not at the end of the
        //buffer we need to move it to the correct place
        out.flush();
        inputProcessor.getBuffer().syncCursor();
    }

    private CompleteOperation findAliases(String buffer, int cursor) {
        if(aliasManager != null) {
            String command = Parser.findFirstWord(buffer);
            Alias alias = aliasManager.getAlias(command);
            if(alias != null) {
                return new CompleteOperation(aeshContext, alias.getValue()+buffer.substring(command.length()),
                        cursor+(alias.getValue().length()-command.length()));
            }
        }

        return new CompleteOperation(aeshContext, buffer, cursor);
    }
}
