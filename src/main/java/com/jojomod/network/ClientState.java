package com.jojomod.network;

/** Copia local (cliente) dos dados do jogador e estado visual. Classe comum, sem codigo de cliente. */
public final class ClientState {
    public static SyncPayload sync = SyncPayload.EMPTY;
    public static boolean timeStopped = false;
    public static int wheelIndex = 0;

    private ClientState() {}

    public static void reset() {
        sync = SyncPayload.EMPTY;
        timeStopped = false;
        wheelIndex = 0;
    }
}
