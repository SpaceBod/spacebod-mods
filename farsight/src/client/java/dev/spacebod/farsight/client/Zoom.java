package dev.spacebod.farsight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Holds the zoom state and does frame-rate independent smoothing.
 * A "divisor" of 4 means the FOV is a quarter of normal, i.e. 4x magnification.
 */
public final class Zoom {
	/** Multiplicative change per scroll notch. */
	private static final double SCROLL_STEP = 1.2;
	/** Higher = snappier. Fraction of remaining distance closed per second, exponentially. */
	private static final double SMOOTHING_SPEED = 12.0;

	private static boolean zooming;
	private static double targetDivisor = 4.0;
	private static double currentDivisor = 1.0;
	private static long lastFrameNanos;
	private static boolean savedSmoothCamera;

	private Zoom() {
	}

	public static boolean isZooming() {
		return zooming;
	}

	/** The magnification being zoomed to, e.g. 4 for 4x. */
	public static double targetMagnification() {
		return targetDivisor;
	}

	public static void setZooming(boolean active) {
		if (active == zooming) {
			return;
		}
		FarsightConfig config = FarsightClient.config();
		zooming = active;

		var options = Minecraft.getInstance().options;
		if (active) {
			if (!config.rememberZoom) {
				targetDivisor = config.defaultZoom;
			}
			targetDivisor = Mth.clamp(targetDivisor, 1.0, config.maxZoom);
			if (config.cinematicCamera) {
				savedSmoothCamera = options.smoothCamera;
				options.smoothCamera = true;
			}
		} else if (config.cinematicCamera) {
			options.smoothCamera = savedSmoothCamera;
		}
	}

	/** Positive delta zooms in, negative zooms out. */
	public static void scroll(double delta) {
		if (delta == 0) {
			return;
		}
		FarsightConfig config = FarsightClient.config();
		double factor = delta > 0 ? SCROLL_STEP : 1.0 / SCROLL_STEP;
		targetDivisor = Mth.clamp(targetDivisor * factor, 1.0, config.maxZoom);
	}

	/** Called once per frame from the FOV hook. Returns the multiplier to apply to the FOV. */
	public static float fovMultiplier() {
		long now = System.nanoTime();
		double dt = lastFrameNanos == 0 ? 0 : (now - lastFrameNanos) / 1_000_000_000.0;
		lastFrameNanos = now;
		dt = Math.min(dt, 0.1);

		double goal = zooming ? targetDivisor : 1.0;
		if (FarsightClient.config().smoothZoom) {
			double t = 1.0 - Math.exp(-SMOOTHING_SPEED * dt);
			currentDivisor = Mth.lerp(t, currentDivisor, goal);
			if (Math.abs(currentDivisor - goal) < 0.001) {
				currentDivisor = goal;
			}
		} else {
			currentDivisor = goal;
		}
		return (float) (1.0 / currentDivisor);
	}
}
