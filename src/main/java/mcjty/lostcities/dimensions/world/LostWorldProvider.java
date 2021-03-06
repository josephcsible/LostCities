package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.ModDimensions;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nonnull;

public class LostWorldProvider extends WorldProvider {

    @Override
    @Nonnull
    public DimensionType getDimensionType() {
        return ModDimensions.lostDimensionType;
    }

    @Override
    @Nonnull
    public String getSaveFolder() {
        return "LOST";
    }

    @Override
    @Nonnull
    public IChunkGenerator createChunkGenerator() {
        return new LostCityChunkGenerator(world, (world.getSeed() >> 3) ^ 34328884229L);
    }

    private BiomeProvider getInternalBiomeProvider(World world) {
        if (biomeProvider == null) {
            for (WorldType type : WorldType.WORLD_TYPES) {
                if ("BIOMESOP".equals(type.getName())) {
                    WorldType orig = world.getWorldInfo().getTerrainType();
                    world.getWorldInfo().setTerrainType(type);
                    biomeProvider = type.getBiomeProvider(world);
                    world.getWorldInfo().setTerrainType(orig);
                    break;
                }
            }
        }
        return biomeProvider;
    }

    @Override
    protected void init() {
        super.init();

        LostCityProfile profile = LostCityConfiguration.profiles.get(LostCityConfiguration.DIMENSION_PROFILE);
        if (profile == null) {
            profile = WorldTypeTools.getProfile(world);
        }
        BiomeProvider biomeProvider;
        if (LostCities.biomesoplenty && LostCityConfiguration.DIMENSION_BOP) {
            biomeProvider = getInternalBiomeProvider(world);
        } else {
            biomeProvider = new BiomeProvider(world.getWorldInfo());
        }
        if (profile.ALLOWED_BIOME_FACTORS.length == 0) {
            this.biomeProvider = biomeProvider;
        } else {
            String[] outsideAllowedbiomeFactors = profile.ALLOWED_BIOME_FACTORS;
            if (profile.isSpace() && profile.CITYSPHERE_LANDSCAPE_OUTSIDE && !profile.CITYSPHERE_OUTSIDE_PROFILE.isEmpty()) {
                LostCityProfile outProfile = LostCityConfiguration.profiles.get(profile.CITYSPHERE_OUTSIDE_PROFILE);
                outsideAllowedbiomeFactors = outProfile.ALLOWED_BIOME_FACTORS;
            }
            this.biomeProvider = new LostWorldFilteredBiomeProvider(world, biomeProvider,
                    profile.ALLOWED_BIOME_FACTORS,
                    outsideAllowedbiomeFactors);
        }
    }

}
