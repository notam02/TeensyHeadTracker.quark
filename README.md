# TeensyHeadTracker.quark

### A convenient interface for the Teensy Head Tracker

This quark is a convenient interface that makes it easy to use the [TeensyHeadTracker](https://github.com/notam02/Teensy-Head-Tracker), a DIY head tracker for 3D audio production. 

It automatically sets up the head tracker, connects SuperCollider to it and patches it into a "global fx" type setup with a binaural decoder at the main output, allowing you to monitor your ambisonics work in SuperCollider and rotate your head inside of the scene using the tracker.

The package supports using native ATK objects to handle all of the rotation and decoding or using the IEM plugins' BinauralDecoder.

See the helpfile for more information.

### Dependencies

- [IEM's vstplugin extension for SuperCollider](https://git.iem.at/pd/vstplugin/-/releases) (optional)
- [The IEM plugins](https://plugins.iem.at/) (optional)
- The `CC14` and `atk-sc3` quarks (these are installed automatically through the quark system when installing this package).

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/notam02/teensyheadtracker.quark")`
