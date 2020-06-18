package com.realme.gouwei.test800;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.ServiceManager;
import android.os.UserHandle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
应用启动过程

1.首先查询要启动的providers
2.实例化application
3.绑定provider（google，facebook就是这里起来的）
4.调用application的onCreate

目前我的方案是在第2步的时候，反射获取第1步providers，然后移除目标provider
*/

public class MyApplication extends Application {
    //com.google.android.gms.ads.MobileAdsInitProvider
    //com.facebook.ads.AudienceNetworkContentProvider

    public MyApplication() {
        super();
        android.util.Log.d("gouwei8", "MyApplication");
        removeProviderInfosBeforeInit();
        setAdProvidesDisabled();
    }

    @Override
    public void onCreate() {
        android.util.Log.d("gouwei8", "onCreate");
        super.onCreate();
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName("com.realme.gouwei.test800", "com.google.android.gms.ads.MobileAdsInitProvider");
        ComponentName componentName1 = new ComponentName("com.realme.gouwei.test800", "com.facebook.ads.AudienceNetworkContentProvider");
        android.util.Log.d("gouwei8", "componentName = " + packageManager.getComponentEnabledSetting(componentName));
        android.util.Log.d("gouwei8", "componentName1 = " + packageManager.getComponentEnabledSetting(componentName1));
    }

    private void setAdProvidesDisabled() {
        try {
            ComponentName componentName = new ComponentName("com.realme.gouwei.test800", "com.google.android.gms.ads.MobileAdsInitProvider");
            ComponentName componentName1 = new ComponentName("com.realme.gouwei.test800", "com.facebook.ads.AudienceNetworkContentProvider");

            Class serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getDeclaredMethod("getService", String.class);
            Object packageBinder = getService.invoke(null, "package");

            Class iPackageManagerStub = Class.forName("android.content.pm.IPackageManager$Stub");
            Method asInterface = iPackageManagerStub.getDeclaredMethod("asInterface", android.os.IBinder.class);
            Object iPackageManagerObject = asInterface.invoke(null, packageBinder);

            Class iPackageManager = Class.forName("android.content.pm.IPackageManager");
            Method setComponentEnabledSetting = iPackageManager.getDeclaredMethod("setComponentEnabledSetting", ComponentName.class, int.class, int.class, int.class);

            setComponentEnabledSetting.invoke(iPackageManagerObject, componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, UserHandle.USER_SYSTEM);
            setComponentEnabledSetting.invoke(iPackageManagerObject, componentName1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, UserHandle.USER_SYSTEM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeProviderInfosBeforeInit() {
        try {
            Class activityThread = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThread = activityThread.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThread.setAccessible(true);
            Object sCurrentActivityThreadObject = sCurrentActivityThread.get(null);

            Field mBoundApplication = activityThread.getDeclaredField("mBoundApplication");
            mBoundApplication.setAccessible(true);
            Object mBoundApplicationObject = mBoundApplication.get(sCurrentActivityThreadObject);

            Class appBindData = Class.forName("android.app.ActivityThread$AppBindData");
            Field providers = appBindData.getDeclaredField("providers");
            providers.setAccessible(true);
            List<ProviderInfo> providerInfos = (List<ProviderInfo>)providers.get(mBoundApplicationObject);
            if ((providerInfos != null) && (providerInfos.size() > 0)) {
                for (int i = 0; i < providerInfos.size(); i++) {
                    ProviderInfo provider = providerInfos.get(i);
                    String className = provider.getComponentName().getClassName();
                    android.util.Log.d("gouwei8", "className = " + className);
                    if ("com.google.android.gms.ads.MobileAdsInitProvider".equals(className)
                            || "com.facebook.ads.AudienceNetworkContentProvider".equals(className)) {
                        providerInfos.remove(i);
                        i--;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
