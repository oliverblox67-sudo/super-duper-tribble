package com.thegods.gametest;

import com.thegods.TheGods;
import com.thegods.god.Ability;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.god.Gods;
import com.thegods.god.Limb;
import com.thegods.god.LimbManager;
import com.thegods.god.LimbState;
import com.thegods.entity.WaterGolemEntity;
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

    @GameTest(template = "empty", timeoutTicks = 420)
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

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void titanFightsAndCanBeRidden(GameTestHelper helper) {
        ServerPlayer p = god(helper);
        GodData data = GodManager.data(p);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 7));
        GodManager.useAbility(p, indexOf("poseidon_titan"));
        helper.runAfterDelay(5, () -> {
            var golems = p.level().getEntitiesOfClass(WaterGolemEntity.class, p.getBoundingBox().inflate(20));
            helper.assertTrue(golems.size() == 1, "Doveva esserci esattamente un Titano");
            WaterGolemEntity golem = golems.get(0);
            helper.assertTrue(golem.isOwnedBy(p), "Il Titano deve appartenere all'evocatore");
            helper.assertTrue(!golem.canAttack(p), "Il Titano non deve attaccare il suo evocatore");
            helper.assertTrue(golem.canAttack(zombie), "Il Titano deve poter attaccare i mostri");
            p.startRiding(golem);
            helper.assertTrue(golem.getControllingPassenger() == p, "Il proprietario deve guidare il Titano");
            GodManager.useAbility(p, 0); // in groppa: R comanda il Titano
            helper.assertTrue(golem.attackAnim > 0 || golem.slamAnim > 0, "Il Titano doveva attaccare su comando");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void bodyForgeSeversAndForgesLimbs(GameTestHelper helper) {
        ServerPlayer p = god(helper);
        double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double damage = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        LimbManager.perform(p, Limb.LEFT_LEG, LimbManager.Action.SEVER);
        helper.assertTrue(GodManager.data(p).limb(Limb.LEFT_LEG) == LimbState.SEVERED, "La gamba doveva essere recisa");
        helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed, "Senza una gamba si va più piano");
        LimbManager.perform(p, Limb.LEFT_LEG, LimbManager.Action.FORGE);
        LimbManager.perform(p, Limb.RIGHT_LEG, LimbManager.Action.FORGE);
        LimbManager.perform(p, Limb.RIGHT_ARM, LimbManager.Action.FORGE);
        GodData d = GodManager.data(p);
        helper.assertTrue(d.limb(Limb.RIGHT_ARM) == LimbState.CONSTRUCT, "Il braccio doveva diventare d'acqua");
        helper.assertTrue(d.count(LimbState.CONSTRUCT, false) == 2, "Entrambe le gambe dovevano essere d'acqua");
        helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed, "Le gambe d'acqua sono più veloci");
        helper.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE) > damage, "Il braccio d'acqua colpisce più forte");
        for (Limb l : Limb.ALL) LimbManager.perform(p, l, LimbManager.Action.RESTORE);
        helper.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "Rigenerando tutto si torna normali");
        helper.succeed();
    }

    private static int indexOf(String id) {
        List<Ability> list = Gods.POSEIDON.abilities();
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(id)) return i;
        throw new IllegalStateException(id);
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
