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
        Scene scene = new Scene(root, screenWidth-(screenWidth/10), screenHeight-(screenHeight/5));

        scene.setOnMouseClicked(this::handleMouseClick);

        primaryStage.setTitle("JavaFX Application");
        primaryStage.setScene(scene);
        primaryStage.show();

        setupKeyHandlers(scene);

        spawnWhiteParticles();
        spawnBlueParticles();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private final double GRAVITY_RADIUS = 50.0;
    private final double GRAVITATIONAL_CONSTANT = 0.1;
    private final double MASS = 20.0;
    private final double MINIMUM_DISTANCE = 4.0;
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
     * 2. Attraction to blue particles takes priority over attraction to white particles
     * 3. Blue particles repulse white particles
     * 4. Blue particles are static
     * 
     * Press R to reset board
     * Press Space to start sim (bug when you spam space it gets faster and faster)
     * Press B to spawn a blue particle at your mouse
     * Press V to spawn a random blue particle
     * Click to spawn a white particle at the mouse cursor
     */
    private void updateParticles() {
        for(int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            double fX = 0;
            double fY = 0;

            double effectiveMass = p.getColor() == Color.BLUE ? BLUE_MASS : MASS;
            
            boolean nearBlue = false;
            if (p.getColor() == Color.WHITE) {
                for (Particle other : particles) {
                    if (other.getColor() == Color.BLUE) {
                        double dx = other.getX() - p.getX();
                        double dy = other.getY() - p.getY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance <= BLUE_REPULSION_RADIUS * 1.2) {
                            nearBlue = true;
                            break;
                        }
                    }
                }
            }

            for(int j = 0; j < particles.size(); j++) {
                if(i == j) continue;
                Particle other = particles.get(j);
                // if (p.getColor() == Color.BLUE) {
                //     continue;
                // }
                double dx = other.getX() - p.getX();
                double dy = other.getY() - p.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                if(distance < MINIMUM_DISTANCE) {
                    double overlap = MINIMUM_DISTANCE - distance;
                    double separationStrength = 0.25;
                    p.setX(p.getX() - dx * overlap * separationStrength);
                    p.setY(p.getY() - dy * overlap * separationStrength);
                }
                else if (distance <= GRAVITY_RADIUS) {
                    double force;
                    boolean isAttractive = true;

                    if (p.getColor() == Color.WHITE && other.getColor() == Color.BLUE) {
                        force = GRAVITATIONAL_CONSTANT * (MASS * BLUE_MASS) / (distance * distance);
                        if (distance < BLUE_REPULSION_RADIUS) {
                            force = BLUE_FORCE_MULTIPLIER * force;
                            isAttractive = false;
                        } else {
                            isAttractive = true;
                        }
                    }
                    else if (p.getColor() == Color.BLUE && other.getColor() == Color.WHITE) {
                        force = GRAVITATIONAL_CONSTANT * (BLUE_MASS * MASS) / (distance * distance);
                        isAttractive = false;
                    }
                    else if (p.getColor() == Color.WHITE && other.getColor() == Color.WHITE) {
                        if (nearBlue) {
                            continue;
                        }
                        force = GRAVITATIONAL_CONSTANT * (MASS * MASS) / (distance * distance);
                        isAttractive = true;
                    }
                    else {
                        continue;
                    }

                    double forceMultiplier = isAttractive ? 1.0 : -1.0;
                    fX += forceMultiplier * force * (dx / distance);
                    fY += forceMultiplier * force * (dy / distance);
                }
                else {
                    continue;
                }
            }

            p.setDx((p.getDx() + fX / effectiveMass) * FRICTION);
            p.setDy((p.getDy() + fY / effectiveMass) * FRICTION);
        }

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.setX(p.getX() + p.getDx());
            p.setY(p.getY() + p.getDy());
    
            Circle circle = (Circle) ((Pane) primaryStage.getScene().getRoot()).getChildren().get(i);
            circle.setCenterX(p.getX());
            circle.setCenterY(p.getY());
        }
    }

    private void setupKeyHandlers(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.B) {
                spawnBlueParticle(lastMouseX, lastMouseY);
            } else if (event.getCode() == KeyCode.V) {
                spawnRandomBlueParticle();
            }
            else if (event.getCode() == KeyCode.SPACE) {
                startSim();
            } else if (event.getCode() == KeyCode.R) {
                clearParticles();
                spawnWhiteParticles();
                spawnBlueParticles();
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
    
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    private void spawnBlueParticle(double x, double y) {
        Particle particle = new Particle(x, y, Color.BLUE);
        particles.add(particle);
        
        Circle circle = new Circle(x, y, 5, Color.BLUE);
        
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
        
        Circle circle = new Circle(x, y, 3, Color.WHITE);
        
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
        for (int i = 0; i < 50; i++) {
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

}
