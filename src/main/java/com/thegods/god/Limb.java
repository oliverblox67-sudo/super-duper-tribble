package com.thegods.god;

/** Gli arti che si possono recidere e sostituire con costrutti divini. */
public enum Limb {
    RIGHT_ARM("right_arm", true),
    LEFT_ARM("left_arm", true),
    RIGHT_LEG("right_leg", false),
    LEFT_LEG("left_leg", false);

    public static final Limb[] ALL = values();

    private final String id;
    private final boolean arm;

    Limb(String id, boolean arm) {
        this.id = id;
        this.arm = arm;
    }

    public String id() {
        return id;
    }

    public boolean isArm() {
        return arm;
    }

    public String nameKey() {
        return "limb.thegods." + id;
    }
}
