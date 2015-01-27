/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cordova.geolocation;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

/*
 * This class is the interface to the Geolocation.  It's bound to the geo object.
 *
 * This class only starts and stops various GeoListeners, which consist of a GPS and a Network Listener
 */

public class GeoBroker extends CordovaPlugin {
    public LocationClient locationClient;
    private BaiduListener baiduListener;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return                True if the action was valid, or false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (locationClient == null) {
            locationClient = new LocationClient(this.cordova.getActivity().getApplicationContext());
        }
        if (baiduListener == null) {
            baiduListener = new BaiduListener(locationClient, this);
            locationClient.registerLocationListener(baiduListener);
        }

        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        option.setNeedDeviceDirect(true);

        if (action.equals("getLocation")) {
            boolean enableHighAccuracy = args.getBoolean(0);
            int maximumAge = args.getInt(1);

            if (enableHighAccuracy) {
                option.setLocationMode(LocationMode.Hight_Accuracy);
            }
            else {
                option.setLocationMode(LocationMode.Battery_Saving);
            }
            locationClient.setLocOption(option);
            Location last = toLocation(locationClient.getLastKnownLocation());
            // Check if we can use lastKnownLocation to get a quick reading and use less battery
            if (last != null && (System.currentTimeMillis() - last.getTime()) <= maximumAge) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(last));
                callbackContext.sendPluginResult(result);
            } else {
                this.getCurrentLocation(callbackContext, enableHighAccuracy, args.optInt(2, 60000));
            }
        }
        else {
            return false;
        }
        return true;
    }

    public Location toLocation(BDLocation bdLocation) {
        Location location = new Location("baidu");
        if (bdLocation != null) {
            int locType = bdLocation.getLocType();
            if (locType == BDLocation.TypeCacheLocation) {
            }
            else if (locType == BDLocation.TypeGpsLocation) {
            }
            else if (locType == BDLocation.TypeNetWorkLocation) {
            }
            else if (locType == BDLocation.TypeOffLineLocation) {
            }
            else {
                return location;
            }
            location.setLatitude(bdLocation.getLatitude());
            location.setLongitude(bdLocation.getLongitude());
            location.setAltitude(bdLocation.getAltitude());
            location.setAccuracy(bdLocation.getRadius());
            location.setBearing(bdLocation.getDirection());
            location.setSpeed(bdLocation.getSpeed());
            String time = bdLocation.getTime();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                if (time != null) {
                    Date date = format.parse(time);
                    location.setTime(date.getTime());
                }
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return location;
    }

    private void getCurrentLocation(CallbackContext callbackContext, boolean enableHighAccuracy, int timeout) {
        if (this.locationClient != null) {
            this.baiduListener.addCallback(callbackContext, timeout);
        }
    }

    /**
     * Called when the activity is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        if (this.baiduListener != null) {
            this.baiduListener.destroy();
            this.baiduListener = null;
        }
    }

    /**
     * Called when the view navigates.
     * Stop the listeners.
     */
    public void onReset() {
        this.onDestroy();
    }

    public JSONObject returnLocationJSON(Location loc) {
        JSONObject o = new JSONObject();

        try {
            o.put("latitude", loc.getLatitude());
            o.put("longitude", loc.getLongitude());
            o.put("altitude", (loc.hasAltitude() ? loc.getAltitude() : null));
            o.put("accuracy", loc.getAccuracy());
            o.put("heading", (loc.hasBearing() ? (loc.hasSpeed() ? loc.getBearing() : null) : null));
            o.put("velocity", loc.getSpeed());
            o.put("timestamp", loc.getTime());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return o;
    }

    public void win(Location loc, CallbackContext callbackContext, boolean keepCallback) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(loc));
        result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    /**
     * Location failed.  Send error back to JavaScript.
     *
     * @param code  The error code
     * @param msg   The error message
     * @throws JSONException
     */
    public void fail(int code, String msg, CallbackContext callbackContext, boolean keepCallback) {
        JSONObject obj = new JSONObject();
        String backup = null;
        try {
            obj.put("code", code);
            obj.put("message", msg);
        } catch (JSONException e) {
            obj = null;
            backup = "{'code':" + code + ",'message':'" + msg.replaceAll("'", "\'") + "'}";
        }
        PluginResult result;
        if (obj != null) {
            result = new PluginResult(PluginResult.Status.ERROR, obj);
        } else {
            result = new PluginResult(PluginResult.Status.ERROR, backup);
        }

        result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    public boolean isGlobalListener(CordovaLocationListener listener)
    {
        if (baiduListener != null)
        {
            return baiduListener.equals(listener);
        }
        else
            return false;
    }
}
