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
     * Calculates the distance traveled upward with deceleration.
     */
    public static double calculateDistanceTraveled(double velocity, int time, double gravity) {
        double totalDistance = 0.0;
        double currentVelocity = velocity;

        for (int i = 0; i < time; i++) {
            totalDistance += currentVelocity;
            currentVelocity = (currentVelocity - gravity) * DRAG;
            if (currentVelocity > TERMINAL_VELOCITY) {
                currentVelocity = TERMINAL_VELOCITY;
            }
        }

        return totalDistance;
    }

    /**
     * Calculates the number of ticks required to fall a given distance under gravity.
     * Returns -1 if it exceeds MAX_TICKS limit.
     */
    public static int calculateFallTime(double initialVelocity, double distance, double gravity) {
        double velocity = Math.abs(initialVelocity);
        double remainingDistance = distance;
        int ticks = 0;

        while (remainingDistance > 0.0) {
            if (ticks > MAX_TICKS) {
                return -1;
            }

            velocity += gravity;
            if (velocity > TERMINAL_VELOCITY) {
                velocity = TERMINAL_VELOCITY;
            }
            velocity *= DRAG;
            remainingDistance -= velocity;
            ticks++;
        }

        return ticks;
    }

    /**
     * Calculates the time in ticks to reach maximum height from vertical velocity.
     */
    public static int calculateTimeToMaxVelocity(double targetVerticalVelocity, double gravity) {
        double a = -gravity * DRAG;
        double b = gravity + TERMINAL_VELOCITY * DRAG;
        double c = -2.0 * targetVerticalVelocity;

        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) {
            return 0;
        }

        double positiveRoot = (-b + Math.sqrt(discriminant)) / (2.0 * a);
        return (int) Math.ceil(positiveRoot * 20.0);
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

