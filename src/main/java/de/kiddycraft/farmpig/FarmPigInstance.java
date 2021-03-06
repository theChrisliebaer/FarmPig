package de.kiddycraft.farmpig;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Descriptes an active FarmPig that has been configure.
 */
public final class FarmPigInstance implements Listener {
	
	private FarmPigPlugin plugin;
	@Getter private String nameTag;
	@Getter private Location location;
	@Getter private EntityType type;
	
	@Getter private long respawnTicks;
	private transient LivingEntity entity;
	
	private transient BukkitTask respawnTask;
	
	FarmPigInstance(FarmPigPlugin plugin, String name, @NonNull Location location, LivingEntity entity, @NonNull EntityType type, long respawnTicks) {
		this.plugin = plugin;
		Preconditions.checkArgument(type.isAlive(), "entity is not alive");
		Preconditions.checkArgument(type.isSpawnable(), "entity is not spawnable by regular means");
		Preconditions.checkArgument(respawnTicks >= 0, "respawn delay must be >= 0");
		this.nameTag = name;
		this.location = location;
		this.entity = entity;
		this.type = type;
		this.respawnTicks = respawnTicks;
	}
	
	/**
	 * Remove the current instance of this farmpig and spawns a new one.
	 */
	public void respawn() {
		despawn();
		spawn();
	}
	
	/**
	 * Remove this instance from the game world but does not remove it's entry.
	 */
	public void despawn() {
		if (entity != null) {
			entity.remove();
			entity = null;
		}
		
		// cancel task to prevent spawn if already scheduled
		if (respawnTask != null)
			respawnTask.cancel();
	}
	
	public void spawn() {
		if (entity == null) {
			// cast is safe since only living entities are allowed as type
			entity = (LivingEntity) location.getWorld().spawnEntity(location, type);

			plugin.getTagManipulation().updateEntity(entity);
			
			if (nameTag != null) {
				entity.setCustomName(nameTag);
				entity.setCustomNameVisible(true);
			}
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent ev) {
		if (ev.getEntity() == entity) {
			respawnTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::respawn, respawnTicks);
		}
	}
	
	@EventHandler
	public void onChunkDespawn(ChunkUnloadEvent ev) {
		if (location.getChunk() == ev.getChunk()) {
			despawn();
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent ev) {
		if (location.getChunk() == ev.getChunk()) {
			respawn();
		}
	}
	
}
