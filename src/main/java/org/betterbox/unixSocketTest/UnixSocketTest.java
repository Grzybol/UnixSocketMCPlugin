package org.betterbox.unixSocketTest;

import org.betterbox.elasticBuffer.ElasticBuffer;
import org.betterbox.elasticBuffer.ElasticBufferAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;
import java.net.*;
import java.util.EnumSet;
import java.util.Set;

public class UnixSocketTest extends JavaPlugin {
    private AFUNIXServerSocket serverSocket;
    private String folderPath;
    private PluginLogger pluginLogger;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        folderPath = getDataFolder().getAbsolutePath();
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configManager = new ConfigManager(this);
        Set<PluginLogger.LogLevel> defaultLogLevels = EnumSet.of(PluginLogger.LogLevel.INFO, PluginLogger.LogLevel.WARNING, PluginLogger.LogLevel.ERROR);
        pluginLogger = new PluginLogger(folderPath, defaultLogLevels, this);
        loadElasticBuffer();
        new Thread(this::startServer).start();
        new Thread(this::startStatusServer).start();
        new CommandManager(this, configManager, pluginLogger,this);
    }

    @Override
    public void onDisable() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        try {
            final File socketFile = new File(configManager.getSocketName());
            socketFile.delete();
            serverSocket = AFUNIXServerSocket.newInstance();
            serverSocket.bind(new AFUNIXSocketAddress(socketFile));

            while (!serverSocket.isClosed()) {
                try (Socket sock = serverSocket.accept()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        handleCommand(line.trim());
                    }
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Error starting server: " + e.getMessage());
        }
    }
    private void startStatusServer() {
        try {
            final File socketFile = new File(configManager.getStatusSocketName()); // np. "/tmp/plugin_status.sock"
            socketFile.delete();
            AFUNIXServerSocket statusSocket = AFUNIXServerSocket.newInstance();
            statusSocket.bind(new AFUNIXSocketAddress(socketFile));

            while (!statusSocket.isClosed()) {
                Socket sock = statusSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3 && parts[1].equalsIgnoreCase("isOnline")) {
                        String token = parts[0];
                        String playerName = parts[2];

                        if (!token.equals(configManager.getAuthToken())) {
                            writer.write("unauthorized\n");
                        } else {
                            boolean online = Bukkit.getPlayerExact(playerName) != null && Bukkit.getPlayerExact(playerName).isOnline();
                            writer.write(online ? "true\n" : "false\n");
                        }
                        writer.flush();
                    } else {
                        writer.write("invalid\n");
                        writer.flush();
                    }
                }

                sock.close();
            }
        } catch (IOException e) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Status server error: " + e.getMessage());
        }
    }

    private void loadElasticBuffer(){
        try{
            PluginManager pm = Bukkit.getPluginManager();
            try {
                // Opóźnienie o 5 sekund, aby dać ElasticBuffer czas na pełną inicjalizację
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                pluginLogger.log(PluginLogger.LogLevel.WARNING, "[BetterElo] Initialization delay interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Przywrócenie statusu przerwania wątku
            }
            ElasticBuffer elasticBuffer = (ElasticBuffer) pm.getPlugin("ElasticBuffer");
            pluginLogger.isElasticBufferEnabled=true;
            pluginLogger.api= new ElasticBufferAPI(elasticBuffer);
        }catch (Exception e){
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "ElasticBufferAPI instance found via ServicesManager, exception: "+e.getMessage());
        }
    }
    void handleCommand(String command) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Received command: " + command);
        String[] parts = command.split(":", 2);  // Zakładamy, że komenda jest formatu "token:komenda"
        if (parts.length < 2) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Received malformed command: " + command);
            return;
        }

        String token = parts[0];
        String actualCommand = parts[1];
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Received token: " + token);

        if (!token.equals(configManager.getAuthToken())) {
            pluginLogger.log(PluginLogger.LogLevel.WARNING, "Unauthorized attempt to execute command: " + command + " from " + token);
            return;
        }

        pluginLogger.log(PluginLogger.LogLevel.INFO, "Authorized command: " + actualCommand);

        // Wykonaj komendę z 1 tick opóźnienia w głównym wątku
        Bukkit.getScheduler().runTaskLater(this, () -> {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            pluginLogger.log(PluginLogger.LogLevel.INFO, "Executing command: " + actualCommand);
            boolean result = Bukkit.dispatchCommand(console, actualCommand);
            pluginLogger.log(PluginLogger.LogLevel.INFO, "Command executed: " + actualCommand + " | Success: " + result);
        }, 1L);
    }


    /*
    void handleCommand(String command) {
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Received command: " + command);
        String[] parts = command.split(":", 2);  // Zakładamy, że komenda jest formatu "token:komenda"
        if (parts.length < 2) {
            pluginLogger.log(PluginLogger.LogLevel.ERROR, "Received malformed command: " + command);
            return;
        }

        String token = parts[0];
        String actualCommand = parts[1];
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Received token: " + token);

        if (!token.equals(configManager.getAuthToken())) {
            pluginLogger.log(PluginLogger.LogLevel.WARNING, "Unauthorized attempt to execute command: " + command+" from "+token);
            return;
        }
        pluginLogger.log(PluginLogger.LogLevel.INFO, "Received command: " + actualCommand);

        // Logowanie i wykonanie komendy, jeśli token jest prawidłowy
        getLogger().info("Received command: " + actualCommand);
        ConsoleCommandSender console = getServer().getConsoleSender();
        console.sendMessage("Executing command: " + actualCommand);
        // Tutaj możesz dodać wykonanie komendy w serwerze Minecraft
        Bukkit.getScheduler().runTask(this, () -> {
            console.sendMessage("Executing command: " + actualCommand);
            boolean result = Bukkit.dispatchCommand(console, actualCommand);
            pluginLogger.log(PluginLogger.LogLevel.INFO, "Command executed: " + actualCommand + " | Success: " + result);
        });

        pluginLogger.log(PluginLogger.LogLevel.INFO, "Command executed: " + actualCommand);


    }

     */

}
