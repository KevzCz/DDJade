package net.pixeldreamstudios.ddjade.fabric;

import net.fabricmc.api.ModInitializer;

import net.pixeldreamstudios.ddjade.DDJade;

public final class DDJadeFabric implements ModInitializer {
    @Override
    public void onInitialize() {

        DDJade.init();
    }
}
