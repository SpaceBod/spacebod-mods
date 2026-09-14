package dev.spacebod.tidy.client;

public enum SortMode {
	TYPE("Type"),
	QUANTITY("Quantity");

	public final String label;

	SortMode(String label) {
		this.label = label;
	}

	public SortMode other() {
		return this == TYPE ? QUANTITY : TYPE;
	}
}
