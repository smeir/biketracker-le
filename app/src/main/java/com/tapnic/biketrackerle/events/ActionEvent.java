package com.tapnic.biketrackerle.events;


public class ActionEvent {
    String action;

    public ActionEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
