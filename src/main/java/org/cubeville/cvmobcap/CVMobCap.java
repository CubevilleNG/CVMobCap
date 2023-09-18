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
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CVMobCap extends JavaPlugin implements Listener
{
    private int localMobcapRadius;
    private int localpassivemax;
    private int localhostilemax;
    private int localspawnermax;

    private Map<Location, Set<LivingEntity>> spawnerMobs = new HashMap<>();

    private Logger logger;

    private final Set<EntityType> hostileMobs = new HashSet<>(Arrays.asList(
            EntityType.BLAZE, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDERMAN, EntityType.ENDERMITE,
            EntityType.EVOKER, EntityType.EVOKER_FANGS, EntityType.GHAST, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK,
            EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER,
            EntityType.RAVAGER, EntityType.SHULKER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SKELETON_HORSE,
            EntityType.SLIME, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR, EntityType.WARDEN,
            EntityType.WITCH, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER));

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

        loadConfig();
        logger.log(Level.INFO, "Local Mobcap Radius set to " + localMobcapRadius);
        logger.log(Level.INFO, "Local Passive Max set to " + localpassivemax);
        logger.log(Level.INFO, "Local Hostile Max set to " + localhostilemax);
        logger.log(Level.INFO, "Local Spawner Max set to " + localspawnermax);
    }

    public void loadConfig() {
        File dataDir = getDataFolder();
        File configFile = new File(dataDir, "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        }
        catch(Exception e) {
            throw new RuntimeException("Config loading failed: " + e.getMessage());
        }
        localMobcapRadius = config.getInt("localmobcapradius", 128);
        localpassivemax = config.getInt("localpassivemax", 100);
        localhostilemax = config.getInt("localhostilemax", 100);
        localspawnermax = config.getInt("localspawnermax", 50);
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

            refreshAllSpawnerEntities();

            Map<EntityType, Integer> passiveMobs = new HashMap<>();
            Map<EntityType, Integer> hostileMobs = new HashMap<>();
            Map<EntityType, Integer> spawnerMobs = new HashMap<>();
            int passiveTotal = 0;
            int hostileTotal = 0;
            int spawnerTotal = 0;

            Set<LivingEntity> allSpawnerMobs = new HashSet<>();
            for(Location spawnerLoc : this.spawnerMobs.keySet()) {
                if(spawnerLoc.getWorld().equals(player.getLocation().getWorld()) && spawnerLoc.distance(player.getLocation()) < localMobcapRadius) {
                    allSpawnerMobs.addAll(this.spawnerMobs.get(spawnerLoc));
                    spawnerTotal = spawnerTotal + this.spawnerMobs.get(spawnerLoc).size();
                }
            }
            for(LivingEntity le : allSpawnerMobs) {
                if(spawnerMobs.containsKey(le.getType())) {
                    spawnerMobs.put(le.getType(), spawnerMobs.get(le.getType()) + 1);
                } else {
                    spawnerMobs.put(le.getType(), 1);
                }
            }
            for(LivingEntity le : player.getLocation().getWorld().getEntitiesByClass(LivingEntity.class)) {
                if(le.getType() == EntityType.ARMOR_STAND || le.getType() == EntityType.PLAYER || le.getType() == EntityType.MARKER) continue;
                if(allSpawnerMobs.contains(le)) continue;
                if(le.getLocation().distance(player.getLocation()) < localMobcapRadius) {
                    if(isMobHostile(le.getType())) {
                        if(hostileMobs.containsKey(le.getType())) {
                            hostileMobs.put(le.getType(), hostileMobs.get(le.getType()) + 1);
                        } else {
                            hostileMobs.put(le.getType(), 1);
                        }
                        hostileTotal++;
                    } else {
                        if(passiveMobs.containsKey(le.getType())) {
                            passiveMobs.put(le.getType(), passiveMobs.get(le.getType()) + 1);
                        } else {
                            passiveMobs.put(le.getType(), 1);
                        }
                        passiveTotal++;
                    }
                }
            }
            sender.sendMessage(ChatColor.YELLOW + "Mobcap statistics for location of " + player.getName() + ": ");
            sender.sendMessage(ChatColor.YELLOW + "Total Passive: " + passiveTotal + " (of max " + localpassivemax + ")");
            for(EntityType et: passiveMobs.keySet()) {
                sender.sendMessage(ChatColor.GREEN + et.toString() + ": " + passiveMobs.get(et));
            }
            sender.sendMessage(ChatColor.YELLOW + "Total Hostile: " + hostileTotal + " (of max " + localhostilemax + ")");
            for(EntityType et: hostileMobs.keySet()) {
                sender.sendMessage(ChatColor.RED + et.toString() + ": " + hostileMobs.get(et));
            }
            sender.sendMessage(ChatColor.YELLOW + "Total Spawner: " + spawnerTotal + " (per spawner max is set at " + localspawnermax);
            for(EntityType et: spawnerMobs.keySet()) {
                sender.sendMessage(ChatColor.DARK_RED + et.toString() + ": " + spawnerMobs.get(et));
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
        } else if(command.getName().equals("mobcapreload")) {
            int localMobcapRadius = this.localMobcapRadius;
            int localpassivemax = this.localpassivemax;
            int localhostilemax = this.localhostilemax;
            int localspawnermax = this.localspawnermax;
            loadConfig();
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Config reloaded! Values: ");
            sender.sendMessage("Mobcap Radius: " + localMobcapRadius + " -> " + this.localMobcapRadius);
            sender.sendMessage("Passive Max: " + localpassivemax + " -> " + this.localpassivemax);
            sender.sendMessage("Hostile Max: " + localhostilemax + " -> " + this.localhostilemax);
            sender.sendMessage("Spawner Max: " + localspawnermax + " -> " + this.localspawnermax);
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if(event.isCancelled()) return;
        if(event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER)) return;
        LivingEntity le = event.getEntity();
        if(le.getType() == EntityType.PLAYER || le.getType() == EntityType.ARMOR_STAND || le.getType() == EntityType.MARKER) return;

        boolean hostile = isMobHostile(le.getType());
        String status;
        int cnt = countMobs(le, hostile);
        if((hostile && cnt >= localhostilemax) || (!hostile && cnt >= localpassivemax)) {
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
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Location spawnerLoc = event.getSpawner().getLocation();
        if(spawnerMobs.containsKey(spawnerLoc)) {
            refreshSpawnerEntities(spawnerLoc);
        } else {
            spawnerMobs.put(spawnerLoc, new HashSet<>());
        }
        if(event.isCancelled()) return;
        if(!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) event.getEntity();
        if(le.getType() == EntityType.PLAYER || le.getType() == EntityType.ARMOR_STAND || le.getType() == EntityType.MARKER) return;

        String status;
        int cnt = spawnerMobs.get(spawnerLoc).size();
        if(cnt >= localspawnermax) {
            event.setCancelled(true);
            status = "TRUE";
        } else {
            Set<LivingEntity> mobs = new HashSet<>(spawnerMobs.get(spawnerLoc));
            mobs.add(le);
            spawnerMobs.put(spawnerLoc, mobs);
            status = "FALSE";
        }
        this.logger.log(Level.CONFIG,"[CVMobCap] Spawning Living Entity: §6" + le.getType() +
                " §rCause: §6SPAWNER" +
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
        boolean hostile = isMobHostile(le.getType());
        String status;
        int cnt = countMobs(le, hostile);
        if((hostile && cnt >= localhostilemax) || (!hostile && cnt >= localpassivemax)) {
            event.setCancelled(true);
            status = "TRUE";
        } else {
            status = "FALSE";
        }
        this.logger.log(Level.CONFIG,"[CVMobCap] Transforming Living Entity: §6" + le.getType() +
                " §rCause: §6" + event.getTransformReason() +
                " §rCancelled: §6" + status);
    }

    private int countMobs(Entity le, boolean hostile) {
        int cnt = 0;
        for(Entity e: le.getLocation().getWorld().getEntitiesByClass(LivingEntity.class)) {
            if(e.getType() != EntityType.PLAYER &&
                    e.getType() != EntityType.ARMOR_STAND &&
                    e.getType() != EntityType.MARKER &&
                    e.getLocation().distance(le.getLocation()) < localMobcapRadius) {
                if((hostile && isMobHostile(e.getType())) || (!hostile && !isMobHostile(e.getType()))) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private int countBucketEntities(Location loc) {
        int i = 0;
        for(Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
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

    private void refreshAllSpawnerEntities() {
        Set<Location> locs = new HashSet<>(spawnerMobs.keySet());
        for(Location loc : locs) {
            refreshSpawnerEntities(loc);
        }
    }

    private void refreshSpawnerEntities(Location loc) {
        if(spawnerMobs.containsKey(loc)) {
            Set<LivingEntity> newEntities = new HashSet<>();
            for(LivingEntity le : spawnerMobs.get(loc)) {
                for(LivingEntity realLE : loc.getWorld().getLivingEntities()) {
                    if(le.getUniqueId().equals(realLE.getUniqueId())) {
                        newEntities.add(le);
                        break;
                    }
                }
            }
            spawnerMobs.put(loc, newEntities);
        }
    }

    private boolean isMobHostile(EntityType type) {
        return hostileMobs.contains(type);
    }

}
