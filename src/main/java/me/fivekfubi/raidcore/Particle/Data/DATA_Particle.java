package me.fivekfubi.raidcore.Particle.Data;

import org.bukkit.Particle;

import java.util.LinkedHashMap;
import java.util.Map;

public class DATA_Particle {
    public String id;
    public Particle particle;

    public int points = 16;
    public int duration = 20;
    public int interval = 2;
    public int count = 1;
    public double speed = 0.0;

    public String x = "0";
    public String y = "0";
    public String z = "0";

    public Map<String, Double> variables = new LinkedHashMap<>();
}