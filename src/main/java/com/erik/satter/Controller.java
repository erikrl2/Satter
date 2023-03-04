package com.erik.satter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    @FXML
    private TextArea input;
    @FXML
    private Text formulaLabel;
    @FXML
    private Text output;

    private Formula formula;

    ExecutorService solverThread;

    public void initialize() {
        formula = new Formula();
        solverThread = Executors.newSingleThreadExecutor();

        String defaultBorderStyle = "-fx-border-color: #039ed3";
        String errorBorderStyle = "-fx-border-color: red";

        input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Formula.isValidCNF(newValue)) {
                input.setStyle(defaultBorderStyle);
                formula.parse(newValue);
                formulaLabel.setText(formula.toString());
                satSolve();
            } else {
                input.setStyle(errorBorderStyle);
            }
        });
    }

    private void satSolve() {
        solverThread.execute(() -> {
            var assignment = formula.getAssignment();
            Platform.runLater(() -> output.setText(!assignment.isEmpty() ? assignment.toString() : "Unsatisfiable"));
        });
    }
}