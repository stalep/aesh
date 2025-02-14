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
package org.aesh.command;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@SuppressWarnings("unchecked")
public class AeshConsoleTest {


    @Test
    public void testAeshConsole() throws IOException, InterruptedException, CommandLineParserException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        ProcessedCommand<FooTestCommand,CommandInvocation> fooCommand =
                ProcessedCommandBuilder.<FooTestCommand, CommandInvocation>builder()
                .name("foo")
                .description("fooing")
                .command(FooTestCommand.class)
                .addOption(ProcessedOptionBuilder.builder()
                        .name("bar")
                        .addDefaultValue("en")
                        .addDefaultValue("to")
                        .type(String.class)
                        .fieldName("bar")
                        .build())
                .create();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(CommandLineParserBuilder.<FooTestCommand,CommandInvocation>builder().processedCommand(fooCommand).create())
                .command(LsCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings = SettingsBuilder.builder()
                .logging(true)
                .commandRegistry(registry)
                .validatorInvocationProvider(new DirectoryValidatorInvocationProvider())
                .connection(connection)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo"+ Config.getLineSeparator());
        connection.read();

        connection.read("ls --files /home:/tmp"+Config.getLineSeparator());

        Thread.sleep(100);
        console.stop();
    }

    public static class FooTestCommand implements Command<CommandInvocation> {

        private String bar;

        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertEquals("en", bar);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...")
    public class LsCommand implements Command {

        @Option(hasValue = false, description = "set foo to true/false")
        private Boolean foo;

        @Option(hasValue = false, description = "set the bar")
        private boolean bar;

        @Option(defaultValue = {"MORE"}, argument = "SIZE")
        private String less;

        @OptionList(defaultValue = "/tmp", description = "file location", valueSeparator = ':',
                validator = DirectoryValidator.class)
        List<File> files;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Arguments
        private java.util.List<File> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertEquals(2, files.size());
            return CommandResult.SUCCESS;
        }
    }

    public class DirectoryValidator implements OptionValidator<DirectoryValidatorInvocation> {
        @Override
        public void validate(DirectoryValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(!validatorInvocation.getValue().isDirectory()) {
                throw new OptionValidatorException("File validation failed, must be a directory.");
            }
        }
    }

    public static class DirectoryValidatorInvocation implements ValidatorInvocation<File, Command> {

        private final File value;
        private final Command command;
        private final AeshContext context;

        DirectoryValidatorInvocation(File value, Command command, AeshContext context) {
            this.value = value;
            this.command = command;
            this.context = context;
        }

        @Override
        public File getValue() {
            return value;
        }

        @Override
        public Command getCommand() {
            return command;
        }

        @Override
        public AeshContext getAeshContext() {
            return context;
        }
    }

    public static class DirectoryValidatorInvocationProvider implements ValidatorInvocationProvider<ValidatorInvocation<File, Command>> {

        @Override
        public ValidatorInvocation<File, Command> enhanceValidatorInvocation(ValidatorInvocation validatorInvocation) {
            if(validatorInvocation.getValue() instanceof File)
                return new DirectoryValidatorInvocation( (File) validatorInvocation.getValue(),
                        (Command) validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
            else
                return validatorInvocation;
        }
    }

}
