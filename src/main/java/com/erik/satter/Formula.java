package com.erik.satter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Formula {
    private final Set<Clause> clauses;
    private final Map<Character, Boolean> assignment;

    public Formula() {
        clauses = new HashSet<>();
        assignment = new HashMap<>();
    }

    public void parse(String formula) {
        clauses.clear();
        assignment.clear();

        Pattern clausePattern = Pattern.compile("\\(.*?\\)");
        Matcher clauseMatcher = clausePattern.matcher(formula);
        while (clauseMatcher.find()) {
            String clause = clauseMatcher.group();
            clause = clause.substring(1, clause.length() - 1);
            Set<Literal> literals = new HashSet<>();
            Stream.of(clause.split("\\+")).map(s -> new Literal(s.charAt(0), s.length() > 1)).forEachOrdered(literals::add);
            clauses.add(new Formula.Clause(literals));
        }
    }

    public Map<Character, Boolean> getAssignment() {
        if (assignment.isEmpty()) {
            solve(new HashSet<>(Set.copyOf(clauses)));
        }
        return assignment;
    }

    private boolean solve(Set<Clause> clauses) {
        Set<Literal> oneLiterals = clauses.stream().filter(Clause::isSizeOne).map(Clause::getAnyLiteral).collect(Collectors.toSet());
        oneLiterals.forEach(l -> setTrue(clauses, l));

        Set<Literal> literals = clauses.stream().flatMap(Clause::stream).collect(Collectors.toSet());
        Set<Literal> pureLiterals = literals.stream().filter(l -> !literals.contains(l.complemented())).collect(Collectors.toSet());

        Set<Literal> assignedLiterals = new HashSet<>(oneLiterals);
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
        Set<Clause> backup = new HashSet<>(Set.copyOf(clauses));
        backup.add(new Clause(Set.of(literal.complemented())));
        clauses.add(new Clause(Set.of(literal)));

        boolean satisfiable = solve(clauses) || solve(backup);
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

    @Override
    public String toString() {
        return clauses.stream().sorted(Comparator.comparingInt(c -> c.id)).map(String::valueOf).collect(Collectors.joining(" ∧ "));
    }

    public static class Clause {
        private final Set<Literal> literals;

        private final int id = s_id++;
        private static int s_id;

        public Clause(Set<Literal> literals) {
            this.literals = literals;
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
            return "(" + literals.stream().sorted(Comparator.comparingInt(l -> l.variable))
                    .map(String::valueOf).collect(Collectors.joining(" ∨ ")) + ")";
        }
    }

    public static class Literal {
        private final char variable;
        private final boolean complement;

        public Literal(char variable, boolean complement) {
            this.variable = variable;
            this.complement = complement;
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
            if (variable != literal.variable) return false;
            return complement == literal.complement;
        }

        @Override
        public int hashCode() {
            int result = variable;
            result = 31 * result + (complement ? 1 : 0);
            return result;
        }
    }

    public static boolean isValidCNF(String formula) {
        return formula.matches("(\\(\\w'?(\\+\\w'?)*\\))*");
    }
}
