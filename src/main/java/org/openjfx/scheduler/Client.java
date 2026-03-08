package org.openjfx.scheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Client extends Application {

    // GUI controls
    private ComboBox<String> actionBox;
    private ComboBox<String> moduleBox;
    private ComboBox<String> dayBox;
    private ComboBox<String> timeBox;
    private TextField roomField;
    private TextArea logArea;
    private Label statusLabel;
    private Button sendBtn, stopBtn, clearBtn;
    private GridPane timetableGrid;

    // Network
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 6969;

    // Timetable structure
    private static final String[] DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };
    private static final String[] TIME_SLOTS = {
            "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00"
    };

    @Override
    public void start(Stage stage) {
        stage.setTitle("LM051-2026 Lecture Scheduler");

        // Top status bar
        statusLabel = new Label("Not connected to server.");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red; -fx-font-size: 13;");
        HBox topBar = new HBox(statusLabel);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setStyle("-fx-background-color: #eceff1;");

        // Dropdowns and inputs
        actionBox = new ComboBox<>();
        actionBox.getItems().addAll("ADD", "REMOVE", "DISPLAY", "OTHER");
        actionBox.setValue("ADD");
        actionBox.setMaxWidth(Double.MAX_VALUE);

        moduleBox = new ComboBox<>();
        moduleBox.getItems().addAll("CS4116", "CS4453", "CS4182", "CS4815", "CS4006");
        moduleBox.setValue("CS4116");
        moduleBox.setMaxWidth(Double.MAX_VALUE);

        dayBox = new ComboBox<>();
        dayBox.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        dayBox.setValue("Monday");
        dayBox.setMaxWidth(Double.MAX_VALUE);

        timeBox = new ComboBox<>();
        timeBox.getItems().addAll("09:00","10:00","11:00","12:00",
                "13:00","14:00","15:00","16:00","17:00");
        timeBox.setValue("09:00");
        timeBox.setMaxWidth(Double.MAX_VALUE);

        roomField = new TextField();
        roomField.setPromptText("e.g. CS1-01");

        // When action changes, grey out fields not needed
        actionBox.setOnAction(e -> updateFieldAvailability());

        // Buttons
        sendBtn = new Button("▶  SEND");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 8;");

        stopBtn = new Button("⏹  STOP");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 8;");

        clearBtn = new Button("🗑  CLEAR LOG");
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        // Form grid layout (label | control)
        GridPane form = new GridPane();
        form.setVgap(8); form.setHgap(10);
        form.setPadding(new Insets(15));
        form.add(new Label("Action:"), 0, 0);  form.add(actionBox,  1, 0);
        form.add(new Label("Module:"), 0, 1);  form.add(moduleBox,  1, 1);
        form.add(new Label("Day:"),    0, 2);  form.add(dayBox,     1, 2);
        form.add(new Label("Time:"),   0, 3);  form.add(timeBox,    1, 3);
        form.add(new Label("Room:"),   0, 4);  form.add(roomField,  1, 4);

        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        VBox leftPanel = new VBox(12, form, sendBtn, stopBtn, clearBtn);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ccc; " +
                "-fx-border-width: 0 1 0 0;");

        // Log area
        Label logTitle = new Label("Communication Log:");
        logTitle.setFont(Font.font(null, FontWeight.BOLD, 13));
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(160);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        // Timetable grid
        Label tableTitle = new Label("  LM051-2026 — Weekly Timetable");
        tableTitle.setMaxWidth(Double.MAX_VALUE);
        tableTitle.setPadding(new Insets(8, 16, 8, 16));
        tableTitle.setStyle("-fx-background-color: #005032; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        timetableGrid = new GridPane();
        buildEmptyTimetable();
        ScrollPane scrollPane = new ScrollPane(timetableGrid);
        scrollPane.setFitToWidth(false);

        VBox rightPanel = new VBox(10, logTitle, logArea, tableTitle, scrollPane);
        rightPanel.setPadding(new Insets(10));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Main layout
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(leftPanel);
        root.setCenter(rightPanel);
        BorderPane.setMargin(rightPanel, new Insets(5));

        // Wire up buttons
        sendBtn.setOnAction(e -> handleSend());
        stopBtn.setOnAction(e -> handleStop());
        clearBtn.setOnAction(e -> logArea.clear());

        Scene scene = new Scene(root, 950, 650);
        stage.setScene(scene);
        stage.show();

        connectToServer();
    }

    // ── Timetable methods ─────────────────────────────────────────────────

    private void buildEmptyTimetable() {
        timetableGrid.getChildren().clear();
        timetableGrid.setGridLinesVisible(true);

        timetableGrid.add(makeHeaderCell(""), 0, 0);
        for (int col = 0; col < DAYS.length; col++)
            timetableGrid.add(makeHeaderCell(DAYS[col]), col + 1, 0);

        for (int row = 0; row < TIME_SLOTS.length; row++) {
            timetableGrid.add(makeTimeCell(TIME_SLOTS[row]), 0, row + 1);
            for (int col = 0; col < DAYS.length; col++)
                timetableGrid.add(makeEmptyCell(), col + 1, row + 1);
        }
    }

    private void displayTimetable(List<Lecture> lectures) {
        buildEmptyTimetable();
        for (Lecture lec : lectures) {
            int col = getDayColumn(lec.getDay());
            int row = getTimeRow(lec.getTime());
            if (col != -1 && row != -1) {
                timetableGrid.getChildren().removeIf(node ->
                        GridPane.getColumnIndex(node) != null &&
                                GridPane.getRowIndex(node) != null &&
                                GridPane.getColumnIndex(node) == col &&
                                GridPane.getRowIndex(node) == row
                );
                timetableGrid.add(makeLectureCard(lec), col, row);
            }
        }
    }

    private int getDayColumn(String day) {
        for (int i = 0; i < DAYS.length; i++)
            if (DAYS[i].equalsIgnoreCase(day)) return i + 1;
        return -1;
    }

    private int getTimeRow(String time) {
        for (int i = 0; i < TIME_SLOTS.length; i++)
            if (TIME_SLOTS[i].equals(time)) return i + 1;
        return -1;
    }

    private Label makeHeaderCell(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(null, FontWeight.BOLD, 13));
        l.setMinWidth(140); l.setMinHeight(44);
        l.setAlignment(Pos.CENTER); l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-background-color: #005032; -fx-text-fill: white; -fx-padding: 5;");
        return l;
    }

    private Label makeTimeCell(String time) {
        Label l = new Label(time + "\n─\n" + nextHour(time));
        l.setMinWidth(80); l.setMinHeight(90);
        l.setAlignment(Pos.CENTER);
        l.setFont(Font.font(null, FontWeight.BOLD, 11));
        l.setStyle("-fx-background-color: #f0f4f8; -fx-text-fill: #505a69; -fx-padding: 5;");
        return l;
    }

    private Pane makeEmptyCell() {
        Pane p = new Pane();
        p.setMinWidth(140); p.setMinHeight(90);
        p.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d2d7dc; -fx-border-width: 0 1 1 0;");
        return p;
    }

    private VBox makeLectureCard(Lecture lec) {
        Label time   = new Label(lec.getTime() + " - " + nextHour(lec.getTime()));
        Label module = new Label(lec.getModule());
        Label room   = new Label(lec.getRoom());
        boolean online = lec.getRoom().equalsIgnoreCase("ONLINE");
        time.setFont(Font.font(null, FontWeight.NORMAL, 10));
        module.setFont(Font.font(null, FontWeight.BOLD, 12));
        room.setFont(Font.font(null, online ? FontWeight.BOLD : FontWeight.NORMAL, 11));
        time.setStyle("-fx-text-fill: #646e78;");
        module.setStyle("-fx-text-fill: #003c78;");
        room.setStyle("-fx-text-fill: " + (online ? "#c85000" : "#323742") + ";");
        VBox card = new VBox(3, time, module, room);
        card.setPadding(new Insets(7, 7, 7, 10));
        card.setMinWidth(140); card.setMinHeight(90);
        card.setStyle("-fx-background-color: #ffffff; " +
                "-fx-border-color: #005032 transparent #d2d7dc #d2d7dc; " +
                "-fx-border-width: 0 1 1 3;");
        return card;
    }

    private String nextHour(String time) {
        int hour = Integer.parseInt(time.split(":")[0]);
        return String.format("%02d:00", hour + 1);
    }

    // ── Field greying out ─────────────────────────────────────────────────

    private void updateFieldAvailability() {
        String action = actionBox.getValue();
        boolean needsDetails = action.equals("ADD") || action.equals("REMOVE");
        boolean needsRoom    = action.equals("ADD");
        moduleBox.setDisable(!needsDetails);
        dayBox.setDisable(!needsDetails);
        timeBox.setDisable(!needsDetails);
        roomField.setDisable(!needsRoom);
    }

    // ── Network ───────────────────────────────────────────────────────────

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            setStatus("✅ Connected to server at " + SERVER_HOST + ":" + SERVER_PORT, "green");
            log("Connected to server.");
        } catch (Exception ex) {
            setStatus("❌ Could not connect — is the server running?", "red");
            log("Connection failed: " + ex.getMessage());
            sendBtn.setDisable(true);
            stopBtn.setDisable(true);
        }
    }

    private void handleSend() {
        if (!connected) { showAlert("Not Connected", "Start the server first."); return; }

        String action = actionBox.getValue();
        String message;

        switch (action) {
            case "ADD":
                String room = roomField.getText().trim();
                if (room.isEmpty()) { showAlert("Missing Room", "Please type a room number."); return; }
                message = "ADD|" + moduleBox.getValue() + "|" + room
                        + "|" + dayBox.getValue() + "|" + timeBox.getValue();
                break;
            case "REMOVE":
                message = "REMOVE|" + moduleBox.getValue()
                        + "|" + dayBox.getValue() + "|" + timeBox.getValue();
                break;
            case "DISPLAY":
                message = "DISPLAY";
                break;
            case "OTHER":
                message = "BOOK_FLIGHT|London|08:00"; // triggers IncorrectActionException
                break;
            default:
                message = action;
        }

        log("→ Sent:     " + message);
        out.println(message);

        try {
            String response = in.readLine();
            log("← Received: " + response);
            handleResponse(response);
        } catch (Exception ex) {
            log("ERROR: " + ex.getMessage());
        }
    }

    private void handleStop() {
        if (!connected) return;
        try {
            log("→ Sent:     STOP");
            out.println("STOP");
            String response = in.readLine();
            log("← Received: " + response);
            socket.close();
            connected = false;
            setStatus("🔌 Disconnected.", "gray");
            sendBtn.setDisable(true);
            stopBtn.setDisable(true);
        } catch (Exception ex) {
            log("Error: " + ex.getMessage());
        }
    }

    private void handleResponse(String response) {
        if (response == null) return;
        if (response.startsWith("SUCCESS")) {
            setStatus("✅ " + response, "green");
        } else if (response.startsWith("CLASH") || response.startsWith("ERROR")) {
            setStatus("⚠️ " + response, "darkorange");
            showAlert("Problem", response);
        } else if (response.startsWith("EXCEPTION")) {
            setStatus("❌ " + response, "red");
            showAlert("Server Exception", response);
        } else if (response.startsWith("SCHEDULE:") || response.startsWith("EMPTY:")) {
            setStatus("📅 Schedule received.", "#1565C0");
            parseAndShowSchedule(response);
        } else if (response.equals("TERMINATE")) {
            setStatus("🔌 Connection terminated.", "gray");
        }
    }

    private void parseAndShowSchedule(String response) {
        List<Lecture> lectures = new ArrayList<>();
        if (response.startsWith("EMPTY:")) {
            buildEmptyTimetable();
            return;
        }

        String data = response.substring("SCHEDULE:".length()).trim();
        if (data.isEmpty()) {
            buildEmptyTimetable();
            return;
        }

        String[] entries = data.split(";");
        for (String entry : entries) {
            if (entry.isBlank()) continue;

            String[] parts = entry.split("\\|");
            if (parts.length == 4) {
                lectures.add(new Lecture(parts[0], parts[1], parts[2], parts[3]));
            }
        }

        displayTimetable(lectures);
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private void log(String msg) { logArea.appendText(msg + "\n"); }

    private void setStatus(String msg, String colour) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: " + colour + ";");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(); }
}