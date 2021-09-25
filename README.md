# TeensyHeadTracker.quark

### A convenient interface for the Teensy Head Tracker

This quark is a convenient interface that makes it easy to use the [TeensyHeadTracker](https://github.com/notam02/Teensy-Head-Tracker), a DIY head tracker for 3D audio production.

It will set up a "main fx" synth at the output of SuperCollider which will process any ambisonics you create in SuperCollider through a properly setup BinauralDecoder with the ambisonic scene being rotated by a SceneRotator. When the users presses ctrl/cmd-period to hardstop the sound, the output synth is automatically respawned. This is equivalent to adding a the SceneRotator and BinauralDecoder to the Master bus of a DAW.

For now, it is assumed that you use ATK to create ambisonics in SuperCollider and it will convert ATK's High Order Ambisonics format to ambix which is compatible with the IEM plugins. It also takes into account the reference radius of IEM's BinauralDecoder (thanks Joseph Andersson for digging this up!).

### Dependencies

- [IEM's vstplugin extension for SuperCollider](https://git.iem.at/pd/vstplugin/-/releases)
- [The IEM plugins](https://plugins.iem.at/)
- The `CC14` and `atk-sc3` quarks (these are installed automatically through the quark system when installing this package).

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/notam02/teensyheadtracker.quark")`
