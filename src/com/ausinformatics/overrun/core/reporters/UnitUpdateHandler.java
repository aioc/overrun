package com.ausinformatics.overrun.core.reporters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.phais.utils.Pair;

public class UnitUpdateHandler {

	private Map<Pair<Integer, Integer>, Pair<Unit, Unit>> unitChanges;

	public UnitUpdateHandler() {
		unitChanges = new HashMap<Pair<Integer, Integer>, Pair<Unit, Unit>>();
	}

	public void sendUpdate(Unit oldU, Unit newU) {
		Pair<Integer, Integer> lookup = new Pair<Integer, Integer>(oldU.ownerId, oldU.myId);
		if (unitChanges.containsKey(lookup)) {
			unitChanges.get(lookup).second = newU;
		} else {
			unitChanges.put(lookup, new Pair<Unit, Unit>(oldU, newU));
		}
	}

	public List<Pair<Unit, Unit>> getAllUpdates() {
		List<Pair<Unit, Unit>> changes = new ArrayList<Pair<Unit, Unit>>();
		for (Entry<Pair<Integer, Integer>, Pair<Unit, Unit>> ch : unitChanges.entrySet()) {
			Unit ol = ch.getValue().first;
			Unit ne = ch.getValue().second;
			if (ol.p.equals(ne.p) && ol.strength == ne.strength) {
				continue;
			}
			changes.add(new Pair<Unit, Unit>(ol, ne));
		}
		return changes;
	}

	public void clear() {
		unitChanges.clear();
	}
	
}
