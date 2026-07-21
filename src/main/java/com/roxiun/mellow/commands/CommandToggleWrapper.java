package com.roxiun.mellow.commands;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class CommandToggleWrapper implements ICommand {

    private final ICommand delegate;

    private CommandToggleWrapper(ICommand delegate) {
        this.delegate = delegate;
    }

    public static ICommand wrap(ICommand command) {
        if (command instanceof CommandToggleWrapper) {
            return command;
        }
        return new CommandToggleWrapper(command);
    }

    @Override
    public String getCommandName() {
        return delegate.getCommandName();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return delegate.getCommandUsage(sender);
    }

    @Override
    public List<String> getCommandAliases() {
        return delegate.getCommandAliases();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
        throws CommandException {
        if (!Mellow.isEnabled()) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cMellow is disabled. Enable it in the config to use commands."
            );
            return;
        }
        delegate.processCommand(sender, args);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return Mellow.isEnabled() && delegate.canCommandSenderUseCommand(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (!Mellow.isEnabled()) {
            return Collections.emptyList();
        }
        return delegate.addTabCompletionOptions(sender, args, pos);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return delegate.isUsernameIndex(args, index);
    }

    @Override
    public int compareTo(ICommand other) {
        return delegate.compareTo(other);
    }
}
