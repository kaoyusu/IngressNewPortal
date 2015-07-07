package su.kaoyu.ingressnewportal;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1;
    private File libFile;
    private String ingressApkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!copyLib()) {
            Toast.makeText(this, "newportal.jar copy failed", Toast.LENGTH_SHORT).show();
            finish();
        }

        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo("com.nianticproject.ingress", PackageManager.GET_CONFIGURATIONS);
            ingressApkPath = packageInfo.applicationInfo.sourceDir;
            if (TextUtils.isDigitsOnly(ingressApkPath)) {
                Toast.makeText(this, "Can't found Ingress on this Phone.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Can't found Ingress on this Phone.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (type != null && type.startsWith("image/")) {
            if (Intent.ACTION_SEND.equals(action)) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    callNewPortalLib(imageUri);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    for (Uri imageUri : imageUris) {
                        callNewPortalLib(imageUri);
                    }
                }
            }
            finish();
        } else {
            selectPic();
        }
    }

    private boolean copyLib() {
        try {
            InputStream in = getAssets().open("newportal.jar");
            libFile = new File(getFilesDir(), "newportal.jar");
            if (libFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                libFile.delete();
            }
            if (libFile.createNewFile()) {
                OutputStream out = new FileOutputStream(libFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
                return true;
            }
        } catch (Exception e) {
            Toast.makeText(this, "newportal.jar open failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return false;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void selectPic() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");

        startActivityForResult(intent, PICK_IMAGE);
    }

    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentUri,
                    new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //http://android-er.blogspot.com/2010/01/convert-exif-gps-info-to-degree-format.html
    private Double convertToDegree(String stringDMS) {
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = Double.valueOf(stringD[0]);
        Double D1 = Double.valueOf(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = Double.valueOf(stringM[0]);
        Double M1 = Double.valueOf(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = Double.valueOf(stringS[0]);
        Double S1 = Double.valueOf(stringS[1]);
        Double FloatS = S0 / S1;

        return FloatD + (FloatM / 60) + (FloatS / 3600);
    }

    private void callNewPortalLib(Uri uri) {
        String path = getRealPathFromURI(uri);
        String lat;
        String lng;
        try {
            ExifInterface exif = new ExifInterface(path);
            lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            lng = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String cmd;
            if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) {
                Toast.makeText(this, "No GPS Data found in picture, using your location", Toast.LENGTH_SHORT).show();
                LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location location = mLocationManager.getLastKnownLocation("gps");
                if (location == null) {
                    location = mLocationManager.getLastKnownLocation("network");
                }
                if (location == null) {
                    location = new Location("");
                    location.setLatitude(30);
                    location.setLongitude(120);
                }
                cmd = String.format("export CLASSPATH=%s:%s && exec app_process /system/bin su.kaoyu.ingress.NewPortal %s %f %f", libFile.getAbsolutePath(), ingressApkPath, Uri.fromFile(new File(path)).toString(), location.getLatitude(), location.getLongitude());
            } else {
                cmd = String.format("export CLASSPATH=%s:%s && exec app_process /system/bin su.kaoyu.ingress.NewPortal %s %s %s", libFile.getAbsolutePath(), ingressApkPath, Uri.fromFile(new File(path)).toString(), convertToDegree(lat), convertToDegree(lng));
            }
            Log.v("NewPort", cmd);
            Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
        } catch (IOException e) {
            Toast.makeText(this, "Error occurred on opening picture", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            if (data == null) {
                finish();
                return;
            }
            callNewPortalLib(data.getData());
        } else {
            finish();
        }
    }
}
