package com.particlesim;

import javafx.scene.paint.Color;

public class Particle {
    // position
    private double x;
    private double y;

    // velocity
    private double dx;
    private double dy;

    // color
    private Color color = Color.WHITE;
    
    // constructor
    public Particle(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    // get methods
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public Color getColor() {
        return color;
    }

    // set methods
    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }
}
