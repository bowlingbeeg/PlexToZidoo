package com.hpn789.plextozidoo;

public enum PlexMediaType
{
    MOVIE("movie", 1),
    EPISODE("show", 4),
    PERSONAL("photo", 14);

    public final String name;
    public final int searchId;

    PlexMediaType(String aName, int aSearchId)
    {
        name = aName;
        searchId = aSearchId;
    }
}
