package io.github.expugn.dungeons.itemdrop;

import io.github.expugn.dungeons.AppStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages the dropping of random items in the world with pretty sound and visual effects.
 * Useful for drop parties or to give random rewards.
 * @author S'pugn
 * @version 0.1
 */
public class ItemDrop {
    private Location location;
    private Map<ItemStack, Integer> table;
    private List<ItemStack> keys;
    private int totalWeight;

    /**
     * Construct a new ItemDrop instance.
     * @param location Location to drop items in.
     * @param table Table of different items and their weights.
     */
    public ItemDrop(Location location, Map<ItemStack, Integer> table) {
        this.location = location;
        this.table = table;
        this.keys = new ArrayList<ItemStack>(table.keySet());

        // DETERMINE TOTAL WEIGHT
        this.totalWeight = 0;
        for (Map.Entry<ItemStack, Integer> entry : this.table.entrySet()) {
            totalWeight += entry.getValue();
        }
    }

    /**
     * Get a random item from the table.
     * @return Random ItemStack object that was in the table.
     */
    public ItemStack getItem() {
        Collections.shuffle(keys);
        double rng = Math.random() * totalWeight;
        int weight = 0;
        for (ItemStack item : keys) {
            weight += table.get(item);
            if (rng > weight) {
                continue;
            }
            return item;
        }
        return null;
    }

    /**
     * Run ItemDrop.
     * Generate a List of drops with the given amount parameter and
     * start dropping them in the world location provided.
     * @param amount Amount of items to generate and drop.
     */
    public void run(int amount) {
        if (amount <= 0) {
            // AMOUNT CAN'T BE LOWER THAN OR EQUAL TO 0
            return;
        }

        // GET ITEMS
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            drops.add(getItem());
        }

        // ACTIVATE ItemDropRunnable (SPIT OUT ONE ITEM AT A TIME, USING .run() WILL JUST BARF EVERYTHING OUT)
        final long delay = 0L; // TIME BEFORE LAUNCH
        final long period = 10L; // PERIOD INBETWEEN LAUNCHES, 10L = 0.5 SECOND
        new ItemDropRunnable(location, drops).runTaskTimer(AppStatus.getPlugin(), delay, period);
    }

    /**
     * Return a list of strings of every item in the drop table and their drop percentage.
     * @return List of strings containing detailed drop table information.
     */
    public List<String> debug() {
        List<String> list = new ArrayList<>();
        final int percentage = 100;
        for (Map.Entry<ItemStack, Integer> entry : table.entrySet()) {
            double chance = ((double) entry.getValue() / (double) totalWeight) * percentage;
            list.add(String.format("%s (x%d) - %.2f%s", entry.getKey().getType(),
                entry.getKey().getAmount(), chance, "%"));
        }
        return list;
    }

    /**
     * Open a GUI to the player with the drop table.
     */
    public void debugGUI(Player player, int page) {
        final int slots = 54;
        Inventory inventory = Bukkit.createInventory(null, slots, "debug");
        List<ItemStack> items;
        if (keys.size() <= slots) {
            items = keys;
        } else {
            int start = (page - 1) * slots;
            if (start >= keys.size()) {
                // PAGE IS OUT OF BOUNDS
                items = keys.subList(0, slots);
            } else {
                // PAGE IS WITHIN BOUNDS
                int end = start + slots;
                end = (end > keys.size()) ? keys.size() : end;
                items = keys.subList(start, end);
            }
        }

        for (ItemStack item : items) {
            ItemMeta itemMeta = item.getItemMeta();
            List<String> lore = itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            } else {
                lore.add("");
            }
            lore.add(String.format("%sDEBUG DROP RATE: %d / %d", ChatColor.AQUA, table.get(item), totalWeight));
            lore.add(String.format("%s%.4f%s", ChatColor.AQUA, (float) table.get(item) / totalWeight, "%"));
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
            inventory.addItem(item);
        }
        player.openInventory(inventory);
    }
}
