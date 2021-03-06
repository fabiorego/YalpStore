package com.github.yeriomin.yalpstore;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.github.yeriomin.playstoreapi.ApiBuilderException;
import com.github.yeriomin.playstoreapi.DeviceInfoProvider;
import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import com.github.yeriomin.playstoreapi.PropertiesDeviceInfoProvider;

import java.io.IOException;
import java.util.Locale;

public class PlayStoreApiAuthenticator {

    static private final String DISPENSER_URL = "http://tokendispenser-yeriomin.rhcloud.com";

    private Context context;

    private static GooglePlayAPI api;

    public PlayStoreApiAuthenticator(Context context) {
        this.context = context;
    }

    public GooglePlayAPI getApi() throws IOException {
        if (api == null) {
            api = build();
        }
        return api;
    }

    public void login() throws IOException {
        build(null, null);
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putBoolean(PreferenceActivity.PREFERENCE_APP_PROVIDED_EMAIL, true);
        prefs.commit();
    }

    public void login(String email, String password) throws IOException {
        build(email, password);
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.remove(PreferenceActivity.PREFERENCE_APP_PROVIDED_EMAIL);
        prefs.commit();
    }

    public void logout() {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.remove(PreferenceActivity.PREFERENCE_EMAIL);
        prefs.remove(PreferenceActivity.PREFERENCE_GSF_ID);
        prefs.remove(PreferenceActivity.PREFERENCE_AUTH_TOKEN);
        prefs.remove(PreferenceActivity.PREFERENCE_APP_PROVIDED_EMAIL);
        prefs.commit();
        api = null;
    }

    private GooglePlayAPI build() throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String email = prefs.getString(PreferenceActivity.PREFERENCE_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        return build(email, null);
    }

    private GooglePlayAPI build(String email, String password) throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String locale = prefs.getString(PreferenceActivity.PREFERENCE_REQUESTED_LANGUAGE, "");
        String gsfId = prefs.getString(PreferenceActivity.PREFERENCE_GSF_ID, "");
        String token = prefs.getString(PreferenceActivity.PREFERENCE_AUTH_TOKEN, "");

        com.github.yeriomin.playstoreapi.PlayStoreApiBuilder builder = new com.github.yeriomin.playstoreapi.PlayStoreApiBuilder()
            .setHttpClient(BuildConfig.DEBUG ? new DebugHttpClientAdapter() : new NativeHttpClientAdapter())
            .setDeviceInfoProvider(getDeviceInfoProvider())
            .setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale))
            .setEmail(email)
            .setPassword(password)
            .setGsfId(gsfId)
            .setToken(token)
            .setTokenDispenserUrl(DISPENSER_URL)
        ;
        try {
            api = builder.build();
        } catch (ApiBuilderException e) {
            // Should not happen
        }

        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(PreferenceActivity.PREFERENCE_EMAIL, builder.getEmail());
        prefsEditor.putString(PreferenceActivity.PREFERENCE_GSF_ID, api.getGsfId());
        prefsEditor.putString(PreferenceActivity.PREFERENCE_AUTH_TOKEN, api.getToken());
        prefsEditor.commit();
        return api;
    }

    private DeviceInfoProvider getDeviceInfoProvider() {
        DeviceInfoProvider deviceInfoProvider;
        String spoofDevice = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PreferenceActivity.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "")
        ;
        if (TextUtils.isEmpty(spoofDevice)) {
            deviceInfoProvider = new NativeDeviceInfoProvider();
            ((NativeDeviceInfoProvider) deviceInfoProvider).setContext(context);
            ((NativeDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        } else {
            deviceInfoProvider = new PropertiesDeviceInfoProvider();
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setProperties(new SpoofDeviceManager(context).getProperties(spoofDevice));
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        }
        return deviceInfoProvider;
    }
}
