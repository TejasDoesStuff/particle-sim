package com.particlesim;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    int n = 4000;
    double dt = 0.02;
    double frictionHalfLife = 0.040;
    double rMax = 0.1;
    int m = 6;
    int forceFactor = 5;
    int numCells = (int) Math.floor(1.0 / rMax);
    double cellSize = rMax;
    double radius = 1;
    ArrayList<ArrayList<ArrayList<Integer>>> grid = new ArrayList<>();

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
    private Pane simulationPane;
    private GridPane controlPane;
    private Rectangle[][] forceSquares;

    private long lastTime = 0;
    private int frames = 0;
    private double fps = 0;
    private Label fpsLabel = new Label("FPS: 0");

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

            Circle circle = new Circle(radius);
            circle.setFill(typeColors.get(particleType));
            circles.add(circle);
        }
    }

    private void initializeGrid() {
        for (int i = 0; i < numCells; i++) {
            ArrayList<ArrayList<Integer>> row = new ArrayList<>();
            for (int j = 0; j < numCells; j++) {
                row.add(new ArrayList<>());
            }
            grid.add(row);
        }
    }

    private int roundPosition(int p) {
        if (p < 0) {
            return 0;
        }

        if (p >= numCells) {
            return numCells - 1;
        }

        return p;
    }

    private int getCell(double value) {
        return roundPosition((int) (value / cellSize));
    }

    private void updateGrid() {
        for (int i = 0; i < numCells; i++) {
            for (int j = 0; j < numCells; j++) {
                grid.get(i).get(j).clear();
            }
        }

        for (int i = 0; i < n; i++) {
            int cx = getCell(positionsX.get(i));
            int cy = getCell(positionsY.get(i));
            grid.get(cx).get(cy).add(i);
        }
    }

    private final int[] offsets = {-1, 0, 1};

    private ArrayList<Integer> getNeighbors(int cx, int cy) {
        ArrayList<Integer> neighbors = new ArrayList<>();

        for (int ox : offsets) {
            for (int oy : offsets) {
                int nx = cx + ox;
                int ny = cy + oy;

                if (nx < 0) {
                    nx += numCells;
                }
                if (ny < 0) {
                    ny += numCells;
                }
                if (nx >= numCells) {
                    nx -= numCells;
                }
                if (ny >= numCells) {
                    ny -= numCells;
                }

                neighbors.addAll(grid.get(nx).get(ny));
            }
        }
        return neighbors;
    }

    @Override
    public void start(Stage primaryStage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        width = screenBounds.getWidth() * 0.7;
        height = screenBounds.getHeight() * 0.9;

        initializeGrid();
        initializeParticles();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, width * 1.3, height, Color.BLACK);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Particle Simulation");

        simulationPane = new Pane();
        simulationPane.setPrefSize(width, height);
        simulationPane.setStyle("-fx-background-color: black;");
        for (Circle circle : circles) {
            simulationPane.getChildren().add(circle);
        }
        root.setCenter(simulationPane);

        controlPane = createControlPanel();
        root.setRight(controlPane);

        fpsLabel.setTextFill(Color.WHITE);
        HBox fpsBox = new HBox(fpsLabel);
        fpsBox.setPadding(new Insets(5));
        fpsBox.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        root.setTop(fpsBox);
        BorderPane.setMargin(fpsBox, new Insets(10, 0, 0, 10));

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime > 0) {
                    frames++;
                    if (now - lastTime >= 1000000000) {
                        fps = frames;
                        fpsLabel.setText("FPS: " + fps);
                        frames = 0;
                        lastTime = now;
                    }
                } else {
                    lastTime = now;
                }

                updateParticles();
                renderParticles();
            }
        };
        timer.start();

        primaryStage.show();
    }

    private GridPane createControlPanel() {
        GridPane panel = new GridPane();
        panel.setPadding(new Insets(10));
        panel.setHgap(5);
        panel.setVgap(5);
        panel.setStyle("-fx-background-color: #000000;");

        Label titleLabel = new Label("Adjust Forces");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);
        panel.add(titleLabel, 0, 0, m + 1, 1);

        Label instructionLabel = new Label("left click to increase \nright click to decrease \nshift + left click to set to increase by 0.5 \nshift + right click to decrease by -0.5");
        instructionLabel.setFont(Font.font("Arial", 12));
        instructionLabel.setTextFill(Color.LIGHTGRAY);
        panel.add(instructionLabel, 0, 1, m + 1, 1);

        forceSquares = new Rectangle[m][m];
        for (int i = 0; i < m; i++) {
            StackPane rowHeaderPane = new StackPane();
            Rectangle rowColorSquare = new Rectangle(30, 30);
            rowColorSquare.setFill(typeColors.get(i));
            rowColorSquare.setStroke(Color.WHITE);

            Label rowTypeLabel = new Label(Integer.toString(i));
            rowTypeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            rowTypeLabel.setTextFill(getContrastColor(typeColors.get(i)));

            rowHeaderPane.getChildren().addAll(rowColorSquare, rowTypeLabel);
            panel.add(rowHeaderPane, 0, i + 4);

            for (int j = 0; j < m; j++) {
                StackPane squarePane = new StackPane();

                Rectangle square = new Rectangle(50, 50);
                double value = matrix.get(i).get(j);
                updateSquareColor(square, value);

                Label valueLabel = new Label(String.format("%.1f", value));
                valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                valueLabel.setTextFill(getTextColor(value));

                squarePane.getChildren().addAll(square, valueLabel);

                final int row = i;
                final int col = j;
                squarePane.setOnMouseClicked(event -> {
                    double currentValue = matrix.get(row).get(col);
                    double newValue;

                    if (event.isShiftDown()) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            newValue = Math.min(currentValue + 0.5, 1.0);
                        } else if (event.getButton() == MouseButton.SECONDARY) {
                            newValue = Math.min(currentValue - 0.5, 1.0);
                        } else {
                            newValue = currentValue;
                        }
                    } else {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            newValue = Math.min(currentValue + 0.1, 1.0);
                        } else if (event.getButton() == MouseButton.SECONDARY) {
                            newValue = Math.max(currentValue - 0.1, -1.0);
                        } else {
                            newValue = currentValue;
                        }
                    }

                    newValue = Math.round(newValue * 10) / 10.0;

                    matrix.get(row).set(col, newValue);
                    updateSquareColor(square, newValue);
                    valueLabel.setText(String.format("%.1f", newValue));
                    valueLabel.setTextFill(getTextColor(newValue));
                });

                panel.add(squarePane, j + 1, i + 4);
                forceSquares[i][j] = square;
            }
        }

        for (int j = 0; j < m; j++) {
            StackPane headerPane = new StackPane();
            Rectangle colorSquare = new Rectangle(30, 30);
            colorSquare.setFill(typeColors.get(j));
            colorSquare.setStroke(Color.WHITE);

            Label typeLabel = new Label(Integer.toString(j));
            typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            typeLabel.setTextFill(Color.BLACK);

            headerPane.getChildren().addAll(colorSquare, typeLabel);
            panel.add(headerPane, j + 1, 3);
        }

        HBox sliderBox = new HBox(10);
        Label forceFactorLabel = new Label("Force Factor: " + forceFactor);
        forceFactorLabel.setFont(Font.font("Arial", 12));
        forceFactorLabel.setTextFill(Color.WHITE);

        Slider forceSlider = new Slider(0, 10, forceFactor);
        forceSlider.setBlockIncrement(1);
        forceSlider.setMajorTickUnit(1);
        forceSlider.setMinorTickCount(0);
        forceSlider.setSnapToTicks(true);
        forceSlider.setShowTickMarks(true);
        forceSlider.setShowTickLabels(true);
        forceSlider.setPrefWidth(200);
        forceSlider.setStyle("-fx-control-inner-background: #333333; -fx-tick-label-fill: white;");

        forceSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            forceFactor = newValue.intValue();
            forceFactorLabel.setText("force factor: " + forceFactor);
        });

        sliderBox.getChildren().addAll(forceFactorLabel, forceSlider);

        panel.add(sliderBox, 0, 4 + m, m + 1, 1);

        return panel;
    }

    private Color getContrastColor(Color bgColor) {
        double luminance = 0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue();
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private Color getTextColor(double value) {
        if (value > 0) {
            return Color.BLACK;
        }
        return Math.abs(value) > 0.5 ? Color.WHITE : Color.BLACK;
    }

    private void updateSquareColor(Rectangle square, double value) {
        if (value < 0) {
            double intensity = Math.abs(value);
            square.setFill(Color.rgb(255, (int) (255 * (1 - intensity)), (int) (255 * (1 - intensity))));
        } else {
            double intensity = value;
            square.setFill(Color.rgb((int) (255 * (1 - intensity)), 255, (int) (255 * (1 - intensity))));
        }
        square.setStroke(Color.GRAY);
        square.setStrokeWidth(1);
    }

    private void updateParticles() {
        updateGrid();
        for (int i = 0; i < n; i++) {
            double totalForceX = 0;
            double totalForceY = 0;

            int cx = getCell(positionsX.get(i));
            int cy = getCell(positionsY.get(i));

            ArrayList<Integer> neighbors = getNeighbors(cx, cy);

            for (int j : neighbors) {
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

                    int typeI = particleTypes.get(i);
                    int typeJ = particleTypes.get(j);

                    double a = matrix.get(typeI).get(typeJ);
                    double f = force(r / rMax, a);

                    totalForceX += rx / r * f;
                    totalForceY += ry / r * f;
                }
            }

            totalForceX *= rMax * forceFactor;
            totalForceY *= rMax * forceFactor;

            velocitiesX.set(i, velocitiesX.get(i) * frictionFactor + totalForceX * dt);
            velocitiesY.set(i, velocitiesY.get(i) * frictionFactor + totalForceY * dt);

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
