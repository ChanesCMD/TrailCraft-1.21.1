package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.inventory.DeathRecord;

/** Последняя смерть, пришедшая с сервера вместе с журналом смертей. */
public final class ClientMapMarkers {
    private static DeathRecord lastDeath;

    public static void setLastDeath(DeathRecord death) {
        lastDeath = death;
    }

    public static DeathRecord lastDeath() {
        return lastDeath;
    }

    private ClientMapMarkers() {}
}
