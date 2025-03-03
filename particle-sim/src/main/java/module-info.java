module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.particlesim to javafx.fxml;
    exports com.particlesim;
}
