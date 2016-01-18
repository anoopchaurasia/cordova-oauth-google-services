package ca.bleathem.plugin;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class OauthGoogleServices extends CordovaPlugin {
    private static final String TAG = "OauthGoogleServices";
    private static final int REQUEST_CODE_EMAIL = 1;
    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQUEST_AUTHORIZATION = 20000;
    private static final String KEY_AUTH_TOKEN = "authtoken";
    private static final String PROFILE_SCOPE = "https://www.googleapis.com/auth/plus.me";
    private CallbackContext _callbackContext = null;
    private String scope;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.i(TAG, "action:" + action);
        cordova.setActivityResultCallback(this);
        if (action.equals("getToken")) {
            _callbackContext = callbackContext;
            scope =  "oauth2:server:client_id:" + "925235058960-2u5bbq3dgbmegsi7rdlg2jd3mds0uvk9.apps.googleusercontent.com";
            scope = scope + ":api_scope:" + ("null".equals(args.getString(0)) ? PROFILE_SCOPE: args.getString(0));
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                        cordova.getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Activity not found: " + e.toString());
                        _callbackContext.error("Plugin cannot find activity: " + e.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Exception: " + e.toString());
                        _callbackContext.error("Plugin failed to get account: " + e.toString());
                    }
                }

                ;
            };
            cordova.getActivity().runOnUiThread(runnable);
            return true;
        } 
        else if (action.equals("clearToken")) {
            try {
                GoogleAuthUtil.clearToken(cordova.getActivity(), args.getString(0));
                _callbackContext.success("success");
            } catch (Exception e) {
                _callbackContext.error("erro");
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "requestCode:" + requestCode);
        if (_callbackContext != null) {
            try {
                if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
                    if (resultCode == Activity.RESULT_OK) {
                        String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        Log.i(TAG, "account:" + accountName);
                        getToken(accountName);
                    } else {
                        _callbackContext.error("plugin failed to get account");
                    }
                } else if (requestCode == REQUEST_AUTHORIZATION) {
                    if (resultCode == Activity.RESULT_OK) {
                        String token = data.getStringExtra(KEY_AUTH_TOKEN);
                        _callbackContext.success(token);
                        Log.e(TAG, "UserRecoverableAuthException: Attempting recovery..." + token);
                    } else {
                        _callbackContext.error("plugin failed to get token");
                    }
                } else {
                    Log.i(TAG, "Unhandled activityResult. requestCode: " + requestCode + " resultCode: " + resultCode);
                }
            } catch (Exception e) {
                _callbackContext.error("Plugin failed to get email: " + e.toString());
                Log.e(TAG, "Exception: " + e.toString());
            }
        } else {
            Log.d(TAG, "No callback to go to!");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getToken(final String accountName) {
        final CordovaPlugin plugin = this;
        Runnable runnable = new Runnable() {
            public void run() {
                String token;
                try {
                    Log.e(TAG, "Retrieving token for: " + accountName);
                    Log.e(TAG, "with scope(s): " + scope);
                    token = GoogleAuthUtil.getToken(cordova.getActivity(), accountName, scope);
                    _callbackContext.success(token);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    Log.e(TAG, "UserRecoverableAuthException: Attempting recovery...");
                    final Intent intent = userRecoverableException.getIntent();
                    try {
                        cordova.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cordova.startActivityForResult(plugin, intent, REQUEST_AUTHORIZATION);
                            }
                        });
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    Log.i(TAG, "error" + e.getMessage());
                    _callbackContext.error("plugin failed to get token: " + e.getMessage());
                }
            }
        };
        cordova.getThreadPool().execute(runnable);
    }
}
