package dev.spacebod.farsight.client;

public enum HudPosition {
	TOP_CENTER("Top centre"),
	TOP_LEFT("Top left"),
	TOP_RIGHT("Top right"),
	BOTTOM_LEFT("Bottom left"),
	BOTTOM_RIGHT("Bottom right");

	public final String label;

	HudPosition(String label) {
		this.label = label;
	}
}
