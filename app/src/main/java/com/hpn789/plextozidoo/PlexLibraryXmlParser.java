package com.hpn789.plextozidoo;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class PlexLibraryXmlParser
{
    private static final String ns = null;
    private String path = "";
    private String ratingKey = "";
    private String videoTitle = "";
    private int duration = 0;
    private int audioIndex = 0;
    private boolean audioSelected = false;
    private int selectedAudioIndex = 0;
    private int subtitleIndex = 0;
    private boolean subtitleSelected = false;
    private int selectedSubtitleIndex = 0;
    private int videoIndex = 0;
    private String parentRatingKey = "";

    private final String libraryKey;

    public PlexLibraryXmlParser(String aKey)
    {
        libraryKey = aKey;
    }

    public String parse(InputStream in) throws XmlPullParserException, IOException
    {
        try
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readXML(parser);
            return path;
        }
        finally
        {
            in.close();
        }
    }

    public String getRatingKey()
    {
        return ratingKey;
    }

    public String getVideoTitle()
    {
        return videoTitle;
    }

    public int getDuration()
    {
        return duration;
    }

    public boolean isAudioSelected()
    {
        return audioSelected;
    }

    public int getSelectedAudioIndex()
    {
        return selectedAudioIndex;
    }

    public boolean isSubtitleSelected()
    {
        return subtitleSelected;
    }

    public int getSelectedSubtitleIndex()
    {
        return selectedSubtitleIndex;
    }

    public int getVideoIndex()
    {
        return videoIndex;
    }

    public String getParentRatingKey()
    {
        return parentRatingKey;
    }

    private void readXML(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");

        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty())
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            if(name.equals("Video"))
            {
                readVideo(parser);
            }
            else
            {
                skip(parser);
            }
        }
    }

    private void readVideo(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "Video");

        ratingKey = parser.getAttributeValue(null, "ratingKey");
        videoTitle = parser.getAttributeValue(null, "title");
        parentRatingKey = parser.getAttributeValue(null, "parentRatingKey");

        String durationText = parser.getAttributeValue(null, "duration");
        try
        {
            duration = Integer.parseInt(durationText);
        }
        catch(NumberFormatException e)
        {
            duration = 0;
        }

        String videoIndexText = parser.getAttributeValue(null, "index");
        try
        {
            videoIndex = Integer.parseInt(videoIndexText);
        }
        catch(NumberFormatException e)
        {
            videoIndex = 0;
        }

        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty())
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            if (name.equals("Media"))
            {
                readMedia(parser);
            }
            else
            {
                skip(parser);
            }
        }
    }

    private void readMedia(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "Media");

        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty())
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            if (name.equals("Part"))
            {
                readPart(parser);
            }
            else
            {
                skip(parser);
            }
        }
    }

    // Processes link tags in the feed.
    private void readPart(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, ns, "Part");

        String keyAttribute = parser.getAttributeValue(null, "key");
        if(libraryKey != null)
        {
            if(keyAttribute.equals(libraryKey))
            {
                path = parser.getAttributeValue(null, "file");

                while (parser.next() != XmlPullParser.END_TAG && (selectedAudioIndex == 0 || selectedSubtitleIndex == 0))
                {
                    if (parser.getEventType() != XmlPullParser.START_TAG)
                    {
                        continue;
                    }

                    String name = parser.getName();
                    if (name.equals("Stream"))
                    {
                        readStream(parser);
                    }
                    else
                    {
                        skip(parser);
                    }
                }
            }
        }
        // If they didn't pass a libraryKey then pass that back
        else
        {
            path = keyAttribute;
        }
    }

    private void readStream(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, ns, "Stream");

        String streamType = parser.getAttributeValue(null, "streamType");
        String selected = parser.getAttributeValue(null, "selected");

        // Audio stream
        if(streamType.equals("2"))
        {
            if(selected != null && selected.equals("1"))
            {
                audioSelected = true;
                selectedAudioIndex = audioIndex;
            }
            // Audio streams start at 0 so increment after checking if it's selected
            audioIndex++;
        }
        // Subtitle stream
        else if(streamType.equals("3"))
        {
            // Subtitle streams start at 1 so increment before checking if it's selected
            subtitleIndex++;
            if(selected != null && selected.equals("1"))
            {
                subtitleSelected = true;
                selectedSubtitleIndex = subtitleIndex;
            }
        }

        // move past this tag
        skip(parser);
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        if (parser.getEventType() != XmlPullParser.START_TAG)
        {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
