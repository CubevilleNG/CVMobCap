package org.cubeville.cvmobcap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CVMobCap extends JavaPlugin implements Listener
{
    static int localMobcapRadius;
    static int localMobcapMax;
    static int localMobcapHostileMax;

    private Logger logger;

    static final EntityType[] hostileMobs = { EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.EVOKER_FANGS, EntityType.GHAST, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SHULKER, EntityType.SKELETON, EntityType.SLIME, EntityType.STRAY, EntityType.SPIDER, EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN };

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        this.logger = this.getLogger();

        final File dataDir = getDataFolder();
        if(!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File configFile = new File(dataDir, "config.yml");
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
                final InputStream inputStream = this.getResource(configFile.getName());
                final FileOutputStream fileOutputStream = new FileOutputStream(configFile);
                final byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch(IOException e) {
                logger.log(Level.WARNING, ChatColor.LIGHT_PURPLE + "Unable to generate config file", e);
                throw new RuntimeException(ChatColor.LIGHT_PURPLE + "Unable to generate config file", e);
            }
        }

        localMobcapRadius = getConfig().getInt("localmobcapradius", 128);
        localMobcapMax = getConfig().getInt("localmobcapmax", 160);
        localMobcapHostileMax = getConfig().getInt("localmobcaphostilemax", 130);
        logger.log(Level.INFO, "Local Mobcap Radius set to " + localMobcapRadius);
        logger.log(Level.INFO, "Local Mobcap Max set to " + localMobcapMax);
        logger.log(Level.INFO, "Local Mobcap Hostile Max set to " + localMobcapHostileMax);
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
            sender.sendMessage("§eTotal: " + total + " (of max " + localMobcapMax + ")");
            for(EntityType et: cnt.keySet()) {
                sender.sendMessage((isMobHostile(et) ? "§c" : "§a") + et.toString() + ": " + cnt.get(et));
            }
            return true;
        } else if(command.getName().equals("mobcapall")) {
            int total = 0;
            for(World world : Bukkit.getWorlds()) {
                total = total + world.getLivingEntities().size();
                System.out.println("Total mobs loaded on world " + world.getName() + ": " + world.getLivingEntities().size());
                for(LivingEntity entity : world.getLivingEntities()) {
                    Location l = entity.getLocation();
                    if(l.getWorld() != null) {
                        System.out.println(entity.getType() + " is located at " + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getWorld().getName());
                    }
                }
            }
            System.out.println("Total mobs loaded on the server: " + total);
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
        int cnt = countMobs(le);
        if(cnt >= localMobcapMax || (cnt >= localMobcapHostileMax && isMobHostile(le.getType()))) {
            event.setCancelled(true);
            status = "TRUE";
        } else {
            status = "FALSE";
        }
        this.logger.log(Level.CONFIG,"[CVMobCap] Spawning Living Entity: §6" + le.getType() +
                " §rCause: §6" + event.getSpawnReason() +
                " §rCancelled: §6" + status);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEntitySpawnAttempt(PlayerBucketEmptyEvent event) {
        if(event.isCancelled()) return;
        Material mat = event.getBucket();
        if(!mat.equals(Material.AXOLOTL_BUCKET) &&
                !mat.equals(Material.PUFFERFISH_BUCKET) &&
                !mat.equals(Material.SALMON_BUCKET) &&
                !mat.equals(Material.COD_BUCKET) &&
                !mat.equals(Material.TROPICAL_FISH_BUCKET)) return;
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        ItemStack bucket;
        if(inv.getItemInMainHand().getType().equals(mat)) {
            bucket = inv.getItemInMainHand();
        } else {
            bucket = inv.getItemInOffHand();
        }
        Location location = event.getBlock().getLocation();
        int initial = countBucketEntities(location);
        this.getServer().getScheduler().runTaskLater(this, () -> {
            if(initial >= countBucketEntities(location)) {
                if(inv.getItemInMainHand().equals(event.getItemStack())) {
                    inv.setItemInMainHand(null);
                } else if(inv.getItemInOffHand().equals(event.getItemStack())) {
                    inv.setItemInOffHand(null);
                }
                inv.addItem(new ItemStack(bucket));
                if(location.getBlock().getType().equals(Material.WATER)) {
                    location.getBlock().setType(Material.AIR);
                }
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTransform(EntityTransformEvent event) {
        Entity le = event.getEntity();
        String status;
        int cnt = countMobs(le);
        if(cnt >= localMobcapMax || (cnt >= localMobcapHostileMax && isMobHostile(le.getType()))) {
            event.setCancelled(true);
            status = "TRUE";
        } else {
            status = "FALSE";
        }
        this.logger.log(Level.CONFIG,"[CVMobCap] Transforming Living Entity: §6" + le.getType() +
                " §rCause: §6" + event.getTransformReason() +
                " §rCancelled: §6" + status);
    }

    private int countMobs(Entity le) {
        int cnt = 0;
        for(Entity e: le.getLocation().getWorld().getEntitiesByClass(LivingEntity.class)) {
            if(e.getType() != EntityType.PLAYER &&
                    e.getType() != EntityType.ARMOR_STAND &&
                    e.getType() != EntityType.MARKER &&
                    e.getLocation().distance(le.getLocation()) < localMobcapRadius) {
                cnt++;
            }
        }
        return cnt;
    }

    private int countBucketEntities(Location loc) {
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
