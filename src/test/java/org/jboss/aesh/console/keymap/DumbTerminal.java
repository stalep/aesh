package org.jboss.aesh.console.keymap;

/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;

import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Attributes.ControlChar;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.impl.AbstractTerminal;

public class DumbTerminal extends AbstractTerminal {
    private final InputStream input;
    private final OutputStream output;
    private final Reader reader;
    private final PrintWriter writer;
    private final Attributes attributes;
    private Size size;

    public DumbTerminal(InputStream in, OutputStream out) throws IOException {
        super("dumb", "ansi");
        this.input = in;
        this.output = out;
        this.reader = new InputStreamReader(in);
        this.writer = new PrintWriter(new OutputStreamWriter(out));
        this.attributes = new Attributes();
        this.attributes.setControlChar(ControlChar.VERASE,  (char) 127);
        this.attributes.setControlChar(ControlChar.VWERASE, (char) 23);
        this.attributes.setControlChar(ControlChar.VKILL,   (char) 21);
        this.attributes.setControlChar(ControlChar.VLNEXT,  (char) 22);
        this.size = new Size(160, 50);
        parseInfoCmp();
    }

    public Reader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    public Attributes getAttributes() {
        Attributes attr = new Attributes();
        attr.copy(attributes);
        return attr;
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
    }

    public void setAttributes(Attributes attr, int actions) {
        setAttributes(attr);
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size sz) {
        size = new Size(sz.getWidth(),sz.getHeight());
    }

    public void close() throws IOException {
    }
}
