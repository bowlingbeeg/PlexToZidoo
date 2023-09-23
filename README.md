# PlexToZidoo
An external player to make link between Plex app or ZidooPlexMod and the native Zidoo player

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
  - Import/Export of PlexToZidoo settings

# Requirements:
  - Zidoo needs direct access to Plex media through SMB.
  - Zidoo needs to have firmware version 6.4.42+ installed.  
    - Releases can be found here: https://www.zidoo.tv/Support/downloads.html
  - Zidoo must have the Play mode set to "Single file" or watched status and resume points may not update properly
    - Quick settings->Playback->Play mode then select "Single file"
  
# Installation
  - Install this app from the release page.  
    - Releases can be found here: https://github.com/bowlingbeeg/PlexToZidoo/releases
  - Choose either the native Plex app(only works on Z9X generation) or the ZidooPlexMod Kodi app
    - Install the Plex app but do NOT install the latest version on Google play.  Please download and install version 9.13.0.37280 or older for the setup.
      - Stay in mobile layout if it asks you to switch to TV layout until you get through the setup and then you can switch to TV mode 
      - Newer versions of Plex don't allow you to switch to TV mode because of a Plex bug.  After setup is complete and you've switched to TV mode then you can update to later versions of Plex and the TV mode option will stick.
    - Install the ZidooPlexMod Kodi app from https://github.com/bowlingbeeg/plex-for-kodi
      - Requires Kodi/ZDMC 20+

# Setup
  - Go into PlexToZidoo settings and fill out the following required settings
    - Part of the path to replace
      - ex: /media
    - Replace with
      - ex: smb://192.168.x.x/media
    - SMB username (could be optional depending on your permissions)
    - SMB password (could be optional depending on your permissions)
  - If you installed the ZidooPlexMod you can skip this step
    - Open the native Plex app and make sure it's set to mobile layout under settings->Experience->Application layout
      - If you don't see that option you may already be in TV layout.  You'll need to reinstall Plex and stay in mobile layout if it asks you to switch
      - You might need to use the mouse mode on the remote to navigate while in mobile layout.
      - Go to settings->Advanced->Player and select "Use external player" and then click on "Yes".
        - You can ignore the warning about not keeping track of the watched status and resume points.  PlexToZidoo will do that for you.
      - Only once you've selected "Use external player" can you switch to TV layout.
      - Go to settings->Experience->Application layout and select TV and then click on "Yes".
        - On newer versions of Plex it won't switch to TV mode.  If this happens please uninstall Plex first and then download and install version 9.13.0.37280 or older and start the setup phase over
      - Select something in Plex to play and it should pop up a window asking what app to use to open the file.  Select "Open with PlexToZidoo" and "Always".
        - If you don't get a window that pops up then you might have already selected a default app to open video files.  You'll need to go into the android settings for that app and clear it's defaults before you can select PlexToZidoo as the default
        - Quick settings->Other->About->Advanced Settings->Apps & Notifications and then click the app that was opened by default.  Once in that menu select Advanced->Open by default->clear defaults.  Now you can go back and select PlexToZidoo as the default
  - Try out some movies/shows and make sure that there are no error messages in the PlexToZiddo debug screen(the PlexToZidoo screen with all of the media details on it).  Also make sure the path substitution value looks right and that the movies/shows play.
  - Once you have everything working you should disable the debug screen in the PlexToZidoo settings. By disabling the debug screen your movie/show will play directly without having to hit the play button on the PlexToZidoo debug screen.
      
# Misc stuff
  - The Plex app will get killed in backgroud every time you start a movie becuase the Zidoo only allows 2 apps to run in the background at a time. Since PlexToZidoo and the Zidoo player are playing the Plex app will get killed.  This shouldn't normally be an issue because PlexToZidoo will try and open Plex back up to where you were but if that isn't working you can try and change this limit in the developer options
    - To enable developer options follow this guide(you don't need to enable usb debugging like the guide says): https://www.mcbluna.net/wp/guide-how-to-enable-developer-options-on-rtd1619dr-based-zidoo-player/
    - Then go to Quick settings->Other->About->Advanced Settings->System->Advacned->Developer options->Background process limit and set it to "at most 3 processes"
    - Unfortunately this settings gets reset to "at most 2 processes" on a reboot so you'll need to change it after every reboot.
