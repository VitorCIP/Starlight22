package br.starlight;
import org.bukkit.plugin.java.JavaPlugin;
public final class StarLight extends JavaPlugin {
    private LegendaryItems legendaryItems;
    @Override public void onEnable() {
        saveDefaultConfig();
        legendaryItems = new LegendaryItems(this);
        getServer().getPluginManager().registerEvents(legendaryItems, this);
        getCommand("starlight").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return true;
            if (!p.hasPermission("starlight.use")) return true;
            legendaryItems.openMenu(p);
            return true;
        });
        getLogger().info("StarLight ativado.");
    }
}
