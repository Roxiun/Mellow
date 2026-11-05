package com.roxiun.mellow.gamestate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PartyState {

    public enum PartyRole {
        LEADER,
        MOD,
        MEMBER,
    }

    private static final PartyState EMPTY = new PartyState(
        false,
        null,
        Collections.emptyMap()
    );

    private final boolean inParty;
    private final UUID leader;
    private final Map<UUID, PartyRole> members;

    public PartyState(boolean inParty, UUID leader, Map<UUID, PartyRole> members) {
        this.inParty = inParty;
        this.leader = leader;
        this.members = Collections.unmodifiableMap(new java.util.HashMap<>(members));
    }

    public static PartyState empty() {
        return EMPTY;
    }

    public boolean isInParty() {
        return inParty;
    }

    public UUID getLeader() {
        return leader;
    }

    public Map<UUID, PartyRole> getMembers() {
        return members;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PartyState)) {
            return false;
        }
        PartyState other = (PartyState) obj;
        return (
            inParty == other.inParty &&
            Objects.equals(leader, other.leader) &&
            Objects.equals(members, other.members)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(inParty, leader, members);
    }
}
