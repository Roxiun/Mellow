package com.roxiun.mellow.platform;
import java.util.*;
import net.minecraft.command.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
public final class ClientCommands {
 private static final Map<String,ICommand> COMMANDS=new LinkedHashMap<>();
 public static void register(ICommand command) {
  COMMANDS.put(command.getCommandName(),command);
  for(String alias:command.getCommandAliases()) COMMANDS.putIfAbsent(alias,command);
 }
 public static boolean owns(String input) {
  return input.startsWith("/") && COMMANDS.containsKey(input.substring(1).split("\\s+",2)[0]);
 }
 public static boolean execute(String input) {
  if(!input.startsWith("/")) return false;
  String[] parts=input.substring(1).trim().split("\\s+");
  ICommand command=COMMANDS.get(parts[0]);
  if(command==null) return false;
  var player=Minecraft.getMinecraft().thePlayer;
  try { command.processCommand(player,Arrays.copyOfRange(parts,1,parts.length)); }
  catch(CommandException e) {player.addChatMessage(new ChatComponentText("§c"+e.getMessage()));}
  return true;
 }
 public static List<String> completeCommandName(String input) {
  if (!input.startsWith("/") || input.chars().anyMatch(Character::isWhitespace)) return List.of();
  String prefix = input.substring(1);
  return COMMANDS.keySet().stream().filter(name -> name.startsWith(prefix)).map(name -> "/" + name).toList();
 }
 public static List<String> complete(String input) {
  String[] parts=input.replaceFirst("^/", "").split("\\s+",-1);
  if(parts.length==1) return completeCommandName(input);
  ICommand command=COMMANDS.get(parts[0]);
  if(command==null) return List.of();
  var player=Minecraft.getMinecraft().thePlayer;
  List<String> out=command.addTabCompletionOptions(player,Arrays.copyOfRange(parts,1,parts.length),player.getPosition());
  return out==null?List.of():out;
 }
}
