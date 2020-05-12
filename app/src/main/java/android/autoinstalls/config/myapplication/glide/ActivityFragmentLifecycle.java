package android.autoinstalls.config.myapplication.glide;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


class ActivityFragmentLifecycle implements Lifecycle, LifecycleListener{
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());

    public final static int STATUS_ATTACH = 1;
    public final static int STATUS_CREATE = 2;
    public final static int STATUS_START = 3;
    public final static int STATUS_RESUME = 4;
    public final static int STATUS_PAUSE = 5;
    public final static int STATUS_STOP = 6;
    public final static int STATUS_DESTROY = 7;
    public final static int STATUS_DETACH = 8;

    private volatile int status = 0;

    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void onAttach() {
        status = STATUS_ATTACH;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onAttach();
        }
    }

    @Override
    public void onCreate() {
        status = STATUS_CREATE;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onCreate();
        }
    }

    @Override
    public void onStart() {
        status = STATUS_START;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onStart();
        }
    }

    @Override
    public void onResume() {
        status = STATUS_RESUME;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onResume();
        }
    }

    @Override
    public void onPause() {
        status = STATUS_PAUSE;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onPause();
        }
    }

    @Override
    public void onStop() {
        status = STATUS_STOP;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onStop();
        }
    }

    @Override
    public void onDestroy() {
        status = STATUS_DESTROY;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onDestroy();
        }
    }


    @Override
    public void onDetach() {
        status = STATUS_DETACH;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onDetach();
        }
    }
}
