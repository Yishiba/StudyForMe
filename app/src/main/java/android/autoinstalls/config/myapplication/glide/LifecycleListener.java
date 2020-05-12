package android.autoinstalls.config.myapplication.glide;

import android.os.Bundle;

/**
 * An interface for listener to {@link android.app.Fragment} and {@link android.app.Activity}
 * lifecycle events.
 */
public interface LifecycleListener {

  /**
   * Callback for when {@link android.app.Fragment#onStart()}} or {@link
   *
   */
  void onAttach();

  /**
   * Callback for when {@link android.app.Fragment#onCreate(Bundle)} ()}} or {@link
   *
   */
  void onCreate();

  /**
   * Callback for when {@link android.app.Fragment#onResume()}} or {@link
   */
  void onResume();

  /**
   * Callback for when {@link android.app.Fragment#onStart()}} or {@link
   *
   */
  void onStart();

  /**
   * Callback for when {@link android.app.Fragment#onPause()}} or {@link
   */
  void onPause();

  /**
   * Callback for when {@link android.app.Fragment#onStop()}} or {@link
   */
  void onStop();

  /**
   * Callback for when {@link android.app.Fragment#onDestroy()}} or {@link
   */
  void onDestroy();

  /**
   * Callback for when {@link android.app.Fragment#onDetach()}} or {@link
   *
   */
  void onDetach();
}
