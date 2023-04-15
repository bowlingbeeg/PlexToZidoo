package com.hpn789.plextozidoo;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity
{
    private final String backupFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/PlexToZidooSettings.txt";
    private static SettingsFragment settingsFragment;
    private static final int PERMISSION_REQUEST_IMPORT = 1;
    private static final int PERMISSION_REQUEST_EXPORT = 2;
    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            settingsFragment = this;
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            EditTextPreference smbPasswordPreference = findPreference("smbPassword");
            if (smbPasswordPreference != null)
            {
                smbPasswordPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            }
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    public void onImportClickMethod(View view)
    {
        // If we had to request permissions, if we did then we'll do the import when we get notified that the access was granted
        if (checkAndRequestPermissions(PERMISSION_REQUEST_IMPORT))
        {
            return;
        }

        importSettings();
    }

    public void importSettings()
    {
        FileInputStream input = null;
        BufferedReader reader = null;
        try
        {
            input = new FileInputStream(backupFile);
            reader = new BufferedReader(new InputStreamReader(input));

            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, ?>>(){}.getType();
            Map<String, ?> map = gson.fromJson(reader, mapType);

            for (Map.Entry<String, ?> entry : map.entrySet())
            {
                try
                {
                    Preference pref = settingsFragment.findPreference(entry.getKey());
                    if(pref instanceof EditTextPreference)
                    {
                        if(entry.getValue().toString().isEmpty())
                        {
                            ((EditTextPreference)pref).setText(null);
                        }
                        else
                        {
                            ((EditTextPreference)pref).setText(entry.getValue().toString());
                        }
                    }
                    else if(pref instanceof SwitchPreference)
                    {
                        // Don't restore a password even if it exists
                        if(!entry.getKey().equals("smbPassword"))
                        {
                            if(entry.getValue().toString().isEmpty())
                            {
                                ((SwitchPreference)pref).setChecked(false);
                            }
                            else
                            {
                                ((SwitchPreference)pref).setChecked(Boolean.parseBoolean(entry.getValue().toString()));
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    // Ignore bad data
                }
            }

            Toast.makeText(getApplicationContext(), "Settings imported successfully from " + backupFile, Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Failed to import settings from " + backupFile, Toast.LENGTH_LONG).show();
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
                if (input != null)
                {
                    input.close();
                }
            }
            catch (IOException ex)
            {
                Toast.makeText(getApplicationContext(), "Failed to close import file", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onExportClickMethod(View view)
    {
        // If we had to request permissions, if we did then we'll do the export when we get notified that the access was granted
        if (checkAndRequestPermissions(PERMISSION_REQUEST_EXPORT))
        {
            return;
        }

        exportSettings();
    }

    public void exportSettings()
    {
        FileOutputStream output = null;
        try
        {
            output = new FileOutputStream(backupFile);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Map<String,?> prefsMap = prefs.getAll();

            // Don't save off the password
            prefsMap.remove("smbPassword");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(prefsMap);
            output.write(json.getBytes(StandardCharsets.UTF_8));

            Toast.makeText(getApplicationContext(), "Settings successfully exported to " + backupFile, Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), e.toString()/*"Failed to export settings to " + backupFile*/, Toast.LENGTH_LONG).show();
        }
        finally
        {
            try
            {
                if (output != null)
                {
                    output.flush();
                    output.close();
                }
            }
            catch (IOException ex)
            {
                Toast.makeText(getApplicationContext(), "Failed to close export file", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Return true if we have to request the permissions, that way the caller can wait until we were granted the permissions before continuing
    public Boolean checkAndRequestPermissions(int requestCode)
    {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(permissions, requestCode);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if(requestCode == PERMISSION_REQUEST_IMPORT || requestCode == PERMISSION_REQUEST_EXPORT)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                if(requestCode == PERMISSION_REQUEST_IMPORT)
                {
                    importSettings();
                }
                else //if(requestCode == PERMISSION_REQUEST_EXPORT)
                {
                    exportSettings();
                }
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Please give storage access in order to import/export settings", Toast.LENGTH_LONG).show();
            }
        }
    }
}