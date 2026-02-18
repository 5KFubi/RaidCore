package me.fivekfubi.raidcore.Holder;

import java.util.HashMap;
import java.util.Map;

public class MANAGER_Holder {
    public Map<String, HOLDER> holders = new HashMap<>();

    public HOLDER get(String id){
        return holders.get(id);
    }
    public void set(HOLDER holder, String id){
        holder.set(id, holder);
    }
}
