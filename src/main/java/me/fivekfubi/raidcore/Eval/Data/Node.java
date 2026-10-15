package me.fivekfubi.raidcore.Eval.Data;

@FunctionalInterface
public interface Node {
    boolean test(Resolver resolver);
}