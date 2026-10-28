/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class CollisionUtil {

    private static final Vector DOWN_VECTOR = new Vector(0, -1, 0);

    private CollisionUtil() {
    }

    /**
     * High performance ground distance calculation.
     * Iterates through the block column directly underneath the player's bounding box
     * and finds the exact top Y of any solid collision shape without running heavy ray-traces.
     *
     * @param player      the player
     * @param maxDistance maximum distance to inspect
     * @return distance from bottom of player bounding box to the highest solid surface directly below
     */
    public static double getFastDistanceToGround(Player player, double maxDistance) {
        BoundingBox bb = player.getBoundingBox();
        double minY = bb.getMinY();
        World world = player.getWorld();

        // 0.001 margin prevents checking neighboring blocks if player is exactly on block border
        int minBlockX = (int) Math.floor(bb.getMinX() + 0.001);
        int maxBlockX = (int) Math.floor(bb.getMaxX() - 0.001);
        int minBlockZ = (int) Math.floor(bb.getMinZ() + 0.001);
        int maxBlockZ = (int) Math.floor(bb.getMaxZ() - 0.001);

        int startY = (int) Math.floor(minY);
        int bottomY = (int) Math.floor(minY - maxDistance);

        double highestCollisionY = minY - maxDistance;

        for (int y = startY; y >= bottomY; y--) {
            boolean foundSolidInLayer = false;

            for (int x = minBlockX; x <= maxBlockX; x++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material mat = block.getType();

                    if (mat.isAir() || block.isPassable()) {
                        continue;
                    }

                    if (mat == Material.WATER || mat == Material.LAVA ||
                            mat == Material.COBWEB || mat == Material.SCAFFOLDING) {
                        continue;
                    }

                    BoundingBox blockBB = block.getBoundingBox();
                    double blockTop = blockBB.getMaxY();

                    // Only consider surfaces at or below the player's feet
                    if (blockTop <= minY + 0.001 && blockTop > highestCollisionY) {
                        highestCollisionY = blockTop;
                        foundSolidInLayer = true;
                    }
                }
            }

            // Early exit optimization: if we found a solid surface in this layer and it's higher
            // than any block in the layer below, no lower layer can beat it.
            if (foundSolidInLayer && highestCollisionY >= y) {
                break;
            }
        }

        return Math.max(0.0, minY - highestCollisionY);
    }

    /**
     * Fallback standard ray-tracing from bounding box corners.
     */
    public static double getRayTraceDistanceToGround(Player player, double maxDistance) {
        BoundingBox bb = player.getBoundingBox();
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return maxDistance;

        double adjustment = 0.01;
        double minX = bb.getMinX() + adjustment;
        double maxX = bb.getMaxX() - adjustment;
        double minZ = bb.getMinZ() + adjustment;
        double maxZ = bb.getMaxZ() - adjustment;
        double y = bb.getMinY();

        double minDistance = maxDistance;

        Location[] corners = new Location[]{
                new Location(world, minX, y, minZ),
                new Location(world, minX, y, maxZ),
                new Location(world, maxX, y, minZ),
                new Location(world, maxX, y, maxZ)
        };

        for (Location corner : corners) {
            RayTraceResult result = world.rayTraceBlocks(corner, DOWN_VECTOR, maxDistance, FluidCollisionMode.NEVER, true);
            if (result != null && result.getHitPosition() != null) {
                double dist = y - result.getHitPosition().getY();
                if (dist < minDistance) {
                    minDistance = dist;
                }
            }
        }

        return Math.max(0.0, minDistance);
    }
}

