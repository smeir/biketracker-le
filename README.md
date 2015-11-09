This app tracks your daily bike ride distance with a low power Bluetooth device.

You will need a CSC Bluetooth LE (low energy) device mounted on your bike. After installation, the app will automatically scan for a CSC device and, if found, record your daily distances from then on.

Tested with “Wahoo Blue SC” but it should work with other devices too. Only available on Android >= 5.x because it use the new low power Bluetooth LE scanning mode.

#download

[Playstore](https://play.google.com/store/apps/details?id=com.tapnic.biketrackerle)

#build

```
./gradlew installDebug
```

#battery usage

http://developer.radiusnetworks.com/2014/10/28/android-5.0-scanning.html

#icon,logo

bike white, 15 % padding
https://jgilfelt.github.io/AndroidAssetStudio/index.html
notification icon
don't trim, 0 padding
https://shreyasachar.github.io/AndroidAssetStudio/
https://romannurik.github.io/AndroidAssetStudio/

#feature graphic

color: #448aff
url: http://www.norio.be/android-feature-graphic-generator/

#specification

https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_speed_and_cadence.xml

#tutorial,credits

http://devblog.blackberry.com/2013/05/nfc-cadence-cascades/
based on the Bluetooth LE Gatt example, Copyright (C) 2013 The Android Open Source Project
csc characteristic calculation idea from https://github.com/deadfalkon/android-simple-bike-computer

#todo

https://github.com/quiqueqs/BabushkaText
https://github.com/krschultz/android-proguard-snippets

