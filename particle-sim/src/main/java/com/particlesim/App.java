package com.particlesim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private enum InteractionType {
        ATTRACT, REPEL, NONE
    }

    private Map<Color, Map<Color, InteractionType>> interactionRules = new HashMap<>();
    private Pane uiPane;
    private boolean uiVisible = false;
    private final Color[] PARTICLE_COLORS = { Color.WHITE, Color.BLUE };

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
        setupUI();
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
            } else if (event.getCode() == KeyCode.U) {

                uiVisible = !uiVisible;
                uiPane.setVisible(uiVisible);
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            lastMouseX = mouseEvent.getSceneX();
            lastMouseY = mouseEvent.getSceneY();
        });
    }

    private void handleMouseClick(MouseEvent event) {
        if (event.getSceneX() > uiPane.getPrefWidth()) {
            spawnWhiteParticle(event.getSceneX(), event.getSceneY());
        }
    }

    private void startSim() {
        startUpdate();
    }

    // constants for physics
    private final double GRAVITY_RADIUS = 100.0;
    private final double GRAVITATIONAL_CONSTANT = 0.1;
    private final double MASS = 40.0;
    private final double MINIMUM_DISTANCE = 5.0;
    private final double FRICTION = 0.97;

    private final double REPULSION_RADIUS = 30.0;

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
            double uiWidth = uiPane.getPrefWidth();
            double screenWidth = primaryStage.getScene().getWidth();
            double screenHeight = primaryStage.getScene().getHeight();

            if (p.getX() > screenWidth + uiWidth) {
                p.setX(uiWidth);
            } else if (p.getX() < uiWidth) {
                p.setX(screenWidth + uiWidth);
            }
    
            if (p.getY() > screenHeight) {
                p.setY(0);
            } else if (p.getY() < 0) {
                p.setY(screenHeight);
            }

            double effectiveMass = MASS;

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

                if (dx > screenWidth / 2)
                    dx -= screenWidth;
                else if (dx < -screenWidth / 2)
                    dx += screenWidth;

                if (dy > screenHeight / 2)
                    dy -= screenHeight;
                else if (dy < -screenHeight / 2)
                    dy += screenHeight;

                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < MINIMUM_DISTANCE * 2) {
                    double overlap = (MINIMUM_DISTANCE * 2) - distance;
                    double separationStrength = 1.5;
                    fX -= (dx / distance) * overlap * separationStrength;
                    fY -= (dy / distance) * overlap * separationStrength;
                }
                else if (distance <= GRAVITY_RADIUS) {
                    double effectiveDistance = Math.max(distance, 0.1);
                    InteractionType interaction = interactionRules.get(p.getColor()).get(other.getColor());

                    if (interaction != InteractionType.NONE) {
                        double force = GRAVITATIONAL_CONSTANT
                                * (effectiveMass * MASS)
                                / ((effectiveDistance * effectiveDistance) + 1);

                        boolean isAttractive = (interaction == InteractionType.ATTRACT);

                        if (p.getColor() != other.getColor() && distance < REPULSION_RADIUS) {
                            force = Math.min(force, 10.0);
                            isAttractive = false;
                        }

                        double forceDirection = isAttractive ? 1.0 : -1.0;
                        fX += forceDirection * force * (dx / effectiveDistance);
                        fY += forceDirection * force * (dy / effectiveDistance);
                    }

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

            double maxVelocity = 5.0;
            double currentVelocity = Math.sqrt(p.getDx() * p.getDx() + p.getDy() * p.getDy());
            if (currentVelocity > maxVelocity) {
                p.setDx(p.getDx() * maxVelocity / currentVelocity);
                p.setDy(p.getDy() * maxVelocity / currentVelocity);
            }
        }

        // update particle positions
        Pane root = (Pane) primaryStage.getScene().getRoot();
        List<Circle> circles = new ArrayList<>();

        for (javafx.scene.Node node : root.getChildren()) {
            if (node instanceof Circle) {
                circles.add((Circle) node);
            }
        }

        for (int i = 0; i < particles.size() && i < circles.size(); i++) {
            Particle particle = particles.get(i);
            particle.setX(particle.getX() + particle.getDx());
            particle.setY(particle.getY() + particle.getDy());

            Circle circle = circles.get(i);
            circle.setCenterX(particle.getX());
            circle.setCenterY(particle.getY());
        }
    }

    // particle spawning

    private void spawnBlueParticle(double x, double y) {
        Particle particle = new Particle(x, y, Color.BLUE);
        particles.add(particle);

        Circle circle = new Circle(x, y, 3, Color.BLUE);

        Pane root = (Pane) primaryStage.getScene().getRoot();
        root.getChildren().add(circle);
    }

    private void spawnRandomBlueParticle() {
        double uiWidth = uiPane.getPrefWidth();
        double x = uiWidth + Math.random() * (primaryStage.getScene().getWidth() - uiWidth);
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
        double uiWidth = uiPane.getPrefWidth();
        for (int i = 0; i < 500; i++) {
            double x = uiWidth + Math.random() * (primaryStage.getScene().getWidth() - uiWidth);
            double y = Math.random() * primaryStage.getScene().getHeight();
            spawnWhiteParticle(x, y);
        }
    }

    private void spawnBlueParticles() {
        double uiWidth = uiPane.getPrefWidth();
        for (int i = 0; i < 500; i++) {
            double x = uiWidth + Math.random() * (primaryStage.getScene().getWidth() - uiWidth);
            double y = Math.random() * primaryStage.getScene().getHeight();
            spawnBlueParticle(x, y);
        }
    }

    private void clearParticles() {
        Pane root = (Pane) primaryStage.getScene().getRoot();

        List<javafx.scene.Node> nodesToKeep = new ArrayList<>();

        for (javafx.scene.Node node : root.getChildren()) {
            if (node == uiPane) {
                nodesToKeep.add(node);
            }
        }

        root.getChildren().clear();
        root.getChildren().addAll(nodesToKeep);

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
        double uiWidth = uiPane.getPrefWidth();
        double centerX = uiWidth + (primaryStage.getScene().getWidth() - uiWidth) / 2;
        double centerY = primaryStage.getScene().getHeight() / 2;
        double radius = 200;

        for (int i = 0; i < numParticles; i++) {
            double angle = 2 * Math.PI * i / numParticles;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            spawnWhiteParticle(x, y);
        }
    }

    // Spawns a grid of white and blue particles
    private void spawnWBGrid() {
        int gridSize = 5;
        double spacing = 30;

        double uiWidth = uiPane.getPrefWidth();
        double centerX = uiWidth + (primaryStage.getScene().getWidth() - uiWidth) / 2;
        double centerY = primaryStage.getScene().getHeight() / 2;

        double startXWhite = centerX - spacing * (gridSize / 2) - 40;
        double startYWhite = centerY - spacing * (gridSize / 2);

        double startXBlue = centerX + 40;
        double startYBlue = centerY - spacing * (gridSize / 2);

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = startXWhite + j * spacing;
                double y = startYWhite + i * spacing;
                spawnWhiteParticle(x, y);
            }
        }

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = startXBlue + j * spacing;
                double y = startYBlue + i * spacing;
                spawnBlueParticle(x, y);
            }
        }
    }

    // UI
    private void setupUI() {
        initializeRules();

        uiPane = new Pane();
        uiPane.setStyle(
                "-fx-background-color: rgba(30,30,30,0.5); -fx-border-color: white; -fx-border-width: 0 1 0 0;");
        double panelWidth = 150;
        uiPane.setPrefWidth(panelWidth);
        uiPane.setMinWidth(panelWidth);
        uiPane.setMaxWidth(panelWidth);

        double sceneHeight = primaryStage.getScene().getHeight();
        uiPane.setPrefHeight(sceneHeight);
        uiPane.setMinHeight(sceneHeight);
        uiPane.setLayoutX(0);
        uiPane.setLayoutY(0);
        uiPane.setVisible(true);

        // title
        javafx.scene.text.Text title = new javafx.scene.text.Text(panelWidth / 2 - 50, 30, "Rules");
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        uiPane.getChildren().add(title);

        // selection grid
        int cellSize = 30;
        int startX = (int) (panelWidth / 2 - cellSize * PARTICLE_COLORS.length / 2);
        int startY = 60;

        for (int i = 0; i < PARTICLE_COLORS.length; i++) {
            javafx.scene.shape.Circle rowLabel = new javafx.scene.shape.Circle(startX - 15,
                    startY + i * cellSize + cellSize / 2, 6, PARTICLE_COLORS[i]);
            javafx.scene.shape.Circle colLabel = new javafx.scene.shape.Circle(startX + i * cellSize + cellSize / 2,
                    startY - 15, 6, PARTICLE_COLORS[i]);
            uiPane.getChildren().addAll(rowLabel, colLabel);
        }

        for (int row = 0; row < PARTICLE_COLORS.length; row++) {
            for (int col = 0; col < PARTICLE_COLORS.length; col++) {
                Color sourceColor = PARTICLE_COLORS[row];
                Color targetColor = PARTICLE_COLORS[col];

                javafx.scene.shape.Rectangle cell = new javafx.scene.shape.Rectangle(
                        startX + col * cellSize,
                        startY + row * cellSize,
                        cellSize - 3,
                        cellSize - 3);

                updateCellColor(cell, sourceColor, targetColor);

                final int r = row;
                final int c = col;
                cell.setOnMouseClicked(event -> {
                    cycleInteraction(PARTICLE_COLORS[r], PARTICLE_COLORS[c]);
                    updateCellColor(cell, PARTICLE_COLORS[r], PARTICLE_COLORS[c]);
                });

                uiPane.getChildren().add(cell);
            }
        }

        // instructions
        String instructions = "Controls:\n" +
                "Click: Spawn white\n" +
                "B: Spawn blue at cursor\n" +
                "V: Random blue\n" +
                "R: Reset particles\n" +
                "SPACE: Start simulation\n" +
                "C: Circle pattern\n" +
                "G: Grid pattern";

        javafx.scene.text.Text instructionsText = new javafx.scene.text.Text(10,
                startY + (PARTICLE_COLORS.length * cellSize) + 30, instructions);
        instructionsText.setFill(Color.WHITE);
        instructionsText.setStyle("-fx-font-size: 11px;");
        uiPane.getChildren().add(instructionsText);

        Pane root = (Pane) primaryStage.getScene().getRoot();
        root.getChildren().add(uiPane);

        uiVisible = true;
    }

    // rules

    private void initializeRules() {
        for (Color color : PARTICLE_COLORS) {
            interactionRules.put(color, new HashMap<>());
        }

        // default rules
        // white + white
        interactionRules.get(Color.WHITE).put(Color.WHITE, InteractionType.ATTRACT);

        // white + blue
        interactionRules.get(Color.WHITE).put(Color.BLUE, InteractionType.ATTRACT);

        // blue x white
        interactionRules.get(Color.BLUE).put(Color.WHITE, InteractionType.REPEL);

        // blue + blue
        interactionRules.get(Color.BLUE).put(Color.BLUE, InteractionType.ATTRACT);
    }

    private void updateCellColor(javafx.scene.shape.Rectangle cell, Color source, Color target) {
        InteractionType type = interactionRules.get(source).get(target);
        switch (type) {
            case ATTRACT:
                cell.setFill(Color.GREEN);
                break;
            case REPEL:
                cell.setFill(Color.RED);
                break;
            case NONE:
                cell.setFill(Color.GRAY);
                break;
        }
    }

    private void cycleInteraction(Color source, Color target) {
        InteractionType current = interactionRules.get(source).get(target);
        InteractionType next;

        switch (current) {
            case ATTRACT:
                next = InteractionType.REPEL;
                break;
            case REPEL:
                next = InteractionType.NONE;
                break;
            case NONE:
            default:
                next = InteractionType.ATTRACT;
                break;
        }

        interactionRules.get(source).put(target, next);
    }

}