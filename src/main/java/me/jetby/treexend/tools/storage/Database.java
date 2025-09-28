package me.jetby.treexend.tools.storage;

import me.jetby.treexend.Main;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

import static me.jetby.treexend.tools.LocationHandler.deserializeLocation;
import static me.jetby.treexend.tools.LocationHandler.serializeLocation;

public class Database implements StorageType {
    private final Main plugin;
    private final boolean useMySQL;
    private Connection connection;
    private final Map<Location, Integer> locations;
    private final Map<Location, EggPrices> eggPrices;
    private ExecutorService executor;
    private ScheduledExecutorService syncScheduler;
    private final Queue<Runnable> taskQueue;
    private volatile boolean shuttingDown = false;

    public Database(Main plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getCfg().getStorageType().equalsIgnoreCase("MYSQL");
        this.locations = new ConcurrentHashMap<>();
        this.eggPrices = new ConcurrentHashMap<>();
        this.taskQueue = new ConcurrentLinkedQueue<>();

        this.executor = Executors.newSingleThreadExecutor();
        this.syncScheduler = Executors.newSingleThreadScheduledExecutor();

        initialize();
    }

    private void initialize() {
        executor.execute(() -> {
            try {
                this.connection = connect();
                createTables();
                loadAllData();
                startSyncTask();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка инициализации базы данных", e);
            }
        });
    }

    private Connection connect() throws SQLException {
        if (useMySQL) {
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&failOverReadOnly=false",
                    plugin.getCfg().getStorageHost(),
                    plugin.getCfg().getStoragePort(),
                    plugin.getCfg().getStorageDatabase());
            return DriverManager.getConnection(url,
                    plugin.getCfg().getStorageUsername(),
                    plugin.getCfg().getStoragePassword());
        } else {
            return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/storage.db");
        }
    }

    private void createTables() {
        String locationsTable = useMySQL ?
                "CREATE TABLE IF NOT EXISTS locations (" +
                        "location VARCHAR(255) PRIMARY KEY, " +
                        "amount INT NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" :
                "CREATE TABLE IF NOT EXISTS locations (" +
                        "location TEXT PRIMARY KEY, " +
                        "amount INTEGER NOT NULL" +
                        ")";

        String pricesTable = useMySQL ?
                "CREATE TABLE IF NOT EXISTS price_locations (" +
                        "location VARCHAR(255), " +
                        "slot INT, " +
                        "price INT, " +
                        "PRIMARY KEY (location, slot)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" :
                "CREATE TABLE IF NOT EXISTS price_locations (" +
                        "location TEXT, " +
                        "slot INTEGER, " +
                        "price INTEGER, " +
                        "PRIMARY KEY (location, slot)" +
                        ")";

        executeUpdate(locationsTable);
        executeUpdate(pricesTable);

        if (useMySQL) {
            executeUpdate("CREATE INDEX IF NOT EXISTS idx_amount ON locations(amount)");
        }
    }

    private void executeUpdate(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка выполнения SQL: " + sql, e);
        }
    }

    private void loadAllData() {
        loadLocations();
        loadPrices();
    }

    private void loadLocations() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, amount FROM locations")) {
            while (rs.next()) {
                Location loc = deserializeLocation(rs.getString("location"), plugin);
                locations.put(loc, rs.getInt("amount"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки локаций", e);
        }
    }

    private void loadPrices() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, slot, price FROM price_locations ORDER BY location, slot")) {
            String currentLoc = null;
            List<Integer> slots = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();

            while (rs.next()) {
                String locStr = rs.getString("location");
                if (currentLoc != null && !currentLoc.equals(locStr)) {
                    addEggPrice(currentLoc, slots, prices);
                    slots.clear();
                    prices.clear();
                }
                currentLoc = locStr;
                slots.add(rs.getInt("slot"));
                prices.add(rs.getInt("price"));
            }

            if (currentLoc != null && !slots.isEmpty()) {
                addEggPrice(currentLoc, slots, prices);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки цен", e);
        }
    }

    private void addEggPrice(String locStr, List<Integer> slots, List<Integer> prices) {
        Location loc = deserializeLocation(locStr, plugin);
        if (!useMySQL || locations.containsKey(loc)) {
            eggPrices.put(loc, new EggPrices(new ArrayList<>(slots), new ArrayList<>(prices)));
        }
    }

    private void startSyncTask() {
        syncScheduler.scheduleAtFixedRate(() -> {
            if (!taskQueue.isEmpty()) {
                processQueue();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void processQueue() {
        if (shuttingDown) return;

        List<Runnable> tasks = new ArrayList<>();
        while (!taskQueue.isEmpty() && tasks.size() < 100) {
            tasks.add(taskQueue.poll());
        }

        if (!tasks.isEmpty()) {
            try {
                connection.setAutoCommit(false);
                for (Runnable task : tasks) {
                    task.run();
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                    if (!shuttingDown) {
                        tasks.forEach(taskQueue::offer);
                    }
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка отката транзакции", ex);
                }
                plugin.getLogger().log(Level.SEVERE, "Ошибка синхронизации данных", e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка установки авто-коммита", e);
                }
            }
        }
    }

    @Override
    public String type() {
        return useMySQL ? "MYSQL" : "SQLITE";
    }

    @Override
    public Map<Location, Integer> getLocationCache() {
        return locations;
    }

    @Override
    public void setLocationCache(@NotNull Location location, int amount) {
        locations.put(location, amount);
        queueTask(() -> {
            String sql = useMySQL ?
                    "INSERT INTO locations (location, amount) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE amount = VALUES(amount)" :
                    "INSERT OR REPLACE INTO locations (location, amount) VALUES (?, ?)";
            executeUpdate(sql, serializeLocation(location), amount);
        });
    }

    @Override
    public int getLocationCache(@NotNull Location location) {
        return locations.getOrDefault(location, 0);
    }

    @Override
    public boolean containsLocationCache(@NotNull Location location) {
        return locations.containsKey(location);
    }

    @Override
    public void setPriceCache(@NotNull Location location, EggPrices eggPrices) {
        this.eggPrices.put(location, eggPrices);
        queueTask(() -> {
            String locStr = serializeLocation(location);
            executeUpdate("DELETE FROM price_locations WHERE location = ?", locStr);

            List<Integer> slots = eggPrices.slots();
            List<Integer> prices = eggPrices.prices();
            String insertSql = "INSERT INTO price_locations (location, slot, price) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                for (int i = 0; i < slots.size(); i++) {
                    stmt.setString(1, locStr);
                    stmt.setInt(2, slots.get(i));
                    stmt.setInt(3, prices.get(i));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения цен", e);
            }
        });
    }

    @Override
    public EggPrices getEggPrice(@NotNull Location location) {
        return eggPrices.get(location);
    }

    public Map<Location, EggPrices> getPriceCache() {
        return eggPrices;
    }

    @Override
    public void removeCache(@NotNull Location location) {
        if (location == null) return;
        locations.remove(location);
        eggPrices.remove(location);
        queueTask(() -> {
            String locStr = serializeLocation(location);
            executeUpdate("DELETE FROM locations WHERE location = ?", locStr);
            executeUpdate("DELETE FROM price_locations WHERE location = ?", locStr);
        });
    }

    @Override
    public boolean cacheExist() {
        return !locations.isEmpty();
    }

    @Override
    public String getTop(int number) {
        return locations.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .skip(number - 1L)
                .findFirst()
                .map(entry -> serializeLocation(entry.getKey()))
                .orElse("0_0_0_world");
    }

    @Override
    public Location getTopLocation(int number) {
        return deserializeLocation(getTop(number), plugin);
    }

    @Override
    public int getTopAmount(int number) {
        return locations.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .skip(number - 1L)
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(0);
    }

    @Override
    public void save() {
        if (shuttingDown) {
            saveImmediately();
        } else {
            queueTask(this::saveImmediately);
        }
    }

    private void saveImmediately() {
        try {
            connection.setAutoCommit(false);

            String locSql = useMySQL ?
                    "INSERT INTO locations (location, amount) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE amount = VALUES(amount)" :
                    "INSERT OR REPLACE INTO locations (location, amount) VALUES (?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(locSql)) {
                for (Map.Entry<Location, Integer> entry : locations.entrySet()) {
                    stmt.setString(1, serializeLocation(entry.getKey()));
                    stmt.setInt(2, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            executeUpdate("DELETE FROM price_locations");
            String priceSql = "INSERT INTO price_locations (location, slot, price) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(priceSql)) {
                for (Map.Entry<Location, EggPrices> entry : eggPrices.entrySet()) {
                    String locStr = serializeLocation(entry.getKey());
                    List<Integer> slots = entry.getValue().slots();
                    List<Integer> prices = entry.getValue().prices();

                    for (int i = 0; i < slots.size(); i++) {
                        stmt.setString(1, locStr);
                        stmt.setInt(2, slots.get(i));
                        stmt.setInt(3, prices.get(i));
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка отката транзакции", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения данных", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка установки авто-коммита", e);
            }
        }
    }

    public void shutdown() {
        shuttingDown = true;

        syncScheduler.shutdown();
        try {
            if (!syncScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                syncScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        saveImmediately();

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка закрытия соединения с базой данных", e);
        }
    }

    private void queueTask(Runnable task) {
        if (shuttingDown) {
            return;
        }

        if (useMySQL) {
            taskQueue.offer(task);
        } else {
            try {
                executor.execute(task);
            } catch (RejectedExecutionException e) {
                if (!shuttingDown) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка выполнения задачи", e);
                }
            }
        }
    }

    private void executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка выполнения SQL: " + sql, e);
        }
    }
}