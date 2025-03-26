package org.betterbox.unixSocketTest;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;


import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class PlayerDataUtils {
    private static PluginLogger pluginLogger;

    public PlayerDataUtils(PluginLogger logger) {
        pluginLogger = logger;
    }

    private static ListTag<?> readInventoryTag(String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        File playerFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + uuid + ".dat");

        if (!playerFile.exists()) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Nie znaleziono pliku gracza: " + playerFile.getPath());
            return null;
        }

        try {
            CompoundTag root = (CompoundTag) NBTUtil.read(playerFile, false).getTag();
            return root.getListTag("Inventory");
        } catch (IOException e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Błąd przy odczycie pliku gracza: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack[] getOfflinePlayerInventory(String uuidString) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie inventory gracza: " + uuidString);
        ListTag<?> inventoryTag = readInventoryTag(uuidString);
        ItemStack[] inventory = new ItemStack[27];

        if (inventoryTag == null) return inventory;

        for (Tag<?> baseTag : inventoryTag) {
            if (!(baseTag instanceof CompoundTag itemTag)) continue;
            int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot >= 9 && slot <= 35) {
                ItemStack item = nbtToItemStack(itemTag);
                inventory[slot - 9] = item;
            }
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano inventory gracza: " + uuidString+" eq: "+inventory);
        return inventory;
    }

    public static ItemStack[] getOfflinePlayerHotbar(String uuidString) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie hotbara gracza: " + uuidString);
        ListTag<?> inventoryTag = readInventoryTag(uuidString);
        ItemStack[] hotbar = new ItemStack[9];

        if (inventoryTag == null) return hotbar;

        for (Tag<?> baseTag : inventoryTag) {
            if (!(baseTag instanceof CompoundTag itemTag)) continue;
            int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot <= 8) {
                ItemStack item = nbtToItemStack(itemTag);
                hotbar[slot] = item;
            }
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano hotbar gracza: " + uuidString+" eq: "+hotbar);
        return hotbar;
    }

    public static ItemStack[] getOfflinePlayerArmorAndOffhand(String uuidString) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie ekwipunku gracza: " + uuidString);
        ListTag<?> inventoryTag = readInventoryTag(uuidString);
        ItemStack[] gear = new ItemStack[5]; // 0-3 = armor, 4 = offhand

        if (inventoryTag == null) return gear;

        for (Tag<?> baseTag : inventoryTag) {
            if (!(baseTag instanceof CompoundTag itemTag)) continue;
            int slot = itemTag.getByte("Slot") & 0xFF;
            ItemStack item = nbtToItemStack(itemTag);
            switch (slot) {
                case 100 -> gear[0] = item;
                case 101 -> gear[1] = item;
                case 102 -> gear[2] = item;
                case 103 -> gear[3] = item;
                case 150 -> gear[4] = item;
            }
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano ekwipunek gracza: " + uuidString+" eq: "+gear);
        return gear;
    }

    private static ItemStack nbtToItemStack(CompoundTag itemTag) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Konwersja NBT → ItemStack: " + itemTag);
        try {
            String id = itemTag.getString("id"); // np. "minecraft:diamond_sword"
            int count = itemTag.getByte("Count");

            Material material = Material.matchMaterial(id.replace("minecraft:", ""));
            if (material == null) {
                pluginLogger.log(PluginLogger.LogLevel.ERROR, "Nieznany materiał: " + id);
                return null;
            }

            ItemStack item = new ItemStack(material, count);

            if (itemTag.containsKey("tag")) {
                CompoundTag tag = itemTag.getCompoundTag("tag");
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    if (tag.containsKey("display")) {
                        CompoundTag display = tag.getCompoundTag("display");
                        if (display.containsKey("Name")) {
                            String name = display.getString("Name").replace("\"", "");
                            meta.setDisplayName(name);
                        }
                    }

                    if (tag.containsKey("Enchantments")) {
                        ListTag<?> enchList = tag.getListTag("Enchantments");
                        for (Tag<?> enchTag : enchList) {
                            if (enchTag instanceof CompoundTag ench) {
                                String enchId = ench.getString("id").replace("minecraft:", "");
                                int lvl = ench.getShort("lvl");
                                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchId));
                                if (enchantment != null) {
                                    meta.addEnchant(enchantment, lvl, true);
                                }
                            }
                        }
                    }

                    item.setItemMeta(meta);
                }
            }

            return item;

        } catch (Exception e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Błąd konwersji NBT → ItemStack: " + e.getMessage());
            return null;
        }
    }
    public static ItemStack[] getOnlinePlayerInventory(Player player) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie inventory gracza: " + player.getName()+" eq: "+player.getInventory());
        PlayerInventory inv = player.getInventory();
        ItemStack[] inventory = new ItemStack[27]; // sloty 9–35

        for (int i = 9; i <= 35; i++) {
            inventory[i - 9] = inv.getItem(i);
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano inventory gracza: " + player.getName()+" eq: "+inventory);
        return inventory;
    }

    public static ItemStack[] getOnlinePlayerHotbar(Player player) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie hotbara gracza: " + player.getName()+" eq: "+player.getInventory());
        PlayerInventory inv = player.getInventory();
        ItemStack[] hotbar = new ItemStack[9];

        for (int i = 0; i <= 8; i++) {
            hotbar[i] = inv.getItem(i);
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano hotbar gracza: " + player.getName()+" eq: "+hotbar);
        return hotbar;
    }

    public static ItemStack[] getOnlinePlayerArmorAndOffhand(Player player) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobieranie ekwipunku gracza: " + player.getName());
        PlayerInventory inv = player.getInventory();
        ItemStack[] gear = new ItemStack[5];

        gear[0] = inv.getBoots();
        gear[1] = inv.getLeggings();
        gear[2] = inv.getChestplate();
        gear[3] = inv.getHelmet();
        gear[4] = inv.getItemInOffHand();
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Pobrano ekwipunek gracza: " + player.getName()+" eq: "+gear);
        return gear;
    }

}
