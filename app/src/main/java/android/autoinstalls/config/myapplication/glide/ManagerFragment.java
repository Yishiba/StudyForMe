package android.autoinstalls.config.myapplication.glide;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A view-less {@link Fragment} used to safely store an {@link
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class ManagerFragment extends Fragment {

    private static final String TAG = "RMFragment";
    private final ActivityFragmentLifecycle lifecycle;

    @SuppressWarnings("deprecation")
    private final Set<ManagerFragment> childRequestManagerFragments = new HashSet<>();


    @SuppressWarnings("deprecation")
    @Nullable
    private ManagerFragment rootRequestManagerFragment;

    @Nullable
    private Fragment parentFragmentHint;

    public ManagerFragment() {
        this(new ActivityFragmentLifecycle());
    }

    @VisibleForTesting
    @SuppressLint("ValidFragment")
    ManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }


    @NonNull
    ActivityFragmentLifecycle getLifecycle() {
        return lifecycle;
    }


    @SuppressWarnings("deprecation")
    private void addChildRequestManagerFragment(ManagerFragment child) {
        childRequestManagerFragments.add(child);
    }

    @SuppressWarnings("deprecation")
    private void removeChildRequestManagerFragment(ManagerFragment child) {
        childRequestManagerFragments.remove(child);
    }

    /**
     * Returns the set of fragments that this ManagerFragment's parent is a parent to. (i.e.
     * our parent is the fragment that we are annotating).
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @NonNull
    Set<ManagerFragment> getDescendantRequestManagerFragments() {
        if (equals(rootRequestManagerFragment)) {
            return Collections.unmodifiableSet(childRequestManagerFragments);
        } else if (rootRequestManagerFragment == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Pre JB MR1 doesn't allow us to get the parent fragment so we can't introspect hierarchy,
            // so just return an empty set.
            return Collections.emptySet();
        } else {
            Set<ManagerFragment> descendants = new HashSet<>();
            for (ManagerFragment fragment :
                    rootRequestManagerFragment.getDescendantRequestManagerFragments()) {
                if (isDescendant(fragment.getParentFragment())) {
                    descendants.add(fragment);
                }
            }
            return Collections.unmodifiableSet(descendants);
        }
    }

    /**
     * Sets a hint for which fragment is our parent which allows the fragment to return correct
     * information about its parents before pending fragment transactions have been executed.
     */
    void setParentFragmentHint(@Nullable Fragment parentFragmentHint) {
        this.parentFragmentHint = parentFragmentHint;
        if (parentFragmentHint != null && parentFragmentHint.getActivity() != null) {
            registerFragmentWithRoot(parentFragmentHint.getActivity());
        }
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Fragment getParentFragmentUsingHint() {
        final Fragment fragment;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fragment = getParentFragment();
        } else {
            fragment = null;
        }
        return fragment != null ? fragment : parentFragmentHint;
    }

    /**
     * Returns true if the fragment is a descendant of our parent.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private boolean isDescendant(@NonNull Fragment fragment) {
        Fragment root = getParentFragment();
        Fragment parentFragment;
        while ((parentFragment = fragment.getParentFragment()) != null) {
            if (parentFragment.equals(root)) {
                return true;
            }
            fragment = fragment.getParentFragment();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void registerFragmentWithRoot(@NonNull Activity activity) {
        unregisterFragmentWithRoot();
        rootRequestManagerFragment = LifecycleManager.getInstance().getRequestManagerFragment(activity);
        if (!equals(rootRequestManagerFragment)) {
            rootRequestManagerFragment.addChildRequestManagerFragment(this);
        }
    }

    private void unregisterFragmentWithRoot() {
        if (rootRequestManagerFragment != null) {
            rootRequestManagerFragment.removeChildRequestManagerFragment(this);
            rootRequestManagerFragment = null;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        lifecycle.onAttach();
        try {
            registerFragmentWithRoot(activity);
        } catch (IllegalStateException e) {
            // OnAttach can be called after the activity is destroyed, see #497.
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to register fragment with root", e);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycle.onCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycle.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycle.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        lifecycle.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        lifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();
        unregisterFragmentWithRoot();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        lifecycle.onDetach();
        unregisterFragmentWithRoot();
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
    }



}
