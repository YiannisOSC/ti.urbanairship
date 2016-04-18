package ti.modules.titanium.urbanairship;

/**
 * Ti.Urbanairship Module
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */


import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import android.os.Bundle;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiProperties;
import org.appcelerator.titanium.util.TiRHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;

import com.urbanairship.UAirship;
import com.urbanairship.push.notifications.DefaultNotificationFactory;
import com.urbanairship.analytics.Analytics;
import com.urbanairship.google.PlayServicesUtils;

import android.content.pm.PackageManager;


@Kroll.module(name="UrbanAirship", id="ti.urbanairship")
public class UrbanAirshipModule extends KrollModule {
	private static final String LCAT = "UrbanAirshipModule";
	private static final String PROPERTY_PREFIX = "UrbanAirship.";
	private static final String ModuleName = "UrbanAirship";

	// Public Events Names
	@Kroll.constant public static final String EVENT_URBAN_AIRSHIP_READY = "UrbanAirship_Ready";
	@Kroll.constant public static final String EVENT_URBAN_AIRSHIP_SUCCESS = "UrbanAirship_Success";
	@Kroll.constant public static final String EVENT_URBAN_AIRSHIP_ERROR = "UrbanAirship_Error";
	@Kroll.constant public static final String EVENT_URBAN_AIRSHIP_CALLBACK = "UrbanAirship_Callback";

	// Property Names
	private static final String PROPERTY_SHOW_APP_ON_CLICK = "showAppOnClick";

	// Flag used so we only check the intent once
	private boolean checkCurrentIntent = true;
	private boolean userNotificationsEnabled = false;
	private static boolean onAppCreateCalled = false;

	private static int foo = 0;

	public UrbanAirshipModule() {
        // Module API level 2 no longer automatically registers the module id or name. In order
        // to have the module name available for the getModuleByName call we must use the
        // constructor with a name.
		super(ModuleName);
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		Log.d(LCAT, "Received onAppCreate notification");

		// MOD-291 - Improve messaging for invalid setup
		onAppCreateCalled = true;

		// Take off!!!!
		airshipTakeOff();
	}

	@Kroll.method
	public void setupActivity() {
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();

		Log.i(LCAT, "Performing Urban Airship setup for activity...");

		// MOD-291 - Improve messaging for invalid setup
		if (!onAppCreateCalled) {
			Log.e(LCAT, "Urban Airship module 'onAppCreate' method was not called during process initialization.");
			Log.e(LCAT, "Please review the module documentation and/or upgrade to the latest version of the module");
		}

		// Handle any Google Play Services errors
		if (PlayServicesUtils.isGooglePlayStoreAvailable()) {
			Log.i(LCAT, "Play Services are available!");
		    PlayServicesUtils.handleAnyPlayServicesError(activity);
		}

		if (hasListeners(EVENT_URBAN_AIRSHIP_CALLBACK)) {
			synthesizeMessageIfNeeded();
		}

	    // Required for analytics
        Analytics.activityStarted(activity);
	}

	@Override
	public void onStart(Activity activity) {
		Log.d(LCAT, "Activity started.");
		super.onStart(activity);
		setupActivity();
 	}

	@Override
	public void onStop(Activity activity) {
		Log.d(LCAT, "Activity stopped.");
        Analytics.activityStopped(activity);
	}

	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy)	{
		super.listenerAdded(type, count, proxy);

		if (EVENT_URBAN_AIRSHIP_CALLBACK.equals(type)) {
	        synthesizeMessageIfNeeded();
		}
	}

	// Kroll Properties
	@Kroll.getProperty @Kroll.method
	public boolean getIsFlying() {
		try {
			boolean flying = UAirship.isFlying();
			// MOD-291 - Improve messaging for invalid setup
			if (flying == false) {
				Log.e(LCAT, "Attempt to access Urban Airship while NOT flying. Check startup and configuration settings.");
			}
			return flying;
	    } catch (Exception e) {
			Log.e(LCAT, "Error occurred during isFlying check!!!");
	    	return false;
	    }
	}

	@Kroll.getProperty @Kroll.method
	public Object[] getTags() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().getTags().toArray();
		}
		return null;
	}

	@Kroll.setProperty @Kroll.method
	public void setTags(Object[] rawTags) {
		if (getIsFlying()) {
			HashSet<String> tags = new HashSet<String>();
			for (Object rawTag : rawTags) {
				tags.add(rawTag.toString());
			}
			UAirship.shared().getPushManager().setTags(tags);
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getAlias() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().getAlias();
		}
		return null;
	}

	@Kroll.setProperty @Kroll.method
	public void setAlias(String alias) {
		if (getIsFlying()) {
			UAirship.shared().getPushManager().setAlias(alias);
		}
	}

	@Kroll.getProperty @Kroll.method
	public boolean getPushEnabled() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().isPushEnabled();
		}
		return false;
	}

	@Kroll.setProperty @Kroll.method
	public void setPushEnabled(boolean enabled) {
		if (getIsFlying()) {
			UAirship.shared().getPushManager().setPushEnabled(enabled);
			Log.i(LCAT, "UrbanAirship has been " + (enabled?"enabled":"disabled"));
	    }
	}

	@Kroll.getProperty @Kroll.method
	public boolean getUserNotificationsEnabled() {
		if (getIsFlying()) {
			return userNotificationsEnabled;
		}
		return false;
	}

	@Kroll.setProperty @Kroll.method
	public void setUserNotificationsEnabled(boolean enabled) {
		if (getIsFlying()) {
			UAirship.shared().getPushManager().setUserNotificationsEnabled(enabled);
			userNotificationsEnabled = enabled;
			Log.i(LCAT, "UrbanAirship Notifications have been " + (enabled?"enabled":"disabled"));
	    }
	}

	@Kroll.getProperty @Kroll.method
	public boolean getSoundEnabled() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().isSoundEnabled();
		}
		return false;
	}

	@Kroll.setProperty @Kroll.method
	public void setSoundEnabled(boolean enabled) {
		if (getIsFlying()) {
			UAirship.shared().getPushManager().setSoundEnabled(enabled);
		}
	}

	@Kroll.getProperty @Kroll.method
    public int getExampleProp(){
        Log.i(LCAT, "In Module - the stored value for exampleProp:" + foo);
        return foo;
    }
    @Kroll.setProperty @Kroll.method
    public void setExampleProp(int value) {
        foo = value;
		Log.i(LCAT, "In Module - the new value for exampleProp:" + foo);
    }

	@Kroll.getProperty @Kroll.method
	public boolean getVibrateEnabled() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().isVibrateEnabled();
		}
		return false;
	}

	@Kroll.setProperty @Kroll.method
	public void setVibrateEnabled(boolean enabled) {
		if (getIsFlying()) {
			UAirship.shared().getPushManager().setVibrateEnabled(enabled);
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getPushId() {
		if (getIsFlying()) {
			return UAirship.shared().getPushManager().getChannelId();
		}
		return null;
	}

	// Since the module can be unloaded when the activity is closed, the ShowOnAppClick
	// property is stored in the application properties so that it can be checked
	// when an intent is received to determine if we should re-launch the activity.

	private static boolean launchAppOnClick() {
		TiProperties appProperties = TiApplication.getInstance().getAppProperties();
		return appProperties.getBool(PROPERTY_PREFIX + PROPERTY_SHOW_APP_ON_CLICK, false);
	}

	@Kroll.setProperty @Kroll.method
	public void setShowOnAppClick(boolean enabled) {
		TiProperties appProperties = TiApplication.getInstance().getAppProperties();
		appProperties.setBool(PROPERTY_PREFIX + PROPERTY_SHOW_APP_ON_CLICK, enabled);
	}

	@Kroll.getProperty @Kroll.method
	public boolean getShowOnAppClick() {
		return launchAppOnClick();
	}

	// Private Helper Methods

	private static void airshipTakeOff() {
		Log.i(LCAT, "Airship taking off");
		try {
			//UAirship.takeOff(TiApplication.getInstance());
			UAirship.takeOff(TiApplication.getInstance(), new UAirship.OnReadyCallback() {
				@Override
	            public void onAirshipReady(UAirship airship) {
	            	Log.i(LCAT, "Airship ready for configuration");

					// HashMap<String, Object> kd = new HashMap<String, Object>();
				    // kd.put("Is Airship ready?", new Boolean(true));
					// UrbanAirshipModule uaModule = getModule();
	            	// uaModule.fireEvent(EVENT_URBAN_AIRSHIP_READY, kd);
					TiApplication appInstance = TiApplication.getInstance();

					TiProperties appProperties = appInstance.getAppProperties();
					DefaultNotificationFactory factory = new DefaultNotificationFactory(appInstance);

					try{
						//This color is been defined from tiapp.xml of the Titanium app.
						//It could be placed as <property name="notificationBgColor" type="string">#bd3e3e</property>
						String colorString = appProperties.getString("notificationBgColor", "nocolor");
						int notificationColor = Color.parseColor(colorString);
						factory.setColor(notificationColor);
					}catch(Exception e){
						Log.e(LCAT, "************* problem setting notification color");
					}

					try{
						//Large Icon is now the App Icon
						int notificationLargeIcon = TiRHelper.getApplicationResource("drawable.icon");
						//Small Icon is just an "i" (information) icon.
						//Could be any icon from this list: http://developer.android.com/reference/android/R.drawable.html
						int notificationIcon = TiRHelper.getAndroidResource("drawable.ic_menu_info_details");

						factory.setSmallIconId(notificationIcon);
						factory.setLargeIcon(notificationLargeIcon);
					}catch(Exception e){
						Log.e(LCAT, "************* problem creating notification icon");
					}

					airship.getPushManager().setNotificationFactory(factory);
	            }
			});
	    } catch (Exception e) {
			Log.e(LCAT, "Error occurred during takeoff!!!");
		}
	}

	private void changeIconColor(){

	}

	/*private int getIdOfModuleAsset(String assetName, String typeWithDot){
	    // Use this to locate resources in the R.java file (e.g. platform/android/res/drawable)
	    // In case the caller passes in the asset name with the extension, strip it off
	    // Prefix the asset name with type

	    int dot = assetName.lastIndexOf(".");
	    String resourceName = typeWithDot + ((dot > 0) ? assetName.substring(0, dot) : assetName);

	    int result = 0;
	    try {
	        result = TiRHelper.getApplicationResource(resourceName);
	    } catch (ResourceNotFoundException e) {
	        Log.d("ModdevguideModule", "[ASSETSDEMO] EXCEPTION -- RESOURCE NOT FOUND");
	    }

	    return result;
	}*/

	// Message Handler Processing Helpers
	private HashMap<String, Object> getIntentExtras(Intent intent) {
		HashMap<String, Object> retval = new HashMap<String, Object>();
		Bundle extras = intent.getExtras();
    	Set<String> keys = extras.keySet();

    	for (String key: keys) {
    		retval.put(key, extras.get(key));
    	}

		return retval;
	}

	private void synthesizeMessageIfNeeded() {
		// In the case where the activity is being re-started by the IntentReceiver because
		// the activity has been unloaded, we need to synthesize the message and call the
		// callback (if registered) so that it acts as if the message was just received.
		// This removes the need for the JS app to handle this special case.

		// We should only do this once per activity start
		if (checkCurrentIntent) {
			checkCurrentIntent = false;
			TiApplication appContext = TiApplication.getInstance();
			Activity activity = appContext.getCurrentActivity();
			if (activity != null) {
				Intent intent = activity.getIntent();
				if (intent != null) {
					String message = intent.getStringExtra("message");
					HashMap<String, Object> payload = getIntentExtras(intent);
					if (message != null) {
						Log.d(LCAT,"User clicked notification (synthesized)");
						handleReceivedMessage(message, payload, true, false);
					}
				}
			}
		}
	}

	private static UrbanAirshipModule getModule() {
		TiApplication appContext = TiApplication.getInstance();
		UrbanAirshipModule uaModule = (UrbanAirshipModule)appContext.getModuleByName(ModuleName);

		if (uaModule == null) {
			Log.w(LCAT,"Urban Airship module not currently loaded");
		}
		return uaModule;
	}

	private static void launchActivity(String message, HashMap<String, Object> payload) {
		TiApplication appContext = TiApplication.getInstance();

		// We need to get the class name for the main application activity. That isn't a problem if
		// we were started from the home screen of the device. However, if we were started as part
		// of the Push Service startup then the main activity may not have been started yet. So, we
		// make use of the PackageManager to provide the information that we need to start the main
		// activity.
		PackageManager pm = appContext.getPackageManager();
		Intent launch = pm.getLaunchIntentForPackage(appContext.getPackageName());

		// We still need to set the category and flags before we try to start the activity
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// Set the extras to the message and payload so that they are picked up on relaunch of the activity
		launch.putExtra("message", message);
		launch.putExtra("payload", payload);

		appContext.startActivity(launch);
	}

	public static void handleReceivedMessage(String message, HashMap<String,Object> payload, Boolean clicked, Boolean launchIfNeeded) {
	    Log.d(LCAT, "Message: " + message + " Payload: " + payload);

	    // Get the currently loaded module object. If the activity has been unloaded then this will
	    // result in uaModule being set to null.
	    UrbanAirshipModule uaModule = getModule();

		// If instructed to bring the app to the foreground when clicked, then launch it
		if (clicked && launchIfNeeded && launchAppOnClick()) {
			launchActivity(message, payload);
		}

	    if (uaModule != null) {
		    HashMap<String, Object> kd = new HashMap<String, Object>();
		    kd.put("clicked", new Boolean(clicked));
		    kd.put("message", message);
		    kd.put("payload", payload);

			uaModule.fireEvent(EVENT_URBAN_AIRSHIP_CALLBACK, kd);
	    }else{
			Log.d(LCAT, "uaModule = Null: " + uaModule);
		}
	}

    public static void handleRegistrationComplete(String apid, Boolean valid) {
        Log.d(LCAT, "APID: " + apid + " IsValid: " + valid);

	    // Get the currently loaded module object. If the activity has been unloaded then this will
	    // result in uaModule being set to null.
        UrbanAirshipModule uaModule = getModule();

        if (uaModule != null) {
        	HashMap<String, Object> kd = new HashMap<String, Object>();
        	kd.put("deviceToken", apid);
        	kd.put("valid", valid);
        	if (valid) {
	        	Log.d(LCAT, "UrbanAirship has received a valid android channel!");
        		uaModule.fireEvent(EVENT_URBAN_AIRSHIP_SUCCESS, kd);
        	} else {
    	    	Log.d(LCAT, "UrbanAirship has received an invalid android channel!");
        		kd.put("error", "Error occurred registering device");
        		uaModule.fireEvent(EVENT_URBAN_AIRSHIP_ERROR, kd);
        	}
        }
        else
        	Log.d(LCAT, "UrbanAirship has been unloaded!");
    }
}
