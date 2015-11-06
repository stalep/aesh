/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "testparams", description = "")
public class TestParams implements Command {

    @Option(shortName = 'f', description = "set the foo value")
    private String foo;

    @Option(shortName = 'b', required = true)
    private String bar;


    public static void main(String[] args) {
        TestParams test = new TestParams();

        try {
            ParserGenerator.parseAndPopulate(test, "testparams", args);

            System.out.println("you set foo to: " + test.foo);
            System.out.println("you set bar to: "+test.bar);
        }
        catch(CommandLineParserException e) {
            System.out.println("Error when parsing: "+e.toString());
        }
        catch(OptionValidatorException e) {
            System.out.println("Validation failed: "+e.toString());
        }

    }

    @Override
    //never called
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        return null;
    }
}
