package net.pixeldreamstudios.ddjade;

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
    }
}