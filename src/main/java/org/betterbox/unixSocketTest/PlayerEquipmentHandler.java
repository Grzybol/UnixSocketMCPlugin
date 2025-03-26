package org.betterbox.unixSocketTest;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerEquipmentHandler {
    private final PlayerDataUtils playerDataUtils;
    private final PluginLogger pluginLogger;
    private final Gson gson = new Gson();

    public PlayerEquipmentHandler(PlayerDataUtils utils, PluginLogger logger) {

        this.playerDataUtils = utils;
        this.pluginLogger = logger;
    }

    public String handleGetPlayerBackpack(String playerName) {
        return getJsonEq(playerName, EqType.BACKPACK);
    }

    public String handleGetPlayerArmor(String playerName) {
        return getJsonEq(playerName, EqType.ARMOR);
    }

    public String handleGetPlayerMainItems(String playerName) {
        return getJsonEq(playerName, EqType.HOTBAR);
    }

    public void startEquipmentServer(String backpackSocketPath, String armorSocketPath, String hotbarSocketPath, String authToken) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "🚀 Start serwera socketów z EQ");
        new Thread(() -> listenSocket(backpackSocketPath, authToken, EqType.BACKPACK)).start();
        new Thread(() -> listenSocket(armorSocketPath, authToken, EqType.ARMOR)).start();
        new Thread(() -> listenSocket(hotbarSocketPath, authToken, EqType.HOTBAR)).start();
        pluginLogger.log(PluginLogger.LogLevel.INFO, "🚀 Serwery socketów z EQ uruchomione");
    }

    private void listenSocket(String socketPath, String authToken, EqType type) {
        try {
            File socketFile = new File(socketPath);
            socketFile.delete();
            AFUNIXServerSocket socket = AFUNIXServerSocket.newInstance();
            socket.bind(new AFUNIXSocketAddress(socketFile));

            pluginLogger.log(PluginLogger.LogLevel.INFO, "✅ Nasłuch na sockecie: " + socketPath + " (typ: " + type + ")");

            while (!socket.isClosed()) {
                try (Socket sock = socket.accept()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

                    String line = reader.readLine();
                    pluginLogger.log(PluginLogger.LogLevel.INFO, "📥 Odebrano: " + line + " (typ: " + type + ")");

                    if (line == null || line.trim().isEmpty()) {
                        pluginLogger.log(PluginLogger.LogLevel.WARNING, "⚠️ Odebrano pusty string na sockecie: " + socketPath);
                        writer.write("invalid\n");
                        writer.flush();
                        continue;
                    }

                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String token = parts[0].trim();
                        String playerName = parts[1].trim();
                        if (token.equals(authToken)) {
                            pluginLogger.log(PluginLogger.LogLevel.INFO, "✅ Autoryzowany request od: " + playerName + " (typ: " + type + ")");
                            String json = getJsonEq(playerName, type);
                            writer.write(json + "\n");
                        } else {
                            pluginLogger.log(PluginLogger.LogLevel.WARNING, "⛔ Niepoprawny token od klienta!");
                            writer.write("unauthorized\n");
                        }
                    } else {
                        pluginLogger.log(PluginLogger.LogLevel.ERROR, "❌ Zły format zapytania: " + line);
                        writer.write("invalid\n");
                    }

                    writer.flush();

                } catch (IOException e) {
                    pluginLogger.log(PluginLogger.LogLevel.ERROR, "❌ Socket error [" + type + "]: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "❌ Nie udało się uruchomić socketu " + socketPath + ": " + e.getMessage());
        }
    }


    private String getJsonEq(String playerName, EqType type) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "🔍 Pobieranie EQ gracza: " + playerName + " (typ: " + type + ")");
        try {
            Player player = Bukkit.getPlayerExact(playerName);
            ItemStack[] items;

            if (player != null && player.isOnline()) {
                pluginLogger.log(PluginLogger.LogLevel.INFO, "🔍 Gracz online: " + playerName);
                switch (type) {
                    case BACKPACK -> items = PlayerDataUtils.getOnlinePlayerInventory(player);
                    case ARMOR -> items = PlayerDataUtils.getOnlinePlayerArmorAndOffhand(player);
                    case HOTBAR -> items = PlayerDataUtils.getOnlinePlayerHotbar(player);
                    default -> items = new ItemStack[0];
                }
            } else {
                pluginLogger.log(PluginLogger.LogLevel.INFO, "🔍 Gracz offline: " + playerName);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    pluginLogger.log(PluginLogger.LogLevel.WARNING, "Gracz nie istnieje: " + playerName);
                    return gson.toJson(List.of());
                }
                String uuid = offlinePlayer.getUniqueId().toString();
                switch (type) {
                    case BACKPACK -> items = playerDataUtils.getOfflinePlayerInventory(uuid);
                    case ARMOR -> items = playerDataUtils.getOfflinePlayerArmorAndOffhand(uuid);
                    case HOTBAR -> items = playerDataUtils.getOfflinePlayerHotbar(uuid);
                    default -> items = new ItemStack[0];
                }
            }
            List<SimpleItem> simplified = Arrays.stream(items)
                    .map(this::convert)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return gson.toJson(simplified);


        } catch (Exception e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Błąd przy przetwarzaniu EQ gracza: " + e.getMessage());
            return gson.toJson(new ItemStack[0]);
        }
    }
    public class SimpleItem {
        public String type;
        public int amount;
        public String name;
        public Map<String, Integer> enchants;
    }
    public SimpleItem convert(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;

        SimpleItem simple = new SimpleItem();
        simple.type = stack.getType().name();
        simple.amount = stack.getAmount();

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            simple.name = meta.getDisplayName();
            simple.enchants = meta.getEnchants().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().getKey().getKey(), Map.Entry::getValue));
        }

        return simple;
    }



    private enum EqType {
        BACKPACK, ARMOR, HOTBAR
    }
}
