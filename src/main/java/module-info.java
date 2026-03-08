module unknown.scheduler {
    requires javafx.controls;
    requires javafx.fxml;


    opens unknown.scheduler to javafx.fxml;
    exports unknown.scheduler;
}