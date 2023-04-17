package com.hpn789.plextozidoo;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlexXmlParser
{
    private final List<PlexLibraryInfo> infos = new ArrayList<>();

    private final String include_libraries;
    private final String exclude_libraries;

    public PlexXmlParser(String include_string, String exclude_string)
    {
        include_libraries = include_string;
        exclude_libraries = exclude_string;
    }

    public List<PlexLibraryInfo> parse(InputStream in) throws XmlPullParserException, IOException
    {
        try
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            infos.clear();
            readXML(parser);
            return infos;
        }
        finally
        {
            in.close();
        }
    }

    private void readXML(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, null, "MediaContainer");
        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            if (name.equals("Directory"))
            {
                readDirectory(parser);
            }
            else
            {
                skip(parser);
            }
        }
    }

    private void readDirectory(XmlPullParser parser)
    {
        String titleValue = parser.getAttributeValue(null, "title");
        // Check the include list first
        if(include_libraries.isEmpty() || include_libraries.equals("*") || Arrays.asList(include_libraries.split("\\s*,\\s*")).contains(titleValue))
        {
            // Now check the exclude list
            if(exclude_libraries.isEmpty() || !Arrays.asList(exclude_libraries.split("\\s*,\\s*")).contains(titleValue))
            {
                String key = parser.getAttributeValue(null, "key");
                String type = parser.getAttributeValue(null, "type");

                for(PlexMediaType mt : PlexMediaType.values())
                {
                    if(mt.name.equals(type))
                    {
                        infos.add(new PlexLibraryInfo(key, mt));
                    }
                }
            }
        }
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
