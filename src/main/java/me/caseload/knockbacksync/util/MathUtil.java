/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.util;

public final class MathUtil {

    private static final double TERMINAL_VELOCITY = 3.92;
    private static final double DRAG = 0.98;
    private static final int MAX_TICKS = 20;

    private MathUtil() {
    }

    /**
     * Calculates the distance traveled upward with deceleration until apex or specified ticks.
     */
    public static double calculateDistanceTraveled(double initialUpwardVelocity, int time, double gravity) {
        if (initialUpwardVelocity <= 0.0 || time <= 0) {
            return 0.0;
        }
        double totalDistance = 0.0;
        double currentVelocity = initialUpwardVelocity;

        for (int i = 0; i < time && currentVelocity > 0.0; i++) {
            totalDistance += currentVelocity;
            currentVelocity = (currentVelocity - gravity) * DRAG;
        }

        return totalDistance;
    }

    /**
     * Calculates the number of ticks required to fall a given distance under gravity and drag.
     * Uses discrete Minecraft simulation.
     * Returns -1 if it exceeds MAX_TICKS limit.
     */
    public static int calculateFallTime(double initialDownwardVelocity, double distance, double gravity) {
        if (distance <= 0.0) {
            return 0;
        }
        double velocity = Math.max(0.0, initialDownwardVelocity);
        double remainingDistance = distance;
        int ticks = 0;

        while (remainingDistance > 0.0) {
            if (ticks >= MAX_TICKS) {
                return -1;
            }

            remainingDistance -= velocity;
            velocity = (velocity + gravity) * DRAG;
            if (velocity > TERMINAL_VELOCITY) {
                velocity = TERMINAL_VELOCITY;
            }
            ticks++;
        }

        return ticks;
    }

    /**
     * Calculates the time in ticks to reach apex (vertical velocity <= 0) using exact Minecraft discrete physics.
     */
    public static int calculateTimeToMaxVelocity(double initialUpwardVelocity, double gravity) {
        if (initialUpwardVelocity <= 0.0) {
            return 0;
        }
        double v = initialUpwardVelocity;
        int ticks = 0;
        while (v > 0.0 && ticks < MAX_TICKS) {
            v = (v - gravity) * DRAG;
            ticks++;
        }
        return ticks;
    }

    public static double clamp(double num, double min, double max) {
        if (num < min) return min;
        return Math.min(num, max);
    }

    public static long clamp(long num, long min, long max) {
        if (num < min) return min;
        return Math.min(num, max);
    }
}

