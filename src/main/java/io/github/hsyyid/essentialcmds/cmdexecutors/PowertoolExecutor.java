/*
 * This file is part of EssentialCmds, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015 HassanS6000
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.hsyyid.essentialcmds.cmdexecutors;

import io.github.hsyyid.essentialcmds.utils.Powertool;

import io.github.hsyyid.essentialcmds.EssentialCmds;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.source.CommandBlockSource;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.util.command.spec.CommandExecutor;

import java.util.Optional;

public class PowertoolExecutor implements CommandExecutor
{
	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
	{
		if (src instanceof Player)
		{
			Optional<String> optionalCommand = ctx.<String> getOne("command");
			Player player = (Player) src;
			if (player.getItemInHand().isPresent())
			{
				if (optionalCommand.isPresent())
				{
					String command = optionalCommand.get();
					Powertool replacePowertool = null;
					for (Powertool powertool : EssentialCmds.powertools)
					{
						if (powertool.getItemID().equals(player.getItemInHand().get().getItem().getName()) && powertool.getPlayer().equals(player))
						{
							replacePowertool = powertool;
							break;
						}
					}

					if (replacePowertool == null)
					{
						Powertool powertool = new Powertool(player, player.getItemInHand().get().getItem().getName(), command);
						EssentialCmds.powertools.add(powertool);
					}
					else
					{
						EssentialCmds.powertools.remove(replacePowertool);
						Powertool powertool = new Powertool(player, player.getItemInHand().get().getItem().getName(), command);
						EssentialCmds.powertools.add(powertool);
					}

					player.sendMessage(Texts.of(TextColors.GREEN, "Success! ", TextColors.YELLOW, "Successfully bound command ", TextColors.BLUE, command, TextColors.YELLOW, " to ", TextColors.RED, player.getItemInHand().get().getItem().getName(), TextColors.YELLOW, "!"));
				}
				else
				{
					Powertool powertoolToRemove = null;
					for (Powertool powertool : EssentialCmds.powertools)
					{
						if (powertool.getPlayer().equals(player) && powertool.getItemID().equals(player.getItemInHand().get().getItem().getName()))
						{
							powertoolToRemove = powertool;
							break;
						}
					}

					if (powertoolToRemove != null)
					{
						EssentialCmds.powertools.remove(powertoolToRemove);
						player.sendMessage(Texts.of(TextColors.GREEN, "Success! ", TextColors.YELLOW, "Removed command from this powertool!"));
					}
					else
					{
						player.sendMessage(Texts.of(TextColors.DARK_RED, "Error! ", TextColors.RED, "There is no command assigned to this!"));
					}
				}
			}
			else
			{
				player.sendMessage(Texts.of(TextColors.DARK_RED, "Error! ", TextColors.RED, "You must be holding something to use /powertool!"));
			}

		}
		else if (src instanceof ConsoleSource)
		{
			src.sendMessage(Texts.of(TextColors.DARK_RED, "Error! ", TextColors.RED, "Must be an in-game player to use /powertool!"));
		}
		else if (src instanceof CommandBlockSource)
		{
			src.sendMessage(Texts.of(TextColors.DARK_RED, "Error! ", TextColors.RED, "Must be an in-game player to use /powertool!"));
		}
		return CommandResult.success();
	}
}