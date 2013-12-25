package com.jabyftw.pd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rafael
 */
public class PDifficulty extends JavaPlugin implements Listener, CommandExecutor {

    private MySQL sql;
    private final FileConfiguration config = getConfig();
    int defaultDif, cooldownTime;
    private double hardM, normalM, easyM, peacefulM;
    private List<String> cooldown = new ArrayList();
    private Map<Player, Integer> difficulty = new HashMap();
    /*
     0 - peaceful: no PVP, no targeting + down
     1 - Easy: no fall/lava damage + down
     2 - normal: no hunger
     3 - hard: normal (all enabled)
     */

    @Override
    public void onEnable() {
        config.addDefault("config.defaultDifficulty", 2);
        config.addDefault("config.changeCooldownInMinutes", 30);
        config.addDefault("config.peacefulDamageMultiplier", 0.5);
        config.addDefault("config.easyDamageMultiplier", 0.75);
        config.addDefault("config.normalDamageMultiplier", 1.0);
        config.addDefault("config.hardDamageMultiplier", 1.5);
        config.addDefault("config.MySQL.username", "root");
        config.addDefault("config.MySQL.password", "pass");
        config.addDefault("config.MySQL.url", "jdbc:mysql://localhost:3306/database");
        config.addDefault("lang.yourDifficultyIs", "&6Your difficulty is &e%diff&6.");
        config.addDefault("lang.noPermission", "&cNo permission.");
        config.addDefault("lang.alreadyOnThisDifficulty", "&cAlready on this difficulty.");
        config.addDefault("lang.youCantChangeToEasier", "&cYou cant change to an easier level.");
        config.addDefault("lang.difficultyChanged", "&6Your new difficulty is &e%diff&6.");
        config.addDefault("lang.changeOnCooldown", "&cYou cant change difficulties everytime.");
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
        defaultDif = config.getInt("config.changeCooldownInMinutes") * 60 * 20;
        cooldownTime = config.getInt("config.defaultDifficulty");
        hardM = config.getDouble("config.hardDamageMultiplier");
        normalM = config.getDouble("config.normalDamageMultiplier");
        easyM = config.getDouble("config.easyDamageMultiplier");
        peacefulM = config.getDouble("config.peacefulDamageMultiplier");
        sql = new MySQL(this, config.getString("config.MySQL.username"), config.getString("config.MySQL.password"), config.getString("config.MySQL.url"));
        sql.createTable();
        getLogger().log(Level.INFO, "Loaded configuration.");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().log(Level.INFO, "Registered events.");
        getServer().getPluginCommand("difficulty").setExecutor(this);
        getLogger().log(Level.INFO, "Registered command.");
    }

    @Override
    public void onDisable() {
        sql.closeConn();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        difficulty.put(p, sql.getDifficulty(p.getName().toLowerCase()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            int dif = difficulty.get(p);
            if (dif == 3) {
                e.setDamage(e.getDamage() * 1.5); // 1.5% more damage
            } else if (dif == 2) {
                e.setDamage(e.getDamage() * 1.0); // same damage as the server
            } else if (dif == 1) {
                e.setDamage(e.getDamage() * 0.75); // 75% dmg
            } else if (dif == 0) {
                e.setDamage(e.getDamage() * 0.50); // half damage
            }
            if (dif < 3) { // normal
                if (e.getCause().equals(DamageCause.STARVATION)) {
                    e.setCancelled(true);
                }
            }
            if (dif < 2) { // easy
                if (e.getCause().equals(DamageCause.FALL) || e.getCause().equals(DamageCause.LAVA)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player) {
            Player p = (Player) e.getTarget();
            int dif = difficulty.get(p);
            if (dif == 0) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPVP(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            if (e.getEntity() instanceof Player) {
                if (difficulty.get(((Player) e.getEntity())) < 1 || difficulty.get(((Player) e.getDamager())) < 1) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("pd.change")) {
                Player p = (Player) sender;
                if (args.length > 0) {
                    if (!cooldown.contains(p.getName().toLowerCase())) {
                        try {
                            int dif = Integer.parseInt(args[0]);
                            int atual = difficulty.get(p);
                            if (dif == atual) {
                                p.sendMessage(getLang("alreadyOnThisDifficulty"));
                                return true;
                            } else if (dif < atual) {
                                if (sender.hasPermission("pd.changeToEasier")) {
                                    difficulty.put(p, dif);
                                    if (atual == defaultDif) {
                                        sql.insertPlayer(p.getName().toLowerCase(), dif);
                                    } else if (dif != defaultDif) {
                                        sql.updateDifficulty(p.getName().toLowerCase(), dif);
                                    }
                                    cooldown.add(p.getName().toLowerCase());
                                    removeCooldown(p.getName());
                                    p.sendMessage(getLang("difficultyChanged").replaceAll("%diff", getDifficulty(dif)));
                                    return true;
                                } else {
                                    p.sendMessage(getLang("youCantChangeToEasier"));
                                    return true;
                                }
                            } else { // dif > atual
                                difficulty.put(p, dif);
                                if (atual == defaultDif) {
                                    sql.insertPlayer(p.getName().toLowerCase(), dif);
                                } else if (dif != defaultDif) {
                                    sql.updateDifficulty(p.getName().toLowerCase(), dif);
                                }
                                cooldown.add(p.getName().toLowerCase());
                                removeCooldown(p.getName());
                                p.sendMessage(getLang("difficultyChanged").replaceAll("%diff", getDifficulty(dif)));
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    } else {
                        p.sendMessage(getLang("changeOnCooldown"));
                        return true;
                    }
                } else {
                    sender.sendMessage(getLang("yourDifficultyIs").replaceAll("%diff", getDifficulty(difficulty.get(p))));
                    return true;
                }
            } else {
                sender.sendMessage(getLang("noPermission"));
                return true;
            }
        } else {
            sender.sendMessage("Not avaliable on Consoles");
            return true;
        }
    }

    private String getLang(String path) {
        return config.getString("lang." + path).replaceAll("&", "§");
    }

    private String getDifficulty(Integer dif) {
        if (dif == 0) {
            return "Peaceful";
        } else if (dif == 1) {
            return "Easy";
        } else if (dif == 2) {
            return "Normal";
        } else {
            return "Hard";
        }
    }

    private void removeCooldown(String name) {
        final String name2 = name.toLowerCase();
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

            @Override
            public void run() {
                if (cooldown.contains(name2)) {
                    cooldown.remove(name2);
                }
            }
        }, cooldownTime);
    }
}
