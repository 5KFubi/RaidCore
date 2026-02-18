package me.fivekfubi.raidcore.Event.Data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class DATA_Location {
    public Location location = null;
    public World world = null;
    public float yaw = 0;
    public float pitch = 0;
    public BlockFace block_face = null;

    public DATA_Location(Location location, BlockFace block_face){
        this.location = location;
        if (location != null){
            this.world = location.getWorld();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }
        this.block_face = block_face;
    }
}
