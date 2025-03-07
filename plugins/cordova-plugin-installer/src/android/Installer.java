package de.mopsdom.installer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import android.content.pm.PackageManager;
import android.content.pm.SigningInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.health.connect.datatypes.AppInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Installer extends CordovaPlugin {

  private static Installer instance;
  private final String pluginName = "cordova-plugin-installer";
  private CallbackContext onDataRequestCallbackContext = null;

  private CallbackContext installContext;

  public Installer() {
    super();
    instance = this;
  }

  public static Installer getInstance() {
    return instance;
  }

  public File copyApkToExternalStorage(String packageid) {
    try {
      // APK-Pfad der aktuellen App abrufen
      ApplicationInfo appInfo = cordova.getActivity().getPackageManager().getApplicationInfo(packageid, 0);
      String sourceApkPath = appInfo.sourceDir;

      // Zielverzeichnis: /sdcard/Android/data/PACKAGEID/files/
      File destDir = cordova.getActivity().getExternalFilesDir(null);
      if (destDir == null) {
        Log.e(Installer.class.getName(), "External files dir is null");
        return null;
      }

      // Zielpfad für die Kopie
      File destFile = new File(destDir, "backup.apk");

      // Datei kopieren
      try (FileInputStream in = new FileInputStream(sourceApkPath);
           FileOutputStream out = new FileOutputStream(destFile)) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }

      return destFile.exists() ? destFile : null; // Gibt die Datei zurück, wenn sie erfolgreich kopiert wurde
    } catch (PackageManager.NameNotFoundException | IOException e) {
      Log.e(Installer.class.getName(), "Fehler beim Kopieren der APK", e);
      return null;
    }
  }

  public void installAPK(CallbackContext installContext, String packageid) {

    this.installContext = installContext;

    File source = copyApkToExternalStorage(packageid);
    if (source == null) {
      PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Fehler bei Extraktion der Installationsdaten.");
      this.installContext.sendPluginResult(result);
      return;
    }

    SharedPreferences prefs = cordova.getActivity().getSharedPreferences("install_prefs", Context.MODE_PRIVATE);
    prefs.edit().putBoolean("install_running", true).apply();

   try {

     

      install2();

      PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
      result.setKeepCallback(true);
      this.installContext.sendPluginResult(result);

    } catch (Exception e) {
      PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
      result.setKeepCallback(true);
      this.installContext.sendPluginResult(result);
      Log.e(Installer.class.getName(), e.getMessage(), e);
    }
  }

  public void install2()
  {
    // Zielpfad für die Kopie
    File source = new File(cordova.getActivity().getExternalFilesDir(null), "backup.apk");

    Intent intent;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
      intent.setData(getUri(source));
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    } else {
      intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndTypeAndNormalize(Uri.fromFile(source), "application/vnd.android.package-archive");
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
    intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending");

    Log.e(Installer.class.getName(),"*************************************************************************************");
    cordova.getActivity().startActivity(intent);
    Log.e(Installer.class.getName(),"*************************************************************************************");

  }

  private void checkInstallationStatus(CallbackContext installContext) {
    SharedPreferences prefs = cordova.getActivity().getSharedPreferences("install_prefs", Context.MODE_PRIVATE);
    boolean wasInstalling = prefs.getBoolean("install_running", false);

    if (wasInstalling) {
      prefs.edit().remove("install_running").apply(); // Reset-Flag entfernen

      // Überprüfe den Installationspfad
      PackageManager pm = cordova.getActivity().getPackageManager();
      try {
        PackageInfo packageInfo = pm.getPackageInfo(cordova.getActivity().getPackageName(), 0);
        long installTime = packageInfo.firstInstallTime; // Erste Installation
        long updateTime = packageInfo.lastUpdateTime; // Letztes Update

        if (installTime == updateTime) {
          Log.d("InstallCheck", "Frisch installiert!");
          PluginResult result = new PluginResult(PluginResult.Status.OK);
          installContext.sendPluginResult(result);
        } else {
          Log.d("InstallCheck", "Update durchgeführt!");
          PluginResult result = new PluginResult(PluginResult.Status.OK);
          installContext.sendPluginResult(result);
        }
      } catch (PackageManager.NameNotFoundException e) {
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
        result.setKeepCallback(true);
        installContext.sendPluginResult(result);
        Log.e(Installer.class.getName(), e.getMessage());
      }
    }
    else
    {
      PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Installation wurde nicht gestartet.");
      result.setKeepCallback(true);
      installContext.sendPluginResult(result);
      Log.e(Installer.class.getName(), "Installation wurde nicht gestartet.");
    }
  }


  public Uri getUri(File file) {
    return FileProvider.getUriForFile(
      cordova.getActivity(),
      cordova.getActivity().getPackageName()+".fileProvider",
      file
    );
  }

  public static String drawableToBase64(Drawable drawable) {
    // Umwandlung des Drawables in Bitmap
    Bitmap bitmap = drawableToBitmap(drawable);

    if (bitmap == null) {
      return null;
    }

    // Konvertieren des Bitmaps in ein ByteArray
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();

    // Konvertieren des ByteArrays in einen Base64-String
    return Base64.encodeToString(byteArray, Base64.DEFAULT);
  }

  // Funktion zum Umwandeln eines Drawables in ein Bitmap
  public static Bitmap drawableToBitmap(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable) drawable).getBitmap();
    }

    // Bitmap erstellen, falls es nicht vom Typ BitmapDrawable ist
    Bitmap bitmap = Bitmap.createBitmap(
            drawable.getIntrinsicWidth(),
            drawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888
    );
    // Die Canvas-Klasse verwendet das Drawable, um das Bitmap zu füllen
    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }

  private void getInstalledApps(CallbackContext installContext){

    JSONArray result = new JSONArray();
    PackageManager pm = cordova.getActivity().getPackageManager();
    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

    for (ApplicationInfo appInfo : packages) {
      // Überprüfen, ob es sich um eine System-App handelt. Wenn es eine System-App ist, überspringen wir sie.
      if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
        try {

          Drawable icon = appInfo.loadIcon(pm);

          String name = appInfo.loadLabel(pm).toString();
          String packageName = appInfo.packageName;
          String b64 = drawableToBase64(icon);

          JSONObject obj = new JSONObject();
          obj.put("name",name);
          obj.put("package",packageName);
          obj.put("icon",b64!=null?b64:"");

          result.put(obj);

        } catch (Exception e) {
          Log.e("TAG", e.getMessage(), e);
        }
      }
    }

    PluginResult r = new PluginResult(PluginResult.Status.OK,result.toString());
    installContext.sendPluginResult(r);
  }


  @Override
  public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) {

    try {
     
      if (action.equals("install")) {
        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            try {
              if (data != null && data.length() > 0)
                installAPK(callbackContext, data.getString(0));
            }
            catch (Exception e)
            {
              PluginResult r = new PluginResult(PluginResult.Status.ERROR,e.getMessage());
              callbackContext.sendPluginResult(r);
            }
          }
        });

        return true;
      } else if (action.equals("checkInstallationStatus")) {
        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            checkInstallationStatus(callbackContext);
          }
        });

        return true;
      } else if (action.equals("getInstalledApps")) {
        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            getInstalledApps(callbackContext);
          }
        });

        return true;
      }

      return false;
    } catch (Exception e) {
      Log.e(pluginName, e.getMessage(), e);
      return false;
    }
  }
}
