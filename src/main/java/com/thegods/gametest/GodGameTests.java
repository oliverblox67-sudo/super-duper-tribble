package com.thegods.gametest;

import com.thegods.TheGods;
import com.thegods.god.Ability;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.god.Gods;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Test automatici lato server: ./gradlew runGameTestServer
 * Verificano che ogni potere si lanci senza errori e che i passivi funzionino.
 */
@GameTestHolder(TheGods.MODID)
@PrefixGameTestTemplate(false)
public class GodGameTests {

    private static ServerPlayer god(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(4.5, 1, 1.5));
        p.teleportTo(pos.x, pos.y, pos.z);
        GodManager.grant(p, Gods.POSEIDON);
        GodManager.toggleManifest(p);
        return p;
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void poseidonCastsEveryPower(GameTestHelper helper) {
        ServerPlayer p = god(helper);
        GodData data = GodManager.data(p);
        helper.assertTrue(data.isManifested(), "Poseidone dovrebbe essere manifestato");
        helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) >= 40, "La forma divina deve dare +20 vita");
        helper.assertTrue(p.getAbilities().mayfly, "La forma divina deve concedere il volo");

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 6));
        List<Ability> abilities = Gods.POSEIDON.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            final int index = i;
            helper.runAfterDelay(5 + i * 25L, () -> {
                p.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.isAlive() ? zombie.getEyePosition() : p.getEyePosition().add(0, 0, 5));
                data.clearCooldowns();
                data.setEnergy(100);
                GodManager.useAbility(p, index);
                Ability a = abilities.get(index);
                boolean needsTarget = a.id().equals("poseidon_prison") && !zombie.isAlive();
                if (!needsTarget) {
                    helper.assertTrue(data.cooldown(a.id()) > 0, "Il potere " + a.id() + " non è stato lanciato");
                }
            });
        }
        helper.runAfterDelay(5 + abilities.size() * 25L + 20, () -> {
            helper.assertTrue(p.isAlive(), "Poseidone non deve morire per i propri poteri");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void eternalTideSavesFromDeath(GameTestHelper helper) {
        ServerPlayer p = god(helper);
        p.hurt(p.damageSources().genericKill(), 10_000f);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(p.isAlive() && p.getHealth() > 0, "Marea Eterna doveva salvare Poseidone");
            helper.assertTrue(GodManager.data(p).escapeCooldown() > 0, "Marea Eterna deve andare in ricarica");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void revokeRemovesForm(GameTestHelper helper) {
        ServerPlayer p = god(helper);
        GodManager.revoke(p);
        helper.assertTrue(!GodManager.data(p).hasGod(), "La divinità doveva essere rimossa");
        helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == 20, "La vita massima deve tornare a 20");
        // (il giocatore finto dei test è sempre in creativa, quindi controlliamo il flag interno)
        helper.assertTrue(!GodManager.data(p).grantedFlight(), "Il volo divino deve essere rimosso");
        helper.succeed();
    }
}
