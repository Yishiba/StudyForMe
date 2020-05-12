package android.autoinstalls.config.myapplication.glide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class LifecycleManager implements Handler.Callback {
    @VisibleForTesting
    static final String FRAGMENT_TAG = "com.bumptech.glide.manager";
    private static final String TAG = "RMRetriever";

    private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

    // Hacks based on the implementation of FragmentManagerImpl in the non-support libraries that
    // allow us to iterate over and retrieve all active Fragments in a FragmentManager.
    private static final String FRAGMENT_INDEX_KEY = "key";

    private static LifecycleManager INSTANCE = null;
    /**
     * Pending adds for RequestManagerFragments.
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    final Map<android.app.FragmentManager, ManagerFragment> pendingRequestManagerFragments =
            new HashMap<>();

    /**
     * Pending adds for SupportRequestManagerFragments.
     */
    @VisibleForTesting
    final Map<FragmentManager, SupportManagerFragment> pendingSupportRequestManagerFragments =
            new HashMap<>();

    /**
     * Main thread handler to handle cleaning up pending fragment maps.
     */
    private final Handler handler;


    /**
     * Objects used to find Fragments and Activities containing views.
      */
    private final ArrayMap<View, Fragment> tempViewToSupportFragment = new ArrayMap<>();
    private final ArrayMap<View, android.app.Fragment> tempViewToFragment = new ArrayMap<>();
    private final Bundle tempBundle = new Bundle();

    private static class SingletonHolder{
        private static final LifecycleManager INSTANCE = new LifecycleManager();
    }

    private LifecycleManager() {
        handler = new Handler(Looper.getMainLooper(), this);
    }

    public static LifecycleManager getInstance(){
        return SingletonHolder.INSTANCE;
    }

    @NonNull
    private Lifecycle getApplicationManager(@NonNull Context context) {
        // Either an application context or we're on a background thread.
        //todo 处理上下文是application的生命周期情况
        return new ApplicationLifecycle();
    }

    @NonNull
    public Lifecycle get(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        } else if (Util.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context);
            } else if (context instanceof Activity) {
                return get((Activity) context);
            } else if (context instanceof ContextWrapper
                    && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
                return get(((ContextWrapper) context).getBaseContext());
            }
        }
        return getApplicationManager(context);
    }

    @NonNull
    public Lifecycle get(@NonNull FragmentActivity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            assertNotDestroyed(activity);
            FragmentManager fm = activity.getSupportFragmentManager();
            return supportFragmentGet(fm, null, isActivityVisible(activity));
        }
    }

    @NonNull
    public Lifecycle get(@NonNull Fragment fragment) {
        Preconditions.checkNotNull(
                fragment.getContext(),
                "You cannot start a load on a fragment before it is attached or after it is destroyed");
        if (Util.isOnBackgroundThread()) {
            return get(fragment.getContext().getApplicationContext());
        } else {
            FragmentManager fm = fragment.getChildFragmentManager();
            return supportFragmentGet(fm, fragment, fragment.isVisible());
        }
    }

    @SuppressWarnings("deprecation")
    @NonNull
    public Lifecycle get(@NonNull Activity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            assertNotDestroyed(activity);
            android.app.FragmentManager fm = activity.getFragmentManager();
            return fragmentGet(fm, null, isActivityVisible(activity));
        }
    }

    @SuppressWarnings("deprecation")
    @NonNull
    public Lifecycle get(@NonNull View view) {
        if (Util.isOnBackgroundThread()) {
            return get(view.getContext().getApplicationContext());
        }

        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(
                view.getContext(), "Unable to obtain a request manager for a view without a Context");
        Activity activity = findActivity(view.getContext());
        // The view might be somewhere else, like a service.
        if (activity == null) {
            return get(view.getContext().getApplicationContext());
        }

        // Support Fragments.
        // Although the user might have non-support Fragments attached to FragmentActivity, searching
        // for non-support Fragments is so expensive pre O and that should be rare enough that we
        // prefer to just fall back to the Activity directly.
        if (activity instanceof FragmentActivity) {
            Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
            return fragment != null ? get(fragment) : get((FragmentActivity) activity);
        }

        // Standard Fragments.
        android.app.Fragment fragment = findFragment(view, activity);
        if (fragment == null) {
            return get(activity);
        }
        return get(fragment);
    }

    private static void findAllSupportFragmentsWithViews(
            @Nullable Collection<Fragment> topLevelFragments, @NonNull Map<View, Fragment> result) {
        if (topLevelFragments == null) {
            return;
        }
        for (Fragment fragment : topLevelFragments) {
            // getFragment()s in the support FragmentManager may contain null values, see #1991.
            if (fragment == null || fragment.getView() == null) {
                continue;
            }
            result.put(fragment.getView(), fragment);
            findAllSupportFragmentsWithViews(fragment.getChildFragmentManager().getFragments(), result);
        }
    }

    @Nullable
    private Fragment findSupportFragment(@NonNull View target, @NonNull FragmentActivity activity) {
        tempViewToSupportFragment.clear();
        findAllSupportFragmentsWithViews(
                activity.getSupportFragmentManager().getFragments(), tempViewToSupportFragment);
        Fragment result = null;
        View activityRoot = activity.findViewById(android.R.id.content);
        View current = target;
        while (!current.equals(activityRoot)) {
            result = tempViewToSupportFragment.get(current);
            if (result != null) {
                break;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }

        tempViewToSupportFragment.clear();
        return result;
    }

    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    @Deprecated
    @Nullable
    private android.app.Fragment findFragment(@NonNull View target, @NonNull Activity activity) {
        tempViewToFragment.clear();
        findAllFragmentsWithViews(activity.getFragmentManager(), tempViewToFragment);

        android.app.Fragment result = null;

        View activityRoot = activity.findViewById(android.R.id.content);
        View current = target;
        while (!current.equals(activityRoot)) {
            result = tempViewToFragment.get(current);
            if (result != null) {
                break;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
        tempViewToFragment.clear();
        return result;
    }

    // TODO: Consider using an accessor class in the support library package to more directly retrieve
    // non-support Fragments.
    @SuppressWarnings("deprecation")
    @Deprecated
    @TargetApi(VERSION_CODES.O)
    private void findAllFragmentsWithViews(
            @NonNull android.app.FragmentManager fragmentManager,
            @NonNull ArrayMap<View, android.app.Fragment> result) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            for (android.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment.getView() != null) {
                    result.put(fragment.getView(), fragment);
                    findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
                }
            }
        } else {
            findAllFragmentsWithViewsPreO(fragmentManager, result);
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    private void findAllFragmentsWithViewsPreO(
            @NonNull android.app.FragmentManager fragmentManager,
            @NonNull ArrayMap<View, android.app.Fragment> result) {
        int index = 0;
        while (true) {
            tempBundle.putInt(FRAGMENT_INDEX_KEY, index++);
            android.app.Fragment fragment = null;
            try {
                fragment = fragmentManager.getFragment(tempBundle, FRAGMENT_INDEX_KEY);
            } catch (Exception e) {
                // This generates log spam from FragmentManager anyway.
            }
            if (fragment == null) {
                break;
            }
            if (fragment.getView() != null) {
                result.put(fragment.getView(), fragment);
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                    findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
                }
            }
        }
    }

    @Nullable
    private static Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
    private static void assertNotDestroyed(@NonNull Activity activity) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @NonNull
    @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
    public Lifecycle get(@NonNull android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException(
                    "You cannot start a load on a fragment before it is attached");
        }
        if (Util.isOnBackgroundThread() || VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR1) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            android.app.FragmentManager fm = fragment.getChildFragmentManager();
            return fragmentGet(fm, fragment, fragment.isVisible());
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @NonNull
    ManagerFragment getRequestManagerFragment(Activity activity) {
        return getRequestManagerFragment(
                activity.getFragmentManager(),null, isActivityVisible(activity));
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private ManagerFragment getRequestManagerFragment(
            @NonNull final android.app.FragmentManager fm,
            @Nullable android.app.Fragment parentHint,
            boolean isParentVisible) {
        ManagerFragment current = (ManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (current == null) {
            current = pendingRequestManagerFragments.get(fm);
            if (current == null) {
                current = new ManagerFragment();
                current.setParentFragmentHint(parentHint);
                if (isParentVisible) {
                    current.getLifecycle().onStart();
                }
                pendingRequestManagerFragments.put(fm, current);
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
                handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    @Deprecated
    @NonNull
    private Lifecycle fragmentGet(
            @NonNull android.app.FragmentManager fm,
            @Nullable android.app.Fragment parentHint,
            boolean isParentVisible) {
        ManagerFragment current = getRequestManagerFragment(fm, parentHint, isParentVisible);
        return current.getLifecycle();
    }

    @NonNull
    SupportManagerFragment getSupportRequestManagerFragment(
            Context context, FragmentManager fragmentManager) {
        return getSupportRequestManagerFragment(
                fragmentManager, /*parentHint=*/ null, isActivityVisible(context));
    }

    private static boolean isActivityVisible(Context context) {
        // This is a poor heuristic, but it's about all we have. We'd rather err on the side of visible
        // and start requests than on the side of invisible and ignore valid requests.
        Activity activity = findActivity(context);
        return activity == null || !activity.isFinishing();
    }

    @NonNull
    private SupportManagerFragment getSupportRequestManagerFragment(
            @NonNull final FragmentManager fm, @Nullable Fragment parentHint, boolean isParentVisible) {
        SupportManagerFragment current =
                (SupportManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (current == null) {
            current = pendingSupportRequestManagerFragments.get(fm);
            if (current == null) {
                current = new SupportManagerFragment();
                current.setParentFragmentHint(parentHint);
                if (isParentVisible) {
                    current.getGlideLifecycle().onStart();
                }
                pendingSupportRequestManagerFragments.put(fm, current);
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
                handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    @NonNull
    private Lifecycle supportFragmentGet(
            @NonNull FragmentManager fm,
            @Nullable Fragment parentHint,
            boolean isParentVisible) {
        SupportManagerFragment current =
                getSupportRequestManagerFragment(fm, parentHint, isParentVisible);
        return current.getGlideLifecycle();
    }

    @Override
    public boolean handleMessage(Message message) {
        boolean handled = true;
        Object removed = null;
        Object key = null;
        switch (message.what) {
            case ID_REMOVE_FRAGMENT_MANAGER:
                android.app.FragmentManager fm = (android.app.FragmentManager) message.obj;
                key = fm;
                removed = pendingRequestManagerFragments.remove(fm);
                break;
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
                FragmentManager supportFm = (FragmentManager) message.obj;
                key = supportFm;
                removed = pendingSupportRequestManagerFragments.remove(supportFm);
                break;
            default:
                handled = false;
                break;
        }
        if (handled && removed == null && Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
        }
        return handled;
    }

}
