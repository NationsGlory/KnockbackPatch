package pw.cinque.knockbackpatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class DamageListener implements Listener {

	private Field fieldPlayerConnection;
	private Method sendPacket;
	private Constructor<?> packetVelocity;

	public DamageListener() {
		try {			
			Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + KnockbackPatch.getInstance().getCraftBukkitVersion() + ".EntityPlayer");
			Class<?> packetVelocityClass = Class.forName("net.minecraft.server." + KnockbackPatch.getInstance().getCraftBukkitVersion() + ".PacketPlayOutEntityVelocity");
			Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + KnockbackPatch.getInstance().getCraftBukkitVersion() + ".PlayerConnection");

			// Get the fields here to improve performance later on			
			this.fieldPlayerConnection = entityPlayerClass.getField("playerConnection");
			this.sendPacket = playerConnectionClass.getMethod("sendPacket", packetVelocityClass.getSuperclass());
			this.packetVelocity = packetVelocityClass.getConstructor(int.class, double.class, double.class, double.class);
		} catch (ClassNotFoundException | NoSuchFieldException | SecurityException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onPlayerVelocity(PlayerVelocityEvent event) {
		Player player = event.getPlayer();
		EntityDamageEvent lastDamage = player.getLastDamageCause();

		if (lastDamage == null || !(lastDamage instanceof EntityDamageByEntityEvent)) {
			return;
		}

		// Cancel the vanilla knockback
		if (((EntityDamageByEntityEvent) lastDamage).getDamager() instanceof Player) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
			return;
		}

		if (event.isCancelled()) {
			return;
		}

		Player damaged = (Player) event.getEntity();
		Player damager = (Player) event.getDamager();

		double horMultiplier = KnockbackPatch.getInstance().getHorMultiplier();
		double verMultiplier = KnockbackPatch.getInstance().getVerMultiplier();
		double sprintMultiplier = damager.isSprinting() ? 0.8D : 0.4D;
		double kbMultiplier = damager.getItemInHand() == null ? 0 : damager.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK) * 0.2D;
		@SuppressWarnings("deprecation")
		double airMultiplier = damager.isOnGround() ? 1 : 0.5;

		Vector knockback = damager.getLocation().getDirection().normalize();
		knockback.setX((knockback.getX() * sprintMultiplier + kbMultiplier) * horMultiplier);
		knockback.setY(0.35D * airMultiplier * verMultiplier);
		knockback.setZ((knockback.getZ() * sprintMultiplier + kbMultiplier) * horMultiplier);
		
		try {
			// Send the velocity packet immediately instead of using setVelocity, which fixes the 'relog bug'
			Object entityPlayer = damaged.getClass().getMethod("getHandle").invoke(damaged);
			Object playerConnection = fieldPlayerConnection.get(entityPlayer);
			Object packet = packetVelocity.newInstance(damaged.getEntityId(), knockback.getX(), knockback.getY(), knockback.getZ());
			sendPacket.invoke(playerConnection, packet);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
			e.printStackTrace();
		}
	}
}
