TeensyHeadTracker : BinauralDecoderCIPIC{
  classvar <rollMidi, <pitchMidi, <yawMidi;

  *new { |hoaOrder=3, id=21|
    order = hoaOrder;
    subjectID = id;
    ^this.init();
  }

  *afterSynthInit{
    this.connectController();
    this.mapController();
  }

  *mapController{
    yawMidi = yawMidi ?? {CC14.new(cc1: 16,  cc2: 48,  chan: 0,  fix: true,  normalizeValues: true)};
    pitchMidi = pitchMidi ?? {CC14.new(cc1: 17,  cc2: 49,  chan: 0,  fix: true,  normalizeValues: true)};
    rollMidi = rollMidi ?? { CC14.new(cc1: 18,  cc2: 50,  chan: 0,  fix: true,  normalizeValues: true)};

    // The inner functions are wrapped in try blocks because they will execute a couple of times before the synth is initialized properly
    yawMidi.func_({|val|
      try{
        synth.set(\yaw, val.linlin(0.0,1.0,(-pi),pi))
      }
    });

    pitchMidi.func_({|val|
      try{
        synth.set(\pitch, val.linlin(0.0,1.0,(-pi),pi))
      }
    });

    rollMidi.func_({|val|
      try{
        synth.set(\roll, val.linlin(0.0,1.0,(-pi),pi))
      }
    });

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
