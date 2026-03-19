package net.pixeldreamstudios.ddjade;

import net.dungeon_difficulty.logic.EntityDifficultyScalable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public enum DDPowerLevelProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation CONFIG_SHOW_DETAILS = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_attribute_details");
    private static final String NBT_KEY = "PowerLevel";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof LivingEntity living) {
            EntityDifficultyScalable scalable = (EntityDifficultyScalable) living;
            int level = scalable.getScalingLevel();
            if (level > 0) {
                data.putInt(NBT_KEY, level);
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(NBT_KEY)) {
            return;
        }

        int level = serverData.getInt(NBT_KEY);
        if (level > 0) {
            tooltip.add(Component.literal("Power Level: " + level).withStyle(ChatFormatting.GOLD));

            if (config.get(CONFIG_SHOW_DETAILS) && accessor.getEntity() instanceof LivingEntity living) {
                List<AttributeInfo> modifiedAttributes = getModifiedAttributes(living);

                if (!modifiedAttributes.isEmpty()) {
                    for (AttributeInfo attrInfo : modifiedAttributes) {
                        tooltip.add(formatAttributeLine(attrInfo));
                    }
                }
            }
        }
    }

    private List<AttributeInfo> getModifiedAttributes(LivingEntity entity) {
        List<AttributeInfo> attributes = new ArrayList<>();

        for (AttributeInstance instance : entity.getAttributes().getSyncableAttributes()) {
            Holder<Attribute> attributeHolder = instance.getAttribute();
            Attribute attribute = attributeHolder.value();
            double baseValue = instance.getBaseValue();

            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.id().toString().contains("dungeon_difficulty")) {
                    attributes.add(new AttributeInfo(attribute, modifier, baseValue));
                    break;
                }
            }
        }

        return attributes;
    }

    private Component formatAttributeLine(AttributeInfo info) {
        String translationKey = info.attribute.getDescriptionId();
        Component attributeName = Component.translatable(translationKey);

        double modifierAmount = info.modifier.amount();
        double displayValue;
        String formatString;

        switch (info.modifier.operation()) {
            case ADD_VALUE -> {
                displayValue = modifierAmount;
                formatString = "%s%.1f";
            }
            case ADD_MULTIPLIED_BASE -> {
                displayValue = modifierAmount * 100;
                formatString = "%s%.1f%%";
            }
            case ADD_MULTIPLIED_TOTAL -> {
                displayValue = modifierAmount * 100;
                formatString = "%s%.1f%%";
            }
            default -> {
                displayValue = modifierAmount;
                formatString = "%s%.1f";
            }
        }

        String sign = displayValue > 0 ? "+" : "";
        ChatFormatting color = displayValue > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;

        Component valueComponent = Component.literal(String.format(formatString, sign, displayValue))
                .withStyle(color);

        return Component.literal("  ")
                .append(attributeName.copy().withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "))
                .append(valueComponent);
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "power_level");
    }

    private record AttributeInfo(Attribute attribute, AttributeModifier modifier, double baseValue) {
    }
}