/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sculk;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.sculkpowered.server.Server;
import io.github.sculkpowered.server.command.CommandSource;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import java.util.List;

public class SculkCommandExecutor extends CommandManager {

    private static final String PRIMARY_ALIAS = "lp";

    private final LPSculkPlugin plugin;

    public SculkCommandExecutor(LPSculkPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        Server server = this.plugin.getBootstrap().server();
        server.commandHandler().register(LiteralArgumentBuilder.<CommandSource>literal(PRIMARY_ALIAS)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arguments", StringArgumentType.greedyString())
                        .executes(context -> command(context, context.getArgument("arguments", String.class)))
                        .suggests((context, builder) -> {
                            var input = context.getInput();
                            input = input.substring(input.indexOf(CommandDispatcher.ARGUMENT_SEPARATOR_CHAR) + 1);
                            final Sender wrapped = this.plugin.getSenderFactory().wrap(context.getSource());
                            final List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(input);
                            for (final String argument : tabCompleteCommand(wrapped, arguments)) {
                                builder.suggest(argument);
                            }
                            return builder.buildFuture();
                        })
                        .build())
                .executes(context -> command(context, ""))
                .build());
    }

    private int command(CommandContext<CommandSource> context, String arguments) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(context.getSource());
        executeCommand(wrapped, PRIMARY_ALIAS, ArgumentTokenizer.EXECUTE.tokenizeInput(arguments));
        return Command.SINGLE_SUCCESS;
    }
}
