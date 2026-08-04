package net.pixeldreamstudios.ddjade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DDJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(DDPowerLevelProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(DDPowerLevelProvider.INSTANCE, LivingEntity.class);

        registration.addConfig(
                ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_attribute_details"),
                false
        );

        registration.addConfig(
                ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_zone"),
                true
        );

        registration.addConfig(
                ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "show_resistances"),
                true
        );

        registration.addConfig(
                ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "colour_level_by_threat"),
                true
        );

        registration.addConfig(DDPowerLevelProvider.CONFIG_DEADLY_GAP, "10",
                DDPowerLevelProvider::validGap);
        registration.addConfig(DDPowerLevelProvider.CONFIG_DEADLY_COLOUR,
                DDPowerLevelProvider.DEFAULT_DEADLY_COLOUR, DDPowerLevelProvider::validColour);

        registration.addConfig(DDPowerLevelProvider.CONFIG_HARD_GAP, "5",
                DDPowerLevelProvider::validGap);
        registration.addConfig(DDPowerLevelProvider.CONFIG_HARD_COLOUR,
                DDPowerLevelProvider.DEFAULT_HARD_COLOUR, DDPowerLevelProvider::validColour);

        registration.addConfig(DDPowerLevelProvider.CONFIG_EVEN_COLOUR,
                DDPowerLevelProvider.DEFAULT_EVEN_COLOUR, DDPowerLevelProvider::validColour);

        registration.addConfig(DDPowerLevelProvider.CONFIG_EASY_GAP, "5",
                DDPowerLevelProvider::validGap);
        registration.addConfig(DDPowerLevelProvider.CONFIG_EASY_COLOUR,
                DDPowerLevelProvider.DEFAULT_EASY_COLOUR, DDPowerLevelProvider::validColour);

        Component category = Component.translatable("config.jade.plugin_ddjade.threat_category");
        registration.setConfigCategoryOverride(
                ResourceLocation.fromNamespaceAndPath(DDJade.MOD_ID, "colour_level_by_threat"),
                category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_DEADLY_GAP, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_DEADLY_COLOUR, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_HARD_GAP, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_HARD_COLOUR, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_EVEN_COLOUR, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_EASY_GAP, category);
        registration.setConfigCategoryOverride(DDPowerLevelProvider.CONFIG_EASY_COLOUR, category);
    }
}