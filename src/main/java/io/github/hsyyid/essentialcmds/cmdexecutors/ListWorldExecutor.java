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

import io.github.hsyyid.essentialcmds.utils.PaginatedList;

import io.github.hsyyid.essentialcmds.EssentialCmds;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Optional;

public class ListWorldExecutor implements CommandExecutor
{
	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
	{
		ArrayList<String> worlds = new ArrayList<>();

		for (World world : EssentialCmds.game.getServer().getWorlds())
		{
			if (world.getProperties().isEnabled())
				worlds.add(world.getName());
		}

		Optional<Integer> optionalPageNo = ctx.<Integer> getOne("page no");
		int pageNo;

		if (optionalPageNo.isPresent())
		{
			pageNo = optionalPageNo.get();
		}
		else
		{
			pageNo = 1;
		}

		PaginatedList pList = new PaginatedList("/worlds");

		for (String name : worlds)
		{
			Text item = Texts.builder(name)
				.onClick(TextActions.runCommand("/tpworld " + name))
				.onHover(TextActions.showText(Texts.of(TextColors.WHITE, "Teleport to world ", TextColors.GOLD, name)))
				.color(TextColors.DARK_AQUA)
				.style(TextStyles.UNDERLINE)
				.build();

			pList.add(item);
		}

		pList.setItemsPerPage(10);

		TextBuilder header = Texts.builder();
		header.append(Texts.of(TextColors.GREEN, "------------"));
		header.append(Texts.of(TextColors.GREEN, " Showing Worlds page " + pageNo + " of " + pList.getTotalPages() + " "));
		header.append(Texts.of(TextColors.GREEN, "------------"));

		pList.setHeader(header.build());

		src.sendMessage(pList.getPage(pageNo));

		return CommandResult.success();
	}
}