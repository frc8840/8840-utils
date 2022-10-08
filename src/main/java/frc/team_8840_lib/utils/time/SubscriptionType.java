package frc.team_8840_lib.utils.time;

public enum SubscriptionType {
    AwaitForTime,
    AfterTime,
    BeforeTime,
    BetweenTimes;

    public static boolean isPeriodic(SubscriptionType type) {
        return type == BeforeTime || type == AfterTime || type == BetweenTimes;
    }

    public boolean isPeriodic() {
        return isPeriodic(this);
    }
}
