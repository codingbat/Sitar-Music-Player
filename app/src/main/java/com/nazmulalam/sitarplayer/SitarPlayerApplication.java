package com.nazmulalam.sitarplayer;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import io.fabric.sdk.android.Fabric;

public class SitarPlayerApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    if (BuildConfig.USE_CRASHLYTICS) {
      Fabric.with(this, new Crashlytics());
      Fabric.with(this, new Answers());
    }
  }

}
