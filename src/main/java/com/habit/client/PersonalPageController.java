package com.habit.client;

public class PersonalPageController {
    @javafx.fxml.FXML
    private javafx.scene.control.ListView<String> taskList;
    @javafx.fxml.FXML
    private javafx.scene.control.TextField taskInput;
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnAddTask;
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnCompleteTask;
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnBack;

    @javafx.fxml.FXML
    public void initialize() {
        btnAddTask.setOnAction(e -> {
            String task = taskInput.getText().trim();
            if (!task.isEmpty()) {
                taskList.getItems().add(task);
                taskInput.clear();
            }
        });
        btnCompleteTask.setOnAction(e -> {
            int idx = taskList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                taskList.getItems().remove(idx);
            }
        });
    }
}