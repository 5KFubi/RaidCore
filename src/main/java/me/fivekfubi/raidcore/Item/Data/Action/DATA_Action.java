package me.fivekfubi.raidcore.Item.Data.Action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DATA_Action {
    public Map<String, List<DATA_Action_State>> action_event = new HashMap<>(); // id / event type | data
    public Map<String, List<DATA_Action_State>> action_passive = new HashMap<>();
}
