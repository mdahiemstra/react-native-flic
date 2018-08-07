package com.opencii.rn.flic;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import io.flic.poiclib.FlicManager;
import io.flic.poiclib.FlicScanWizard;
import io.flic.poiclib.FlicButton;
import io.flic.poiclib.FlicButtonAdapter;
import io.flic.poiclib.FlicButtonListener;
import io.flic.poiclib.FlicButtonMode;

public class RNFlicModule extends ReactContextBaseJavaModule {
  private final ReactApplicationContext reactContext;

  private static final String TAG = "FLIC";
  private static final String EVENT_NAMESPACE = "FLIC";
  private static final String APP_ID = "";
  private static final String APP_SECRET = "";

  HashMap<FlicButton, FlicButtonListener> listeners = new HashMap<>();

  public RNFlicModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    FlicManager.init(this.reactContext, APP_ID, APP_SECRET);
  }

  @Override
  public String getName() {
    return "RNFlic";
  }

  private void sendEventMessage(HashMap<String, String> body) {
    WritableMap args = new WritableNativeMap();
    for (Map.Entry<String, String> entry : body.entrySet()) {
        args.putString(entry.getKey(), entry.getValue());
    }

    this.getReactApplicationContext()
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(EVENT_NAMESPACE, args);
  }

	private void setupEventListenerForButtonInActivity(FlicButton button) {
		FlicButtonListener listener = new FlicButtonAdapter() {
      @Override
      public void onConnect(FlicButton button) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_CONNECTED");
        data.put("buttonId", button.getBdAddr());
        Log.i(TAG, "onConnect: " + button.getBdAddr());
      }
  
      @Override
      public void onReady(FlicButton button) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_READY");
        data.put("buttonId", button.getBdAddr());
        Log.i(TAG, "onReady: " + button.getBdAddr());
      }
  
      @Override
      public void onDisconnect(FlicButton button, int flicError, boolean willReconnect) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_DISCONNECTED");
        data.put("buttonId", button.getBdAddr());
        sendEventMessage(data);
        Log.i(TAG, "onDisconnect: " + button.getBdAddr() + ", error: " + flicError + ", willReconnect: " + willReconnect);
      }
  
      @Override
      public void onConnectionFailed(FlicButton button, int status) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_CONNECTING_FAILED");
        data.put("buttonId", button.getBdAddr());
        sendEventMessage(data);
        Log.e(TAG, "onConnectionFailed " + button.getBdAddr() + ", status " + status);
      }
  
      @Override
      public void onButtonUpOrDown(final FlicButton button, boolean wasQueued, int timeDiff, final boolean isUp, final boolean isDown) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_" + (isDown ? "PRESSED" : "RELEASED"));
        data.put("buttonId", button.getBdAddr());
        sendEventMessage(data);
        Log.i(TAG, "Button " + button.getBdAddr() + " was " + (isDown ? "PRESSED" : "RELEASED"));
      }
		};

		button.addEventListener(listener);
		button.setTemporaryMode(FlicButtonMode.SuperActive);

		// Save the event listener so we can remove it later
		listeners.put(button, listener);
	}

  @ReactMethod
  public void getKnownButtons(String name, final Promise promise) {
    for (FlicButton button : FlicManager.getManager().getKnownButtons()) {
      Log.i(TAG, "Connected button " + button.getBdAddr());

      button.connect();

      HashMap<String, String> data = new HashMap<>();
      data.put("event", "BUTTON_CONNECTED");
      data.put("buttonId", button.getBdAddr());
      sendEventMessage(data);
      
      setupEventListenerForButtonInActivity(button);
    }

    promise.resolve("GET_KNOWN_BUTTONS");
  }

  @ReactMethod
  public void makeCall(String number, final Promise promise) {
    String dial = "tel:" + number;

    Intent intent = new Intent(Intent.ACTION_CALL);
    intent.setData(Uri.parse(dial));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.getReactApplicationContext().startActivity(intent);

    HashMap<String, String> data = new HashMap<>();
    data.put("event", "MAKING_CALL");
    sendEventMessage(data);
    
    promise.resolve("MAKING_CALL");
  }

  @ReactMethod
  public void searchButtons(String name, final Promise promise) {

    FlicManager.getManager().getScanWizard().start(new FlicScanWizard.Callback() {
      @Override
      public void onDiscovered(FlicScanWizard wizard, String bdAddr, int rssi, final boolean isPrivateMode, int revision) {
        if (isPrivateMode) {
          HashMap<String, String> data = new HashMap<>();
          data.put("event", "}");
          data.put("buttonId", bdAddr);
          sendEventMessage(data);
          Log.i(TAG, "Found a private button. Hold it down for 7 seconds to make it public.");
        } else {
          HashMap<String, String> data = new HashMap<>();
          data.put("event", "FOUND_PUBLIC_BUTTON");
          data.put("buttonId", bdAddr);
          sendEventMessage(data);
          Log.i(TAG, "Found a button. Now connecting...");
        }
      }
  
      @Override
      public void onBLEConnected(FlicScanWizard wizard, String bdAddr) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "CONNECTION_ESTABLISHED");
        data.put("buttonId", bdAddr);
        sendEventMessage(data);
        Log.i(TAG, "Connection established. Now verifying...");
      }
  
      @Override
      public void onCompleted(FlicScanWizard wizard, final FlicButton button) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_ADDED_SUCCESS");
        data.put("buttonId", button.getBdAddr());
        sendEventMessage(data);
        Log.i(TAG, "New button successfully added!");
      }
  
      @Override
      public void onFailed(FlicScanWizard wizard, int flicScanWizardErrorCode) {
        HashMap<String, String> data = new HashMap<>();
        data.put("event", "BUTTON_ADDED_FAILED");
        sendEventMessage(data);
        Log.i(TAG, "Adding a button failed! " + Integer.toString(flicScanWizardErrorCode));
      }
    });

    promise.resolve("ready to scan");
  }

  // @Override
  // public void onHostDestroy() {
  //   // super.onDestroy();

  //   Log.i(TAG, "Destroy FlicModule");

	// 	for (Map.Entry<FlicButton, FlicButtonListener> entry : listeners.entrySet()) {
  //     Log.i(TAG, "Remove Flic Event Listener");
	// 		entry.getKey().removeEventListener(entry.getValue());
	// 		entry.getKey().returnTemporaryMode(FlicButtonMode.SuperActive);
	// 	}

  //   FlicManager.getManager().getScanWizard().cancel();
  // }
}