# Fluid Glance

A standalone Wear OS tile for time, date, watch battery, and current weather.

## Install

Use Android Platform Tools to connect to your watch through wireless debugging,
then run `adb install -r Fluid-Glance.apk`. Swipe to the tile carousel on the watch,
press and hold a tile, tap Add tile, and choose Fluid Glance. Tap Customize on the
tile or open Fluid Glance from the watch's app list.

## Customize

Choose Clock first, Weather first, or Compact. Pick a text size and accent and
toggle each reading individually. Set the time mode, date format, battery
format, and temperature unit. Compact uses a shorter reading when weather and
battery share a row.

Enter and save a city to enable weather. The city name is sent to Open-Meteo
for lookup, and the matching coordinates are used to fetch conditions. No
location permission is required. A watch internet connection is required for
new readings. A star marks weather older than two hours.

Weather data by [Open-Meteo](https://open-meteo.com/).

On renderers that support dynamic time, the clock updates in the tile. Older
renderers show the time at the last system refresh. Date and battery are also
updated when the tile is refreshed, at the system's discretion.

## Build

Use JDK 17, Android SDK platform 36, build-tools 35.0.0, and Gradle 8.13.
Run `gradle :app:assembleDebug` from this directory. The APK is generated at
`app/build/outputs/apk/debug/app-debug.apk`.

