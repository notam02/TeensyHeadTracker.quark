TeensyHeadTracker{
  classvar synth;
  classvar bypassVal;
  classvar <numChans, <order;
  classvar <iemBinDecRefRadius = 3.25;
  classvar <enabled=false;
  classvar <sdName;
  classvar <vstController;
  classvar <rollMidi, <pitchMidi, <yawMidi;
  classvar <decoderType;
  classvar <binauralDecoder;

  *new { |order=3, type=\atk|
    ^this.init(order);
  }

  *init { |ambisonicsOrder, type|
    order = ambisonicsOrder;
    decoderType = type;

    if(type != \iem or: { type != \atk }, { "type is neither \atk or \iem. It is: %. Please choose one of the former".format(type).error });

    numChans = HoaOrder.new(order).size;

    if(Server.local.hasBooted, {
      "%: Server is not booted. Won't do anything until it has booted".format(this.name).warn
    });

    Server.local.doWhenBooted{
      this.addSynthDef();
      this.connectController();

      if(type == \atk, {
        binauralDecoder = FoaDecoderKernel.newCIPIC;
      });

      s.sync;

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

      synth = Synth.after(1, sdName, [\bus, 0, \bypass, bypassVal]);

      /*

      Open plugins

      */

            if(type == \iem, {
              vstController = VSTPluginController.collect(synth);
              Server.local.sync;
              vstController.sceneRot.open("SceneRotator");
              Server.local.sync;
              vstController.binauralDec.open("BinauralDecoder");
            });

            /*

            Map head tracker to scene rotator

            */

            yawMidi = yawMidi ?? {CC14.new(cc1: 16,  cc2: 48,  chan: 0,  fix: true,  normalizeValues: true)};
            pitchMidi = pitchMidi ?? {CC14.new(cc1: 17,  cc2: 49,  chan: 0,  fix: true,  normalizeValues: true)};
            rollMidi = rollMidi ?? { CC14.new(cc1: 18,  cc2: 50,  chan: 0,  fix: true,  normalizeValues: true)};

            if(type=\iem, {
              yawMidi.func_({|val|
                if(vstController.sceneRot.loaded, {
                  vstController.sceneRot.set('Yaw Angle', val);
                });
              });

              pitchMidi.func_({|val|
                if(vstController.sceneRot.loaded,{
                  vstController.sceneRot.set('Pitch Angle', val);
                });
              });

              rollMidi.func_({|val|
                if(vstController.sceneRot.loaded,{
                  vstController.sceneRot.set('Roll Angle', val)
                });
              });

            }, {
              // If not IEM = Assume ATK
              yawMidi.func_({|val|
                synth.set(\yaw, val.linlin(0.0,1.0,(-pi),pi))
              });

              pitchMidi.func_({|val|
                synth.set(\pitch, val.linlin(0.0,1.0,(-pi),pi))
              });

              rollMidi.func_({|val|
                synth.set(\roll, val.linlin(0.0,1.0,(-pi),pi))
              });

            });
    }

  }

  }

  *addSynthDef{
    sdName="headtracker%".format(order).asSymbol;
    if(type == \iem, {
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

    }, {

      // ATK based
      SynthDef.new(sdName, {|out=0, yaw=0, pitch=0, roll=0|

        // HOA input
        var hoa = In.ar(out, numChans);

        // format exchange: HOA >> FOA
        var lowCutFreq = 30.0;  // highpass freq

        // design encoder to exchange (ordering, normalisation)
        var encoder = FoaEncoderMatrix.newHoa1;

        var foa;

        // Rotate scene
        hoa = HoaYPR.ar(in: hoa,  yaw: yaw,  pitch: pitch,  roll: roll,  order: ~order);

        // exchange (ordering, normalisation)
        // truncate to HOA1
        foa = FoaEncode.ar(hoa.keep(AtkFoa.defaultOrder.asHoaOrder.size), encoder);

        // pre-condition FOA to make it work with FoaProximity
        foa = HPF.ar(foa, lowCutFreq);

        // Exchange reference radius
        foa = FoaProximity.ar(foa, AtkHoa.refRadius);

        // Decode to binaural
        foa = FoaDecode.ar(in: foa,  decoder: binauralDecoder);

        ReplaceOut.ar(bus: out,  channelsArray: foa);
      }).add;

    })
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
