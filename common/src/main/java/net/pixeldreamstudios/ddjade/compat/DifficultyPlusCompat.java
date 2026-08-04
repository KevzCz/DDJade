package net.pixeldreamstudios.ddjade.compat;

import dev.architectury.platform.Platform;
import net.dungeon_difficulty.logic.EntityDifficultyScalable;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class DifficultyPlusCompat {
    private DifficultyPlusCompat() {}

    private static boolean available = Platform.isModLoaded("difficultyplus");

    public static boolean isLoaded() {
        return available;
    }

    public static Optional<ZoneInfo> zoneAt(LivingEntity entity) {
        if (!isLoaded() || !(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        try {
            BlockPos pos = scaledAt(entity).orElseGet(entity::blockPosition);
            Optional<ZoneContext> zone = DifficultyPlusAPI.zoneAt(level, pos);
            if (zone.isEmpty()) {
                return Optional.empty();
            }
            ZoneContext context = zone.get();
            return Optional.of(new ZoneInfo(
                    context.difficulty(),
                    context.zoneLevel(),
                    context.structureId().map(Object::toString).orElse(null)));
        } catch (Throwable t) {
            available = false;
            return Optional.empty();
        }
    }

    public static List<ResistanceInfo> resistancesOf(LivingEntity entity) {
        if (!isLoaded() || !(entity.level() instanceof ServerLevel)) {
            return List.of();
        }
        try {
            List<ResistanceInfo> out = new ArrayList<>();
            for (DifficultyPlusAPI.ResistanceInfo info : DifficultyPlusAPI.resistancesOf(entity)) {
                out.add(new ResistanceInfo(
                        info.damageId().toString(),
                        info.isTag(),
                        info.percent(),
                        info.flat(),
                        info.isVulnerability(),
                        info.displayKey(),
                        info.displayText()));
            }
            return out;
        } catch (Throwable t) {
            available = false;
            return List.of();
        }
    }

    public static OptionalInt levelAt(ServerLevel level, BlockPos pos) {
        if (!isLoaded()) {
            return OptionalInt.empty();
        }
        try {
            return DifficultyPlusAPI.levelAt(level, pos);
        } catch (Throwable t) {
            available = false;
            return OptionalInt.empty();
        }
    }

    private static Optional<BlockPos> scaledAt(LivingEntity entity) {
        try {
            PatternMatching.LocationData data =
                    ((EntityDifficultyScalable) entity).getScalingLocationData();
            return data == null ? Optional.empty() : Optional.ofNullable(data.position());
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static OptionalInt levelGap(ServerPlayer viewer, LivingEntity mob) {
        if (!isLoaded() || viewer == null || mob == null) {
            return OptionalInt.empty();
        }
        try {
            return DifficultyPlusAPI.levelGap(viewer, mob);
        } catch (Throwable t) {
            available = false;
            return OptionalInt.empty();
        }
    }

    public record ZoneInfo(String difficulty, int level, String structureId) {}

    public record ResistanceInfo(String damageId, boolean isTag, float percent, float flat,
                                 boolean isVulnerability, String displayKey, String displayText) {}
}
