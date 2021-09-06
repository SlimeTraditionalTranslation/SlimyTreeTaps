package io.github.thebusybiscuit.slimytreetaps;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;

public class MagicalMirror extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final NamespacedKey mirrorLocation;

    public MagicalMirror(TreeTaps plugin, ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        mirrorLocation = new NamespacedKey(plugin, "mirror_location");
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();
            e.getPlayer().sendMessage(ChatColor.GREEN + "請給你的位置取個名字! 在聊天室中輸入.");
            ChatUtils.awaitInput(e.getPlayer(), name -> setLocation(e.getPlayer(), e.getItem(), name, e.getPlayer().getLocation()));
        };
    }

    public void teleport(Player p, ItemStack item) {
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.ENDER_PEARL), 1)) {
            p.sendMessage(ChatColor.RED + "你必須至少有一顆終界珍珠來使用魔法鏡子!");
            return;
        }

        Optional<Location> location = getLocation(item);

        if (location.isPresent()) {
            if (p.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL)).isEmpty()) {
                PaperLib.teleportAsync(p, location.get()).thenAccept(hasTeleported -> {
                    if (hasTeleported.booleanValue()) {
                        p.sendTitle(item.getItemMeta().getDisplayName(), ChatColor.GRAY + "- 魔法鏡子 -", 20, 60, 20);
                    } else {
                        p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                        p.sendMessage(ChatColor.RED + "傳送已取消!");
                    }
                });
            } else {
                p.sendMessage(ChatColor.RED + "你至少要有一顆終界珍珠來進行傳送!");
            }
        } else {
            p.sendMessage(ChatColor.RED + "此魔法鏡子似乎沒有有效的目的地!");
        }
    }

    private void setLocation(Player p, ItemStack item, String name, Location l) {
        ItemMeta meta = item.getItemMeta();

        JsonObject json = new JsonObject();
        json.addProperty("world", l.getWorld().getUID().toString());
        json.addProperty("x", l.getX());
        json.addProperty("y", l.getY());
        json.addProperty("z", l.getZ());
        json.addProperty("pitch", l.getPitch());
        json.addProperty("yaw", l.getYaw());

        meta.getPersistentDataContainer().set(mirrorLocation, PersistentDataType.STRING, json.toString());
        meta.setDisplayName(ChatColor.AQUA + ChatUtils.removeColorCodes(name));
        item.setItemMeta(meta);
        p.sendMessage(ChatColor.GREEN + "成功設置你的鏡子位置!");
    }

    private Optional<Location> getLocation(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String data = meta.getPersistentDataContainer().get(mirrorLocation, PersistentDataType.STRING);

        if (data != null) {
            JsonObject json = new JsonParser().parse(data).getAsJsonObject();
            UUID uuid = UUID.fromString(json.get("world").getAsString());
            World world = Bukkit.getWorld(uuid);

            if (world != null) {
                double x = json.get("x").getAsDouble();
                double y = json.get("y").getAsDouble();
                double z = json.get("z").getAsDouble();
                float pitch = json.get("pitch").getAsFloat();
                float yaw = json.get("yaw").getAsFloat();
                Location loc = new Location(world, x, y, z, yaw, pitch);

                return Optional.of(loc);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

}
