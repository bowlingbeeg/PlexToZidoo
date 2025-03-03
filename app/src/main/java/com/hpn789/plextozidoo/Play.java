package com.hpn789.plextozidoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Play extends AppCompatActivity
{
    static final String tokenParameter = "X-Plex-Token=";
    static final String clientParameter = "X-Plex-Client-Identifier=";
    private Intent originalIntent;
    private Intent newIntent;
    private String address = "";
    private PlexLibraryInfo libraryInfo;
    private String ratingKey = "";
    private String partKey = "";
    private String partId = "";
    private String token = "";
    private int duration = 0;
    private int viewOffset = 0;
    private String directPath = "";
    private String videoTitle = "";
    private boolean audioSelected = false;
    private int selectedAudioIndex = -1;
    private boolean subtitleSelected = false;
    private int selectedSubtitleIndex = -1;
    private String password = "";
    private int videoIndex = 0;
    private String parentRatingKey = "";
    private String server = "";
    private String message = "";
    private boolean foundSubstitution = false;
    private String videoPath = "";
    private boolean remoteStream = false;
    private boolean zdmc = false;
    private int mediaIndex = -1;

    private TextView textView1;
    private TextView textView2;
    private Button playButton;

    private boolean useNewZdiooPlayer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        useNewZdiooPlayer = useNewZdiooPlayer();
        setContentView(R.layout.activity_play);
    }

    private boolean useNewZdiooPlayer()
    {
        try
        {
            PackageInfo packageInfo;
            try
            {
                packageInfo = getPackageManager().getPackageInfo("com.zidoo.player", 0);
            }
            catch (Exception e)
            {
                packageInfo = null;
            }

            if (packageInfo != null)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        this.finish();
    }

    private String getPrintableString(String inputString)
    {
        String outputString = inputString;

        // Replace the address, token, and client identifier in the intent and path strings
        outputString = outputString.replaceFirst("https://[^/ ]+", "https://<address>");
        outputString = outputString.replaceFirst(tokenParameter + "[^& ]+", tokenParameter + "<token>");
        outputString = outputString.replaceFirst(clientParameter + "[^& ]+", clientParameter + "<client>");

        return outputString;
    }

    private void updateDebugPage()
    {
        String originalIntentToPrint = getPrintableString(intentToString(originalIntent));
        String newIntentToPrint = getPrintableString(intentToString(newIntent));
        String pathToPrint = getPrintableString(directPath);

        // If the path has a password in it then hide it from the debug output
        if(!password.isEmpty())
        {
            pathToPrint = pathToPrint.replaceFirst(":" + password + "@", ":********@");
        }

        String librarySection = "";
        String mediaType = "";
        if(libraryInfo != null)
        {
            librarySection = libraryInfo.getKey();
            mediaType = libraryInfo.getType().name;
        }

        if(!foundSubstitution && message.isEmpty())
        {
            message = "ERROR: No substitution found";
        }

        if(!message.isEmpty())
        {
            textView1.setVisibility(View.VISIBLE);
            textView1.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            if(message.contains("WARNING"))
            {
                textView1.setBackgroundColor(0xFFFFDB58);
            }
            textView1.setText(String.format(Locale.ENGLISH, message + "\n"));
        }

        textView2.setText(String.format(Locale.ENGLISH, "Intent: %s\n\nPath Substitution: %s\n\nVideo Path: %s\n\nView Offset: %d\n\nDuration: %d\n\nRating Key: %s\n\nPart Key: %s\n\nPart ID: %s\n\nLibrary Section: %s\n\nMedia Type: %s\n\nSelected Audio Index: %d\n\nSelected Subtitle Index: %d\n\nVideo Index: %d\n\nParent Rating Key: %s\n\nNew Zidoo Player: %b\n\nNew Intent: %s", originalIntentToPrint, pathToPrint, videoPath, viewOffset, duration, ratingKey, partKey, partId, librarySection, mediaType, selectedAudioIndex, selectedSubtitleIndex, videoIndex, parentRatingKey, useNewZdiooPlayer, newIntentToPrint));
    }

    private void showDebugPageOrSendIntent()
    {
        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("useZidooPlayer", true))
        {
            buildZidooIntent(directPath, viewOffset);
        }
        else
        {
            buildDefaultIntent(directPath);
        }

        // If the debug flag is on then update the text field
        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("debug", false))
        {
            updateDebugPage();

            playButton.setEnabled(true);
            playButton.setVisibility(View.VISIBLE);
        }
        // Else just play the movie
        else
        {
            playButton.callOnClick();
        }
    }

    private void searchFiles()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        // Attempt to find the next video
        videoIndex++;
        String url = address + "/library/sections/" + libraryInfo.getKey() + "/search?type=" + libraryInfo.getType().searchId + "&index=" + videoIndex + "&parent=" + parentRatingKey + "&" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response ->
                {
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(null, -1);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try
                    {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            String inputString = originalIntent.getDataString();
                            inputString = inputString.replace(partKey, path);
                            originalIntent.setData(Uri.parse(inputString));
                            originalIntent.putExtra("viewOffset", 0);

                            startActivity(originalIntent);
                        }
                    }
                    catch (Exception e)
                    {
                        message = "ERROR 6: " + e;
                        showDebugPageOrSendIntent();
                        return;
                    }
                },
                error ->
                {
                    message = "WARNING: Couldn't find next file - " + error.toString();
                    showDebugPageOrSendIntent();
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchMetadata()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/metadata/" + ratingKey + "?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response ->
                {
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(partKey, mediaIndex);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try
                    {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            if(!audioSelected)
                            {
                                audioSelected = parser.isAudioSelected();
                                if (audioSelected)
                                {
                                    selectedAudioIndex = parser.getSelectedAudioIndex();
                                }
                            }

                            if(!subtitleSelected)
                            {
                                subtitleSelected = parser.isSubtitleSelected();
                                if (subtitleSelected)
                                {
                                    selectedSubtitleIndex = parser.getSelectedSubtitleIndex();
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        message = "ERROR 5: " + e;
                    }

                    showDebugPageOrSendIntent();
                },
                error ->
                {
                    message = "ERROR: Couldn't find metadata - " + error.toString();
                    showDebugPageOrSendIntent();
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void doSubstitution(String path)
    {
        // Check if we can actually do the substitution, if not then pass along the original file and see if it plays
        String[] pref_index = {"", "_02", "_03", "_04", "_05", "_06", "_07", "_08", "_09", "_10"};
        for (String s: pref_index)
        {
            String[] path_to_replace_array = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("path_to_replace" + s, "").split("\\s*,\\s*");
            String[] replaced_with_array = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("replaced_with" + s, "").split("\\s*,\\s*");
            String smb_username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbUsername" + s, "");
            String smb_password = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbPassword" + s, "");

            if (path_to_replace_array.length > 0 && replaced_with_array.length > 0 && path_to_replace_array.length == replaced_with_array.length)
            {
                for (int i = 0; i < path_to_replace_array.length; i++)
                {
                    if (!path_to_replace_array[i].isEmpty() && path.contains(path_to_replace_array[i]))
                    {
                        path = path.replaceFirst(Pattern.quote(path_to_replace_array[i]), replaced_with_array[i]).replace("\\", "/");

                        if (path.contains("nfs://") || directPath.contains("/mnt/nfs/"))
                        {
                            // 8k model suppors NFS mounting. For example:  "path_to_replace": "/Volumes/share1" ----  "replaced_with": "nfs://192.168.11.113/share1", The player will automatically mount NFS：/mnt/nfs/192.168.11.113#share1
                            if (!useNewZdiooPlayer)
                            {
                                path = path.replaceAll("nfs://(.*?)/", "/mnt/nfs/$1#");
                            }
                        }
                        else
                        {
                            path = Uri.encode(path, "/ :");
                        }

                        // If this is an SMB request add user name and password to the path
                        if (!smb_username.isEmpty())
                        {
                            password = smb_password;
                            path = path.replace("smb://", "smb://" + smb_username + ":" + password + "@");
                        }

                        foundSubstitution = true;
                        directPath = path;

                        return;
                    }
                }
            }
        }
    }

    private void searchPath(List<PlexLibraryInfo> infos, int index)
    {
        PlexLibraryInfo info = infos.get(index);
        libraryInfo = info;
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/sections/" + info.getKey() + "/search?type=" + info.getType().searchId + "&part=" + partId + "&" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response ->
                {
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(partKey, mediaIndex);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try
                    {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            videoPath = path;
                            ratingKey = parser.getRatingKey();
                            videoTitle = parser.getVideoTitle();
                            duration = parser.getDuration();
                            videoIndex = parser.getVideoIndex();
                            parentRatingKey = parser.getParentRatingKey();
                            password = "";

                            doSubstitution(path);

                            if(!foundSubstitution && originalIntent.getDataString().contains("&location=wan&"))
                            {
                                remoteStream = true;
                                message = "WARNING: Remote Stream - May Not Work";
                            }

                            // Search the metadata for audio and subtitle indexes
                            searchMetadata();
                        }
                        else if(index + 1 < infos.size())
                        {
                            searchPath(infos, index + 1);
                        }
                        else
                        {
                            message = "ERROR: Video not found on Plex";
                            showDebugPageOrSendIntent();
                        }
                    }
                    catch (Exception e)
                    {
                        message = "ERROR 4: " + e;
                        showDebugPageOrSendIntent();
                        return;
                    }
                },
                error ->
                {
                    if(index + 1 < infos.size())
                    {
                        searchPath(infos, index + 1);
                    }
                    else
                    {
                        message = "ERROR: Couldn't find path - " + error.toString();
                        showDebugPageOrSendIntent();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchLibrary()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/sections/?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response ->
                {
                    String include_libraries = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("libraries", "").trim();
                    String exclude_libraries = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("exclude_libraries", "").trim();
                    PlexXmlParser parser = new PlexXmlParser(include_libraries, exclude_libraries);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try
                    {
                        List<PlexLibraryInfo> libraries = parser.parse(targetStream);
                        searchPath(libraries, 0);
                        return;
                    }
                    catch (Exception e)
                    {
                        message = "ERROR 3: " + e;
                        showDebugPageOrSendIntent();
                        return;
                    }
                },
                error ->
                {
                    message = "ERROR: Couldn't find library - " + error.toString();
                    showDebugPageOrSendIntent();
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void findServer()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/identity?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response ->
                {
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try
                    {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                        parser.setInput(targetStream, null);
                        parser.nextTag();
                        parser.require(XmlPullParser.START_TAG, null, "MediaContainer");
                        server = parser.getAttributeValue(null, "machineIdentifier");
                    }
                    catch (Exception e)
                    {
                        message = "WARNING 2: " + e;
                    }

                    searchLibrary();
                },
                error ->
                {
                    message = "WARNING: Couldn't find server - " + error.toString();
                    searchLibrary();
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        originalIntent = getIntent();

        String inputString = originalIntent.getDataString();
        directPath = inputString;
        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(v ->
        {
            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("useZidooPlayer", true))
            {
                startActivityForResult(newIntent, 98);
            }
            else
            {
                startActivity(newIntent);
            }
        });

        try
        {
            Pattern p = Pattern.compile("[?&]PlexToZidoo-([^=]+)=([^&]+)");
            Matcher m = p.matcher(inputString);
            boolean pathMapped = false;
            while(m.find())
            {
                zdmc = true;
                if(m.group(1).equals("ViewOffset"))
                {
                    viewOffset = Integer.parseInt(m.group(2));
                }
                else if(m.group(1).equals("AudioIndex"))
                {
                    audioSelected = true;
                    selectedAudioIndex = Integer.parseInt(m.group(2));
                }
                else if(m.group(1).equals("SubtitleIndex"))
                {
                    subtitleSelected = true;
                    selectedSubtitleIndex = Integer.parseInt(m.group(2));
                }
                else if(m.group(1).equals("Title"))
                {
                    videoTitle = URLDecoder.decode(m.group(2), StandardCharsets.UTF_8.toString());
                }
                else if(m.group(1).equals("Path"))
                {
                    videoPath = URLDecoder.decode(m.group(2), StandardCharsets.UTF_8.toString());
                }
                else if(m.group(1).equals("PathMapped"))
                {
                    pathMapped = true;
                }
            }

            if(zdmc)
            {
                directPath = m.replaceAll("");
                if(pathMapped)
                {
                    // Already did the substitution in kodi
                    if (directPath.contains("nfs://") || directPath.contains("/mnt/nfs/"))
                    {
                        directPath = Uri.decode(directPath);
                        directPath = directPath.replaceAll("nfs://(.*?)/", "/mnt/nfs/$1#");
                    }

                    foundSubstitution = true;
                    showDebugPageOrSendIntent();
                    return;
                }
                else if(!videoPath.isEmpty())
                {
                    doSubstitution(videoPath);
                    showDebugPageOrSendIntent();
                    return;
                }
            }
            else
            {
                try
                {
                    viewOffset = originalIntent.getIntExtra("viewOffset", 0);
                }
                catch (Exception e)
                {
                    // There is some strange error that is seen on newer levels of Plex such that if
                    // I just try again it will work.  I've only seen this on the first extra we try
                    // and read so we'll just ignore the first error and hope the second one succeeds
                    viewOffset = originalIntent.getIntExtra("viewOffset", 0);
                }
                mediaIndex = originalIntent.getIntExtra("mediaIndex", -1);
            }
        }
        catch (Exception e)
        {
            message = "ERROR 1.0: " + e;
            showDebugPageOrSendIntent();
            return;
        }

        try
        {
            Pattern tokenPattern = Pattern.compile(tokenParameter + "([^&]+)");
            Matcher tokenMatcher = tokenPattern.matcher(inputString);
            if(tokenMatcher.find() && tokenMatcher.groupCount() >= 1)
            {
                token = tokenMatcher.group(1);
            }

            Pattern addressPattern = Pattern.compile("^https://[^/]+");
            Matcher addressMatcher = addressPattern.matcher(inputString);
            if(addressMatcher.find())
            {
                address = addressMatcher.group();

                if(address.contains("provider.plex.tv"))
                {
                    message = "WARNING: Plex Free Stream - May Not Work";
                    showDebugPageOrSendIntent();
                    return;
                }
            }
            else
            {
                message = "ERROR: No address found";
                showDebugPageOrSendIntent();
                return;
            }
        }
        catch (Exception e)
        {
            message = "ERROR 1.1: " + e;
            showDebugPageOrSendIntent();
            return;
        }

        try
        {
            Pattern partKeyPattern = Pattern.compile("/(library|services)/[^?]+");
            Matcher partKeyMatcher = partKeyPattern.matcher(inputString);
            if(partKeyMatcher.find())
            {
                partKey = partKeyMatcher.group();

                // Is this an online trailer?
                if(partKey.contains("services"))
                {
                    showDebugPageOrSendIntent();
                    return;
                }

                String[] partDirs = partKey.split("/");
                if(partDirs.length > 3)
                {
                    partId = partDirs[3];
                }
            }
            else
            {
                message = "ERROR: No partKey found";
                showDebugPageOrSendIntent();
                return;
            }
        }
        catch (Exception e)
        {
            message = "ERROR 1.2: " + e;
            showDebugPageOrSendIntent();
            return;
        }

        findServer();
    }

    protected void buildDefaultIntent(String path)
    {
        newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndTypeAndNormalize(Uri.parse(path), "video/*" );
    }

    protected void buildZidooIntent(String path, int viewOffset)
    {
        // see https://github.com/Andy2244/jellyfin-androidtv-zidoo/blob/Zidoo-Edition/app/src/main/java/org/jellyfin/androidtv/ui/playback/ExternalPlayerActivity.java
        // NOTE: This code requires the new ZIDOO API to work.
        //       For Z9X and Z9X Pro lines that means firmware version 6.4.42+
        //       For Z9X 8K Line that means firmware version 1.1.42+
        newIntent = new Intent(Intent.ACTION_VIEW);

        // If it is a file, it will be played directly
        if(path.startsWith("/") && new File(path).exists())
        {
            newIntent.setDataAndTypeAndNormalize(Uri.fromFile(new File(path)), "video/*");
        }
        else
        {
            newIntent.setDataAndTypeAndNormalize(Uri.parse(path), "video/*");
        }

        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        if (useNewZdiooPlayer)
        {
            newIntent.setPackage("com.zidoo.player");
            newIntent.setClassName("com.zidoo.player", "com.zidoo.player.activity.PlayerActivity");
        }
        else
        {
            newIntent.setPackage("com.android.gallery3d");
            newIntent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.MovieActivity");
        }

        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("showTitle", true))
        {
            newIntent.putExtra("title", videoTitle);
        }
        else
        {
            newIntent.putExtra("title", "");
        }

        if(viewOffset > 0)
        {
            newIntent.putExtra("from_start", false);
            newIntent.putExtra("position", viewOffset);
        }
        else
        {
            newIntent.putExtra("from_start", true);
        }

        if(audioSelected)
        {
            newIntent.putExtra("audio_idx", selectedAudioIndex);
        }

        if(subtitleSelected)
        {
            newIntent.putExtra("subtitle_idx", selectedSubtitleIndex);
        }

        newIntent.putExtra("return_result", true);
    }


    @Override
    protected void onStop()
    {
        super.onStop();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        boolean issuePlexIntent = true;
        if(resultCode == Activity.RESULT_OK && requestCode == 98)
        {
            int position = data.getIntExtra("position", 0);
            if(position > 0 && !address.isEmpty() && !ratingKey.isEmpty() && !token.isEmpty() && !zdmc)
            {
                RequestQueue queue = Volley.newRequestQueue(this);
                String url;
                if(duration > 0 && position > (duration * .9))
                {
                    // Mark it as watched
                    url = address + "/:/scrobble?key=" + ratingKey + "&identifier=com.plexapp.plugins.library&" + tokenParameter + token;

                    if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("auto_play", false))
                    {
                        // Go search for the next file to play
                        if(videoIndex > 0 && parentRatingKey != null)
                        {
                            searchFiles();
                        }
                    }
                }
                else
                {
                    // Update progress
                    url = address + "/:/progress?key=" + ratingKey + "&identifier=com.plexapp.plugins.library&time=" + position + "&state=stopped&" + tokenParameter + token;

                    // Can't update the progress on remote streams so just return .. get "Unauthorized" for some reason
                    if(remoteStream)
                    {
                        return;
                    }
                }

                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response ->
                        {
                            Intent plex = new Intent(Intent.ACTION_VIEW);
                            plex.setClassName("com.plexapp.android", "com.plexapp.plex.activities.SplashActivity");
                            if(!server.isEmpty())
                            {
                                // This will try and automatically refresh the plex client with the progress/watched status we just updated on the plex server
                                plex.setData(Uri.parse("plex://server://" + server + "/com.plexapp.plugins.library/library/metadata/" + ratingKey));
                            }

                            startActivity(plex);
                        },
                        error -> Toast.makeText(getApplicationContext(), "Couldn't update progress or watched status", Toast.LENGTH_LONG).show()
                );

                issuePlexIntent = false;

                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        }

        if(issuePlexIntent && !zdmc)
        {
            Intent plex = new Intent(Intent.ACTION_VIEW);
            plex.setClassName("com.plexapp.android", "com.plexapp.plex.activities.SplashActivity");
            startActivity(plex);
        }
    }

    public static String intentToString(Intent intent)
    {
        if (intent == null)
        {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder("action: ")
                .append(intent.getAction())
                .append(" type: ")
                .append(intent.getType())
                .append(" data: ")
                .append(intent.getDataString())
                ;
        if(intent.getExtras() != null)
        {
            stringBuilder.append(" extras: ");
            for (String key : intent.getExtras().keySet())
            {
                stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");
            }
        }

        return stringBuilder.toString();
    }
}