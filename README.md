# PlexToZidoo
An external player to make link between Plex app and the native Zidoo player

Things this app will do:
  - Looks up video in your Plex library and does path substitution so that the file can be played directly over SMB in the native Zidoo player
  - Updates watched status when 90% of video has been watched
  - Fully supports resume points
    - Starts from resume point stored in Plex server
    - Updates resume point in Plex server when video stops
  - Allows you to play trailers and some direct play videos on remote Plex servers
  - Uses subtitle and audio streams selected in the Plex GUI
  - Has the ability to play the next video automatically in a TV series
    - This only supports playing the next video in the current season.

# Requirements:
  - Zidoo needs direct access to Plex media through SMB.
  - Zidoo needs to have firmware version 6.4.42+ installed.  
    - Releases can be found here: https://www.zidoo.tv/Support/downloads.html
  - Zidoo must have the Play mode set to "Single file" or watched status and resume points may not update properly
    - Quick settings->Playback->Play mode then select "Single file"
  
# Installation
  - Install this app from the release page.  
    - Releases can be found here: https://github.com/bowlingbeeg/PlexToZidoo/releases
  - Install native Plex app from google play store.  Stay in mobile layout if it asks you to switch to TV layout until we get through the setup and then you can switch to TV mode

# Setup
  - Go into PlexToZidoo settings and fill out the following required settings
    - Plex libraries to search
      - ex: Movies,TV Shows
    - Part of the path to replace
      - ex: /media
    - Replace with
      - ex: smb://192.168.x.x/media
    - SMB username (could be optional depending on your permissions)
    - SMB password (could be optional depending on your permissions)
  - Open the native Plex app and make sure it's set to mobile layout under settings->Experience->Application layout
    - If you don't see that option you may already be in TV layout.  You'll need to reinstall Plex and stay in mobile layout if it asks you to switch
    - You might need to use the mouse mode on the remote to navigate while in mobile layout.
    - Go to settings->Advanced->Player and select "Use external player" and then click on "Yes".
      - You can ignore the warning about not keeping track of the watched status and resume points.  PlexToZidoo will do that for you.
    - Only once you've selected "Use external player" can you switch to TV layout.
    - Go to settings->Experience->Application layout and select TV and then click on "Yes".
      - On some versions of Plex it won't switch to TV mode, but you can download a version from Dec '22 and then later update to the newest version on google play and the setting will stick
    - Select something in Plex to play and it should pop up a window asking what app to use to open the file.  Select "Open with PlexToZidoo" and "Always".
      - If you don't get a window that pops up then you might have already selected a default app to open video files.  You'll need to go into the android settings for that app and clear it's defaults before you can select PlexToZidoo as the default
      - Quick settings->Other->About->Advanced Settings->Apps & Notifications and then click the app that was opened by default.  Once in that menu select Advanced->Open by default->clear defaults.  Now you can go back and select PlexToZidoo as the default
      
# Misc stuff
  - The Plex app will get killed in backgroud every time you start a movie becuase the Zidoo only allows 2 apps to run in the background at a time. Since PlexToZidoo and the Zidoo player are playing the Plex app will get killed.  This shouldn't normally be an issue because PlexToZidoo will try and open Plex back up to where you were but if that isn't working you can try and change this limit in the developer options
    - To enable developer options follow this guide(you don't need to enable usb debugging like the guide says): https://www.mcbluna.net/wp/guide-how-to-enable-developer-options-on-rtd1619dr-based-zidoo-player/
    - Then go to Quick settings->Other->About->Advanced Settings->System->Advacned->Developer options->Background process limit and set it to "at most 3 processes"
    - Unfortunately this settings gets reset to "at most 2 processes" on a reboot so you'll need to change it after every reboot.
