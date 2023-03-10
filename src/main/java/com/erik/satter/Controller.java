package com.erik.satter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.erik.satter.Utils.Lit;

public class Controller {

    @FXML
    GridPane sudoku;
    TextField[][] cells;
    Vec<IVecInt> sudokuFormula;
    ISolver sudokuSolver;
    @FXML
    Button solveButton;

    @FXML
    TextArea formulaText, input, output;
    Formula satFormula;

    ExecutorService solverThread;

    public Controller() {
        sudokuSolver = SolverFactory.newDefault();
        solverThread = Executors.newSingleThreadExecutor();
    }

    public void initialize() {
        initSudoku();
        initSudokuFormula();
        initSatSolver();
    }

    private void initSudoku() {
        TextField[][] gridCells = sudoku.getChildren().stream().filter(e -> e instanceof GridPane).map(e ->
                ((GridPane) e).getChildren().stream().filter(node -> node instanceof TextField).map(node ->
                        (TextField) node).toArray(TextField[]::new)).toArray(TextField[][]::new);

        cells = new TextField[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextField cell = gridCells[r / 3 * 3 + c / 3][r % 3 * 3 + c % 3];
                cell.textProperty().addListener((o, oldValue, newValue) -> {
                    if (newValue.matches("[1-9]+")) {
                        int index = !oldValue.isEmpty() && newValue.startsWith(oldValue) ? 1 : 0;
                        cell.setText(newValue.charAt(index) + "");
                    } else if (newValue.length() > 0) {
                        cell.setText(oldValue);
                    }
                });
                cell.addEventFilter(KeyEvent.ANY, e -> {
                    if (e.getCode() == KeyCode.Z && e.isShortcutDown()) e.consume(); // Prevents crash
                    if (e.getCode() == KeyCode.BACK_SPACE || e.getCode() == KeyCode.DELETE) cell.clear();
                });
                cells[r][c] = cell;
            }
        }
    }

    private void initSudokuFormula() {
        sudokuFormula = new Vec<>(3240);

        IntStream.range(1, 10).forEach(r -> IntStream.range(0, 27).mapToObj(c -> IntStream.range(1, 10).map(v -> {
            if (c < 9) return Lit.of(r, c + 1, v);
            else if (c < 18) return Lit.of(r, v, c - 8);
            return Lit.of(v, r, c - 17);
        }).toArray()).forEach(clause -> sudokuFormula.push(new VecInt(clause))));

        IntStream.range(1, 10).forEach(r -> IntStream.range(1, 10).forEach(c -> IntStream.range(1, 9).forEach(v1 ->
                IntStream.range(v1 + 1, 10).forEach(v2 -> sudokuFormula.push(new VecInt(new int[]{Lit.compOf(r, c, v1), Lit.compOf(r, c, v2)}))))));

        IntStream.range(0, 3).forEach(i -> IntStream.range(0, 3).forEach(j -> IntStream.range(1, 10).mapToObj(v ->
                IntStream.range(1, 4).flatMap(r -> IntStream.range(1, 4).map(c -> Lit.of(r + 3 * i, c + 3 * j, v)))
                        .toArray()).forEach(clause -> sudokuFormula.push(new VecInt(clause)))));
    }

    private void initSatSolver() {
        satFormula = new Formula();
        input.textProperty().addListener((o, oldValue, newValue) -> {
            if (Formula.isValidCNF(newValue)) {
                input.setId("valid");
                satFormula.parse(newValue);
                formulaText.setText(satFormula.toString());
                solverThread.execute(() -> {
                    var assignment = satFormula.getAssignment();
                    Platform.runLater(() -> output.setText(!assignment.isEmpty() ? assignment.toString() : "Unsatisfiable"));
                });
            } else {
                input.setId("invalid");
            }
        });
    }

    @FXML
    private void solveSudoku() throws ContradictionException {
        solveButton.setDisable(true);

        Vec<IVecInt> formula = new Vec<>(sudokuFormula.size());
        sudokuFormula.copyTo(formula);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (!cells[r][c].getText().isEmpty()) {
                    formula.push(new VecInt(new int[]{Lit.of(r + 1, c + 1, Integer.parseInt(cells[r][c].getText()))}));
                }
            }
        }
        sudokuSolver.addAllClauses(formula);

        solverThread.execute(() -> {
            boolean satisfiable = false;
            try {
                satisfiable = sudokuSolver.isSatisfiable();
            } catch (TimeoutException e) {
                System.out.println(e.getLocalizedMessage());
            }
            boolean finalSatisfiable = satisfiable;
            Platform.runLater(() -> {
                if (finalSatisfiable) {
                    IntStream.of(sudokuSolver.model()).filter(Lit::isTrue).forEach(var ->
                            cells[Lit.row(var) - 1][Lit.col(var) - 1].setText(Lit.val(var) + ""));
                    solveButton.setId("valid");
                } else {
                    solveButton.setId("invalid");
                }
                solveButton.setDisable(false);
            });
            sudokuSolver.reset();
        });
    }

    @FXML
    private void clearSudoku() {
        Stream.of(cells).flatMap(Stream::of).forEach(TextField::clear);
    }
}