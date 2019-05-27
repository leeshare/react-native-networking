package com.learnium.RNNetworkingManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;

public class RNNetworkingPackage implements ReactPackage {

  @Override
  public List<NativeModule> createNativeModules(
                              ReactApplicationContext reactContext) {
    //List<RNNetworkingManagerNew> modules = new ArrayList<>();
    //modules.add(new RNNetworkingManagerNew(reactContext));
    //return modules;

    return Arrays.<NativeModule>asList(new RNNetworkingManager(reactContext));
  }

  @Override
  public List<Class<? extends JavaScriptModule>> createJSModules() {
  	return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(
                            ReactApplicationContext reactContext) {
  	return Collections.emptyList();
  }

}
