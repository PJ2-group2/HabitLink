package com.habit.client;

import com.habit.domain.Task;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class TaskDisplayItem {
    private Task task;
    private BooleanProperty doneProperty;

    public TaskDisplayItem(Task task, boolean isDone) {
        this.task = task;
        this.doneProperty = new SimpleBooleanProperty(isDone);
    }

    public BooleanProperty doneProperty() {
        return doneProperty;
    }

    public boolean isDone() {
        return doneProperty.get();
    }

    public Task getTask() {
        return task;
    }
}