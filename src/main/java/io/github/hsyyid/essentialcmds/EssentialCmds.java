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
package io.github.hsyyid.essentialcmds;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.hsyyid.essentialcmds.cmdexecutors.*;
import io.github.hsyyid.essentialcmds.listeners.*;
import io.github.hsyyid.essentialcmds.utils.*;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.TeleportHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.github.hsyyid.essentialcmds.PluginInfo.*;

@Plugin(id = ID, name = NAME, version = VERSION)
public class EssentialCmds {

	public static Game game;
	public static ConfigurationNode config;
	public static ConfigurationLoader<CommentedConfigurationNode> configurationManager;
	public static TeleportHelper helper;
	public static List<PendingInvitation> pendingInvites = Lists.newArrayList();
	public static List<AFK> movementList = Lists.newArrayList();
	public static List<Player> recentlyJoined = Lists.newArrayList();
	public static List<Powertool> powertools = Lists.newArrayList();
	public static Set<UUID> socialSpies = Sets.newHashSet();
	public static List<Message> recentlyMessaged = Lists.newArrayList();
	public static Set<UUID> muteList = Sets.newHashSet();
	public static Set<UUID> frozenPlayers = Sets.newHashSet();

	@Inject
	private Logger logger;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path dConfig;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> confManager;

	@Listener
	public void onPreInitialization(GamePreInitializationEvent event) {
		getLogger().info(ID + " loading...");

		// Create Config Directory for EssentialCmds
		if (!Files.exists(configDir)) {
			try {
				Files.createDirectories(configDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Listener
	public void onServerInit(GameInitializationEvent event) {
		game = event.getGame();
		helper = game.getTeleportHelper();

		// Config File
		try
		{
			if (!Files.exists(dConfig))
			{
				Files.createFile(dConfig);
				config = confManager.load();
				config.getNode("afk", "timer").setValue(30000);
				config.getNode("afk", "kick", "use").setValue(false);
				config.getNode("afk", "kick", "timer").setValue(30000);
				config.getNode("joinmsg").setValue("&4Welcome!");
				confManager.save(config);
			}

			configurationManager = confManager;
			config = confManager.load();
		}
		catch (IOException exception)
		{
			getLogger().error("The default configuration could not be loaded or created!");
		}
		
		Utils.readMutes();
		Utils.startAFKService();

		CommandSpec homeCommandSpec =
			CommandSpec.builder().description(Texts.of("Home Command")).permission("essentialcmds.home.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new HomeExecutor()).build();
		game.getCommandDispatcher().register(this, homeCommandSpec, "home");
		
		CommandSpec mobSpawnerCommandSpec =
			CommandSpec.builder().description(Texts.of("Mob Spawner Command")).permission("essentialcmds.mobspawner.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("mob name")))).executor(new MobSpawnerExecutor()).build();
		game.getCommandDispatcher().register(this, mobSpawnerCommandSpec, "spawner", "mobspawner");
		
		CommandSpec removeRuleCommandSpec =
			CommandSpec.builder().description(Texts.of("Home Command")).permission("essentialcmds.rules.remove")
				.arguments(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("rule number")))).executor(new RemoveRuleExecutor()).build();
		game.getCommandDispatcher().register(this, removeRuleCommandSpec, "removerule", "delrule", "deleterule");
		
		CommandSpec addRuleCommandSpec =
			CommandSpec.builder().description(Texts.of("Add Rule Command")).permission("essentialcmds.rules.add")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("rule")))).executor(new AddRuleExecutor()).build();
		game.getCommandDispatcher().register(this, addRuleCommandSpec, "addrule");
		
		CommandSpec deleteWorldCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete World Command")).permission("essentialcmds.world.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("name")))).executor(new DeleteWorldExecutor()).build();
		game.getCommandDispatcher().register(this, deleteWorldCommandSpec, "delworld", "deleteworld");

		CommandSpec moreCommandSpec =
			CommandSpec.builder().description(Texts.of("More Command")).permission("essentialcmds.more.use")
				.executor(new MoreExecutor()).build();
		game.getCommandDispatcher().register(this, moreCommandSpec, "more", "stack");
		
		CommandSpec thruCommandSpec =
			CommandSpec.builder().description(Texts.of("Thru Command")).permission("essentialcmds.thru.use")
				.executor(new ThruExecutor()).build();
		game.getCommandDispatcher().register(this, thruCommandSpec, "through", "thru");
		
		CommandSpec directionCommandSpec =
			CommandSpec.builder().description(Texts.of("Direction Command")).permission("essentialcmds.direction.use")
				.executor(new DirectionExecutor()).build();
		game.getCommandDispatcher().register(this, directionCommandSpec, "direction", "compass");
		
		CommandSpec itemInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("ItemInfo Command")).permission("essentialcmds.iteminfo.use")
				.executor(new ItemInfoExecutor()).build();
		game.getCommandDispatcher().register(this, itemInfoCommandSpec, "iteminfo");
		
		CommandSpec blockInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("BlockInfo Command")).permission("essentialcmds.blockinfo.use")
				.executor(new BlockInfoExecutor()).build();
		game.getCommandDispatcher().register(this, blockInfoCommandSpec, "blockinfo");
		
		CommandSpec entityInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("EntityInfo Command")).permission("essentialcmds.entityinfo.use")
				.executor(new EntityInfoExecutor()).build();
		game.getCommandDispatcher().register(this, entityInfoCommandSpec, "entityinfo");
		
		CommandSpec rtpCommandSpec =
			CommandSpec.builder().description(Texts.of("RTP Command")).permission("essentialcmds.rtp.use")
				.executor(new RTPExecutor()).build();
		game.getCommandDispatcher().register(this, rtpCommandSpec, "rtp", "randomtp");

		CommandSpec butcherCommandSpec =
			CommandSpec.builder().description(Texts.of("Butcher Command")).permission("essentialcmds.butcher.use")
				.executor(new ButcherExecutor()).build();
		game.getCommandDispatcher().register(this, butcherCommandSpec, "butcher");
		
		CommandSpec rulesCommandSpec =
			CommandSpec.builder().description(Texts.of("Rules Command")).permission("essentialcmds.rules.use")
				.executor(new RuleExecutor()).build();
		game.getCommandDispatcher().register(this, rulesCommandSpec, "rules");

		CommandSpec vanishCommandSpec =
			CommandSpec.builder().description(Texts.of("Vanish Command")).permission("essentialcmds.vanish.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)))).executor(new VanishExecutor()).build();
		game.getCommandDispatcher().register(this, vanishCommandSpec, "vanish");

		CommandSpec igniteCommandSpec =
			CommandSpec.builder().description(Texts.of("Ignite Command")).permission("essentialcmds.ignite.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("ticks"))),
						GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)))))
				.executor(new IgniteExecutor()).build();
		game.getCommandDispatcher().register(this, igniteCommandSpec, "burn", "ignite", "fire");

		CommandSpec whoIsCommandSpec =
			CommandSpec.builder().description(Texts.of("WhoIs Command")).permission("essentialcmds.whois.use")
				.arguments(
					GenericArguments.firstParsing(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("player name")))))
				.executor(new WhoisExecutor()).build();
		game.getCommandDispatcher().register(this, whoIsCommandSpec, "whois", "realname", "seen");

		CommandSpec playerFreezeCommandSpec =
			CommandSpec.builder().description(Texts.of("Player Freeze Command")).permission("essentialcmds.playerfreeze.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))).executor(new PlayerFreezeExecutor()).build();
		game.getCommandDispatcher().register(this, playerFreezeCommandSpec, "playerfreeze", "freezeplayer");

		CommandSpec skullCommandSpec =
			CommandSpec.builder().description(Texts.of("Skull Command")).permission("essentialcmds.skull.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)))).executor(new SkullExecutor()).build();
		game.getCommandDispatcher().register(this, skullCommandSpec, "skull", "playerskull", "head");

		CommandSpec getPosCommandSpec =
			CommandSpec.builder().description(Texts.of("GetPos Command")).permission("essentialcmds.getpos.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new GetPosExecutor()).build();
		game.getCommandDispatcher().register(this, getPosCommandSpec, "getpos");

		CommandSpec gamemodeCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Gamemode Command"))
				.permission("essentialcmds.gamemode.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("gamemode"))),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.player(Texts.of("player"), game)))))
				.executor(new GamemodeExecutor()).build();
		game.getCommandDispatcher().register(this, gamemodeCommandSpec, "gamemode", "gm");

		CommandSpec motdCommandSpec =
			CommandSpec.builder().description(Texts.of("MOTD Command")).permission("essentialcmds.motd.use").executor(new MotdExecutor()).build();
		game.getCommandDispatcher().register(this, motdCommandSpec, "motd");

		CommandSpec socialSpyCommandSpec =
			CommandSpec.builder().description(Texts.of("Allows Toggling of Seeing Other Players Private Messages")).permission("essentialcmds.socialspy.use")
				.executor(new SocialSpyExecutor()).build();
		game.getCommandDispatcher().register(this, socialSpyCommandSpec, "socialspy");

		CommandSpec mailListCommandSpec =
			CommandSpec.builder().description(Texts.of("List Mail Command")).permission("essentialcmds.mail.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new MailListExecutor()).build();
		game.getCommandDispatcher().register(this, mailListCommandSpec, "listmail");

		CommandSpec mailReadCommandSpec =
			CommandSpec.builder().description(Texts.of("Read Mail Command")).permission("essentialcmds.mail.read")
				.arguments(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("mail no")))).executor(new MailReadExecutor()).build();
		game.getCommandDispatcher().register(this, mailReadCommandSpec, "readmail");

		CommandSpec msgRespondCommandSpec =
			CommandSpec.builder().description(Texts.of("Respond to Message Command")).permission("essentialcmds.message.respond")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message"))))
				.executor(new RespondExecutor()).build();
		game.getCommandDispatcher().register(this, msgRespondCommandSpec, "r");

		CommandSpec timeCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Set Time Command"))
				.permission("essentialcmds.time.set")
				.arguments(
					GenericArguments.firstParsing(
						GenericArguments.string(Texts.of("time")), 
						GenericArguments.integer(Texts.of("ticks"))))
				.executor(new TimeExecutor()).build();
		game.getCommandDispatcher().register(this, timeCommandSpec, "time");

		CommandSpec repairCommandSpec =
			CommandSpec.builder().description(Texts.of("Repair Item in Player's Hand")).permission("essentialcmds.repair.use").executor(new RepairExecutor())
				.build();
		game.getCommandDispatcher().register(this, repairCommandSpec, "repair");

		CommandSpec mailCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mail Command"))
				.permission("essentialcmds.mail.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("player")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message")))).executor(new MailExecutor())
				.build();
		game.getCommandDispatcher().register(this, mailCommandSpec, "mail");

		CommandSpec weatherCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Weather Command"))
				.permission("essentialcmds.weather.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("weather")))),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("duration")))))
				.executor(new WeatherExecutor()).build();
		game.getCommandDispatcher().register(this, weatherCommandSpec, "weather");

		CommandSpec mobSpawnCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mob Spawn Command"))
				.permission("essentialcmds.mobspawn.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("amount")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("mob name"))))
				.executor(new MobSpawnExecutor()).build();
		game.getCommandDispatcher().register(this, mobSpawnCommandSpec, "mobspawn", "entityspawn");

		CommandSpec enchantCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Enchant Command"))
				.permission("essentialcmds.enchant.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("level")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("enchantment"))))
				.executor(new EnchantExecutor()).build();
		game.getCommandDispatcher().register(this, enchantCommandSpec, "enchant", "ench");

		CommandSpec banCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Ban Command"))
				.permission("essentialcmds.ban.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)), GenericArguments
						.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("reason"))))))
				.executor(new BanExecutor()).build();
		game.getCommandDispatcher().register(this, banCommandSpec, "ban");

		CommandSpec pardonCommandSpec =
			CommandSpec.builder().description(Texts.of("Unban Command")).permission("essentialcmds.unban.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("player")))).executor(new PardonExecutor()).build();
		game.getCommandDispatcher().register(this, pardonCommandSpec, "unban", "pardon");

		CommandSpec teleportPosCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Teleport Position Command"))
				.permission("essentialcmds.teleport.pos.use")
				.arguments(
					GenericArguments.seq(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"),
						game)))), GenericArguments.onlyOne(GenericArguments.integer(Texts.of("x"))),
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("y"))),
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("z")))).executor(new TeleportPosExecutor()).build();
		game.getCommandDispatcher().register(this, teleportPosCommandSpec, "tppos", "teleportpos", "teleportposition");
		
		CommandSpec teleportCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Teleport Command"))
				.permission("essentialcmds.teleport.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)),
						GenericArguments.optional(GenericArguments.player(Texts.of("target"), game))))
				.executor(new TeleportExecutor()).build();
		game.getCommandDispatcher().register(this, teleportCommandSpec, "tp", "teleport");

		CommandSpec kickCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Kick Command"))
				.permission("essentialcmds.kick.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("reason")))).executor(new KickExecutor())
				.build();
		game.getCommandDispatcher().register(this, kickCommandSpec, "kick");

		CommandSpec messageCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Message Command"))
				.permission("essentialcmds.message.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("recipient"), game))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message"))))
				.executor(new MessageExecutor()).build();
		game.getCommandDispatcher().register(this, messageCommandSpec, "message", "m", "msg", "tell");

		CommandSpec lightningCommandSpec =
			CommandSpec.builder().description(Texts.of("Lightning Command")).permission("essentialcmds.lightning.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new LightningExecutor()).build();
		game.getCommandDispatcher().register(this, lightningCommandSpec, "thor", "smite", "lightning");

		CommandSpec fireballCommandSpec =
			CommandSpec.builder().description(Texts.of("Fireball Command")).permission("essentialcmds.fireball.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new FireballExecutor()).build();
		game.getCommandDispatcher().register(this, fireballCommandSpec, "fireball", "ghast");
		
		CommandSpec sudoCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Sudo Command"))
				.permission("essentialcmds.sudo.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)),
						GenericArguments.remainingJoinedStrings(Texts.of("command")))).executor(new SudoExecutor()).build();
		game.getCommandDispatcher().register(this, sudoCommandSpec, "sudo");

		CommandSpec createWorldCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Create World Command"))
				.permission("essentialcmds.world.create")
				.arguments(
					GenericArguments.seq(
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("name"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("environment"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("gamemode"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("difficulty")))))
				.executor(new CreateWorldExecutor()).build();
		game.getCommandDispatcher().register(this, createWorldCommandSpec, "createworld");
		
		CommandSpec loadWorldCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Load World Command"))
				.permission("essentialcmds.world.load")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("name"))))
				.executor(new LoadWorldExecutor()).build();
		game.getCommandDispatcher().register(this, loadWorldCommandSpec, "loadworld", "importworld");

		CommandSpec afkCommandSpec =
			CommandSpec.builder().description(Texts.of("AFK Command")).permission("essentialcmds.afk.use").executor(new AFKExecutor()).build();
		game.getCommandDispatcher().register(this, afkCommandSpec, "afk");

		CommandSpec broadcastCommandSpec =
			CommandSpec.builder().description(Texts.of("Broadcast Command")).permission("essentialcmds.broadcast.use")
				.arguments(GenericArguments.remainingJoinedStrings(Texts.of("message"))).executor(new BroadcastExecutor()).build();
		game.getCommandDispatcher().register(this, broadcastCommandSpec, "broadcast");

		CommandSpec spawnCommandSpec =
			CommandSpec.builder().description(Texts.of("Spawn Command")).permission("essentialcmds.spawn.use").executor(new SpawnExecutor()).build();
		game.getCommandDispatcher().register(this, spawnCommandSpec, "spawn");

		CommandSpec setSpawnCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Spawn Command")).permission("essentialcmds.spawn.set").executor(new SetSpawnExecutor()).build();
		game.getCommandDispatcher().register(this, setSpawnCommandSpec, "setspawn");

		CommandSpec tpaCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Command")).permission("essentialcmds.tpa.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))).executor(new TPAExecutor()).build();
		game.getCommandDispatcher().register(this, tpaCommandSpec, "tpa");

		CommandSpec tpaHereCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Here Command")).permission("essentialcmds.tpahere.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))).executor(new TPAHereExecutor())
				.build();
		game.getCommandDispatcher().register(this, tpaHereCommandSpec, "tpahere");

		CommandSpec tpWorldSpec =
			CommandSpec.builder().description(Texts.of("TP World Command")).permission("essentialcmds.tpworld.use")
				.arguments(GenericArguments.seq(GenericArguments.remainingJoinedStrings(Texts.of("name")),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)))))
				.executor(new TeleportWorldExecutor())
				.build();
		game.getCommandDispatcher().register(this, tpWorldSpec, "tpworld");

		CommandSpec tpHereCommandSpec =
			CommandSpec.builder().description(Texts.of("TP Here Command")).permission("essentialcmds.tphere.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))).executor(new TPHereExecutor())
				.build();
		game.getCommandDispatcher().register(this, tpHereCommandSpec, "tphere");

		CommandSpec tpaAcceptCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Accept Command")).permission("essentialcmds.tpa.accept").executor(new TPAAcceptExecutor()).build();
		game.getCommandDispatcher().register(this, tpaAcceptCommandSpec, "tpaccept");

		CommandSpec listHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("List Home Command")).permission("essentialcmds.home.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListHomeExecutor()).build();
		game.getCommandDispatcher().register(this, listHomeCommandSpec, "homes");

		CommandSpec listWorldsCommandSpec =
			CommandSpec.builder().description(Texts.of("List World Command")).permission("essentialcmds.worlds.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListWorldExecutor()).build();
		game.getCommandDispatcher().register(this, listWorldsCommandSpec, "worlds");

		CommandSpec healCommandSpec =
			CommandSpec.builder().description(Texts.of("Heal Command")).permission("essentialcmds.heal.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new HealExecutor()).build();
		game.getCommandDispatcher().register(this, healCommandSpec, "heal");

		CommandSpec backCommandSpec =
			CommandSpec.builder().description(Texts.of("Back Command")).permission("essentialcmds.back.use").executor(new BackExecutor()).build();
		game.getCommandDispatcher().register(this, backCommandSpec, "back");

		CommandSpec tpaDenyCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Deny Command")).permission("essentialcmds.tpadeny.use").executor(new TPADenyExecutor()).build();
		game.getCommandDispatcher().register(this, tpaDenyCommandSpec, "tpadeny");

		CommandSpec hatCommandSpec =
			CommandSpec.builder().description(Texts.of("Hat Command")).permission("essentialcmds.hat.use").executor(new HatExecutor()).build();
		game.getCommandDispatcher().register(this, hatCommandSpec, "hat");

		CommandSpec flyCommandSpec =
			CommandSpec.builder().description(Texts.of("Fly Command")).permission("essentialcmds.fly.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("palyer"), game))))
				.executor(new FlyExecutor()).build();
		game.getCommandDispatcher().register(this, flyCommandSpec, "fly");

		CommandSpec setHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Home Command")).permission("essentialcmds.home.set")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new SetHomeExecutor()).build();
		game.getCommandDispatcher().register(this, setHomeCommandSpec, "sethome");

		CommandSpec deleteHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete Home Command")).permission("essentialcmds.home.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new DeleteHomeExecutor())
				.build();
		game.getCommandDispatcher().register(this, deleteHomeCommandSpec, "deletehome", "delhome");

		CommandSpec warpCommandSpec =
			CommandSpec.builder().description(Texts.of("Warp Command")).permission("essentialcmds.warp.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new WarpExecutor()).build();
		game.getCommandDispatcher().register(this, warpCommandSpec, "warp");

		CommandSpec listWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("List Warps Command")).permission("essentialcmds.warps.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListWarpExecutor()).build();
		game.getCommandDispatcher().register(this, listWarpCommandSpec, "warps");

		CommandSpec setWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Warp Command")).permission("essentialcmds.warp.set")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new SetWarpExecutor()).build();
		game.getCommandDispatcher().register(this, setWarpCommandSpec, "setwarp");

		CommandSpec deleteWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete Warp Command")).permission("essentialcmds.warp.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new DeleteWarpExecutor())
				.build();
		game.getCommandDispatcher().register(this, deleteWarpCommandSpec, "deletewarp", "delwarp");

		CommandSpec feedCommandSpec =
			CommandSpec.builder().description(Texts.of("Feed Command")).permission("essentialcmds.feed.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new FeedExecutor()).build();
		game.getCommandDispatcher().register(this, feedCommandSpec, "feed");

		CommandSpec unmuteCommnadSpec =
			CommandSpec.builder().description(Texts.of("Unmute Command")).permission("essentialcmds.unmute.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))).executor(new UnmuteExecutor())
				.build();
		game.getCommandDispatcher().register(this, unmuteCommnadSpec, "unmute");

		CommandSpec killCommandSpec =
			CommandSpec.builder().description(Texts.of("Kill Command")).permission("essentialcmds.kill.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))))
				.executor(new KillExecutor()).build();
		game.getCommandDispatcher().register(this, killCommandSpec, "kill");

		CommandSpec jumpCommandSpec =
			CommandSpec.builder().description(Texts.of("Jump Command")).permission("essentialcmds.jump.use").executor(new JumpExecutor()).build();
		game.getCommandDispatcher().register(this, jumpCommandSpec, "jump");

		CommandSpec speedCommandSpec =
			CommandSpec.builder().description(Texts.of("Speed Command")).permission("essentialcmds.speed.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("speed"))),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)))))
				.executor(new SpeedExecutor()).build();
		game.getCommandDispatcher().register(this, speedCommandSpec, "speed");

		CommandSpec powertoolCommandSpec =
			CommandSpec.builder().description(Texts.of("Powertool Command")).permission("essentialcmds.powertool.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("command")))))
				.executor(new PowertoolExecutor()).build();
		game.getCommandDispatcher().register(this, powertoolCommandSpec, "powertool");
		
		CommandSpec asConsoleCommandSpec =
			CommandSpec.builder().description(Texts.of("AsConsole Command")).permission("essentialcmds.asconsole.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("command"))))
				.executor(new AsConsoleExecutor()).build();
		game.getCommandDispatcher().register(this, asConsoleCommandSpec, "asConsole", "asconsole");

		CommandSpec nickCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Nick Command"))
				.permission("essentialcmds.nick.use")
				.arguments(
					GenericArguments.seq(
						GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game))),
						GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("nick")))))
				.executor(new NickExecutor()).build();
		game.getCommandDispatcher().register(this, nickCommandSpec, "nick");

		CommandSpec muteCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mute Command"))
				.permission("essentialcmds.mute.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), game)),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.integer(Texts.of("time")))),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.string(Texts.of("time unit"))))))
				.executor(new MuteExecutor()).build();
		game.getCommandDispatcher().register(this, muteCommandSpec, "mute");

		game.getEventManager().registerListeners(this, new SignChangeListener());
		game.getEventManager().registerListeners(this, new PlayerJoinListener());
		game.getEventManager().registerListeners(this, new MessageSinkListener());
		game.getEventManager().registerListeners(this, new PlayerClickListener());
		game.getEventManager().registerListeners(this, new PlayerInteractListener());
		game.getEventManager().registerListeners(this, new PlayerMoveListener());
		game.getEventManager().registerListeners(this, new PlayerDeathListener());
		game.getEventManager().registerListeners(this, new TPAListener());
		game.getEventManager().registerListeners(this, new MailListener());
		game.getEventManager().registerListeners(this, new PlayerDisconnectListener());

		getLogger().info("-----------------------------");
		getLogger().info("EssentialCmds was made by HassanS6000!");
		getLogger().info("Please post all errors on the Sponge Thread or on GitHub!");
		getLogger().info("Have fun, and enjoy! :D");
		getLogger().info("-----------------------------");
		getLogger().info("EssentialCmds loaded!");
	}

	public static ConfigurationLoader<CommentedConfigurationNode> getConfigManager()
	{
		return configurationManager;
	}

	public Path getConfigDir() {
		return configDir;
	}

	public Logger getLogger() {
		return logger;
	}

}