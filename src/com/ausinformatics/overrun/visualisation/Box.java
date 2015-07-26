package com.ausinformatics.overrun.visualisation;

import java.awt.Graphics2D;

public class Box {

    public final int top;
    public final int left;
    public final int right;
    public final int bottom;

    public final int width;
    public final int height;

    public Box(int t, int l, int r, int b) {
        top = t;
        left = l;
        right = r;
        bottom = b;
        width = r - l;
        height = b - t;
    }

    public void fill(Graphics2D g) {
        g.fillRect(left, top, width, height);
    }
    
    public String toString() {
        return "(" + left + " " + top + " " + right + " " + bottom + ")[" + width + " " + height + "]";
    }
    
}
