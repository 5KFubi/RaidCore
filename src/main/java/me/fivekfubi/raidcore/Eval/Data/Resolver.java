package me.fivekfubi.raidcore.Eval.Data;

import java.util.Collection;

/** gives the value(s) of a %placeholder% (name without the %) */
@FunctionalInterface
public interface Resolver {
    Collection<String> resolve(String name);
}