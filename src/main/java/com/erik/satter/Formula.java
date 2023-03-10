package com.erik.satter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Formula {
    private final Set<Clause> clauses;
    private final Map<String, Boolean> assignment;

    public Formula() {
        clauses = new HashSet<>();
        assignment = new HashMap<>();
    }

    public static boolean isValidCNF(String formula) {
        return formula.matches("(\\(\\w+'?(\\+\\w+'?)*\\))*");
    }

    public void parse(String formula) {
        clear();
        if (formula.length() == 0) return;
        formula = formula.substring(1, formula.length() - 1);
        Stream.of(formula.split("\\)\\(")).map(Clause::new).forEach(clauses::add);
    }

    public Map<String, Boolean> getAssignment() {
        if (assignment.isEmpty()) {
            solve(clauses);
        }
        return assignment;
    }

    private void clear() {
        clauses.clear();
        assignment.clear();
    }

    private boolean solve(Set<Clause> clauses) {
        Set<Literal> assignedLiterals = new HashSet<>();

        do {
            Set<Literal> oneLiterals = clauses.stream().filter(Clause::isSizeOne).map(Clause::getAnyLiteral).collect(Collectors.toSet());
            assignedLiterals.addAll(oneLiterals);
            oneLiterals.forEach(l -> setTrue(clauses, l));
        } while (clauses.stream().anyMatch(Clause::isSizeOne));

        Set<Literal> literals = clauses.stream().flatMap(Clause::stream).collect(Collectors.toSet());
        Set<Literal> pureLiterals = literals.stream().filter(l -> !literals.contains(l.complemented())).collect(Collectors.toSet());

        assignedLiterals.addAll(pureLiterals);
        for (Literal l : pureLiterals) {
            if (!setTrue(clauses, l)) {
                assignedLiterals.remove(l);
            }
        }

        var variableMap = assignedLiterals.stream().collect(Collectors.toMap(l -> l.variable, l -> !l.complement, (l1, l2) -> l1));

        if (clauses.isEmpty()) {
            assignment.putAll(variableMap);
            return true;
        }
        if (clauses.stream().anyMatch(Clause::isEmpty)) {
            return false;
        }

        Literal literal = clauses.stream().findAny().get().getAnyLiteral();
        Set<Clause> clausesCopy = getCopyOfClauses();
        clausesCopy.add(new Clause(literal.complemented()));
        clauses.add(new Clause(literal));

        boolean satisfiable = solve(clauses) || solve(clausesCopy);
        if (satisfiable) {
            assignment.putAll(variableMap);
        }
        return satisfiable;
    }

    private boolean setTrue(Set<Clause> clauses, Literal literal) {
        boolean removed = false;
        var it = clauses.iterator();
        while (it.hasNext()) {
            Clause clause = it.next();
            if (clause.contains(literal)) {
                it.remove();
                removed = true;
                continue;
            }
            Literal complement = literal.complemented();
            if (clause.contains(complement)) {
                clause.remove(complement);
                removed = true;
            }
        }
        return removed;
    }

    private Set<Clause> getCopyOfClauses() {
        return clauses.stream().map(Clause::getCopy).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return clauses.stream().sorted(Comparator.comparingInt(c -> c.id)).map(String::valueOf).collect(Collectors.joining(" ∧ "));
    }

    private static class Clause {
        private final Set<Literal> literals;

        private final int id = s_id++;
        private static int s_id;

        public Clause(Set<Literal> literals) {
            this.literals = literals;
        }

        public Clause(Literal... literals) {
            this.literals = new HashSet<>(Set.of(literals));
        }

        public Clause(String clauseString) {
            this(Stream.of(clauseString.split("\\+")).map(Literal::new).toArray(Literal[]::new));
        }

        public boolean contains(Literal literal) {
            return literals.contains(literal);
        }

        public void remove(Literal literal) {
            literals.remove(literal);
        }

        public boolean isSizeOne() {
            return literals.size() == 1;
        }

        public Literal getAnyLiteral() {
            return literals.stream().findAny().orElse(null);
        }

        public boolean isEmpty() {
            return literals.isEmpty();
        }

        public Stream<Literal> stream() {
            return literals.stream();
        }

        public Clause getCopy() {
            return new Clause(stream().collect(Collectors.toSet()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Clause clause = (Clause) o;
            return literals.equals(clause.literals);
        }

        @Override
        public int hashCode() {
            return literals.hashCode();
        }

        @Override
        public String toString() {
            return "(" + literals.stream().sorted(Comparator.comparing(l -> l.variable))
                    .map(String::valueOf).collect(Collectors.joining(" ∨ ")) + ")";
        }
    }

    private static class Literal {
        private final String variable;
        private final boolean complement;

        public Literal(String variable, boolean complement) {
            this.variable = variable;
            this.complement = complement;
        }

        public Literal(String literal) {
            boolean comp = literal.endsWith("'");
            variable = comp ? literal.substring(0, literal.length() - 1) : literal;
            complement = comp;
        }

        public Literal complemented() {
            return new Literal(variable, !complement);
        }

        @Override
        public String toString() {
            return (complement ? "¬" : "") + variable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Literal literal = (Literal) o;
            if (!Objects.equals(variable, literal.variable)) return false;
            return complement == literal.complement;
        }

        @Override
        public int hashCode() {
            int result = variable.hashCode();
            result = 31 * result + (complement ? 1 : 0);
            return result;
        }
    }
}
