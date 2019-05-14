package pw.cinque.knockbackpatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DamageListener implements Listener {

	private Field fieldPlayerConnection;
	private Method sendPacket;
	private Constructor<?> packetVelocity;
	private static final List<Integer> BLACKLISTED_IDS = Arrays.asList(11834, 11835, 11836, 11837, 11838);

	public DamageListener() {
		try {
			Class<?> entityPlayerClass = Class.forName("net.minecraft.entity.player.EntityPlayerMP");
			Class<?> packetVelocityClass = Class.forName("net.minecraft.network.packet.Packet28EntityVelocity");
			Class<?> playerConnectionClass = Class.forName("net.minecraft.network.NetServerHandler");

			// Get the fields here to improve performance later on			
			this.fieldPlayerConnection = entityPlayerClass.getField("field_71135_a");
			this.sendPacket = playerConnectionClass.getMethod("func_72567_b", packetVelocityClass.getSuperclass());
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

		ItemStack itemStack = ((Player) event.getDamager()).getItemInHand();

		if(itemStack != null && BLACKLISTED_IDS.contains(itemStack.getTypeId()))
			return;

		Player damaged = (Player) event.getEntity();
		Player damager = (Player) event.getDamager();
		
		if (damaged.getNoDamageTicks() > damaged.getMaximumNoDamageTicks() / 2D) {
			return;
		}

		double horMultiplier = KnockbackPatch.getInstance().getHorMultiplier();
		double verMultiplier = KnockbackPatch.getInstance().getVerMultiplier();
		double sprintMultiplier = damager.isSprinting() ? 0.8D : 0.5D;
		double kbMultiplier = damager.getItemInHand() == null ? 0 : damager.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK) * 0.2D;
		@SuppressWarnings("deprecation")
		double airMultiplier = damaged.isOnGround() ? 1 : 0.5;

		Vector knockback = damaged.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
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
