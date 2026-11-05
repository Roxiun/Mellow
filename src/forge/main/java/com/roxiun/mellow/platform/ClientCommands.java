package com.roxiun.mellow.platform;

import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;

public final class ClientCommands {
    private ClientCommands() {}

    public static void register(ICommand command) {
        ClientCommandHandler.instance.registerCommand(command);
    }
}
