package com.particlesim;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;

public class App extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Screen primaryScreen = Screen.getPrimary();
        Rectangle2D screenBounds = primaryScreen.getBounds();

        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        Pane root = new Pane();
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, screenWidth - (screenWidth / 10), screenHeight - (screenHeight / 5));

        scene.setOnMouseClicked(this::handleMouseClick);

        primaryStage.setTitle("Particle Gravity Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        setupKeyHandlers(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private double lastMouseX = 0;
    private double lastMouseY = 0;

    private void setupKeyHandlers(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.B) {
                spawnBlueParticle(lastMouseX, lastMouseY);
            } else if (event.getCode() == KeyCode.V) {
                spawnRandomBlueParticle();
            } else if (event.getCode() == KeyCode.SPACE) {
                startSim();
            } else if (event.getCode() == KeyCode.R) {
                clearParticles();
                spawnWhiteParticles();
                spawnBlueParticles();
            } else if (event.getCode() == KeyCode.C) {
                clearParticles();
                spawnCirclePattern();
            } else if (event.getCode() == KeyCode.G) {
                clearParticles();
                spawnWBGrid();
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            lastMouseX = mouseEvent.getSceneX();
            lastMouseY = mouseEvent.getSceneY();
        });
    }

    private void handleMouseClick(MouseEvent event) {
        spawnWhiteParticle(event.getSceneX(), event.getSceneY());
    }

    private void startSim() {
        startUpdate();
    }

    // constants for physics
    private final double GRAVITY_RADIUS = 500.0;
    private final double GRAVITATIONAL_CONSTANT = 0.1;
    private final double MASS = 20.0;
    private final double MINIMUM_DISTANCE = 8.0;
    private final double FRICTION = 0.97;

    private final double BLUE_REPULSION_RADIUS = 30.0;
    private final double BLUE_FORCE_MULTIPLIER = 5.0;
    private final double BLUE_MASS = MASS * 5.0;

    private List<Particle> particles = new ArrayList<>();

    /**
     * Update the positions of all particles based on the forces acting on them
     * 
     * Rules:
     * 1. White particles are attracted to white particles and blue particles
     * 2. Attraction to blue particles takes priority over attraction to white
     * particles
     * 3. Blue particles repulse white particles
     * 4. Blue particles attract blue particles
     * 
     * 
     * Press R to reset board
     * Press Space to start sim (bug when you spam space it gets faster and faster)
     * Press B to spawn a blue particle at your mouse
     * Press V to spawn a random blue particle
     * Click to spawn a white particle at the mouse cursor
     * 
     * Patterns:
     * Press C to spawn a circle of white particles in the center of the screen
     */
    private void updateParticles() {
        System.out.println("Particles count: " + particles.size());
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            double fX = 0;
            double fY = 0;

            // screen wrapping
            double screenWidth = primaryStage.getScene().getWidth();
            double screenHeight = primaryStage.getScene().getHeight();

            if (p.getX() > screenWidth) {
                p.setX(0);
            } else if (p.getX() < 0) {
                p.setX(screenWidth);
            }

            if (p.getY() > screenHeight) {
                p.setY(0);
            } else if (p.getY() < 0) {
                p.setY(screenHeight);
            }

            double effectiveMass = p.getColor() == Color.BLUE ? BLUE_MASS : MASS;

            // if a white particle is near a blue particle it wont be attracted to white
            // particles
            boolean nearBlue = false;

            double clusterVX = 0;
            double clusterVY = 0;
            int clusterCount = 0;

            for (int j = 0; j < particles.size(); j++) {
                if (i == j) {
                    continue;
                }
                Particle other = particles.get(j);
                double dx = other.getX() - p.getX();
                double dy = other.getY() - p.getY();

                // maintain forces even if its on the other side of the screen
                if (dx > screenWidth / 2)
                    dx -= screenWidth;
                else if (dx < -screenWidth / 2)
                    dx += screenWidth;

                if (dy > screenHeight / 2)
                    dy -= screenHeight;
                else if (dy < -screenHeight / 2)
                    dy += screenHeight;

                double distance = Math.sqrt(dx * dx + dy * dy);

                // if particles get too close to each other they get pushed back a little bit
                if (distance < MINIMUM_DISTANCE * 2) {
                    double overlap = (MINIMUM_DISTANCE) - distance;
                    double separationStrength = 0.5;
                    fX -= (dx / distance) * overlap * separationStrength;
                    fY -= (dy / distance) * overlap * separationStrength;
                }

                // if particles are within a certain radius of each other apply a force to them
                // based on rules
                else if (distance <= GRAVITY_RADIUS) {
                    double force;
                    boolean isAttractive = true;

                    if (p.getColor() == Color.WHITE && other.getColor() == Color.BLUE) {
                        force = GRAVITATIONAL_CONSTANT * (MASS * BLUE_MASS) / ((distance * distance)
                                + 1);
                        if (distance < BLUE_REPULSION_RADIUS) {
                            force = Math.min( BLUE_FORCE_MULTIPLIER * force, 10.0);
                            isAttractive = false;
                        }
                    } else if (p.getColor() == Color.BLUE && other.getColor() == Color.WHITE) {
                        force = GRAVITATIONAL_CONSTANT * (BLUE_MASS * MASS) / ((distance * distance)
                                + 1);
                        isAttractive = false;
                    } else if (p.getColor() == Color.WHITE && other.getColor() == Color.WHITE) {
                        if (nearBlue) {
                            continue;
                        }
                        force = 1.5 * GRAVITATIONAL_CONSTANT * (MASS * MASS) / ((distance * distance)
                                + 1);
                    } else if (p.getColor() == Color.BLUE && other.getColor() == Color.BLUE) {
                        force = GRAVITATIONAL_CONSTANT * (MASS * MASS) / ((distance * distance) + 1);
                    } else {
                        continue;
                    }

                    double forceMultiplier = isAttractive ? 1.0 : -1.0;
                    if (!isAttractive) {
                        forceMultiplier *= 0.8;
                    }
                    fX += forceMultiplier * force * (dx / distance);
                    fY += forceMultiplier * force * (dy / distance);

                    clusterVX += other.getDx();
                    clusterVY += other.getDy();
                    clusterCount++;
                }
            }

            if (clusterCount > 0) {
                clusterVX /= clusterCount;
                clusterVY /= clusterCount;
                double cohesionFactor = 0.1;
                fX += (clusterVX - p.getDx()) * cohesionFactor;
                fY += (clusterVY - p.getDy()) * cohesionFactor;

                if (clusterCount > 100) {
                    fX *= 1.2;
                    fY *= 1.2;
                }

                double clusterCenterX = p.getX() + clusterVX;
                double clusterCenterY = p.getY() + clusterVY;
                double toClusterX = clusterCenterX - p.getX();
                double toClusterY = clusterCenterY - p.getY();
                fX += toClusterX * 0.02;
                fY += toClusterY * 0.02;
            }

            p.setDx((p.getDx() + fX / effectiveMass) * FRICTION);
            p.setDy((p.getDy() + fY / effectiveMass) * FRICTION);
        }

        // update particle positions on the screen
        for (int k = 0; k < particles.size(); k++) {
            Particle particle = particles.get(k);
            particle.setX(particle.getX() + particle.getDx());
            particle.setY(particle.getY() + particle.getDy());

            Circle circle = (Circle) ((Pane) primaryStage.getScene().getRoot()).getChildren().get(k);
            circle.setCenterX(particle.getX());
            circle.setCenterY(particle.getY());
        }
    }

    // particle spawning

    private void spawnBlueParticle(double x, double y) {
        Particle particle = new Particle(x, y, Color.BLUE);
        particles.add(particle);

        Circle circle = new Circle(x, y, 1, Color.BLUE);

        Pane root = (Pane) primaryStage.getScene().getRoot();
        root.getChildren().add(circle);
    }

    private void spawnRandomBlueParticle() {
        double x = Math.random() * primaryStage.getScene().getWidth();
        double y = Math.random() * primaryStage.getScene().getHeight();

        spawnBlueParticle(x, y);
    }

    private void spawnWhiteParticle(double x, double y) {
        Particle particle = new Particle(x, y, Color.WHITE);
        particles.add(particle);

        Circle circle = new Circle(x, y, 1, Color.WHITE);

        Pane root = (Pane) primaryStage.getScene().getRoot();
        root.getChildren().add(circle);
    }

    private void spawnWhiteParticles() {
        for (int i = 0; i < 1000; i++) {
            double x = Math.random() * primaryStage.getScene().getWidth();
            double y = Math.random() * primaryStage.getScene().getHeight();
            spawnWhiteParticle(x, y);
        }
    }

    private void spawnBlueParticles() {
        for (int i = 0; i < 500; i++) {
            double x = Math.random() * primaryStage.getScene().getWidth();
            double y = Math.random() * primaryStage.getScene().getHeight();
            spawnBlueParticle(x, y);
        }
    }

    private void clearParticles() {
        Pane root = (Pane) primaryStage.getScene().getRoot();
        root.getChildren().clear();
        particles.clear();
    }

    private void startUpdate() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateParticles();
            }
        }.start();
    }

    // special patterns

    // Spawns a circle of white particles in the center of the screen
    private void spawnCirclePattern() {
        int numParticles = 100;
        double centerX = primaryStage.getScene().getWidth() / 2;
        double centerY = primaryStage.getScene().getHeight() / 2;
        double radius = 200;

        for (int i = 0; i < numParticles; i++) {
            double angle = 2 * Math.PI * i / numParticles;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            spawnWhiteParticle(x, y);
        }
    }

    private void spawnWBGrid() {
        int gridSize = 5; // 5x5 grid
        double spacing = 30; // Distance between particles

        // Center coordinates
        double centerX = primaryStage.getScene().getWidth() / 2;
        double centerY = primaryStage.getScene().getHeight() / 2;

        // White grid positioned 40px left of center
        double startXWhite = centerX - spacing * (gridSize / 2) - 40;
        double startYWhite = centerY - spacing * (gridSize / 2);

        // Blue grid positioned 40px right of center
        double startXBlue = centerX + 40;
        double startYBlue = centerY - spacing * (gridSize / 2);

        // Spawn white grid
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = startXWhite + j * spacing;
                double y = startYWhite + i * spacing;
                spawnWhiteParticle(x, y);
            }
        }

        // Spawn blue grid
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = startXBlue + j * spacing;
                double y = startYBlue + i * spacing;
                spawnBlueParticle(x, y);
            }
        }
    }
}