package de.selebrator.customlogging;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CustomLoggingPlugin extends JavaPlugin {

	private boolean papi;

	@Override
	public void onLoad() {
		this.saveDefaultConfig();
	}

	@Override
	public void onEnable() {
		Bukkit.getPluginCommand("customlog").setExecutor(this::onLogCommand);
		Bukkit.getPluginCommand("customloggingreload").setExecutor(this::onReloadCommand);
		this.papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

	}

	private boolean onReloadCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("customlogging.reload")) {
			sender.sendMessage(message(sender, "message.command.reload.no_permission"));
			return true;
		}
		this.reloadConfig();
		sender.sendMessage(message(sender, "message.command.reload.success"));
		return true;
	}

	private boolean onLogCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("customlogging.writelog")) {
			sender.sendMessage(message(sender, "message.command.log.no_permission"));
			return true;
		}
		if(args.length == 0) {
			return false;
		}

		final String loggerId = args[0];
		if(!sender.hasPermission("customlogging.writelog." + loggerId) && !sender.hasPermission("customlogging.writelog.*")) {
			sender.sendMessage(message(sender, "message.command.log.no_permission_loggerId"));
			return true;
		}
		final String message = Arrays.stream(args)
				.skip(1)
				.collect(Collectors.joining(" "));
		final ConfigurationSection logger = this.getConfig().getConfigurationSection("loggers." + loggerId);
		if(logger == null) {
			sender.sendMessage(message(sender, "message.command.log.unknown_loggerId")
					.replace("{loggerId}", loggerId));
			return true;
		}
		final String fileName = this.setPapiPlaceholders(sender, logger.getString("file", loggerId));
		final String format = logger.getString("format", "{message}");

		final Path file = Paths.get(fileName);
		final String formatted = format
				.replace("{message}", message)
				.replace("\\n", System.lineSeparator());
		final String log = this.setPapiPlaceholders(sender, formatted);
		try {
			if(!Files.exists(file)) {
				Path dir = file.getParent();
				if(dir != null && !Files.exists(dir)) {
					Files.createDirectories(file.getParent());
				}
				Files.createFile(file);
			}
			Files.write(file, log.concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			sender.sendMessage(message(sender, "message.command.log.success")
					.replace("{loggerId}", loggerId)
					.replace("{file}", file.toString())
					.replace("{log}", log));
			return true;
		} catch(IOException e) {
			e.printStackTrace();
			sender.sendMessage(message(sender, "message.command.log.error"));
			return true;
		}
	}

	private String message(CommandSender sender, String messagePath) {
		return this.setPapiPlaceholders(sender, ChatColor.translateAlternateColorCodes('&', this.getConfig().getString(messagePath)));
	}

	private String setPapiPlaceholders(CommandSender sender, String message) {
		if(!this.papi) {
			return message;
		}
		OfflinePlayer player = sender instanceof Player ? (Player) sender : null;
		return PlaceholderAPI.setPlaceholders(player, message);
	}
}
