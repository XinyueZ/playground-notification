package com.playground.notification.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.chopping.application.BasicPrefs;
import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.app.activities.AppActivity;

import static com.playground.notification.app.activities.AppActivity.MENU_ITEM_OTHERS;

/**
 * Store app and device information.
 *
 * @author Chris.Xinyue Zhao
 */
public final class Prefs extends BasicPrefs {
	/**
	 * Storage. Whether the "End User License Agreement" has been shown and agreed at application's first start.
	 * <p/>
	 * {@code true} if EULA has been shown and agreed.
	 */
	private static final String KEY_EULA_SHOWN = "key.eula.shown";
	/**
	 * Download-info of application.
	 */
	private static final String KEY_APP_DOWNLOAD = "key.app.download";
	/**
	 * Google's ID
	 */
	private static final String KEY_GOOGLE_ID = "key.google.id";
	/**
	 * The display-name of Google's user.
	 */
	private static final String KEY_GOOGLE_DISPLAY_NAME = "key.google.display.name";
	/**
	 * Url to user's profile-image.
	 */
	private static final String KEY_GOOGLE_THUMB_URL = "key.google.thumb.url";

	/**
	 * API host defined in config.
	 */
	private static final String KEY_API_HOST = "api_host";
	/**
	 * API search defined in config.
	 */
	private static final String KEY_API_SEARCH = "api_search";
	/**
	 * Google API's host.
	 */
	private static final String KEY_GOOGLE_API_HOST = "google_api_host";
	/**
	 * Map preview size for my-location-list.
	 */
	private static final String KEY_MY_LOC_PRE = "my_loc_pre";
	/**
	 * Map preview size for detail-view.
	 */
	private static final String KEY_DETAIL_PRE = "detail_pre";
	/**
	 * Host name for Google's map search engine.
	 */
	private static final String KEY_GOOGLE_MAP_SEARCH_HOST = "google_map_search_host";
	/**
	 * Url to load a icon of current weather.
	 */
	private static final String KEY_WEATHER_ICON_URL = "weather_icon_url";
	/**
	 * The weather-API host.
	 */
	private static final String KEY_WEATHER_API_HOST = "weather_api";

	/**
	 * Limit count for showing cluster on map.
	 */
	private static final String KEY_CLUSTER_LIMIT = "cluster_limit";
	/**
	 * Latitude of selected item.
	 */
	private static final String KEY_SELECT_PLAYGROUND_LAT = "key.selected.playground.lat";
	/**
	 * Longitude of selected item.
	 */
	private static final String KEY_SELECT_PLAYGROUND_LNG = "key.selected.playground.lng";
	/**
	 * Selected  menu-item.
	 */
	private static final String KEY_CURRENT_MENU_ITEM = "kex.current.menu.item";

	private static final String ADS = "ads";

	//All settings
	public static final String KEY_MAP_TYPES = "key.map.types";
	public static final String KEY_BATTERY_TYPES = "key.battery.types";
	public static final String KEY_TRAFFIC_SHOWING = "key.traffic.showing";
	public static final String KEY_DISTANCE_UNITS = "key.distance.units";
	public static final String KEY_TRANSPORTATION = "key.transportation";
	public static final String KEY_ALARM_AREA = "key.alarm.area";
	public static final String KEY_WEATHER_SHOW_WEATHER_BOARD = "key.weather.show.weather.board";
	public static final String KEY_WEATHER_UNITS = "key.weather.units";
	public static final String KEY_NOTIFICATION_WARM_TIPS = "key.notification.warm.tips";
	public static final String KEY_NOTIFICATION_WEEKEND_CALL = "key.notification.weekend.call";

	public static final String KEY_SHOWCASE_MY_LOCATION = "showcase.my.location";
	public static final String KEY_SHOWCASE_NEAR_RING = "showcase.near.ring";


	/**
	 * The Instance.
	 */
	private static Prefs sInstance;

	private Prefs() {
		super(null);
	}

	/**
	 * Created a DeviceData storage.
	 *
	 * @param context A context object.
	 */
	private Prefs(Context context) {
		super(context);
	}

	/**
	 * Singleton method.
	 *
	 * @param context A context object.
	 * @return single instance of DeviceData
	 */
	public static Prefs createInstance(Context context) {
		if (sInstance == null) {
			synchronized (Prefs.class) {
				if (sInstance == null) {
					sInstance = new Prefs(context);
				}
			}
		}
		return sInstance;
	}

	/**
	 * Singleton getInstance().
	 *
	 * @return The instance of Prefs.
	 */
	public static Prefs getInstance() {
		return sInstance;
	}


	/**
	 * Whether the "End User License Agreement" has been shown and agreed at application's first start.
	 * <p/>
	 *
	 * @return {@code true} if EULA has been shown and agreed.
	 */
	public boolean isEULAOnceConfirmed() {
		return getBoolean(KEY_EULA_SHOWN, false);
	}

	/**
	 * Set whether the "End User License Agreement" has been shown and agreed at application's first start.
	 * <p/>
	 *
	 * @param isConfirmed {@code true} if EULA has been shown and agreed.
	 */
	public void setEULAOnceConfirmed(boolean isConfirmed) {
		setBoolean(KEY_EULA_SHOWN, isConfirmed);
	}


	/**
	 * @return Download-info of application.
	 */
	public String getAppDownloadInfo() {
		return getString(KEY_APP_DOWNLOAD, null);
	}

	/**
	 * Set download-info of application.
	 */
	public void setAppDownloadInfo(String appDownloadInfo) {
		setString(KEY_APP_DOWNLOAD, appDownloadInfo);
	}

	/**
	 * Google's ID
	 */
	public String getGoogleId() {
		return getString(KEY_GOOGLE_ID, null);
	}

	/**
	 * Google's ID
	 */
	public void setGoogleId(String id) {
		setString(KEY_GOOGLE_ID, id);
	}

	/**
	 * The display-name of Google's user.
	 */
	public String getGoogleDisplayName() {
		return getString(KEY_GOOGLE_DISPLAY_NAME, null);
	}

	/**
	 * The display-name of Google's user.
	 */
	public void setGoogleDisplayName(String displayName) {
		setString(KEY_GOOGLE_DISPLAY_NAME, displayName);
	}

	/**
	 * Url to user's profile-image.
	 */
	public String getGoogleThumbUrl() {
		return getString(KEY_GOOGLE_THUMB_URL, null);
	}

	/**
	 * The display-name of Google's user.
	 */
	public void setGoogleThumbUrl(String thumbUrl) {
		setString(KEY_GOOGLE_THUMB_URL, thumbUrl);
	}

	/**
	 * API host defined in config.
	 */
	public String getApiHost() {
		return getString(KEY_API_HOST, null);
	}

	/**
	 * API search defined in config.
	 */
	public String getApiSearch() {
		return getString(KEY_API_SEARCH, null);
	}

	/**
	 * Google API's host.
	 */
	public String getGoogleApiHost() {
		return getString(KEY_GOOGLE_API_HOST, null);
	}

	/**
	 * Map preview size for my-location-list.
	 *
	 * @return Size in format {@code "23x34"};
	 */
	public String getMyLocationPreviewSize() {
		return getString(KEY_MY_LOC_PRE, null);
	}

	/**
	 * Map preview size for detail-view.
	 *
	 * @return Size in format {@code "23x34"};
	 */
	public String getDetailPreviewSize() {
		return getString(KEY_DETAIL_PRE, null);
	}

	public String getMapType() {
		return getString(KEY_MAP_TYPES, "0");
	}

	public String getBatteryLifeType() {
		return getString(KEY_BATTERY_TYPES, "0");
	}

	public boolean isTrafficShowing() {
		return getBoolean(KEY_TRAFFIC_SHOWING, false);
	}

	public String getDistanceUnitsType() {
		return getString(KEY_DISTANCE_UNITS, "0");
	}

	public String getTransportationMethod() {
		return getString(KEY_TRANSPORTATION, "1");
	}

	public int getAlarmArea() {
		switch (getString(KEY_ALARM_AREA, "0")) {
			case "0":
				return 100;
			case "1":
				return 150;
			case "2":
				return 200;
			default:
				return 100;
		}
	}

	public String getAlarmAreaValue() {
		return getString(KEY_ALARM_AREA, "0");
	}

	public String getWeatherUnitsType() {
		return getString(KEY_WEATHER_UNITS, "0");
	}

	public boolean showWeatherBoard() {
		return getBoolean(KEY_WEATHER_SHOW_WEATHER_BOARD, true);
	}

	public boolean notificationWarmTips() {
		return getBoolean(KEY_NOTIFICATION_WARM_TIPS, true);
	}

	public boolean notificationWeekendCall() {
		return getBoolean(KEY_NOTIFICATION_WEEKEND_CALL, true);
	}

	/**
	 * Host name for Google's map search engine.
	 *
	 * @return The host name of Google'map search.
	 */
	public String getGoogleMapSearchHost() {
		return getString(KEY_GOOGLE_MAP_SEARCH_HOST, null);
	}

	/**
	 * Url to load a icon of current weather.
	 *
	 * @param name Icon's name.
	 * @return A completed URL to an icon.
	 */
	public String getWeatherIconUrl(String name) {
		return String.format(getString(KEY_WEATHER_ICON_URL, null), name);
	}

	/**
	 * The weather-API host.
	 *
	 * @return The host of API.
	 */
	public String getWeatherApiHost() {
		return getString(KEY_WEATHER_API_HOST, null);
	}

	public int getAds() {
		return getInt(ADS, 1);
	}

	public void setAds(int c) {
		setInt(ADS, c);
	}

	/**
	 * Set status of showcase of a type like : my-location etc.
	 * <p/>
	 * see. {@link #KEY_SHOWCASE_MY_LOCATION}, {@link #KEY_SHOWCASE_NEAR_RING}
	 */
	public void setShowcase(String name, boolean value) {
		setBoolean(name, value);
	}

	/**
	 * Get status of showcase of a type like : my-location etc.
	 * <p/>
	 * see. {@link #KEY_SHOWCASE_MY_LOCATION}, {@link #KEY_SHOWCASE_NEAR_RING}
	 */
	public boolean isShowcaseShown(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Map shows clusters, in order to show them, the map needs limit count if the clusters will be populated.
	 *
	 * @return Limit count for showing cluster on map.
	 */
	public int getClusterLimit() {
		return getInt(KEY_CLUSTER_LIMIT, 50);
	}

	public void setSelectedPlayground(@Nullable LatLng latLng) {
		if (latLng == null) {
			setString(KEY_SELECT_PLAYGROUND_LAT, null);
			setString(KEY_SELECT_PLAYGROUND_LNG, null);
			return;
		}
		setString(KEY_SELECT_PLAYGROUND_LAT, latLng.latitude + "");
		setString(KEY_SELECT_PLAYGROUND_LNG, latLng.longitude + "");
	}

	public
	@Nullable
	LatLng getSelectedPlayground() {
		if (TextUtils.isEmpty(getString(KEY_SELECT_PLAYGROUND_LAT, null)) && TextUtils.isEmpty(getString(KEY_SELECT_PLAYGROUND_LNG, null))) {
			return null;
		}
		return new LatLng(Double.valueOf(getString(KEY_SELECT_PLAYGROUND_LAT, null)), Double.valueOf(getString(KEY_SELECT_PLAYGROUND_LNG, null)));
	}

	/**
	 * Mark current selected  menu-item,  see {@link AppActivity}.
	 *
	 * @param currentSelectedMenuItem , see {@link  AppActivity#MENU_ITEM_OTHERS},{@link  AppActivity#MENU_ITEM_FAVORITE},{@link  AppActivity#MENU_ITEM_NEAR_RING}
	 */
	public void setCurrentSelectedMenuItem(int currentSelectedMenuItem) {
		setInt(KEY_CURRENT_MENU_ITEM, currentSelectedMenuItem);
	}

	/**
	 * Get marked current selected  menu-item,  see {@link AppActivity}.
	 *
	 * @return see {@link  AppActivity#MENU_ITEM_OTHERS},{@link  AppActivity#MENU_ITEM_FAVORITE},{@link  AppActivity#MENU_ITEM_NEAR_RING}
	 */
	public int getCurrentSelectedMenuItem() {
		return getInt(KEY_CURRENT_MENU_ITEM, MENU_ITEM_OTHERS);
	}
}
