package me.fivekfubi.raidcore.Eval.Data;

@FunctionalInterface
public interface Formula {
    double eval(double[] vars);
}