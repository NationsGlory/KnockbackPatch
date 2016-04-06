package pw.cinque.knockbackpatch;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class KnockbackPatch extends JavaPlugin {

	private static KnockbackPatch instance;
	
	private String craftBukkitVersion;
	private double horMultiplier = 1D;
	private double verMultiplier = 1D;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getConfig().options().copyDefaults(true);
		getConfig().addDefault("knockback-multiplier.horizontal", 1D);
		getConfig().addDefault("knockback-multiplier.vertical", 1D);
		saveConfig();
		
		// CraftBukkit version is used to access NMS classes using reflection
		this.craftBukkitVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		this.horMultiplier = getConfig().getDouble("knockback-multiplier.horizontal");
		this.verMultiplier = getConfig().getDouble("knockback-multiplier.vertical");
		
		Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("knockbackpatch.setknockback")) {
			sender.sendMessage(ChatColor.RED + "No permission.");
			return true;
		}
		
		if (args.length < 2){
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <horizontal multiplier> <vertical multiplier>.");
			return true;
		}
		
		double horMultiplier = NumberUtils.toDouble(args[0], -1D);
		double verMultiplier = NumberUtils.toDouble(args[1], -1D);
		
		if (horMultiplier < 0D || verMultiplier < 0D) {
			sender.sendMessage(ChatColor.RED + "Invalid horizontal/vertical multiplier!");
			return true;
		}
		
		this.horMultiplier = horMultiplier;
		this.verMultiplier = verMultiplier;
		
		getConfig().set("knockback-multiplier.horizontal", horMultiplier);
		getConfig().set("knockback-multiplier.vertical", verMultiplier);
		saveConfig();
		
		sender.sendMessage(ChatColor.GREEN + "Successfully updated the knockback multipliers!");
		return true;
	}
	
	public static KnockbackPatch getInstance() {
		return instance;
	}

	public String getCraftBukkitVersion() {
		return craftBukkitVersion;
	}
	
	public double getHorMultiplier() {
		return horMultiplier;
	}

	public void setHorMultiplier(double horMultiplier) {
		this.horMultiplier = horMultiplier;
	}

	public double getVerMultiplier() {
		return verMultiplier;
	}

	public void setVerMultiplier(double verMultiplier) {
		this.verMultiplier = verMultiplier;
	}
	
}
