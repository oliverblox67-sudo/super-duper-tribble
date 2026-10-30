package com.thegods.item;

import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** Una reliquia che concede la divinità a chi la usa (viene consumata). */
public class DivineRelicItem extends Item {
    private final Supplier<God> god;

    public DivineRelicItem(Supplier<God> god, Properties properties) {
        super(properties);
        this.god = god;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            GodData data = GodManager.data(sp);
            God g = god.get();
            if (data.is(g)) {
                sp.displayClientMessage(Component.translatable("message.thegods.already", g.displayName())
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            GodManager.grant(sp, g);
            GodManager.sendTitle(sp, "god.thegods." + g.id(), "message.thegods.ascended", g.color());
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2f, 0.6f);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8f, 1.5f);
            stack.consume(1, sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.thegods.relic.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
