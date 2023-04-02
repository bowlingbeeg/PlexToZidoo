package com.hpn789.plextozidoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class Play extends AppCompatActivity {

    static final String tokenParameter = "X-Plex-Token=";
    private Intent intent;
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

    private TextView textView;
    private Button playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.finish();
    }

    private void updateDebugPage()
    {
        String pathToPrint = directPath;

        // If the path has a password in it then hide it from the debug output
        if(!password.isEmpty())
        {
            pathToPrint = directPath.replaceAll(":" + password + "@", ":********@");
        }

        String librarySection = "";
        String mediaType = "";
        if(libraryInfo != null)
        {
            librarySection = libraryInfo.getKey();
            mediaType = libraryInfo.getType().name;
        }

        textView.setText(String.format(Locale.ENGLISH, "Intent: %s\n\nPath Substitution: %s\n\nView Offset: %d\n\nDuration: %d\n\nAddress: %s\n\nRating Key: %s\n\nPart Key: %s\n\nPart ID: %s\n\nToken: %s\n\nLibrary Section: %s\n\nMedia Type: %s\n\nSelected Audio Index: %d\n\nSelected Subtitle Index: %d\n\nVideo Index: %d\n\nParent Rating Key: %s\n\nServer: %s", intentToString(intent), pathToPrint, viewOffset, duration, address, ratingKey, partKey, partId, token, librarySection, mediaType, selectedAudioIndex, selectedSubtitleIndex, videoIndex, parentRatingKey, server));
    }

    private void showDebugPageOrSendIntent()
    {
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
                response -> {
                    // Display the first 500 characters of the response string.
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(null);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            String inputString = intent.getDataString();
                            inputString = inputString.replace(partKey, path);
                            intent.setData(Uri.parse(inputString));
                            intent.putExtra("viewOffset", 0);

                            startActivity(intent);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                },
                error -> Toast.makeText(getApplicationContext(), "That didn't work! (5)", Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchMetadata()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/metadata/" + ratingKey + "?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(partKey);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            audioSelected = parser.isAudioSelected();
                            if(audioSelected)
                            {
                                selectedAudioIndex = parser.getSelectedAudioIndex();
                            }

                            subtitleSelected = parser.isSubtitleSelected();
                            if(subtitleSelected)
                            {
                                selectedSubtitleIndex = parser.getSelectedSubtitleIndex();
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    showDebugPageOrSendIntent();
                },
                error -> Toast.makeText(getApplicationContext(), "That didn't work! (3)", Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchPath(List<PlexLibraryInfo> infos, int index)
    {
        PlexLibraryInfo info = infos.get(index);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/sections/" + info.getKey() + "/search?type=" + info.getType().searchId + "&part=" + partId + "&" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
                    PlexLibraryXmlParser parser = new PlexLibraryXmlParser(partKey);
                    InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                    try {
                        String path = parser.parse(targetStream);
                        if(!path.isEmpty())
                        {
                            libraryInfo = info;
                            ratingKey = parser.getRatingKey();
                            videoTitle = parser.getVideoTitle();
                            duration = parser.getDuration();
                            videoIndex = parser.getVideoIndex();
                            parentRatingKey = parser.getParentRatingKey();
                            password = "";
                            directPath = intent.getDataString();

                            // Check if we can actually do the substitution, if not then pass along the original file and see if it plays
                            String[] path_to_replace = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("path_to_replace", "").split(",");
                            String[] replaced_with = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("replaced_with", "").split(",");
                            if(path_to_replace.length > 0 && replaced_with.length > 0 && path_to_replace.length == replaced_with.length)
                            {
                                for (int i = 0; i < path_to_replace.length; i++)
                                {
                                    if (path.contains(path_to_replace[i]))
                                    {
                                        path = path.replaceFirst(Pattern.quote(path_to_replace[i]), replaced_with[i]).replace("\\", "/");

                                        // If this is an SMB request add user name and password to the path
                                        String username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbUsername", "");
                                        if(!username.isEmpty())
                                        {
                                            password = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("smbPassword", "");
                                            path = path.replace("smb://", "smb://" + username + ":" + password + "@");
                                        }

                                        directPath = path;

                                        break;
                                    }
                                }
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
                            directPath = url;
                            updateDebugPage();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },
                error -> Toast.makeText(getApplicationContext(), "That didn't work! (2)", Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void searchLibrary()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/library/sections/?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
                    String[] names = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("libraries", "").split(",");
                    if(names.length > 0 && !names[0].isEmpty())
                    {
                        PlexXmlParser parser = new PlexXmlParser(Arrays.asList(names));
                        InputStream targetStream = new ByteArrayInputStream(response.getBytes());
                        try {
                            List<PlexLibraryInfo> libraries = parser.parse(targetStream);
                            searchPath(libraries, 0);
                            return;

                        } catch (XmlPullParserException | IOException e) {
                            e.printStackTrace();
                        }
                    }

                    updateDebugPage();
                },
                error -> Toast.makeText(getApplicationContext(), "That didn't work! (1)", Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void findServer()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = address + "/identity?" + tokenParameter + token;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Display the first 500 characters of the response string.
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
                    catch (XmlPullParserException | IOException e)
                    {
                        e.printStackTrace();
                    }

                    searchLibrary();
                },
                error -> Toast.makeText(getApplicationContext(), "That didn't work! (6)", Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();

        intent = getIntent();

        String inputString = intent.getDataString();
        Log.d("plex", "" + inputString);
        viewOffset = intent.getIntExtra("viewOffset", 0);
        textView = findViewById(R.id.textView2);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(v -> {
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("useZidooPlayer", true))
            {
                startZidooPlayer(directPath, viewOffset);
            }
            else
            {
                startPlayer(directPath);
            }
        });

        try
        {
            if(!inputString.contains("/library/"))
            {
                throw new Exception("Not a library file, no need to continue");
            }

            int indexOfLibrary = inputString.indexOf("/library/");
            address = inputString.substring(0, indexOfLibrary);
            partKey = inputString.substring(indexOfLibrary, inputString.indexOf("?"));
            String[] partDirs = partKey.split("/");
            if(partDirs.length > 3)
            {
                partId = partDirs[3];
            }
            String tmpToken = inputString.substring(inputString.indexOf(tokenParameter) + tokenParameter.length());
            token = tmpToken.contains("&") ? tmpToken.substring(0, tmpToken.indexOf("&")) : tmpToken;
        }
        catch (Exception e)
        {
            // Doesn't appear to be local content so just pass the intent through to the video player and hope for the best
            directPath = inputString;

            showDebugPageOrSendIntent();

            return;
        }

        findServer();
    }

    protected void startPlayer(String path)
    {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.parse(path), "video/*" );
        startActivity(newIntent);
    }

    protected void startZidooPlayer(String path, int viewOffset)
    {
        // see https://github.com/Andy2244/jellyfin-androidtv-zidoo/blob/Zidoo-Edition/app/src/main/java/org/jellyfin/androidtv/ui/playback/ExternalPlayerActivity.java
        // NOTE: This code requires the new ZIDOO API to work. 6.4.42+
        Intent newIntent = new Intent(Intent.ACTION_VIEW);

        newIntent.setDataAndType(Uri.parse(path), "video/mkv");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        newIntent.setPackage("com.android.gallery3d");
        newIntent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.MovieActivity");

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

        startActivityForResult(newIntent, 98);
    }


    @Override
    protected void onStop() {
        super.onStop();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && requestCode == 98)
        {
            int position = data.getIntExtra("position", 0);
            if(position > 0 && !address.isEmpty() && !ratingKey.isEmpty() && !token.isEmpty())
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
                    if(intent.getDataString().contains("&location=wan&"))
                    {
                        return;
                    }
                }

                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {
                            if(!server.isEmpty())
                            {
                                // This will try and automatically refresh the plex client with the progress/watched status we just updated on the plex server
                                Intent plex = new Intent(Intent.ACTION_VIEW);
                                plex.setClassName("com.plexapp.android", "com.plexapp.plex.activities.SplashActivity");
                                plex.setData(Uri.parse("plex://server://" + server + "/com.plexapp.plugins.library/library/metadata/" + ratingKey));
                                startActivity(plex);
                            }
                        },
                        error -> Toast.makeText(getApplicationContext(), "That didn't work! (4)", Toast.LENGTH_LONG).show()
                );

                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        }
    }

    public static String intentToString(Intent intent)
    {
        if (intent == null)
            return "";

        StringBuilder stringBuilder = new StringBuilder("action: ")
                .append(intent.getAction())
                .append(" data: ")
                .append(intent.getDataString())
                .append(" extras: ")
                ;
        for (String key : intent.getExtras().keySet())
            stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");

        return stringBuilder.toString();

    }
}