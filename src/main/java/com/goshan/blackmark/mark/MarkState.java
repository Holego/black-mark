package com.goshan.blackmark.mark;

/**
 * The mark has no fixed body. It drifts between the two.
 */
public enum MarkState {

    /** Mist. Cannot be grasped at all - the cursor passes through it. */
    FOG,

    /** It has taken a shell. You may lift it, and it will come back anyway. */
    FLESH;

    public MarkState flip() {
        return this == FOG ? FLESH : FOG;
    }

    public static MarkState byId(int id) {
        return id == 1 ? FLESH : FOG;
    }

    public int id() {
        return this == FLESH ? 1 : 0;
    }
}
