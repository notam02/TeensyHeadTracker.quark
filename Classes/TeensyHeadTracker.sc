TeensyHeadTracker{
  classvar synth;
  classvar bypassVal;
  classvar <numChans, <order;
  classvar <iemBinDecRefRadius = 3.25;
  classvar <enabled=false;
  classvar <sdName;
  classvar <vstController;
  classvar <rollMidi, <pitchMidi, <yawMidi;

  *new { |order=3|
    ^this.init(order);
  }

  *init { |ambisonicsOrder|
    order = ambisonicsOrder;

    numChans = ((order+1)**2).asInteger;

    if(Server.local.hasBooted, {
      "%: Server is not booted. Won't do anything until it has booted".format(this.name).warn
    });

    Server.local.doWhenBooted{
      this.addSynthDef();
      this.connectController();

      if(enabled.not,{
        // This will respawn the synth on hardstop/cmd-period. Inspired by SafetyNet
        ServerTree.add(this.treeFunc, Server.local);
        this.treeFunc.value;
        enabled = true;
      }, { "Headtracker already setup and enabled!".warn});

    }
  }

  *bypassVSTPlugins{|value|
    bypassVal=value;
    synth.set(\bypass, value);
  }

  *treeFunc{
  ^{
    "Adding % order ambisonics headtracker to main output".format(order).postln;

    forkIfNeeded{

      /*

      Open plugins

      */
            synth = Synth.after(1, sdName, [\bus, 0, \bypass, bypassVal]);
            vstController = VSTPluginController.collect(synth);
            Server.local.sync;

            vstController.sceneRot.open("SceneRotator");

            Server.local.sync;
            vstController.binauralDec.open("BinauralDecoder");

            /*

            Map head tracker to scene rotator

            */

            yawMidi = yawMidi ?? {CC14.new(cc1: 16,  cc2: 48,  chan: 0,  fix: true,  normalizeValues: true)};
            yawMidi.func_({|val|
              if(vstController.sceneRot.loaded, {
                vstController.sceneRot.set('Yaw Angle', val);
              });
            });

            pitchMidi = pitchMidi ?? {CC14.new(cc1: 17,  cc2: 49,  chan: 0,  fix: true,  normalizeValues: true)};
            pitchMidi.func_({|val|
              if(vstController.sceneRot.loaded,{
                vstController.sceneRot.set('Pitch Angle', val);
              });
            });

            rollMidi = rollMidi ?? { CC14.new(cc1: 18,  cc2: 50,  chan: 0,  fix: true,  normalizeValues: true)};
            rollMidi.func_({|val|
              if(vstController.sceneRot.loaded,{
                vstController.sceneRot.set('Roll Angle', val)
              });
            });
    }

  }

  }

  *addSynthDef{
    sdName="headtracker%".format(order).asSymbol;

    SynthDef(sdName, { |bus, bypass=0|

        // HOA input
        var sig = In.ar(bus, numChans);

        // Format exchange from ATK's HOA-format to what IEM expects (ambix) with the binauralDecoder's expected radius.
        // (for source, see https://github.com/ambisonictoolkit/atk-sc3/issues/95)
        // exchange reference radius
        sig = HoaNFCtrl.ar(
          in: sig,
          encRadius: AtkHoa.refRadius,
          decRadius: iemBinDecRefRadius,
          order: order
        );

      // exchange normalisation
      sig = HoaDecodeMatrix.ar(
        in: sig,
        hoaMatrix: HoaMatrixDecoder.newFormat(\ambix, order)
      );

        /*
        the resulting signal can be
        fed directly to the IEM BinauralDecoder plugin
        and is encoded as:

        Ambisonic order: order
        Ambisonic component ordering: ACN
        Ambisonic component normalisation: SN3D
        Ambisonic reference radius: 3.25
        */

       // This will be the SceneRotator
       sig = VSTPlugin.ar(sig, numChans, id: \sceneRot, bypass: bypass);

       // This will be the BinauralDecoder
       sig = VSTPlugin.ar(sig, numChans, id: \binauralDec, bypass: bypass);

       ReplaceOut.ar(bus, sig);
    }).add;

  }

  *connectController{
    // Connect midi controller
    if(MIDIClient.initialized.not, {
      "MIDIClient not initialized... initializing now".postln;
      MIDIClient.init;
    });

    MIDIClient.sources.do{|src, srcNum|
      if(src.device == "Teensy Head Tracker", {
        if(try{MIDIIn.isTeensyHeadTrackerConnected}.isNil, {
          if(MIDIClient.sources.any({|e| e.device=="Teensy Head Tracker"}), {
            "Connecting Teensy Head Tracker".postln;
            MIDIIn.connect(srcNum, src).addUniqueMethod(\isTeensyHeadTrackerConnected, {true});
          });
        }, {"Teensy Head Tracker is already connected... (device is busy)".postln});
      });
    };
  }

}
