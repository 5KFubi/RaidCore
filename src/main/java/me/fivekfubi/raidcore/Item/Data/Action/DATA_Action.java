package me.fivekfubi.raidcore.Item.Data.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DATA_Action implements Cloneable{
    public Map<String, List<DATA_Action_State>> action_event = new HashMap<>(); // id / event type | data
    public Map<String, List<DATA_Action_State>> action_passive = new HashMap<>();

    @Override
    public DATA_Action clone() {
        try {
            DATA_Action copy = (DATA_Action) super.clone();

            if (this.action_event != null) {
                copy.action_event = new HashMap<>();
                for (Map.Entry<String, List<DATA_Action_State>> entry : this.action_event.entrySet()) {
                    List<DATA_Action_State> newList = null;
                    if (entry.getValue() != null) {
                        newList = new ArrayList<>();
                        for (DATA_Action_State state : entry.getValue()) {
                            newList.add(state == null ? null : state.clone());
                        }
                    }
                    copy.action_event.put(entry.getKey(), newList);
                }
            }

            if (this.action_passive != null) {
                copy.action_passive = new HashMap<>();
                for (Map.Entry<String, List<DATA_Action_State>> entry : this.action_passive.entrySet()) {
                    List<DATA_Action_State> newList = null;
                    if (entry.getValue() != null) {
                        newList = new ArrayList<>();
                        for (DATA_Action_State state : entry.getValue()) {
                            newList.add(state == null ? null : state.clone());
                        }
                    }
                    copy.action_passive.put(entry.getKey(), newList);
                }
            }

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
