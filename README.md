<!--
SPDX-FileCopyrightText: The LineageOS Project
SPDX-License-Identifier: Apache-2.0
-->

Updater
=======
Simple application to download and apply OTA packages.


Server requirements
-------------------
The app sends `GET` requests to the URL defined by the `updater_server_url`
resource. The `{device}` placeholder is replaced with `ro.clover.device` as is,
with the same upper and lower case letters (for example `Pong`). The `{flavour}`
and `{type}` placeholders are replaced with the lowercase values of
`ro.clover.flavour` and `ro.clover.releasetype`. The changelog URL also uses
`ro.clover.device` as is. If `ro.clover.flavour` is not set, the flavour is the
`-GMS` or `-VANILLA` suffix of `ro.clover.version`. A `404` reply means that
no update is available.

The app expects as response a JSON with the following structure:
```json
{
  "response": [
    {
      "datetime": 1791650879,
      "filename": "CloverProject-4.0-20261010-OFFICIAL-peridot-GMS.zip",
      "id": "12b99c5f880d5863ceeda784874192f4266af23a2b633225e1b742961b1ded78",
      "os_patch_level": "2026-10-01",
      "os_sdk_level": 36,
      "ota_property_files": "payload_metadata.bin:4662:187245,payload.bin:4662:1926274191,payload_properties.txt:1926278911:156,metadata:69:683,metadata.pb:820:1352",
      "romtype": "official",
      "size": 3777677544,
      "url": "https://ota.thecloverproject.com/peridot/4.0/CloverProject-4.0-20261010-OFFICIAL-peridot-GMS.zip",
      "version": "4.0"
    }
  ]
}
```

The `datetime` attribute is the build date expressed as UNIX timestamp.  
The `filename` attribute is the name of the file to be downloaded.  
The `id` attribute is the sha256 of the OTA update. It also identifies the update.  
The `os_patch_level` optional attribute is the security patch level of the OTA update.  
The `os_sdk_level` optional attribute is the SDK level of the OTA update. If it is missing, the SDK level of the device is used.  
The `ota_property_files` optional attribute is the `ota-property-files` value from `META-INF/com/android/metadata` of the OTA update. It is necessary for streaming updates.  
The `romtype` attribute is the string to be compared with the `ro.clover.releasetype` property.  
The `size` attribute is the size of the update expressed in bytes.  
The `url` attribute is the URL of the file to be downloaded.  
The `version` attribute is the string to be compared with the `ro.clover.build.version` property. The format is `MAJOR.MINOR`.  

Additional attributes are ignored.

Downloads use 3 parallel connections when the server supports `Range`
requests and the file is 16 MB or larger. Otherwise the app uses one
connection.

Clean flash rules
-----------------
Before each check, the app also sends a `GET` request to the URL defined by the
`updater_compatibility_url` resource and expects a JSON with the following
structure:
```json
{
  "ver_clean_required": ["4.0"],
  "ver_dirty_allowed": ["4.1"],
  "devices": {
    "peridot": {
      "ver_clean_required": ["4.6"]
    }
  }
}
```

The `ver_clean_required` attribute lists the versions that need a clean flash.  
The `ver_dirty_allowed` attribute lists the versions that can be installed over the previous version.  
The `devices.<device>.ver_clean_required` attribute adds versions that need a clean flash on that device only. The app compares `<device>` with `ro.clover.device` and ignores upper and lower case.  

The app blocks an update when a version `v` in the global or device list
satisfies `current < v <= target`. Versions that are in no list can be
installed over the previous version. The order of versions is
`4.6 < 4.7 < 5.0`. The app keeps the last response and
uses it when the server cannot be reached.


Build with Android Studio
-------------------------
Updater needs access to the system API, therefore it can't be built only using
the public SDK. You first need to generate the libraries with all the needed
classes. The application also needs elevated privileges, so you need to sign
it with the right key to update the one in the system partition. To do this:

 - Place this directory anywhere in the Android source tree
 - Generate a keystore and keystore.properties using `gen-keystore.sh`
 - Build the platform artifacts that provide the non-public classes used by
   the app. At minimum, the jars copied by `pull-system-libs.sh` must exist
   under `out/soong/.intermediates/`
 - Run `./pull-system-libs.sh` from this directory. By default it reads from
   `../../../out` relative to this repository path and will populate
   `system_libs/` with the jars Gradle expects:
   - `framework.jar`
   - `SettingsLib.jar`
   - `SpaLib.jar`
 - If your build output lives somewhere else, pass it explicitly:
   `./pull-system-libs.sh /path/to/out`

You need to do the above once, unless Android Studio can't find some symbol.
In that case, rebuild the relevant platform targets and rerun
`./pull-system-libs.sh`.
