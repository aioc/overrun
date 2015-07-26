package com.ausinformatics.overrun.visualisation;

public class BoxFactory {

    private int width;
    private int height;

    public BoxFactory(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Box fromDimensions(int x, int y, int width, int height) {
        return new Box(y, x, x + width, y + height);
    }

    public Box fromPoints(int l, int t, int r, int b) {
        return new Box(t, l, width - r, height - b);
    }
    
    public Box fromMixedWidth(int l, int t, int width, int b) {
        return new Box(t, l, l + width, b);
    }
    
    public Box fromMixedHeight(int l, int t, int r, int height) {
        return new Box(t, l, r, t + height);
    }
}
