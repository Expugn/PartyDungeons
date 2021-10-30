package io.github.expugn.dungeons.itemdrop;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Manages a BukkitRunnable instance where items will
 * continue to be dropped in the world until the given
 * List of drops runs out.
 * @author S'pugn
 * @version 0.1
 */
public class ItemDropRunnable extends BukkitRunnable {
    private Location location;
    private List<ItemStack> drops;
    private final float volume = 0.5F;
    private final double velocityTweak = 0.5;
    private final int particleCount = 30;
    private final int pickupDelay = 50;

    public ItemDropRunnable(Location location, List<ItemStack> drops) {
        this.location = location;
        this.drops = drops;
    }

    @Override
    public void run() {
        World world = location.getWorld();
        Random rng = new Random();
        Item itemEntity = world.dropItem(location, drops.remove(0));
        Vector velocity = new Vector(
            (rng.nextInt() % 2 == 0) ? rng.nextDouble() * velocityTweak : -rng.nextDouble() * velocityTweak,
            1.0,
            (rng.nextInt() % 2 == 0) ? rng.nextDouble() * velocityTweak : -rng.nextDouble() * velocityTweak);

        itemEntity.setVelocity(velocity);
        itemEntity.setPickupDelay(pickupDelay);
        itemEntity.setGlowing(true);

        // PRETTY EFFECTS
        world.spawnParticle(Particle.CLOUD, location, particleCount, 0, 0, 0);
        world.spawnParticle(Particle.SPELL_MOB, location, particleCount, 0, 0, 0);
        world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, volume, rng.nextFloat());

        if (drops.size() <= 0) {
            // NO MORE DROPS, STOP THIS TASK
            this.cancel();
        }
    }
}
