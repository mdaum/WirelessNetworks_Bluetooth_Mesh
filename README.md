# WirelessNetworks Class Project: Bluetooth Mesh with Range Extension
using http://underdark.io as a base

Peer-to-peer networking library for Android, with Wi-Fi and Bluetooth support.
Our extension of this libary only focuses on Bluetooth, and are consolidated to the application itself (udapp) rather than the libary code due to time constraints.

## Installation (From Underdark)
First, add underdark repository in your root or app's build.gradle:
```
repositories {
    maven {
        url 'https://dl.bintray.com/underdark/android/'
    }
}
```
Next, add Underdark library dependency in your apps' build.gradle:
```
dependencies {
    compile 'io.underdark:underdark:1.+'
}
```
