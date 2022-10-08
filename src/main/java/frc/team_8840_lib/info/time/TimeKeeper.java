package frc.team_8840_lib.info.time;

import edu.wpi.first.wpilibj.Timer;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.interfaces.Callback;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.time.SubscriptionType;

import java.sql.Time;
import java.util.Date;
import java.util.HashMap;

public class TimeKeeper {
    private static TimeKeeper instance = null;

    public static TimeKeeper getInstance() {
        return instance;
    }

    public static void init() {
        if (instance == null) {
            instance = new TimeKeeper();
        }
    }

    private HashMap<String, Timer> timers;

    private TimeKeeper() {
        timers = new HashMap<>();

        createTimer("main");
        createTimer("auto");
        createTimer("teleop");
        createTimer("test");

        resetAndStartTimer("main");

        CommunicationManager.getInstance().updateStatus("Time Keeper", "Running");

        subscriptions = new HashMap<>();
    }

    public void resetGameTimers() {
        resetTimer("auto");
        resetTimer("teleop");
        resetTimer("test");
    }

    public void createTimer(String name) {
        timers.put(name, new Timer());
    }

    public void resetTimer(String name) {
        timers.get(name).reset();
    }

    public void startTimer(String name) {
        timers.get(name).start();
    }

    public void resetAndStartTimer(String name) {
        resetTimer(name);
        startTimer(name);
    }

    public void stopTimer(String name) {
        timers.get(name).stop();
    }

    public double get(String name) {
        return timers.get(name).get();
    }

    public Date time() {
        return new Date();
    }

    public double getRealTime() {
        return time().getTime();
    }

    public String getRealTimeStr() {
        return time().toString();
    }

    public double getRobotTime() {
        return get("main");
    }

    public double getPhaseTime(GamePhase phase) {
        switch (phase) {
            case Autonomous:
                return get("auto");
            case Teleop:
                return get("teleop");
            case Test:
                return get("test");
            default:
                return 0;
        }
    }

    public void changePhaseTimers(GamePhase phase) {
        stopTimer("auto");
        stopTimer("teleop");
        stopTimer("test");
        resetGameTimers();
        if (phase != GamePhase.Disabled) resetAndStartTimer(phase.getTimerName());
    }

    private HashMap<String, Subscription> subscriptions;

    public TimeKeeper subscribe(String key, String timer, double time, SubscriptionType type, Callback callback) {
        subscriptions.put(key, new Subscription(key, timer, time, type, callback));

        Logger.Log("Subscribed event '" + key + "' for " + timer + ".");

        return this;
    }

    public TimeKeeper subscribe(String key, String timer, double time, SubscriptionType type, Callback callback, Callback onceFinished) {
        if (type != SubscriptionType.BeforeTime) throw new IllegalArgumentException("Only BeforeTime subscriptions are supported for a OnceFinished subscription.");
        subscriptions.put(key, new Subscription(key, timer, time, type, callback));
        //need to add some case to prevent the above being called again after the onceFinished is called
        subscriptions.put(key + "_onceFinished", new Subscription(key + "_onceFinished", timer, time, SubscriptionType.AwaitForTime, onceFinished));

        Logger.Log("Subscribed event '" + key + "' and '" + key + "_onceFinished' for " + timer + ".");

        return this;
    }

    public TimeKeeper subscribe(String key, String timer, double startTime, double endTime, SubscriptionType type, Callback callback) {
        if (type != SubscriptionType.BetweenTimes) throw new IllegalArgumentException("Only BetweenTimes subscriptions are supported for a BetweenTimes subscription.");
        Subscription newSub = new Subscription(key, timer, startTime, type, callback);
        newSub.setEndTime(endTime);
        subscriptions.put(key, newSub);

        return this;
    }

    public TimeKeeper subscribe(String key, String timer, double startTime, double endTime, SubscriptionType type, Callback callback, Callback onceFinished) {
        if (type != SubscriptionType.BetweenTimes) throw new IllegalArgumentException("Only BetweenTimes subscriptions are supported for a BetweenTimes subscription.");

        Subscription newSub = new Subscription(key, timer, startTime, type, callback);
        newSub.setEndTime(endTime);
        subscriptions.put(key, newSub);

        subscriptions.put(key + "_onceFinished", new Subscription(key + "_onceFinished", timer, endTime, SubscriptionType.AwaitForTime, onceFinished));

        Logger.Log("Subscribed event '" + key + "' and '" + key + "_onceFinished' for " + timer + ".");

        return this;
    }

    public void checkSubscribers(GamePhase currentPhase) {
        subscriptions.values().forEach(sub -> {
            if (sub.timer.equals(currentPhase.getTimerName())) {
                sub.run();
            }
        });
    }

    public void unsubscribe(String key) {
        subscriptions.remove(key);
    }

    public void resubscribe(String key) {
        if (!subscriptions.containsKey(key)) {
            Logger.Log("Cannot resubscribe '" + key + "' because it does not exist.");
            return;
        }

        subscriptions.get(key).hasRun = false;
        subscriptions.get(key).timesRan = 0;

        if (subscriptions.containsKey(key + "_onceFinished")) {
            subscriptions.get(key + "_onceFinished").hasRun = false;
            subscriptions.get(key + "_onceFinished").timesRan = 0;
        }
    }

    public class Subscription {
        private String name;
        private double time;
        private SubscriptionType type;
        private Callback callback;
        private String timer;

        private boolean isPeriodic;
        private boolean hasRun = false;

        public boolean ignoreNextRun = false;

        private int timesRan = 0;

        private double maxTime = 0;
        public void setEndTime(double t) {
            if (type != SubscriptionType.BetweenTimes) throw new IllegalArgumentException("Cannot set end time for a non-BetweenTimes subscription.");
            maxTime = t;
        }

        public Subscription(String name, String timer, double time, SubscriptionType type, Callback callback) {
            this.name = name;
            this.time = time;
            this.type = type;
            this.timer = timer; //Used to track which timer the subscription is for
            this.callback = callback;

            isPeriodic = type.isPeriodic();
        }

        public void run() {
            if (!isPeriodic && hasRun) return;

            if (ignoreNextRun) return;

            double currentTime = TimeKeeper.getInstance().get(timer);

            if (type == SubscriptionType.AwaitForTime || type == SubscriptionType.AfterTime || type == SubscriptionType.BetweenTimes) {
                if (currentTime >= time) {
                    if (type == SubscriptionType.BetweenTimes && currentTime > maxTime) return;

                    hasRun = true;
                    timesRan++;
                    callback.run();
                }
            } else if (type == SubscriptionType.BeforeTime) {
                if (currentTime <= time) {
                    hasRun = true;
                    timesRan++;
                    callback.run();
                }
            }
        }

        public String getName() {
            return name;
        }

        public double getTime() {
            return time;
        }

        public SubscriptionType getType() {
            return type;
        }

        public Callback getCallback() {
            return callback;
        }

        public boolean hasRun() {
            return hasRun;
        }

        public int timesRan() {
            return timesRan;
        }
    }
}
