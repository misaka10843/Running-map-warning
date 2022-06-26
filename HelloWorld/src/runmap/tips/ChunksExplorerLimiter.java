package runmap.tips;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class ChunksExplorerLimiter extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<UUID, AtomicInteger> chunksExplorerMap = new ConcurrentHashMap<>();
    private String prefix;
    private String Blockprefix;
    private String BlockLimitKickMessage;
    private int BlockLimit;
    private String BlockLimitWarningMessage;
    private List<String> worlds;
    private double warningRate;
    private boolean outMassage;
    private String consoleText1;
    private String consoleText2;
    private String runMapList;
    private String runMapListCmd;

    public void onLoad() {
        getServer().getConsoleSender().sendMessage("§d+======================================================================+");
        getServer().getConsoleSender().sendMessage("  §bRunMapTips §a插件已装载!    §b作者：§amisaka10843/雪绫钩纱   §b版本：§fv1.1");
        getServer().getConsoleSender().sendMessage("§d+======================================================================+");
    }


    @Override
    public void onEnable() {
        //先保存默认配置
        saveDefaultConfig();
        settingVars();
        //注册命令
        Objects.requireNonNull(getCommand("RMT")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, chunksExplorerMap::clear, 0, getConfig().getLong("cleanTime") * 20);
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    @Override
    //走个形式qwq
    public void onDisable() {
        getServer().getConsoleSender().sendMessage("§d+======================================================================+");
        getServer().getConsoleSender().sendMessage("  §bRunMapTips §a插件已卸载!    §b作者：§amisaka10843/雪绫钩纱   §b版本：§fv1.1");
        getServer().getConsoleSender().sendMessage("§d+======================================================================+");
    }
    private void settingVars() {
        //防止与其他冲突（应该不会吧
        if (getConfig().getInt("version") == 1) {
            getConfig().set("warningRate", 0.8);
            getConfig().set("version", 2);
            saveConfig();
        }
        //获取配置
        prefix = getConfig().getString("prefix");
        Blockprefix = getConfig().getString("blockprefix");
        BlockLimit = getConfig().getInt("BlockLimit");
        worlds = getConfig().getStringList("worlds");
        warningRate = getConfig().getDouble("warningRate");
        BlockLimitWarningMessage = getConfig().getString("BlockLimitWarningMessage");
        BlockLimitKickMessage = getConfig().getString("BlockLimitKickMessage");
        outMassage = getConfig().getBoolean("outMassage");
        consoleText1 = getConfig().getString("text1");
        consoleText2 = getConfig().getString("text2");
        runMapList = getConfig().getString("runMapList");
        runMapListCmd = getConfig().getString("runMapListCmd");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            //定义命令参数
            if (args[0].equalsIgnoreCase("reload") && sender.isOp()) {
                //不直接调用onDisable和onEnable方法
                PluginManager pluginManager = getServer().getPluginManager();
                pluginManager.disablePlugin(this);
                pluginManager.enablePlugin(this);
                getServer().getConsoleSender().sendMessage("§d+======================================================================+");
                getServer().getConsoleSender().sendMessage("  §bRunMapTips §a插件已重载!    §b作者：§amisaka10843/雪绫钩纱   §b版本：§fv1.1");
                getServer().getConsoleSender().sendMessage("§d+======================================================================+");
                return true;
            } else if (args[0].equalsIgnoreCase(runMapListCmd)) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () ->
                        sender.sendMessage(runMapList + chunksExplorerMap.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().get())).limit(10).map(e -> getServer().getOfflinePlayer(e.getKey()).getName() + ":" + e.getValue().get() + "个区块").collect(Collectors.joining("\n"))));

                return true;
            }
        }
        sender.sendMessage("/rmt reload ——重载插件" + "\n /rmt stats ——查看前10人的跑图信息");
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkGenerated(ChunkPopulateEvent event) {
        if (!worlds.contains(event.getChunk().getWorld().getName())) {
            return;
        }
        Location location = event.getChunk().getBlock(7, 127, 7).getLocation();
        event.getChunk().getWorld().getPlayers().stream().min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(location))).ifPresent(
                player -> {
                    UUID uuid = player.getUniqueId();
                    int current = chunksExplorerMap.computeIfAbsent(uuid, (key) -> new AtomicInteger(0)).incrementAndGet();
                    //强转后赋值给变量，防止没计算完就过了
                    int warningBlockLimit = (int) (BlockLimit * warningRate);
                    if (warningBlockLimit == current) {
                        if(outMassage){
                            getLogger().info(consoleText1+player+consoleText2+ warningBlockLimit);
                        }
                        player.sendMessage(prefix + BlockLimitWarningMessage);
                    }
                    if (current >= BlockLimit) {
                        Bukkit.getScheduler().runTaskLater(this, () ->
                        {
                            if (player.isOnline()) {
                                player.kickPlayer(Blockprefix + BlockLimitKickMessage);
                            }
                        }, 3);
                    }
                }
        );
    }
}