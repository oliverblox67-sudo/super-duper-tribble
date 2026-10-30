package com.thegods.god;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Un modificatore di attributo concesso dalla forma divina. */
public record DivineModifier(Holder<Attribute> attribute, AttributeModifier modifier) {}
