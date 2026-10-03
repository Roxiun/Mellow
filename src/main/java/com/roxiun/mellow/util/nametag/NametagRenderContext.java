package com.roxiun.mellow.util.nametag;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.api.seraph.SeraphClientType;

public final class NametagRenderContext {

    private static final ThreadLocal<State> CURRENT_STATE = new ThreadLocal<>();

    private static final ThreadLocal<String> LABEL = new ThreadLocal<>();
    public static void beginLabel(String label) { LABEL.set(label); }
    public static void endLabel() { LABEL.remove(); }
    public static String getRenderedLabel() { return LABEL.get(); }

    private NametagRenderContext() {}

    public static void setState(
        RgbaColor color,
        SeraphClientType clientType,
        boolean clientIconLeft,
        String primaryLabelText
    ) {
        if (color == null && clientType == null) {
            clear();
            return;
        }
        CURRENT_STATE.set(
            new State(color, clientType, clientIconLeft, primaryLabelText)
        );
    }

    public static RgbaColor getColor() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.color;
    }

    public static SeraphClientType getClientType() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.clientType;
    }

    public static boolean isClientIconLeft() {
        State state = CURRENT_STATE.get();
        return state == null || state.clientIconLeft;
    }

    public static String getPrimaryLabelText() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.primaryLabelText;
    }

    public static boolean isActive() {
        return CURRENT_STATE.get() != null;
    }

    public static void clear() {
        CURRENT_STATE.remove();
        LABEL.remove();
    }

    private static final class State {

        private final RgbaColor color;
        private final SeraphClientType clientType;
        private final boolean clientIconLeft;
        private final String primaryLabelText;

        private State(
            RgbaColor color,
            SeraphClientType clientType,
            boolean clientIconLeft,
            String primaryLabelText
        ) {
            this.color = color;
            this.clientType = clientType;
            this.clientIconLeft = clientIconLeft;
            this.primaryLabelText = primaryLabelText;
        }
    }
}
