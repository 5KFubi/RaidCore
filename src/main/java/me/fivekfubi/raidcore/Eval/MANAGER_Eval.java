package me.fivekfubi.raidcore.Eval;

import me.fivekfubi.raidcore.Eval.Data.Formula;
import me.fivekfubi.raidcore.Eval.Data.Node;
import me.fivekfubi.raidcore.Eval.Data.Resolver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static me.fivekfubi.raidcore.RaidCore.RANDOM;
import static me.fivekfubi.raidcore.RaidCore.utils;

public class MANAGER_Eval {

    @FunctionalInterface
    private interface Operand { Collection<String> values(Resolver resolver); }

    /** variables = names in the same order as the double[] you pass to eval. Throws if the formula is invalid */
    public Formula compile_math(String expression, List<String> variables) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Math expression cannot be null or blank");
        }
        if (variables == null) {
            throw new IllegalArgumentException("Variables cannot be null");
        }

        List<String> normalized_variables = new ArrayList<>(variables.size());
        Set<String> unique_variables = new HashSet<>();

        for (String variable : variables) {
            if (variable == null || variable.isBlank()) {
                throw new IllegalArgumentException("Variable names cannot be null or blank");
            }

            String normalized_variable = variable.toLowerCase(Locale.ROOT);
            if (!unique_variables.add(normalized_variable)) {
                throw new IllegalArgumentException("Duplicate variable name: " + variable);
            }

            normalized_variables.add(normalized_variable);
        }

        Formula formula = new MathParser(expression, normalized_variables).parse();
        int expected_length = normalized_variables.size();

        return vars -> {
            if (vars == null) {
                throw new IllegalArgumentException("Variables array cannot be null");
            }
            if (vars.length != expected_length) {
                throw new IllegalArgumentException("Expected " + expected_length + " variable values, got " + vars.length);
            }

            double result = formula.eval(vars);
            if (!Double.isFinite(result)) {
                throw new ArithmeticException("Math formula produced a non-finite result");
            }

            return result;
        };
    }

    private static final Node ALWAYS_TRUE = r -> true;
    private static final Node ALWAYS_FALSE = r -> false;

    // keyed by the raw config string, so it never grows with placeholder values
    private final Map<String, Node> cache = new ConcurrentHashMap<>();

    /** empty = true, invalid = false (logged once) */
    public boolean evaluate(String expression, Resolver resolver) {
        try {
            return compile(expression).test(resolver);
        } catch (RuntimeException e) {
            utils.error_message("<red>Failed to evaluate condition: <yellow>" + expression, e);
            return false;
        }
    }

    private static final int MAX_CACHE = 2000;

    public Node compile(String expression) {
        if (expression == null || expression.isBlank()) return ALWAYS_TRUE;
        if (cache.size() >= MAX_CACHE && !cache.containsKey(expression)) return parse(expression);
        return cache.computeIfAbsent(expression, this::parse);
    }

    private Node parse(String expression) {
        try {
            return new Parser(expression).parse();
        } catch (RuntimeException t) {
            utils.error_message("<red>Invalid condition: <yellow>" + expression, t);
            return ALWAYS_FALSE;
        }
    }

    // ---------------------------------------------------------------------------------
    private static class Parser {
        final String s;
        int pos = 0;

        Parser(String s) { this.s = s; }

        Node parse() {
            Node node = or();
            skip();
            if (pos < s.length()) throw error("Unexpected '" + s.charAt(pos) + "'");
            return node;
        }

        // a || b
        Node or() {
            Node left = and();
            while (eat("||")) {
                Node l = left, r = and();
                left = res -> l.test(res) || r.test(res);
            }
            return left;
        }

        // a && b
        Node and() {
            Node left = not();
            while (eat("&&")) {
                Node l = left, r = not();
                left = res -> l.test(res) && r.test(res);
            }
            return left;
        }

        // !a
        Node not() {
            if (eat("!")) {
                Node inner = not();
                return res -> !inner.test(res);
            }
            return primary();
        }

        // ( a ) or a single condition
        Node primary() {
            if (eat("(")) {
                Node inner = or();
                expect(")");
                return inner;
            }
            return condition();
        }

        // %x%.contains('a'|'b')  |  <operand> <op> <operand>  |  <operand>
        Node condition() {
            boolean is_placeholder = peek('%');
            Operand left = operand();

            if (eat(".contains(")) {
                if (!is_placeholder) throw error(".contains only works on %placeholders%");
                List<Predicate<String>> matchers = new ArrayList<>();
                do {
                    matchers.add(glob(quoted().toLowerCase(Locale.ROOT)));
                } while (eat("|"));
                expect(")");

                return res -> {
                    for (String value : left.values(res)) {
                        if (value == null) continue;
                        String lower = value.toLowerCase(Locale.ROOT);
                        for (Predicate<String> m : matchers) if (m.test(lower)) return true;
                    }
                    return false;
                };
            }

            String op = comparator();
            if (op == null) {
                // bare value: true when any value is "true"
                return res -> {
                    for (String v : left.values(res)) {
                        if (v != null && v.equalsIgnoreCase("true")) return true;
                    }
                    return false;
                };
            }

            Operand right = operand();
            return res -> compare(left.values(res), op, right.values(res));
        }

        // %name%  |  'text'  |  5  |  true
        Operand operand() {
            skip();
            if (peek('%')) {
                pos++;
                int end = s.indexOf('%', pos);
                if (end == -1) throw error("Missing closing %");
                String name = s.substring(pos, end);
                pos = end + 1;
                return res -> {
                    Collection<String> values = res.resolve(name);
                    return values == null ? List.of() : values;
                };
            }
            if (peek('\'') || peek('"')) {
                List<String> literal = List.of(quoted());
                return res -> literal;
            }
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || "-._+".indexOf(s.charAt(pos)) >= 0)) pos++;
            if (start == pos) throw error("Expected a value");
            List<String> literal = List.of(s.substring(start, pos));
            return res -> literal;
        }

        String comparator() {
            for (String op : new String[]{"<=", ">=", "==", "!=", "<", ">"}) {
                if (eat(op)) return op;
            }
            return null;
        }

        String quoted() {
            skip();
            if (pos >= s.length() || (s.charAt(pos) != '\'' && s.charAt(pos) != '"')) throw error("Expected a quote");

            char quote = s.charAt(pos++);
            StringBuilder text = new StringBuilder();

            while (pos < s.length()) {
                char c = s.charAt(pos++);

                if (c == quote) return text.toString();

                if (c == '\\' && pos < s.length()) {
                    char escaped = s.charAt(pos++);
                    if (escaped == quote || escaped == '\\') {
                        text.append(escaped);
                    } else {
                        text.append('\\').append(escaped);
                    }
                } else {
                    text.append(c);
                }
            }

            throw error("Missing closing " + quote);
        }

        void skip() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }

        boolean peek(char c) {
            skip();
            return pos < s.length() && s.charAt(pos) == c;
        }

        boolean eat(String token) {
            skip();
            if (s.startsWith(token, pos)) { pos += token.length(); return true; }
            return false;
        }

        void expect(String token) {
            if (!eat(token)) throw error("Expected '" + token + "'");
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + pos + " in: " + s);
        }
    }

    // ---------------------------------------------------------------------------------
    // math formulas: + - * / % ^ ( ) numbers, variables and functions
    // sin cos tan asin acos atan atan2 rad deg work in DEGREES (same as EvalEx)
    // also: sqrt abs floor ceil round min max pow, and the constant PI
    private static class MathParser {
        final String s;
        final List<String> variables;
        int pos = 0;

        MathParser(String s, List<String> variables) {
            this.s = s;
            this.variables = variables.stream().map(variable -> variable.toLowerCase(Locale.ROOT)).toList();
        }

        Formula parse() {
            Formula f = sum();
            skip();
            if (pos < s.length()) throw error("Unexpected '" + s.charAt(pos) + "'");
            return f;
        }

        Formula sum() {
            Formula left = product();
            while (true) {
                if (eat("+")) { Formula l = left, r = product(); left = v -> l.eval(v) + r.eval(v); }
                else if (eat("-")) { Formula l = left, r = product(); left = v -> l.eval(v) - r.eval(v); }
                else return left;
            }
        }

        Formula product() {
            Formula left = unary();
            while (true) {
                if (eat("*")) { Formula l = left, r = unary(); left = v -> l.eval(v) * r.eval(v); }
                else if (eat("/")) { Formula l = left, r = unary(); left = v -> l.eval(v) / r.eval(v); }
                else if (eat("%")) { Formula l = left, r = unary(); left = v -> l.eval(v) % r.eval(v); }
                else return left;
            }
        }

        Formula unary() {
            if (eat("-")) { Formula inner = unary(); return v -> -inner.eval(v); }
            if (eat("+")) return unary();
            return power();
        }

        Formula power() {
            Formula base = atom();
            if (eat("^")) {
                Formula exponent = unary();
                return v -> Math.pow(base.eval(v), exponent.eval(v));
            }
            return base;
        }

        Formula atom() {
            skip();
            if (eat("(")) {
                Formula inner = sum();
                expect(")");
                return inner;
            }

            if (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                int start = pos;
                boolean has_digit = false;

                while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                    has_digit = true;
                }

                if (pos < s.length() && s.charAt(pos) == '.') {
                    pos++;
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                        pos++;
                        has_digit = true;
                    }
                }

                if (!has_digit) throw error("Invalid number");

                if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                    pos++;
                    if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;

                    int exponent_start = pos;
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;

                    if (exponent_start == pos) throw error("Invalid exponent");
                }

                double value;
                try {
                    value = Double.parseDouble(s.substring(start, pos));
                } catch (NumberFormatException e) {
                    throw error("Invalid number");
                }

                return v -> value;
            }

            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) pos++;
            if (start == pos) throw error("Expected a value");
            String name = s.substring(start, pos).toLowerCase(Locale.ROOT);

            if (eat("(")) {
                List<Formula> args = new ArrayList<>();
                if (!eat(")")) {
                    do { args.add(sum()); } while (eat(","));
                    expect(")");
                }
                return function(name, args);
            }

            int index = variables.indexOf(name);
            if (index >= 0) return v -> v[index];
            if (name.equals("pi")) return v -> Math.PI;
            throw error("Unknown variable '" + name + "'");
        }

        Formula function(String name, List<Formula> a) {
            int need = switch (name) {
                case "random" -> 0;
                case "sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "abs", "floor", "ceil", "round", "rad", "deg" -> 1;
                case "min", "max", "pow", "atan2" -> 2;
                default -> throw error("Unknown function '" + name + "'");
            };
            if (a.size() != need) throw error(name + " needs " + need + " argument(s)");

            Formula x = need >= 1 ? a.get(0) : null;
            Formula y = need == 2 ? a.get(1) : null;

            return switch (name) {
                case "random" -> v -> RANDOM.nextDouble();
                case "sin"   -> v -> Math.sin(Math.toRadians(x.eval(v)));
                case "cos"   -> v -> Math.cos(Math.toRadians(x.eval(v)));
                case "tan"   -> v -> Math.tan(Math.toRadians(x.eval(v)));
                case "asin"  -> v -> Math.toDegrees(Math.asin(x.eval(v)));
                case "acos"  -> v -> Math.toDegrees(Math.acos(x.eval(v)));
                case "atan"  -> v -> Math.toDegrees(Math.atan(x.eval(v)));
                case "atan2" -> v -> Math.toDegrees(Math.atan2(x.eval(v), y.eval(v)));
                case "rad"   -> v -> Math.toRadians(x.eval(v));
                case "deg"   -> v -> Math.toDegrees(x.eval(v));
                case "sqrt"  -> v -> Math.sqrt(x.eval(v));
                case "abs"   -> v -> Math.abs(x.eval(v));
                case "floor" -> v -> Math.floor(x.eval(v));
                case "ceil"  -> v -> Math.ceil(x.eval(v));
                case "round" -> v -> Math.round(x.eval(v));
                case "min"   -> v -> Math.min(x.eval(v), y.eval(v));
                case "max"   -> v -> Math.max(x.eval(v), y.eval(v));
                case "pow"   -> v -> Math.pow(x.eval(v), y.eval(v));
                default -> throw error("Unknown function '" + name + "'");
            };
        }

        void skip() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }

        boolean eat(String token) {
            skip();
            if (s.startsWith(token, pos)) { pos += token.length(); return true; }
            return false;
        }

        void expect(String token) {
            if (!eat(token)) throw error("Expected '" + token + "'");
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + pos + " in: " + s);
        }
    }

    // ---------------------------------------------------------------------------------
    // true when ANY left value and ANY right value satisfy the comparison
    private static boolean compare(Collection<String> lefts, String op, Collection<String> rights) {
        for (String l : lefts) {
            if (l == null) continue;
            for (String r : rights) {
                if (r != null && compare(l, op, r)) return true;
            }
        }
        return false;
    }

    private static boolean compare(String l, String op, String r) {
        Double a = number(l), b = number(r);
        if (a != null && b != null) {
            return switch (op) {
                case "<"  -> a < b;
                case "<=" -> a <= b;
                case ">"  -> a > b;
                case ">=" -> a >= b;
                case "==" -> a.doubleValue() == b.doubleValue()
                        || (Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) < 1e-9);
                case "!=" -> !(a.doubleValue() == b.doubleValue()
                        || (Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) < 1e-9));
                default -> false;
            };
        }
        boolean wildcard = r.indexOf('*') != -1;
        return switch (op) {
            case "==" -> wildcard ? glob(r.toLowerCase(Locale.ROOT)).test(l.toLowerCase(Locale.ROOT)) : l.equalsIgnoreCase(r);
            case "!=" -> wildcard ? !glob(r.toLowerCase(Locale.ROOT)).test(l.toLowerCase(Locale.ROOT)) : !l.equalsIgnoreCase(r);
            default -> false;
        };
    }

    private static final Pattern NUMBER = Pattern.compile("[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?");

    private static Double number(String text) {
        if (!NUMBER.matcher(text.trim()).matches()) return null;
        try { return Double.valueOf(text); } catch (NumberFormatException e) { return null; }
    }

    // 'zom*' style wildcard, compiled once at parse time
    public static Predicate<String> glob(String pattern) {
        if (pattern.indexOf('*') == -1) return pattern::equals;
        StringJoiner regex = new StringJoiner(".*");
        for (String part : pattern.split("\\*", -1)) regex.add(Pattern.quote(part));
        return Pattern.compile(regex.toString()).asMatchPredicate();
    }
}