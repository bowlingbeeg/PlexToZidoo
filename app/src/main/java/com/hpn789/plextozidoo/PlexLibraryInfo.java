package com.hpn789.plextozidoo;

public class PlexLibraryInfo
{
    private final String key;
    private final PlexMediaType type;

    public PlexLibraryInfo(String aKey, PlexMediaType aType)
    {
        key = aKey;
        type = aType;
    }

    public PlexMediaType getType()
    {
        return type;
    }

    public String getKey()
    {
        return key;
    }

}
