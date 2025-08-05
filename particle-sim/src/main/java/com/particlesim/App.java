package com.particlesim;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    int n = 1000;
    double dt = 0.02;
    double frictionHalfLife = 0.040;
    double rMax = 0.1;
    int m = 6;
    int forceFactor = 5;

    ArrayList<ArrayList<Double>> matrix = makeRandomMatrix();

    ArrayList<Color> colors = new ArrayList<>();
    ArrayList<Integer> particleTypes = new ArrayList<>();
    ArrayList<Double> positionsX = new ArrayList<>();
    ArrayList<Double> positionsY = new ArrayList<>();
    ArrayList<Double> velocitiesX = new ArrayList<>();
    ArrayList<Double> velocitiesY = new ArrayList<>();
    ArrayList<Circle> circles = new ArrayList<>();

    ArrayList<Color> typeColors = new ArrayList<>();

    double frictionFactor = Math.pow(0.5, dt / frictionHalfLife);

    private double width;
    private double height;
    private Pane root;

    private double force(double r, double a) {
        double beta = 0.3;
        if (r < beta) {
            return r / beta - 1;
        } else if (beta < r && r < 1) {
            return a * (1 - Math.abs(2 * r - 1 - beta) / (1 - beta));
        } else {
            return 0;
        }
    }

    private ArrayList<ArrayList<Double>> makeRandomMatrix() {
        ArrayList<ArrayList<Double>> rows = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                row.add(Math.random() * 2 - 1);
            }
            rows.add(row);
        }
        return rows;
    }

    private void initializeTypeColors() {
        for (int i = 0; i < m; i++) {
            double hue = 360.0 * i / m;
            Color color = Color.hsb(hue, 1.0, 1.0);
            typeColors.add(color);
        }
    }

    private void initializeParticles() {
        initializeTypeColors();

        for (int i = 0; i < n; i++) {
            int particleType = i % m;
            colors.add(typeColors.get(particleType));
            particleTypes.add(particleType);

            positionsX.add(Math.random());
            positionsY.add(Math.random());

            velocitiesX.add(0.0);
            velocitiesY.add(0.0);

            Circle circle = new Circle(2);
            circle.setFill(typeColors.get(particleType));
            circles.add(circle);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        width = screenBounds.getWidth() * 0.9;
        height = screenBounds.getHeight() * 0.9;

        initializeParticles();

        root = new Pane();
        Scene scene = new Scene(root, width, height, Color.BLACK);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Particle Simulation");

        for (Circle circle : circles) {
            root.getChildren().add(circle);
        }

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateParticles();
                renderParticles();
            }
        };
        timer.start();

        primaryStage.show();
    }

    private void updateParticles() {
        for (int i = 0; i < n; i++) {
            double totalForceX = 0;
            double totalForceY = 0;

            for (int j = 0; j < n; j++) {
                if (j == i) {
                    continue;
                }
                double rx = positionsX.get(j) - positionsX.get(i);
                double ry = positionsY.get(j) - positionsY.get(i);

                if (rx > 0.5) {
                    rx -= 1;
                }
                if (rx < -0.5) {
                    rx += 1;
                }
                if (ry > 0.5) {
                    ry -= 1;
                }
                if (ry < -0.5) {
                    ry += 1;
                }

                double r = Math.hypot(rx, ry);
                if (r > 0 && r < rMax) {
                    double f = force(r / rMax, matrix.get(particleTypes.get(i)).get(particleTypes.get(j)));
                    totalForceX += rx / r * f;
                    totalForceY += ry / r * f;
                }
            }

            totalForceX *= rMax * forceFactor;
            totalForceY *= rMax * forceFactor;

            velocitiesX.set(i, velocitiesX.get(i) * frictionFactor);
            velocitiesY.set(i, velocitiesY.get(i) * frictionFactor);

            velocitiesX.set(i, velocitiesX.get(i) + totalForceX * dt);
            velocitiesY.set(i, velocitiesY.get(i) + totalForceY * dt);

        }

        for (int i = 0; i < n; i++) {
            positionsX.set(i, positionsX.get(i) + velocitiesX.get(i) * dt);
            positionsY.set(i, positionsY.get(i) + velocitiesY.get(i) * dt);

            double x = positionsX.get(i);
            double y = positionsY.get(i);

            x = (x % 1 + 1) % 1;
            y = (y % 1 + 1) % 1;

            positionsX.set(i, x);
            positionsY.set(i, y);
        }
    }

    private void renderParticles() {
        for (int i = 0; i < n; i++) {
            double screenX = positionsX.get(i) * width;
            double screenY = positionsY.get(i) * height;

            circles.get(i).setCenterX(screenX);
            circles.get(i).setCenterY(screenY);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
