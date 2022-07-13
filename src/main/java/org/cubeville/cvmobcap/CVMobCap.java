package org.cubeville.cvmobcap;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CVMobCap extends JavaPlugin implements Listener
{
    static final int localMobcapRadius = 128;
    static final int localMobcapCount = 160;
    static final int localHostileMobcapCount = 130;

    static final EntityType[] hostileMobs = { EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.EVOKER_FANGS, EntityType.GHAST, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SHULKER, EntityType.SKELETON, EntityType.SLIME, EntityType.STRAY, EntityType.SPIDER, EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN };

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if(command.getName().equals("mobcap")) {
            Player player;
            if(args.length > 1) {
                sender.sendMessage("§c/mobcap [player]");
                return true;
            }
            if(args.length == 0) { player = (Player) sender; }
            else {
                String playerName = args[0];
                player = Bukkit.getServer().getPlayer(playerName);
                if(player == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            Location loc = player.getLocation();
            Map<EntityType, Integer> cnt = new HashMap<>();
            int total = 0;
            for(Entity e: player.getLocation().getWorld().getEntitiesByClass(LivingEntity.class)) {
                if(e.getType() == EntityType.ARMOR_STAND || e.getType() == EntityType.PLAYER || e.getType() == EntityType.MARKER) continue;
                if(e.getLocation().distance(player.getLocation()) < localMobcapRadius) {
                    if(cnt.containsKey(e.getType())) {
                        cnt.put(e.getType(), cnt.get(e.getType()) + 1);
                    }
                    else {
                        cnt.put(e.getType(), 1);
                    }
                    total++;
                }
            }
            sender.sendMessage("§eMobcap statistics for location of " + player.getName() + ": ");
            sender.sendMessage("§eTotal: " + total + " (of max " + localMobcapCount + ")");
            for(EntityType et: cnt.keySet()) {
                sender.sendMessage((isMobHostile(et) ? "§c" : "§a") + et.toString() + ": " + cnt.get(et));
            }
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if(event.isCancelled()) return;
        
        LivingEntity le = event.getEntity();
        
        if(le.getType() == EntityType.PLAYER || le.getType() == EntityType.ARMOR_STAND || le.getType() == EntityType.MARKER) return;
        
        // if(event.getEntity() instanceof Squid) {
        //     int squidCount = 0;
        //     for(Squid s: event.getEntity().getLocation().getWorld().getEntitiesByClass(Squid.class)) {
        //         if(s.getLocation().distance(le.getLocation()) < localMobcapRadius) squidCount++;
        //     }
        //     if(squidCount > 12) {
        //         event.setCancelled(true);
        //         return;
        //     }
        // }
        
        // if(event.getEntity() instanceof Dolphin) {
        //     int dolphinCount = 0;
        //     for(Dolphin s: event.getEntity().getLocation().getWorld().getEntitiesByClass(Dolphin.class)) {
        //         if(s.getLocation().distance(le.getLocation()) < localMobcapRadius) dolphinCount++;
        //     }
        //     if(dolphinCount > 12) {
        //         event.setCancelled(true);
        //         return;
        //     }
        // }
        
        // if(event.getEntity() instanceof Cod) {
        //     int codCount = 0;
        //     for(Cod s: event.getEntity().getLocation().getWorld().getEntitiesByClass(Cod.class)) {
        //         if(s.getLocation().distance(le.getLocation()) < localMobcapRadius) codCount++;
        //     }
        //     if(codCount > 12) {
        //         event.setCancelled(true);
        //         return;
        //     }
        // }
        String status;
        int cnt = 0;
        for(Entity e: event.getEntity().getLocation().getWorld().getEntitiesByClass(LivingEntity.class)) {
            if(e.getType() != EntityType.PLAYER &&
                    e.getType() != EntityType.ARMOR_STAND &&
                    e.getType() != EntityType.MARKER &&
                    e.getLocation().distance(le.getLocation()) < localMobcapRadius) {
                cnt++;
            }
        }
        if(cnt >= localMobcapCount || (cnt >= localHostileMobcapCount && isMobHostile(le.getType()))) {
            event.setCancelled(true);
            status = "TRUE";
        } else {
            status = "FALSE";
        }
        Bukkit.getConsoleSender().sendMessage("[CVMobCap] Spawning Living Entity: §6" + le.getType() +
                " §rCause: §6" + event.getSpawnReason() +
                " §rCancelled: §6" + status);
    }

    @EventHandler
    public void onAxolotlSpawnAttempt(PlayerBucketEmptyEvent event) {
        if(event.isCancelled()) return;
        if(!event.getBucket().equals(Material.AXOLOTL_BUCKET) &&
                !event.getBucket().equals(Material.PUFFERFISH_BUCKET) &&
                !event.getBucket().equals(Material.SALMON_BUCKET) &&
                !event.getBucket().equals(Material.COD_BUCKET) &&
                !event.getBucket().equals(Material.TROPICAL_FISH_BUCKET)) return;
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        int initial = countBucketEntities(location);
        this.getServer().getScheduler().runTaskLater(this, () -> {
            if(initial >= countBucketEntities(location)) {
                PlayerInventory inv = player.getInventory();
                inv.remove(Objects.requireNonNull(event.getItemStack()));
                inv.addItem(new ItemStack(event.getBucket()));
                if(location.getBlock().getType().equals(Material.WATER)) {
                    location.getBlock().setType(Material.AIR);
                }
            }
        }, 1);
    }

    private Integer countBucketEntities(Location loc) {
        int i = 0;
        for(Entity entity : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
            if(entity instanceof Axolotl ||
                    entity instanceof PufferFish ||
                    entity instanceof Salmon ||
                    entity instanceof  Cod ||
                    entity instanceof TropicalFish) {
                i++;
            }
        }
        return i;
    }

    private boolean isMobHostile(EntityType type) {
        for(EntityType t: hostileMobs) {
            if(type == t) return true;
        }
        return false;
    }

}
