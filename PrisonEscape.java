package pe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PrisonEscape implements ModInitializer {
    static BlockPos center;
    static final int R = 15;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((d, r, e) -> d.register(
            CommandManager.literal("prison").executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                build(c.getSource().getWorld(), p);
                return 1;
            })));

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (center == null || s.getTicks() % 20 != 0) return;
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                if (!p.getCommandTags().contains("prisoner")) continue;
                if (Math.abs(p.getX() - center.getX()) > R + 2 || Math.abs(p.getZ() - center.getZ()) > R + 2) {
                    p.removeCommandTag("prisoner");
                    p.sendMessage(Text.literal("§a§lТЫ СБЕЖАЛ! Побег удался!"), false);
                }
            }
        });
    }

    static void set(ServerWorld w, BlockPos o, int x, int y, int z, Block b) {
        w.setBlockState(o.add(x, y, z), b.getDefaultState());
    }

    static void build(ServerWorld w, ServerPlayerEntity p) {
        BlockPos o = p.getBlockPos().add(0, 0, 0);
        center = o;
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            set(w, o, x, -1, z, Blocks.STONE_BRICKS);
            for (int y = 0; y <= 8; y++) {
                boolean wall = Math.abs(x) == R || Math.abs(z) == R;
                set(w, o, x, y, z, wall ? Blocks.STONE_BRICKS : Blocks.AIR);
            }
        }
        // камера 7x7 в центре
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (Math.abs(x) == 3 || Math.abs(z) == 3)
                for (int y = 0; y <= 3; y++) set(w, o, x, y, z, Blocks.STONE_BRICKS);
            set(w, o, x, 4, z, Blocks.STONE_BRICKS);
        }
        // решётка-дверь и окно
        set(w, o, 3, 0, 0, Blocks.IRON_BARS); set(w, o, 3, 1, 0, Blocks.IRON_BARS);
        set(w, o, -3, 2, 0, Blocks.IRON_BARS);
        // койка и факел
        set(w, o, -2, 0, -2, Blocks.RED_BED);
        set(w, o, 2, 2, 2, Blocks.LANTERN);

        // охрана
        int[][] g = {{8, 8}, {-8, 8}, {8, -8}, {-8, -8}};
        for (int[] q : g) {
            var m = EntityType.VINDICATOR.spawn(w, o.add(q[0], 0, q[1]), SpawnReason.COMMAND);
            if (m instanceof MobEntity mob) {
                mob.setCustomName(Text.literal("§cОхранник"));
                mob.setPersistent();
                mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            }
        }

        p.requestTeleport(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        p.addCommandTag("prisoner");
        ItemStack spoon = new ItemStack(Items.WOODEN_PICKAXE);
        spoon.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Ложка"));
        p.giveItemStack(spoon);
        p.sendMessage(Text.literal("§6Ты в тюрьме. Выкопай стену камеры и выберись за внешнюю стену. Охрана злая!"), false);
    }
}
