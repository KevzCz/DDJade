package net.pixeldreamstudios.ddjade;

import net.dungeon_difficulty.logic.EntityDifficultyScalable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.ddjade.compat.DifficultyPlusCompat;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public enum DDPowerLevelProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation CONFIG_SHOW_DETAILS = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_attribute_details");
    private static final ResourceLocation CONFIG_SHOW_ZONE = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_zone");
    private static final ResourceLocation CONFIG_SHOW_RESISTANCES = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_resistances");
    private static final ResourceLocation CONFIG_COLOUR_THREAT = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "colour_level_by_threat");
    private static final String NBT_KEY = "PowerLevel";
    private static final String NBT_ZONE = "DpZone";
    private static final String NBT_ZONE_LEVEL = "DpZoneLevel";
    private static final String NBT_RESISTANCES = "DpResistances";
    private static final String NBT_GAP = "DpLevelGap";

    public static final ResourceLocation CONFIG_DEADLY_GAP = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "deadly_gap");
    public static final ResourceLocation CONFIG_HARD_GAP = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "hard_gap");
    public static final ResourceLocation CONFIG_EASY_GAP = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "easy_gap");
    public static final ResourceLocation CONFIG_DEADLY_COLOUR = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "deadly_colour");
    public static final ResourceLocation CONFIG_HARD_COLOUR = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "hard_colour");
    public static final ResourceLocation CONFIG_EVEN_COLOUR = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "even_colour");
    public static final ResourceLocation CONFIG_EASY_COLOUR = ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "easy_colour");

    public static final String DEFAULT_DEADLY_COLOUR = "#FF5555";
    public static final String DEFAULT_HARD_COLOUR = "#FFAA00";
    public static final String DEFAULT_EVEN_COLOUR = "#FFFFFF";
    public static final String DEFAULT_EASY_COLOUR = "#55FF55";

    public static boolean validGap(String value) {
        return parseGap(value, Integer.MIN_VALUE) != Integer.MIN_VALUE;
    }

    public static boolean validColour(String value) {
        return parseColour(value, -1) != -1;
    }

    private static int parseGap(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.abs(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseColour(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }


    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof LivingEntity living) {
            EntityDifficultyScalable scalable = (EntityDifficultyScalable) living;
            int level = scalable.getScalingLevel();
            if (level > 0) {
                data.putInt(NBT_KEY, level);
            }

            if (accessor.getPlayer() instanceof ServerPlayer viewer) {
                DifficultyPlusCompat.levelGap(viewer, living)
                        .ifPresent(gap -> data.putInt(NBT_GAP, gap));
            }

            if (DifficultyPlusCompat.isLoaded()) {
                DifficultyPlusCompat.zoneAt(living).ifPresent(zone -> {
                    data.putString(NBT_ZONE, zone.difficulty());
                    data.putInt(NBT_ZONE_LEVEL, zone.level());
                });

                List<DifficultyPlusCompat.ResistanceInfo> resistances =
                        DifficultyPlusCompat.resistancesOf(living);
                if (!resistances.isEmpty()) {
                    ListTag list = new ListTag();
                    for (DifficultyPlusCompat.ResistanceInfo info : resistances) {
                        CompoundTag entry = new CompoundTag();
                        entry.putString("id", info.damageId());
                        entry.putBoolean("tag", info.isTag());
                        entry.putFloat("pct", info.percent());
                        entry.putFloat("flat", info.flat());
                        if (info.displayKey() != null && !info.displayKey().isEmpty()) {
                            entry.putString("key", info.displayKey());
                        }
                        if (info.displayText() != null && !info.displayText().isEmpty()) {
                            entry.putString("text", info.displayText());
                        }
                        list.add(entry);
                    }
                    data.put(NBT_RESISTANCES, list);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();

        int level = serverData.getInt(NBT_KEY);
        if (level > 0) {
            tooltip.add(Component.literal("Power Level: " + level)
                    .withStyle(style -> style.withColor(
                            config.get(CONFIG_COLOUR_THREAT) && serverData.contains(NBT_GAP)
                                    ? bandColour(serverData.getInt(NBT_GAP), config)
                                    : ChatFormatting.GOLD.getColor())));

            if (config.get(CONFIG_SHOW_DETAILS) && accessor.getEntity() instanceof LivingEntity living) {
                List<AttributeInfo> modifiedAttributes = getModifiedAttributes(living);

                if (!modifiedAttributes.isEmpty()) {
                    for (AttributeInfo attrInfo : modifiedAttributes) {
                        tooltip.add(formatAttributeLine(attrInfo));
                    }
                }
            }
        }

        if (config.get(CONFIG_SHOW_ZONE) && serverData.contains(NBT_ZONE)) {
            String zone = serverData.getString(NBT_ZONE);
            int zoneLevel = serverData.getInt(NBT_ZONE_LEVEL);
            tooltip.add(Component.literal("Zone: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(zone).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" " + zoneLevel).withStyle(ChatFormatting.AQUA)));
        }

        if (config.get(CONFIG_SHOW_RESISTANCES) && serverData.contains(NBT_RESISTANCES)) {
            ListTag list = serverData.getList(NBT_RESISTANCES, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                tooltip.add(formatResistanceLine(list.getCompound(i)));
            }
        }
    }

    private static Component formatResistanceLine(CompoundTag entry) {
        String id = entry.getString("id");
        boolean isTag = entry.getBoolean("tag");
        float percent = entry.getFloat("pct");
        float flat = entry.getFloat("flat");

        Component name = damageName(entry, id, isTag);

        boolean vulnerable = percent < 0f || flat < 0f;
        ChatFormatting colour = vulnerable ? ChatFormatting.RED : ChatFormatting.GREEN;

        StringBuilder value = new StringBuilder();
        value.append(vulnerable ? "takes " : "resists ");

        if (percent != 0f) {
            int shown = Math.abs(Math.round(percent * 100f));
            if (vulnerable) {
                value.append('+').append(shown).append('%');
            } else if (shown >= 100) {
                value.setLength(0);
                value.append("immune");
            } else {
                value.append(shown).append('%');
            }
        }
        if (flat != 0f) {
            if (value.length() > 0 && !value.toString().endsWith(" ")) {
                value.append(", ");
            }
            value.append(String.format("%s%.1f", vulnerable ? "+" : "-", Math.abs(flat)));
        }

        return Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(name.copy().withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value.toString()).withStyle(colour));
    }

    private static int bandColour(int gap, IPluginConfig config) {
        if (gap <= -parseGap(config.getString(CONFIG_DEADLY_GAP), 10)) {
            return parseColour(config.getString(CONFIG_DEADLY_COLOUR),
                    parseColour(DEFAULT_DEADLY_COLOUR, 0xFF5555));
        }
        if (gap <= -parseGap(config.getString(CONFIG_HARD_GAP), 5)) {
            return parseColour(config.getString(CONFIG_HARD_COLOUR),
                    parseColour(DEFAULT_HARD_COLOUR, 0xFFAA00));
        }
        if (gap >= parseGap(config.getString(CONFIG_EASY_GAP), 5)) {
            return parseColour(config.getString(CONFIG_EASY_COLOUR),
                    parseColour(DEFAULT_EASY_COLOUR, 0x55FF55));
        }
        return parseColour(config.getString(CONFIG_EVEN_COLOUR),
                parseColour(DEFAULT_EVEN_COLOUR, 0xFFFFFF));
    }

    private static Component damageName(CompoundTag entry, String id, boolean isTag) {
        String key = entry.getString("key");
        if (!key.isEmpty()) {
            return Component.translatable(key);
        }
        String text = entry.getString("text");
        if (!text.isEmpty()) {
            return Component.literal(text);
        }

        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String shipped = "difficultyplus.damagetype." + path.replace('/', '.');
        if (I18n.exists(shipped)) {
            return Component.translatable(shipped);
        }

        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        if (isTag && path.startsWith("is_")) {
            path = path.substring(3);
        }
        path = path.replace('_', ' ');
        if (!path.isEmpty()) {
            path = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return Component.literal(path);
    }

    private List<AttributeInfo> getModifiedAttributes(LivingEntity entity) {
        List<AttributeInfo> attributes = new ArrayList<>();

        for (AttributeInstance instance : entity.getAttributes().getSyncableAttributes()) {
            Holder<Attribute> attributeHolder = instance.getAttribute();
            Attribute attribute = attributeHolder.value();
            double baseValue = instance.getBaseValue();
            Map<MergeKey, Double> merged = new LinkedHashMap<>();

            for (AttributeModifier modifier : instance.getModifiers()) {
                String source = sourceOf(modifier);
                if (source != null) {
                    merged.merge(new MergeKey(modifier.operation(), source), modifier.amount(), Double::sum);
                }
            }
            merged.forEach((key, amount) -> attributes.add(new AttributeInfo(attribute,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, MERGED_ID), amount, key.operation()), baseValue, key.source())));
        }

        return attributes;
    }

    private record MergeKey(AttributeModifier.Operation operation, String source) {
    }

    private static final String MERGED_ID = "merged";

    private static String sourceOf(AttributeModifier modifier) {
        String id = modifier.id().toString();
        if (id.contains("dungeon_difficulty")) {
            return SOURCE_DD;
        }
        if (id.contains("difficultyplus")) {
            return SOURCE_DP;
        }
        return null;
    }

    private static final String SOURCE_DD = "dungeon_difficulty";
    private static final String SOURCE_DP = "difficultyplus";

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

        var line = Component.literal("  ")
                .append(attributeName.copy().withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": "))
                .append(valueComponent);

        if (SOURCE_DP.equals(info.source())) {
            line.append(Component.literal(" [D+]").withStyle(ChatFormatting.DARK_AQUA));
        }
        return line;
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "power_level");
    }

    private record AttributeInfo(Attribute attribute, AttributeModifier modifier, double baseValue,
                                 String source) {
    }
}