package dev.spacebod.farsight.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** While zoomed, shows the coordinates of and distance to the block under the crosshair. */
public final class ZoomHud implements HudElement {
	/** How far to look for a block. Vanilla's own target only reaches ~5 blocks. */
	private static final double PICK_RANGE = 512.0;
	private static final int MARGIN = 6;
	private static final int PADDING = 4;
	private static final int LINE_GAP = 2;

	private static final int LABEL = 0xFFF2C56B;      // warm gold for X / Y / Z
	private static final int VALUE = 0xFFFFFFFF;      // white numbers
	private static final int DISTANCE = 0xFF7EDCE2;   // soft aqua number
	private static final int UNIT = 0xFF9FB3B8;       // muted "blocks"
	private static final int BACKDROP = 0x88000000;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		FarsightConfig config = FarsightClient.config();
		if (!Zoom.isZooming() || !config.showHud || (!config.showCoordinates && !config.showDistance && !config.showZoomLevel)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Entity camera = mc.getCameraEntity();
		if (mc.level == null || camera == null) {
			return;
		}

		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
		HitResult hit = camera.pick(PICK_RANGE, partialTick, false);
		// Looking at sky or beyond range: no block lines, but the zoom level line can still show.
		BlockHitResult blockHit = hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult b ? b : null;

		List<Component> lines = new ArrayList<>(3);
		if (config.showCoordinates && blockHit != null) {
			BlockPos pos = blockHit.getBlockPos();
			lines.add(Component.empty()
					.append(Component.literal("X ").withColor(LABEL)).append(Component.literal(String.valueOf(pos.getX())).withColor(VALUE))
					.append(Component.literal("  Y ").withColor(LABEL)).append(Component.literal(String.valueOf(pos.getY())).withColor(VALUE))
					.append(Component.literal("  Z ").withColor(LABEL)).append(Component.literal(String.valueOf(pos.getZ())).withColor(VALUE)));
		}
		if (config.showDistance && blockHit != null) {
			long blocks = Math.round(camera.getEyePosition(partialTick).distanceTo(hit.getLocation()));
			lines.add(Component.empty()
					.append(Component.literal(String.valueOf(blocks)).withColor(DISTANCE))
					.append(Component.literal(blocks == 1 ? " block" : " blocks").withColor(UNIT)));
		}
		if (config.showZoomLevel) {
			long magnification = Math.round(Zoom.targetMagnification());
			lines.add(Component.empty()
					.append(Component.literal(String.valueOf(magnification)).withColor(DISTANCE))
					.append(Component.literal("x zoom").withColor(UNIT)));
		}
		if (lines.isEmpty()) {
			return;
		}

		Font font = mc.font;
		int textWidth = 0;
		for (Component line : lines) {
			textWidth = Math.max(textWidth, font.width(line));
		}
		int boxWidth = textWidth + PADDING * 2;
		int boxHeight = lines.size() * font.lineHeight + (lines.size() - 1) * LINE_GAP + PADDING * 2;

		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		int left = switch (config.hudPosition) {
			case TOP_CENTER -> (guiWidth - boxWidth) / 2;
			case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
			case TOP_RIGHT, BOTTOM_RIGHT -> guiWidth - boxWidth - MARGIN;
		};
		int top = switch (config.hudPosition) {
			case TOP_CENTER, TOP_LEFT, TOP_RIGHT -> MARGIN;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> guiHeight - boxHeight - MARGIN;
		};

		graphics.fill(left, top, left + boxWidth, top + boxHeight, BACKDROP);

		int y = top + PADDING;
		for (Component line : lines) {
			int x = config.hudPosition == HudPosition.TOP_CENTER
					? left + PADDING + (textWidth - font.width(line)) / 2
					: left + PADDING;
			graphics.text(font, line, x, y, VALUE);
			y += font.lineHeight + LINE_GAP;
		}
	}
}
