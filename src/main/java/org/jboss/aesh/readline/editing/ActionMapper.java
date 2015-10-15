/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.KeyMapper;
import org.jboss.aesh.readline.Keys;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.DeleteChar;
import org.jboss.aesh.readline.actions.DeleteNextChar;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.Enter;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.readline.actions.StartOfLine;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ActionMapper {

    private Map<KeyEvent,Action> mapping;

    public static ActionMapper getEmacs() {

        ActionMapper mapper = new ActionMapper();
        mapper.createEmacsMapping();
        return mapper;
    }

    private Map<KeyEvent, Action> createEmacsMapping() {
        mapping = new HashMap<>();
        KeyMapper mapper = new KeyMapper();

        mapping.put(Keys.CTRL_A, new StartOfLine());
        mapping.put(Keys.CTRL_E, new EndOfLine());
        mapping.put(Keys.CTRL_J, new Enter());
        mapping.put(Keys.CTRL_M, new Enter());
        mapping.put(mapper.getActionEventByName("up"), new PrevHistory());
        mapping.put(Keys.UP_FALLBACK, new PrevHistory());
        mapping.put(mapper.getActionEventByName("down"), new NextHistory());
        mapping.put(Keys.DOWN_FALLBACK, new NextHistory());
        mapping.put(mapper.getActionEventByName("left"), new BackwardChar());
        mapping.put(Keys.LEFT_FALLBACK, new BackwardChar());
        mapping.put(mapper.getActionEventByName("right"), new ForwardChar());
        mapping.put(Keys.RIGHT_FALLBACK, new ForwardChar());
        mapping.put(Keys.BACKSPACE, new DeletePrevChar());
        mapping.put(mapper.getActionEventByName("delete"), new DeleteChar());

        return mapping;
    }

    public Map<KeyEvent, Action> getMapping() {
        return mapping;
    }

    public Action findAction(KeyEvent keyEvent) {
        return mapping.get(keyEvent);
    }

}
