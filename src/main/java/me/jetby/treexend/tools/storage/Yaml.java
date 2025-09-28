package me.jetby.treexend.tools.storage;

import lombok.AccessLevel;
import lombok.Getter;
import me.jetby.treexend.Main;
import me.jetby.treexend.tools.Logger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.jetby.treexend.tools.LocationHandler.deserializeLocation;
import static me.jetby.treexend.tools.LocationHandler.serializeLocation;

@Getter
public class Yaml implements StorageType {

    private final Map<Location, EggPrices> egg_price = new HashMap<>();
    private final Map<Location, Integer> locations = new ConcurrentHashMap<>();
    private final File file;
    private final YamlConfiguration yaml;

    @Getter(AccessLevel.NONE) private final Main plugin;

    public Yaml(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "storage.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        createOrLoad();
    }

    public void createOrLoad() {
        if (!file.exists()) {
            try {
                plugin.saveResource("storage.yml", false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }}
        load();
        Logger.info("storage.yml успешно загружен.");
    }

    public void load() {
        ConfigurationSection section = yaml.getConfigurationSection("locations");
        ConfigurationSection priceSection = yaml.getConfigurationSection("price_locations");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                int amount = yaml.getInt("locations." + key + ".amount");
                locations.put(deserializeLocation(key, plugin), amount);
            }
        }

        if (priceSection != null) {
            for (String locationKey : priceSection.getKeys(false)) {
                List<Integer> prices = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();
                ConfigurationSection slotSection = priceSection.getConfigurationSection(locationKey);

                if (slotSection != null) {
                    for (String slotStr : slotSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            int price = slotSection.getInt(slotStr);

                            slots.add(slot);
                            prices.add(price);

                            if (plugin.getCfg().isDebug()) {
                                Logger.info("Локация: " + locationKey + " | Слот: " + slot + " | Цена: " + price);
                            }
                        } catch (NumberFormatException e) {
                            Logger.warn("Некорректный ключ слота в price_locations: " + slotStr + " для локации " + locationKey);
                        }
                    }

                    if (!slots.isEmpty() && !prices.isEmpty()) {
                        EggPrices eggPrice = new EggPrices(new ArrayList<>(slots), new ArrayList<>(prices));
                        egg_price.put(deserializeLocation(locationKey, plugin), eggPrice);
                    }
                }
            }
        }
    }


    @Override
    public String type() {
        return "YAML";
    }

    @Override
    public Map<Location, Integer> getLocationCache() {
        return locations;
    }

    @Override
    public void setLocationCache(@NotNull Location location, int amount) {
        plugin.getRunner().runAsync(() ->{locations.put(location, amount);});

    }

    @Override
    public int getLocationCache(@NotNull Location location) {
        return locations.get(location);
    }

    @Override
    public boolean containsLocationCache(@NotNull Location location) {
        return locations.containsKey(location);
    }

    @Override
    public void setPriceCache(@NotNull Location location, EggPrices eggPrices) {
        plugin.getRunner().runAsync(()->egg_price.put(location, eggPrices));
    }

    @Override
    public Map<Location, EggPrices> getPriceCache() {return egg_price;}

    @Override
    public EggPrices getEggPrice(@NotNull Location location) {
        return egg_price.get(location);
    }

    @Override
    public void removeCache(@NotNull Location location) {
        if (location == null) return;

        plugin.getRunner().runAsync(() ->{
            locations.put(location, -1);
        });
    }

    @Override
    public boolean cacheExist() {
        return !locations.isEmpty();
    }

    @Override
    public String getTop(int number) {
        if (locations.isEmpty()) return "0_0_0_world";
        List<Map.Entry<Location, Integer>> sorted = new ArrayList<>(locations.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return number <= 0 || number > sorted.size() ? "0_0_0_world" : serializeLocation(sorted.get(number - 1).getKey());
    }

    @Override
    public int getTopAmount(int number) {
        String locationStr = getTop(number);
        if (locationStr == null || locationStr.equals("0_0_0_world")) {
            return 0;
        }

        Location location = deserializeLocation(locationStr, plugin);
        if (location == null) {
            return 0;
        }

        return locations.getOrDefault(location, -1);
    }

    @Override
    public void save() {
        for (Map.Entry<Location, Integer> entry : locations.entrySet()) {
            if (entry.getValue() == -1) continue;
            Location loc = entry.getKey();
            String key = serializeLocation(loc);
            yaml.set("locations." + key + ".amount", entry.getValue());
        }

        for (Location loc : egg_price.keySet()) {
            String key = serializeLocation(loc);
            EggPrices eggPrice = egg_price.get(loc);
            List<Integer> slots = eggPrice.slots();
            List<Integer> prices = eggPrice.prices();

            yaml.set("price_locations." + key, null);

            for (int i = 0; i < slots.size(); i++) {
                yaml.set("price_locations." + key + "." + slots.get(i), prices.get(i));
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            Logger.warn("Не удалось сохранить файл (storage.yml)\n" + e);
        }
    }

    @Override
    public Location getTopLocation(int number) {
        if (locations.isEmpty()) return null;
        List<Map.Entry<Location, Integer>> sorted = new ArrayList<>(locations.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return number <= 0 || number > sorted.size() ? null : sorted.get(number - 1).getKey();
    }
}
