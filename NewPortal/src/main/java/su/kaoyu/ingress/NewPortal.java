/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


package su.kaoyu.ingress;

import android.annotation.SuppressLint;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AndroidException;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class NewPortal {

    private IActivityManager mAm;
    protected String[] mArgs;
    private int mNextArg;

    public static void main(String[] args) {
        (new NewPortal()).run(args);
    }

    public void run(String[] args) {
        if (args.length < 1) {
            return;
        }
        this.mArgs = args;
        this.mNextArg = 0;

        try {
            onRun();
        } catch (IllegalArgumentException e) {
            System.err.println();
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public String nextArg() {
        return this.mNextArg < this.mArgs.length ? this.mArgs[this.mNextArg++] : null;
    }

    public void onRun() throws Exception {

        mAm = ActivityManagerNative.getDefault();
        if (mAm == null) {
            System.err.println("Error type 2");// BaseCommand.NO_SYSTEM_ERROR_CODE
            throw new AndroidException("Can't connect to activity manager; is the system running?");
        }

        Intent intent = new Intent();

        intent.setComponent(new ComponentName("com.nianticproject.ingress", "com.nianticproject.ingress.PortalAddActivity"));
        String uriPath = nextArg();
        String lat = nextArg();
        String lng = nextArg();

        Class<?> ImmutableLocationE6Class = Class.forName("com.nianticproject.ingress.gameentity.components.ImmutableLocationE6");
        Field latLngField = ImmutableLocationE6Class.getDeclaredField("latLng");
        latLngField.setAccessible(true);

        Constructor<?> ImmutableLocationE6ClassConstructor = ImmutableLocationE6Class.getConstructor(int.class, int.class);
        intent.putExtra("initial_lat_lng", (Serializable) latLngField.get(
                ImmutableLocationE6ClassConstructor.newInstance(
                        (int) (Double.valueOf(lat) * 1000000), (int) (Double.valueOf(lng) * 1000000))));

        intent.putExtra("android.intent.extra.STREAM", Uri.parse(uriPath));

        String mimeType = intent.getType();

        System.out.println("Starting: " + intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int res = 99;

        Method[] methods = mAm.getClass().getDeclaredMethods();
        @SuppressLint("InlinedApi") int sdkInt = Build.VERSION.SDK_INT;
        for (Method method : methods) {
            if ("startActivity".equals(method.getName())) {
                Type[] paramTypes = method.getGenericParameterTypes();
                if ((int.class).equals(method.getGenericReturnType())) {
                    if (sdkInt >= 21 && paramTypes.length == 10) { //
                        res = (int) method.invoke(mAm, null, null, intent, mimeType,
                                null, null, 0, 0, null, null);
                    } else if (sdkInt >= 18 && paramTypes.length == 11) { // JELLY_BEAN_MR2
                        res = (int) method.invoke(mAm, null, null, intent, mimeType,
                                null, null, 0, 0, null, null, null);
                    } else if (sdkInt >= 16 && paramTypes.length == 10) { // JELLY_BEAN
                        res = (int) method.invoke(mAm, null, intent, mimeType,
                                null, null, 0, 0, null, null, null);
                    } else if (sdkInt >= 14 && paramTypes.length == 13) { // JELLY_BEAN
                        res = (int) method.invoke(mAm, null, intent, mimeType,
                                null, 0, null, null, 0, false, false,
                                null, null, false);
                    } else if ((sdkInt == 9 || sdkInt == 10) && paramTypes.length == 10) {
                        res = (int) method.invoke(mAm, null, intent, mimeType,
                                null, 0, null, null, 0, false, false);
                    }
                    break;
                }
            }
        }

        PrintStream out = System.err;
        switch (res) {
            case 0://ActivityManager.START_SUCCESS:
                break;
            case 4://ActivityManager.START_SWITCHES_CANCELED:
                out.println(
                        "Warning: Activity not started because the "
                                + " current activity is being kept for the user.");
                break;
            case 3://ActivityManager.START_DELIVERED_TO_TOP:
                out.println(
                        "Warning: Activity not started, intent has "
                                + "been delivered to currently running "
                                + "top-most instance.");
                break;
            case 1://ActivityManager.START_RETURN_INTENT_TO_CALLER:
                out.println(
                        "Warning: Activity not started because intent "
                                + "should be handled by the caller");
                break;
            case 2://ActivityManager.START_TASK_TO_FRONT:
                out.println(
                        "Warning: Activity not started, its current "
                                + "task has been brought to the front");
                break;
            case -1://ActivityManager.START_INTENT_NOT_RESOLVED:
                out.println(
                        "Error: Activity not started, unable to "
                                + "resolve " + intent.toString());
                break;
            case -2://ActivityManager.START_CLASS_NOT_FOUND:
                out.println("Error type 3"); //BaseCommand.NO_CLASS_ERROR_CODE
                out.println("Error: Activity class " +
                        intent.getComponent().toShortString()
                        + " does not exist.");
                break;
            case -3://ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT:
                out.println(
                        "Error: Activity not started, you requested to "
                                + "both forward and receive its result");
                break;
            case -4://ActivityManager.START_PERMISSION_DENIED:
                out.println(
                        "Error: Activity not started, you do not "
                                + "have permission to access it.");
                break;
            case -7://ActivityManager.START_NOT_VOICE_COMPATIBLE:
                out.println(
                        "Error: Activity not started, voice control not allowed for: "
                                + intent);
                break;
            default:
                out.println(
                        "Error: Activity not started, unknown error code " + res);
                break;
        }
    }
}
