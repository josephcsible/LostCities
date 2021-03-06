package mcjty.lostcities;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.varia.CustomTeleporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Random;

public class ForgeEventHandlers {

    @SubscribeEvent
    public void onCreateSpawnPoint(WorldEvent.CreateSpawnPosition event) {
        World world = event.getWorld();
        if (!world.isRemote) {

            LostCityProfile profile = WorldTypeTools.getProfile(world);
            Biome spawnBiome = null;
            if (!profile.SPAWN_BIOME.isEmpty()) {
                spawnBiome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(profile.SPAWN_BIOME));
                if (spawnBiome == null) {
                    LostCities.logger.error("Cannot find biome '" + profile.SPAWN_BIOME + "' for the player to spawn in !");
                }
            }

            // Potentially set the spawn point
            switch (profile.LANDSCAPE_TYPE) {
                case DEFAULT:
                case CAVERN:
                    if (spawnBiome != null) {
                        findSafeSpawnPoint(world, spawnBiome);
                        event.setCanceled(true);
                    }
                    break;
                case FLOATING:
                case SPACE:
                    findSafeSpawnPoint(world, spawnBiome);
                    event.setCanceled(true);
                    break;
            }
        }

    }
    private void findSafeSpawnPoint(World world, @Nullable Biome spawnBiome) {
        Random rand = new Random(world.getSeed());
        rand.nextFloat();
        rand.nextFloat();
        int radius = 200;
        int attempts = 0;
        int bottom = world.getWorldType().getMinimumSpawnHeight(world);
        while (true) {
            for (int i = 0 ; i < 200 ; i++) {
                int x = rand.nextInt(radius * 2) - radius;
                int z = rand.nextInt(radius * 2) - radius;
                attempts++;

                if (spawnBiome != null) {
                    Biome biome = world.getBiome(new BlockPos(x, 128, z));
                    if (spawnBiome != biome) {
                        continue;
                    }
                }

                for (int y = bottom ; y < 150 ; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (state.getBlock().isTopSolid(state) && state.getBlock().isFullCube(state) && state.getBlock().isOpaqueCube(state) && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2))) {
                        world.setSpawnPoint(pos.up());
                        return;
                    }
                }
            }
            radius += 100;
        }
    }

    @SubscribeEvent
    public void onPlayerSleepInBedEvent(PlayerSleepInBedEvent event) {
        if (LostCityConfiguration.DIMENSION_ID == -1) {
            return;
        }

        World world = event.getEntityPlayer().getEntityWorld();
        if (world.isRemote) {
            return;
        }
        BlockPos bedLocation = event.getPos();
        IBlockState state = world.getBlockState(bedLocation);
        if (!(state.getBlock() instanceof BlockBed)) {
            return;
        }
        EnumFacing direction = Blocks.BED.getBedDirection(state, world, bedLocation);
        Block b1 = world.getBlockState(bedLocation.down()).getBlock();
        Block b2 = world.getBlockState(bedLocation.offset(direction.getOpposite()).down()).getBlock();

        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(LostCityConfiguration.SPECIAL_BED_BLOCK));
        if (b1 == b && b2 == b) {
            // Check if the bed is surrounded by 6 skulls
            if (world.getBlockState(bedLocation.offset(direction)).getBlock() != Blocks.SKULL) {
                return;
            }
            if (world.getBlockState(bedLocation.offset(direction.rotateY())).getBlock() != Blocks.SKULL) {
                return;
            }
            if (world.getBlockState(bedLocation.offset(direction.rotateYCCW())).getBlock() != Blocks.SKULL) {
                return;
            }
            if (world.getBlockState(bedLocation.offset(direction.getOpposite(), 2)).getBlock() != Blocks.SKULL) {
                return;
            }
            if (world.getBlockState(bedLocation.offset(direction.getOpposite()).offset(direction.getOpposite().rotateY())).getBlock() != Blocks.SKULL) {
                return;
            }
            if (world.getBlockState(bedLocation.offset(direction.getOpposite()).offset(direction.getOpposite().rotateYCCW())).getBlock() != Blocks.SKULL) {
                return;
            }

            if (world.provider.getDimension() == LostCityConfiguration.DIMENSION_ID) {
                event.setResult(Event.Result.DENY);
                BlockPos top = DimensionManager.getWorld(0).getTopSolidOrLiquidBlock(bedLocation);
                CustomTeleporter.teleportToDimension(event.getEntityPlayer(), 0, top);
            } else {
                event.setResult(Event.Result.DENY);
                WorldServer worldServer = event.getEntity().getEntityWorld().getMinecraftServer().getWorld(LostCityConfiguration.DIMENSION_ID);
                BlockPos top = worldServer.getTopSolidOrLiquidBlock(bedLocation);
                CustomTeleporter.teleportToDimension(event.getEntityPlayer(), LostCityConfiguration.DIMENSION_ID, top);
            }
        }
    }
}
