/*
 * This file is part of FlightControl, which is licensed under the MIT License.
 * Copyright (c) 2023 Spazzinq
 */

package org.spazzinq.flightcontrol.check.combat;

import org.spazzinq.flightcontrol.api.object.Cause;
import org.spazzinq.flightcontrol.check.Check;

public abstract class CombatCheck extends Check {
    @Override public Cause getCause() {
        return Cause.COMBAT;
    }
}
