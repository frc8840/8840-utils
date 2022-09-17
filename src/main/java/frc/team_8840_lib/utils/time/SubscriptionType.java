package frc.team_8840_lib.utils.time;

public enum SubscriptionType {
    AwaitForTime,
    AfterTime,
    BeforeTime;

    public static boolean isPeriodic(SubscriptionType type) {
        return type == BeforeTime || type == AfterTime;
    }

    public boolean isPeriodic() {
        return isPeriodic(this);
    }
}
