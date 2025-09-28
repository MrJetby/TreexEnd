package me.jetby.treexend.configurations;

import lombok.Getter;
import me.jetby.treexend.Main;
import me.jetby.treexend.tools.Logger;
import me.jetby.treexend.tools.colorizer.Colorize;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Config {
    private List<String> chestOnly;
    private List<String> noEggs;
    private List<String> portalIsBlocked;
    private List<String> endIsClose;
    private List<String> tradingIsDisabled;
    private String placeholderTopFormat;
    private String dragonEggName;
    private List<String> dragonEggLore;
    private List<String> dragonEggPrizes;
    private boolean antistack;
    private boolean listenersDropOnQuit;
    private boolean listenersPlace;
    private boolean dragonEggGlowing;
    private boolean listenersTeleportOnPortalJoin;
    private String storageType;
    private String storageHost;
    private int storagePort;
    private String storageDatabase;
    private String storageUsername;
    private String storagePassword;
    private String tradeCommand;
    private String endCloseTeleport;
    private String endEnterTeleport;

    private BarColor barColorDuration;
    private BarStyle barStyleDuration;
    private String barDurationTitle;
    private boolean barDuration;

    private BarColor barColorEgg;
    private BarStyle barStyleEgg;
    private String barEggTitle;
    private boolean barEgg;

    private List<String> formattedTimeSeconds;
    private List<String> formattedTimeMinutes;
    private List<String> formattedTimeHours;
    private List<String> formattedTimeDays;
    private List<String> formattedTimeWeeks;
    private String formattedTimeFormat;

    private List<String> actionsIfDragonAlive;
    private int actionsIfDragonAliveDelay;
    private List<String> actionsOnDeath;

    private boolean growing;
    private int price;
    private int priceMax;
    private int updateInterval;
    private int updateAmount;

    private boolean debug;

    private final Main plugin;
    public Config(Main plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration configuration) {

        debug = configuration.getBoolean("debug", false);

        growing = configuration.getBoolean("dragon-egg.priceGrowing.enable", false);
        price = configuration.getInt("dragon-egg.priceGrowing.price", 0);
        priceMax = configuration.getInt("dragon-egg.priceGrowing.price-max", 2500);
        updateInterval = configuration.getInt("dragon-egg.priceGrowing.update-interval", 60);
        updateAmount = configuration.getInt("dragon-egg.priceGrowing.update-amount", 5);

        if (configuration.contains("dragon.IfDragonAlive")) {
            actionsIfDragonAlive = configuration.getStringList("dragon.IfDragonAlive.actions");
            actionsIfDragonAliveDelay = configuration.getInt("dragon.IfDragonAlive.delay", 60);
        } else {
            actionsIfDragonAlive = new ArrayList<>();
        }

        if (configuration.contains("dragon.onDeath")) {
            actionsOnDeath = configuration.getStringList("dragon.onDeath");
        } else {
            actionsOnDeath = new ArrayList<>();
        }

        barColorEgg = BarColor.valueOf(configuration.getString("BossBar.egg.Color", String.valueOf(BarColor.RED)));
        barStyleEgg = BarStyle.valueOf(configuration.getString("BossBar.egg.Style", String.valueOf(BarStyle.SOLID)));
        barEggTitle = configuration.getString("BossBar.egg.title", "Осторожно у вас в инвентаре яйцо дракона");
        barEgg = configuration.getBoolean("BossBar.egg.enable", false);

        barColorDuration = BarColor.valueOf(configuration.getString("BossBar.duration.Color", String.valueOf(BarColor.RED)));
        barStyleDuration = BarStyle.valueOf(configuration.getString("BossBar.duration.Style", String.valueOf(BarStyle.SOLID)));
        barDurationTitle = configuration.getString("BossBar.duration.title", "Энд закроется через %tend_scheduler_time_to_end% сек.");
        barDuration = configuration.getBoolean("BossBar.duration.enable", false);

        formattedTimeFormat = configuration.getString("formattedTime.format", "%weeks% %days% %hours% %minutes% %seconds%");

        List<String> formattedTimeSecondsDefault = new ArrayList<>(List.of("секунду", "секунды", "секунд"));
        formattedTimeSeconds = getOrDefaultList(configuration, "formattedTime.seconds", formattedTimeSecondsDefault);

        List<String> formattedTimeMinutesDefault = new ArrayList<>(List.of("минуту", "минуты", "минут"));
        formattedTimeMinutes = getOrDefaultList(configuration, "formattedTime.minutes", formattedTimeMinutesDefault);

        List<String> formattedTimeHoursDefault = new ArrayList<>(List.of("час", "часа", "часов"));
        formattedTimeHours = getOrDefaultList(configuration, "formattedTime.hours", formattedTimeHoursDefault);

        List<String> formattedTimeDaysDefault = new ArrayList<>(List.of("день", "дня", "дней"));
        formattedTimeDays = getOrDefaultList(configuration, "formattedTime.days", formattedTimeDaysDefault);

        List<String> formattedTimeWeeksDefault = new ArrayList<>(List.of("неделю", "недели", "недель"));
        formattedTimeWeeks = getOrDefaultList(configuration, "formattedTime.weeks", formattedTimeWeeksDefault);

        List<String> chestOnlyDefault = new ArrayList<>(List.of("&cЯйцо Дракона можно хранить только в сундуке."));
        chestOnly = getOrDefaultList(configuration, "messages.chestOnly", chestOnlyDefault);

        List<String> endIsCloseDefault = new ArrayList<>(List.of("&fЭндер мир сейчас закрыт, возвращайтесь в &bвоскресенье в 16:00 &fпо МСК"));
        endIsClose = getOrDefaultList(configuration, "messages.endIsClose", endIsCloseDefault);

        List<String> tradingIsDisabledDefault = new ArrayList<>(List.of("&cСейчас вы не можете обменять яйца."));
        tradingIsDisabled = getOrDefaultList(configuration, "messages.tradingIsDisabled", tradingIsDisabledDefault);

        List<String> noEggsDefault = new ArrayList<>(List.of("&cУ вас нету яиц дракона для получения приза!"));
        noEggs = getOrDefaultList(configuration, "messages.noEggs", noEggsDefault);

        List<String> portalIsBlockedDefault = new ArrayList<>(List.of(
                "&b▶ &fВ настоящий момент измерение закрыто.",
                "&b Приходи в воскресенье в 16:00"));
        portalIsBlocked = getOrDefaultList(configuration, "messages.portalIsBlocked", portalIsBlockedDefault);

        placeholderTopFormat = configuration.getString("placeholder-top-format", "x: %x%, y: %y%, z: %z%");
        antistack = configuration.getBoolean("dragon-egg.anti-stack", false);
        dragonEggName = configuration.getString("dragon-egg.name", "&dЯйцо дракона");

        List<String> dragonEggLoreDefault = new ArrayList<>(List.of(
                "",
                "&#FB0783&n◢&f В ваших руках самый ценный предмет.",
                "&#FB0783◤&f Будьте с ним осторожнее!",
                "",
                "&#FB43F9Вот его особенности:",
                "&#FB0783● &fВ конце вайпа яйцо(а) можно",
                "  &fобменять на Сапфиры - &#FB0783/egg-swap",
                "  &fКурс обмена: &#FB07831 яйцо &f= &#FB07832500 сапфиров",
                "&#FB0783● &fПрятать яйца можно только",
                "&#FB0783  в обычном сундуке.",
                "&#FB0783● &fНа спавне формируется топ,",
                "  &fпо спрятанным яйцам - &#FB0783/warp top",
                ""));
        dragonEggLore = getOrDefaultList(configuration, "dragon-egg.lore", dragonEggLoreDefault);

        List<String> dragonEggPrizesDefault = new ArrayList<>();
        dragonEggPrizes = getOrDefaultList(configuration, "dragon-egg.prizes", dragonEggPrizesDefault);

        listenersDropOnQuit = configuration.getBoolean("listeners.dropOnQuit", true);
        listenersPlace = configuration.getBoolean("dragon-egg.place", true);
        listenersTeleportOnPortalJoin = configuration.getBoolean("teleportOnPortalJoin", false);
        dragonEggGlowing = configuration.getBoolean("listeners.glowing", true);

        storageType = configuration.getString("storage.type", "YAML");
        storageHost = configuration.getString("storage.host", "localhost");
        storagePort = configuration.getInt("storage.port", 3306);
        storageDatabase = configuration.getString("storage.database", "treexend");
        storageUsername = configuration.getString("storage.username", "root");
        storagePassword = configuration.getString("storage.password", "");

        tradeCommand = configuration.getString("trade-command", "egg-swap");
        endEnterTeleport = configuration.getString("end-enter-teleport");
        endCloseTeleport = configuration.getString("end-close-teleport");

        List<String> dragonDamageDefault = new ArrayList<>(List.of(
                "",
                "Могущественный Дракон &#d27c33скоро будет поврежен, &fи всё",
                "благодаря этим воинам:",
                "&#ebcd35▪ 1 место &7- &#3dcde0%tend_damage_top_1_name% &7| &#d27c33%tend_damage_top_1_damage%",
                "&f▪ 2 место &7- &#3dcde0%tend_damage_top_2_name% &7| &#d27c33%tend_damage_top_2_damage%",
                "&#d27c33▪ 3 место &7- &#3dcde0%tend_damage_top_3_name% &7| &#d27c33%tend_damage_top_3_damage%",
                "",
                "Игрок, занявший первое место, &#ebcd35&lполучит Яйцо дракона.",
                ""
        ));
        Logger.info("config.yml успешно загружен.");
    }

    private List<String> getOrDefaultList(FileConfiguration config, String path, List<String> defaultValue) {
        List<String> list = config.getStringList(path);
        defaultValue.replaceAll(Colorize::hex);
        list.replaceAll(Colorize::hex);
        return list.isEmpty() ? defaultValue : list;
    }

    public FileConfiguration getFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    public void save(String path, FileConfiguration config, String fileName) {
        plugin.getRunner().runAsync(() -> {
            try {
                config.save(new File(path, fileName));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }
}
