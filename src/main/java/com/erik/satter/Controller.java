package com.erik.satter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {

    @FXML
    GridPane sudoku;
    @FXML
    Button solveButton;
    @FXML
    TextArea formulaText, input, output;

    ExecutorService solverThread;
    Formula sudokuFormula;
    Formula satFormula;
    TextField[][] cells;

    public void initialize() {
        solverThread = Executors.newSingleThreadExecutor();
        initSudoku();
        initSatSolver();

        // DEBUG
        System.gc();
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_000_000.d + " MB");
    }

    private void initSudoku() {
        cells = sudoku.getChildren().stream().filter(e -> e instanceof GridPane).map(e ->
                ((GridPane) e).getChildren().stream().filter(node -> node instanceof TextField).map(node ->
                        (TextField) node).toList().toArray(new TextField[0])).toList().toArray(new TextField[0][]);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int finalI = i, finalJ = j;
                cells[i][j].textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.matches("[1-9]+")) {
                        int index = !oldValue.isEmpty() && newValue.startsWith(oldValue) ? 1 : 0;
                        cells[finalI][finalJ].setText(newValue.charAt(index) + "");
                    } else if (newValue.length() > 0) {
                        cells[finalI][finalJ].setText(oldValue);
                    }
                });
            }
        }

        initSudokuFormula(); // Maybe call in constructor?

        // DEBUG
//        String[] a = {"xxxxxxxxx", "129458637", "543167892", "365921487", "894375216", "271684935", "612549738", "943782561", "758316429"};
//        String[] a = {"876293154", "129458637", "543167892", "365921487", "894375216", "271684935", "612549738", "943782561", "758316429"};
        String[] a = {"987654321", "123789456", "456123789", "213546879", "564897231", "897231564", "798465132", "312978645", "645312978"};
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j].setText(a[i].charAt(j) + "");
            }
        }
    }

    private void initSudokuFormula() {
        StringBuilder formula = new StringBuilder();
        StringJoiner clause = null;

        // TODO: Add clauses directly (without string parsing) and store vars as short instead of strings

        // No empty cells
        for (int b = 1; b <= 9; b++) {
            for (int c = 1; c <= 9; c++) {
                clause = new StringJoiner("+", "(", ")");
                for (int x = 1; x <= 9; x++) {
                    clause.add("" + x + b + c);
                }
                formula.append(clause);
            }
        }
        // No duplicates in blocks
        for (int x = 1; x <= 9; x++) {
            for (int b = 1; b <= 9; b++) {
                clause = new StringJoiner("+", "(", ")");
                for (int c = 1; c <= 9; c++) {
                    clause.add("" + x + b + c);
                }
                formula.append(clause);
            }
        }
        // TODO: Clean up using 2 outer loops of range 2 (http://cse.unl.edu/~choueiry/S17-235H/files/SATslides02.pdf page 9)
        // No duplicates in rows
        for (int x = 1; x <= 9; x++) {
            for (int bi = 0; bi < 9 * 3; bi++) {
                int b = bi % 3 + (bi / 9 * 3 + 1);
                if (b % 3 == 1) clause = new StringJoiner("+", "(", ")");
                int ci = bi / 3 % 3 * 3 + 1;
                for (int c = ci; c < ci + 3; c++) {
                    clause.add("" + x + b + c);
                }
                if (b % 3 == 0) formula.append(clause);
            }
        }
        // No duplicates in columns
        for (int x = 1; x <= 9; x++) {
            for (int bi = 0; bi < 9 * 3; bi++) {
                int bj = bi % 3 + (bi / 9 * 3 + 1);
                if (bj % 3 == 1) clause = new StringJoiner("+", "(", ")");
                int b = (bj - bi / 9 * 3 - 1) * 3 + bi / 9 + 1;
                int ci = bi / 3 % 3 + 1;
                for (int c = ci; c < ci + 9; c += 3) {
                    clause.add("" + x + b + c);
                }
                if (bj % 3 == 0) formula.append(clause);
            }
        }
        // Only one number per cell
        for (int b = 1; b <= 9; b++) {
            for (int c = 1; c <= 9; c++) {
                for (int x = 1; x <= 8; x++) {
                    for (int y = x + 1; y <= 9; y++) {
                        clause = new StringJoiner("+", "(", ")");
                        clause.add("" + x + b + c + "'");
                        clause.add("" + y + b + c + "'");
                        formula.append(clause);
                    }
                }
            }
        }

        sudokuFormula = new Formula(formula.toString());
    }

    @FXML
    private void solveSudoku() {
        // TODO: If sudoku unchanged, return last assignment

        solveButton.setDisable(true);

        StringBuilder clauseBuilder = new StringBuilder();
        for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
                if (!cells[b][c].getText().isEmpty()) {
                    clauseBuilder.append("(").append(cells[b][c].getText()).append(b + 1).append(c + 1).append(")");
                }
            }
        }
        Formula formula = new Formula(clauseBuilder.toString());
        formula.mergeWithCopyOf(sudokuFormula);

        solverThread.execute(() -> {
            var assignment = formula.getAssignment();
            Platform.runLater(() -> {
                if (assignment.isEmpty()) {
                    solveButton.setId("invalid");
                } else {
                    solveButton.setId("valid");
                    assignment.entrySet().stream().filter(Map.Entry::getValue).forEach(e -> {
                        String var = e.getKey();
                        cells[var.charAt(1) - '1'][var.charAt(2) - '1'].setText(var.charAt(0) + "");
                    });
                }
                solveButton.setDisable(false);
            });
        });
    }

    private void initSatSolver() {
        satFormula = new Formula();
        input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Formula.isValidCNF(newValue)) {
                input.setId("valid");
                satFormula.parse(newValue);
                formulaText.setText(satFormula.toString());
                satSolve();
            } else {
                input.setId("invalid");
            }
        });
    }

    private void satSolve() {
        solverThread.execute(() -> {
            var assignment = satFormula.getAssignment();
            Platform.runLater(() -> output.setText(!assignment.isEmpty() ? assignment.toString() : "Unsatisfiable"));
        });
    }
}