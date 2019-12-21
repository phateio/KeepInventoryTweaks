package io.github.phateio.keepinventorytweaks;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import static io.github.phateio.keepinventorytweaks.ExpFix.getTotalExperience;
import static io.github.phateio.keepinventorytweaks.ExpFix.setTotalExperience;

public class KeepInventoryTweaks extends JavaPlugin implements Listener {

    // Level
    int dropXP;
    int dropXPLevel;

    enum BindingHandle {
        DROP_ITEM, MOVE_INVENTORY
    }

    // BINDING
    BindingHandle bindingHandle;

    // VANISHING
    boolean vanishingOnlyAffectEquipment;

    @Override
    public void onEnable() {
        final FileConfiguration config = getConfig();

        final String path_dropXP = "level.drop-xp";
        final String path_dropXPLevel = "level.drop-xp-level";
        final String path_Binding_handle = "binding_curse.handle";
        final String path_Vanishing_onlyAffectEquipment = "vanishing.only-affect-equipment";

        config.addDefault(path_dropXP, 100);
        config.addDefault(path_dropXPLevel, 0);
        config.addDefault(path_Binding_handle, BindingHandle.MOVE_INVENTORY.name());
        config.addDefault(path_Vanishing_onlyAffectEquipment, true);

        config.options().copyDefaults(true);

        dropXP = config.getInt(path_dropXP);
        dropXPLevel = config.getInt(path_dropXPLevel);
        bindingHandle = BindingHandle.valueOf(config.getString(path_Binding_handle));
        vanishingOnlyAffectEquipment = config.getBoolean(path_Vanishing_onlyAffectEquipment);

        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isKeepInventoryEnable(player.getWorld())) return;

        // level
        final int origin = getTotalExperience(player);

        if (dropXP > 0) {
            setTotalExperience(player, Math.max(origin - dropXP, 0));
        }

        if (dropXPLevel > 0) {
            if (player.getLevel() < dropXPLevel) {
                setTotalExperience(player, 0);
            } else {
                player.setLevel(player.getLevel() - dropXPLevel);
            }
        }

        int dropXPAmount = origin - getTotalExperience(player);
        if ((dropXP > 0 || dropXPLevel > 0) && dropXPAmount > 0) {
            ExperienceOrb xp_ball = (ExperienceOrb) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.EXPERIENCE_ORB);
            xp_ball.setExperience(dropXPAmount);
        }

        final PlayerInventory inventory = player.getInventory();
        // VANISHING
        ItemStack[] vanishingContents = vanishingOnlyAffectEquipment
                ? inventory.getArmorContents()
                : inventory.getContents();

        for (int i = 0; i < vanishingContents.length; i++) {
            ItemStack item = vanishingContents[i];
            if (item != null && item.containsEnchantment(Enchantment.VANISHING_CURSE))
                vanishingContents[i] = null;
        }

        if (vanishingOnlyAffectEquipment)
            inventory.setArmorContents(vanishingContents);
        else
            inventory.setContents(vanishingContents);

        // BINDING
        final ItemStack[] armorContents = inventory.getArmorContents();

        for (int i = 0; i < armorContents.length; i++) {
            ItemStack item = armorContents[i];
            if (item == null || !item.containsEnchantment(Enchantment.BINDING_CURSE)) continue;
            switch (bindingHandle) {
                case DROP_ITEM:
                    armorContents[i] = null;
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    break;
                case MOVE_INVENTORY:
                    armorContents[i] = inventory.addItem(item).get(0);
                    break;
            }
        }

        inventory.setArmorContents(armorContents);
    }


    private boolean isKeepInventoryEnable(World world) {
        Boolean keepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (keepInventory == null) keepInventory = false;
        return keepInventory;
    }


}
