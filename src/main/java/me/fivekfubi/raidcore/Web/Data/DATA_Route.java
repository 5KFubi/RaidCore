package me.fivekfubi.raidcore.Web.Data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A single registered REST endpoint under a site's /api/ prefix.
 */
public class DATA_Route {
    public String method;          // "GET" | "POST" | "PUT" | "DELETE"
    public String[] path_segments; // e.g. {"markers"} or {"markers", ":id"}
    public Logic logic;

    public DATA_Route(String method, String[] path_segments, Logic logic) {
        this.method = method;
        this.path_segments = path_segments;
        this.logic = logic;
    }

    /**
     * id is the value of the ":id" path segment if this route declared one, else null.
     * body is the parsed JSON request body for POST/PUT (empty JsonObject if none/unparseable), else null.
     * query is the raw query string (may be null).
     * Return null to have the site respond 404 "not found" (useful for "not found by id" cases).
     * Any other JsonElement is sent as 201 for POST, 200 otherwise.
     */
    @FunctionalInterface
    public interface Logic {
        JsonElement run(String id, JsonObject body, String query) throws Exception;
    }
}