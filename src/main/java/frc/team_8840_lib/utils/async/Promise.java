package frc.team_8840_lib.utils.async;

import java.util.ArrayList;

import frc.team_8840_lib.utils.interfaces.Callback;
import frc.team_8840_lib.utils.interfaces.Condition;
import frc.team_8840_lib.utils.interfaces.ErrorCallback;
import frc.team_8840_lib.utils.interfaces.PromiseMethod;

public class Promise {
    public static void WaitThen(Condition cond, Callback res, ErrorCallback rej) {
        WaitThen(cond, res, rej, 10);
    }
    
    public static void WaitThen(Condition cond, Callback res, ErrorCallback rej, int delay) {
        //create a new thread to avoid blocking the main thread
        new Thread(() -> {
            try {
                while (!cond.isTrue()) {
                    Thread.sleep(delay);
                }

                res.run();
            } catch (Exception e) {
                rej.onError(e);
            }
        }).start();
    }

    private boolean isResolved = false;

    private ErrorCallback userErrorCallback;

    private PromiseMethod primaryPromiseMethod;
    
    private ArrayList<PromiseMethod> additionalCallbacks = new ArrayList<>();
    private int addtionalCallbackIndex = 0;

    public Promise(PromiseMethod method) {
        primaryPromiseMethod = method;
    }

    public Promise then(PromiseMethod callback) {
        additionalCallbacks.add(callback);
        return this;
    }

    public Promise finish() {
        start();
        return this;
    }

    public Promise finish(PromiseMethod method) {
        then(method);
        start();
        return this;
    }

    public boolean resolved() {
        return isResolved;
    }

    public Promise catch_err(ErrorCallback callback) {
        userErrorCallback = callback;

        start();

        return this;
    }

    private void start() {
        primaryPromiseMethod.run(this::next, (e) -> {
            onError(e);
        });
    }

    private void onError(Exception e) {
        if (userErrorCallback != null) {
            userErrorCallback.onError(e);

            isResolved = true;
        } else {
            throw new RuntimeException(e);
        }
    }

    private void next() {
        if (additionalCallbacks.size() == 0) {
            isResolved = true;
            return;
        }

        if (addtionalCallbackIndex >= additionalCallbacks.size()) {
            isResolved = true;
            return;
        }

        addtionalCallbackIndex++;

        additionalCallbacks.get(addtionalCallbackIndex - 1).run(this::next, (e) -> {
            onError(e);
        });
    }
}
