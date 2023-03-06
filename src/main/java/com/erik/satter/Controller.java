package com.erik.satter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class Controller {

    @FXML
    GridPane sudoku;
    TextField[][] cells;

    @FXML
    TextArea input;
    @FXML
    TextArea formulaText;
    @FXML
    TextArea output;

    Formula formula;

    ExecutorService solverThread;

    public void initialize() {
        formula = new Formula();
        solverThread = Executors.newSingleThreadExecutor();

        input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Formula.isValidCNF(newValue)) {
                input.setId("valid");
                formula.parse(newValue);
                formulaText.setText(formula.toString());
                satSolve();
            } else {
                input.setId("invalid");
            }
        });

        cells = sudoku.getChildren().stream().filter(e -> e instanceof GridPane).map(e ->
                ((GridPane)e).getChildren().stream().filter(node -> node instanceof TextField).map(node ->
                        (TextField)node).toList().toArray(new TextField[0])).toList().toArray(new TextField[0][]);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int finalI = i, finalJ = j;
                cells[i][j].textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.length() > 1 || newValue.matches("\\D"))
                        cells[finalI][finalJ].setText(oldValue);
                });
            }
        }

        // DEBUG
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j].setText(j + 1 + "");
            }
        }
    }

    @FXML
    private void solveSudoku() {
        /*

        ^X=1-9: ^Z=1-9: (XZ,1 ∨ XZ,2 ∨ · · · ∨ XZ,9) ^
        ^X=1-9: ^S=1-9: (X1,S ∨ X2,S ∨ · · · ∨ X9,S) ^

         */
    }

    private void satSolve() {
        solverThread.execute(() -> {
            var assignment = formula.getAssignment();
            Platform.runLater(() -> output.setText(!assignment.isEmpty() ? assignment.toString() : "Unsatisfiable"));
        });
    }
}