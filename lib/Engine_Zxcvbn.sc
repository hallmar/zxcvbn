// Engine_Zxcvbn


// Inherit methods from CroneEngine
Engine_Zxcvbn : CroneEngine {

    // Zxcvbn specific v0.1.0
    var buses;
    var syns;
    var nons; // notes on
    var mods;
    var im;
    var bufs;
    var oscs;
    var mx;
    var ouroboro;
    var dx7syn;
    var bufsDelay;
    // Zxcvbn ^

    *new { arg context, doneCallback;
        ^super.new(context, doneCallback);
    }

    synthWatch {
        arg id,syn;
        id=id.asString;
        if (mods[id].isNil,{
            mods[id]=Array.newClear(10);
            im[id]=1.neg;
        });
        im[id]=(im[id]+1)%10;
        mods[id][im[id]]=syn;
    }

    synthChange {
        arg id,k,v;
        id=id.asString;
        if (mods[id].notNil,{
            10.do({ arg i;
                if (mods[id][i].notNil,{
                    if (mods[id][i].isRunning,{
                        ["synthChange",id,k,v,i].postln;
                        mods[id][i].set(k.asString,v);
                    });
                });
            });
        });
    }

    reverbOn {
        arg on; // on = 0 or 1
        var oldSyn = syns.at("reverb");
        ["reverbOn","reverb"++on.asInteger].postln;
        syns.put("reverb", Synth.new("reverb"++on.asInteger, [
            out: buses.at("busCompressible"),
            outsc: buses.at("busCompressing"),
            outnsc: buses.at("busNotCompressible"),
            compressible: 1,
            compressing: 0,
            in: buses.at("busReverb"), gate: 0], syns.at("main"), \addBefore));
        NodeWatcher.register(syns.at("reverb"));
        if (oldSyn.notNil,{
            oldSyn.free;
        });
    }

    alloc {
        // Zxcvbn specific v0.0.1
        var s=context.server;


        buses = Dictionary.new();
        syns = Dictionary.new();
        bufs = Dictionary.new();
        oscs = Dictionary.new();
        mods = Dictionary.new();
        im = Dictionary.new();
        nons = Dictionary.new();
        bufsDelay = Buffer.allocConsecutive(2,context.server,48000*4,1);


        bufs.put("tape",Buffer.alloc(context.server, context.server.sampleRate * 18.0, 2));
        oscs.put("position",OSCFunc({ |msg| NetAddr("127.0.0.1", 10111).sendMsg("progress",msg[3],msg[3]); }, '/position'));
        oscs.put("audition",OSCFunc({ |msg| NetAddr("127.0.0.1", 10111).sendMsg("audition",msg[3],msg[3]); }, '/audition'));
        oscs.put("loopPosition",OSCFunc({ |msg| NetAddr("127.0.0.1", 10111).sendMsg("loopPosition",msg[3],msg[4]); }, '/loopPosition'));
        oscs.put("amplitude",OSCFunc({ |msg| NetAddr("127.0.0.1", 10111).sendMsg("amplitude",
                msg[3],msg[4],
                msg[5],msg[6],
                msg[7],msg[8],
                msg[9],msg[10],
                msg[11],msg[12],
                msg[13],msg[14],
                msg[15],msg[16],
                msg[17],msg[18],
                msg[19],msg[20],
                msg[21],msg[22]
        ); }, '/amplitude'));

        dx7syn = ("/home/we/dust/code/zxcvbn/lib/DX7.scd").load;
        context.server.sync;

        // mixer
        SynthDef("mixer",{
            arg bus1,bus2,bus3,bus4,bus5,
                bus6,bus7,bus8,bus9,bus10;
            var attackTime=0.01;
            var releaseTime=2;
            var movingAverage=5;
            var amp1=Clip.kr(Amplitude.kr(In.ar(bus1,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp2=Clip.kr(Amplitude.kr(In.ar(bus2,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp3=Clip.kr(Amplitude.kr(In.ar(bus3,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp4=Clip.kr(Amplitude.kr(In.ar(bus4,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp5=Clip.kr(Amplitude.kr(In.ar(bus5,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp6=Clip.kr(Amplitude.kr(In.ar(bus6,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp7=Clip.kr(Amplitude.kr(In.ar(bus7,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp8=Clip.kr(Amplitude.kr(In.ar(bus8,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp9=Clip.kr(Amplitude.kr(In.ar(bus9,2),attackTime,releaseTime).ampdb+24,-48,12);
            var amp10=Clip.kr(Amplitude.kr(In.ar(bus10,2),attackTime,releaseTime).ampdb+24,-48,12);
            SendReply.kr(Impulse.kr(10),"/amplitude",[
                amp1[0],amp1[1],
                amp2[0],amp2[1],
                amp3[0],amp3[1],
                amp4[0],amp4[1],
                amp5[0],amp5[1],
                amp6[0],amp6[1],
                amp7[0],amp7[1],
                amp8[0],amp8[1],
                amp9[0],amp9[1],
                amp10[0],amp10[1],
            ]);
        }).add;

        // looper
        2.do({
            arg n;
            var ch=n+1;
            SynthDef("defLoop0"++ch,{
                arg id,bufnum,t_trig,frames;
                var pos=Phasor.ar(end:frames);
                var snd=BufRd.ar(ch,bufnum,pos,1);
                // var pan=SinOsc.kr(1/Rand(5,40),Rand(0,6))/1.5;
                // var pan2=SinOsc.kr(1/Rand(5,40),Rand(0,6))/1.5;
                // var pan3=SinOsc.kr(1/Rand(5,40),Rand(0,6))/1.5;
                // var amp=SinOsc.kr(1/Rand(5,40),Rand(0,6)).range(0.25,0.75);
                snd=snd*EnvGen.ar(Env.new([1,0],[1]),t_trig,doneAction:2);
                snd=Pan2.ar(snd,0);
                snd=snd*EnvGen.ar(Env.new([0,1],[1]),1);

                // snd=[snd[0],snd[1]];
                // snd=[
                //     LPF.ar(snd[0],LinExp.kr((pan2<0)*pan2.abs,0,1,6000,18000)),
                //     LPF.ar(snd[1],LinExp.kr((pan2>0)*pan2.abs,0,1,6000,18000))
                // ];
                // snd[0]=SelectX.ar(((pan>0)*pan.abs),[snd[0],DelayN.ar(snd[0],0.04,0.04)]);
                // snd[1]=SelectX.ar(((pan<0)*pan.abs),[snd[1],DelayN.ar(snd[1],0.04,0.04)]);
                // snd=Balance2.ar(snd[0],snd[1],pan3,amp);

                SendReply.kr(Impulse.kr(10),"/loopPosition",[id,pos/frames]);
                Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
                Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
                Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
                Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
            }).add;
        });
        // "ambisonic" version
        2.do({
            arg n;
            var ch=n+1;
            SynthDef("defLoop1"++ch,{
                arg id,bufnum,t_trig,frames;
                var pos=Phasor.ar(end:frames);
                var snd=BufRd.ar(ch,bufnum,pos,1);
                var pan=SinOsc.kr(1/Rand(5,40),Rand(0,6))*0.8;
                var pan2=SinOsc.kr(1/Rand(5,40),Rand(0,6))*0.8;
                var pan3=SinOsc.kr(1/Rand(5,40),Rand(0,6))*0.8;
                var amp=SinOsc.kr(1/Rand(5,40),Rand(0,6)).range(0.6,1.2);
                snd=snd*EnvGen.ar(Env.new([1,0],[1]),t_trig,doneAction:2);
                snd=Pan2.ar(snd,0);
                snd=snd*EnvGen.ar(Env.new([0,1],[1]),1);

                snd=[snd[0],snd[1]];
                snd=[
                    LPF.ar(snd[0],LinExp.kr((pan2<0)*pan2.abs,0,1,6000,18000)),
                    LPF.ar(snd[1],LinExp.kr((pan2>0)*pan2.abs,0,1,6000,18000))
                ];
                snd[0]=SelectX.ar(((pan>0)*pan.abs),[snd[0],DelayN.ar(snd[0],0.03,0.03)]);
                snd[1]=SelectX.ar(((pan<0)*pan.abs),[snd[1],DelayN.ar(snd[1],0.03,0.03)]);
                snd=Balance2.ar(snd[0],snd[1],pan3,amp);

                SendReply.kr(Impulse.kr(5),"/loopPosition",[id,pos/frames]);
                Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            }).add;
        });

        // <mx.synths>
        SynthDef("synthy",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,note,env,detune,stereo,lowcut,chorus,res;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            note=Lag.kr(hz,0.001).cpsmidi+bend;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            sub=Lag.kr(sub,1);
            snd=Pan2.ar(Pulse.ar((note-12).midicps,LinLin.kr(LFTri.kr(0.5),-1,1,0.2,0.8))*sub);
            stereo=LinLin.kr(mod1,-1,1,0,1);
            lowcut=LinExp.kr(mod2,-1,1,25,11000);
            res=LinExp.kr(mod3,-1,1,0.25,1.75);
            detune=LinExp.kr(mod4,-1,1,0.00001,0.3);
            snd=snd+Mix.ar({
                arg i;
                var snd2;
                snd2=SawDPW.ar((note+(detune*(i*2-1))).midicps);
                snd2=RLPF.ar(snd2,LinExp.kr(SinOsc.kr(rrand(1/30,1/10),rrand(0,2*pi)),-1,1,lowcut,12000),res);
                snd2=DelayC.ar(snd2, rrand(0.01,0.03), LFNoise1.kr(Rand(5,10),0.01,0.02)/15);
                Pan2.ar(snd2,VarLag.kr(LFNoise0.kr(1/3),3,warp:\sine)*stereo)
            }!2);
            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // https://gist.github.com/audionerd/fe50790b7601cba65ddd855caffb05ad
        SynthDef("supersaw",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var detuneFactor,snd,note,mix,freq,env,detune,stereo,lowcut,chorus,res,detuneCurve,centerGain,sideGain,center,freqs,side;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            note=Lag.kr(hz,0.001).cpsmidi+bend;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            sub=Lag.kr(sub,1);
            snd=Pan2.ar(Pulse.ar((note-12).midicps,LinLin.kr(LFTri.kr(0.5),-1,1,0.2,0.8))*sub);
            mix=LinLin.kr(mod1,-1,1,0,1);
            stereo=LinLin.kr(mod1,-1,1,0,1);
            res=LinExp.kr(mod3,-1,1,0.65,1.0);
            detune=LinLin.kr(mod4,-1,1,0,1);
            freq=note.midicps;
            detuneCurve = { |x|
                (10028.7312891634*x.pow(11)) -
                (50818.8652045924*x.pow(10)) +
                (111363.4808729368*x.pow(9)) -
                (138150.6761080548*x.pow(8)) +
                (106649.6679158292*x.pow(7)) -
                (53046.9642751875*x.pow(6)) +
                (17019.9518580080*x.pow(5)) -
                (3425.0836591318*x.pow(4)) +
                (404.2703938388*x.pow(3)) -
                (24.1878824391*x.pow(2)) +
                (0.6717417634*x) +
                0.0030115596
            };
            centerGain = { |x| (-0.55366 * x) + 0.99785 };
            sideGain = { |x| (-0.73764 * x.pow(2)) + (1.2841 * x) + 0.044372 };

            center = Pan2.ar(LFSaw.ar(freq, Rand()));
            // https://scsynth.org/t/simple-fm-synthesis-question/5483/9
            detuneFactor = freq * detuneCurve.(detune);
            freqs = [
                (freq - (detuneFactor * 0.11002313)),
                (freq - (detuneFactor * 0.06288439)),
                (freq - (detuneFactor * 0.01952356)),
                (freq + (detuneFactor * 0.01991221)),
                (freq + (detuneFactor * 0.06216538)),
                (freq + (detuneFactor * 0.10745242))
            ];
            // freqs = [
            //     (freq - (freq*(detuneCurve.(detune))*0.11002313)),
            //     (freq - (freq*(detuneCurve.(detune))*0.06288439)),
            //     (freq - (freq*(detuneCurve.(detune))*0.01952356)),
            //     (freq + (freq*(detuneCurve.(detune))*0.01991221)),
            //     (freq + (freq*(detuneCurve.(detune))*0.06216538)),
            //     (freq + (freq*(detuneCurve.(detune))*0.10745242))
            // ];
            side=Pan2.ar(LFSaw.ar(freqs[0], Rand(0, 2))+LFSaw.ar(freqs[1], Rand(0, 2))+LFSaw.ar(freqs[2], Rand(0, 2)),stereo);
            side=side+Pan2.ar(LFSaw.ar(freqs[3], Rand(0, 2))+LFSaw.ar(freqs[4], Rand(0, 2))+LFSaw.ar(freqs[5], Rand(0, 2)),stereo.neg);

            snd = (center * centerGain.(mix)) + (side * sideGain.(mix));

            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = RLPF.ar(snd,lpf,res) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("bigbass",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,note,freq,oscfreq,env,envFilter,detune,distortion,lowcut,chorus,res;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            note=Lag.kr(hz,0.001).cpsmidi+bend;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            sub=Lag.kr(sub,1);
            distortion=LinLin.kr(mod1,-1,1,1,20);
            lowcut=LinLin.kr(mod2,-1,1,1,16);
            res=LinLin.kr(mod3,-1,1,-4,8);
            detune=LinLin.kr(mod4,-1,1,-0.6,0.62);
            freq=note.midicps/2;

            oscfreq = {freq * LFNoise2.kr(0.5).range(1-detune, 1+detune)}!3;
            snd = Splay.ar(LFSaw.ar(oscfreq));
            envFilter = Env.adsr(attack/4, 4, 0, release).kr(gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))));
            snd = (snd*distortion).tanh;
            snd=BLowShelf.ar(snd,freq,1,res);
            snd = LPF.ar(snd, (envFilter*freq*lowcut) + (2*freq));
            snd = (snd*envFilter).tanh;

            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 2;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("casio",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var freq, env, freqBase, freqRes, pdbase, pd, pdres, pdi, snd,res,detuning,artifacts,phasing;
            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            artifacts=LinLin.kr(mod1,-1,1,1,10);
            phasing=LinExp.kr(mod2,-1,1,0.125,8);
            res=LinExp.kr(mod3,-1,1,0.1,10);
            detuning=LinExp.kr(mod4,-1,1,0.000001,0.02);
            freq=[hz*(1-detuning),hz*(1+detuning)];
            freqBase=freq;
            freqRes=SinOsc.kr(Rand(0.01,0.2),0).range(freqBase/2,freqBase*2)*res;
            pdbase=Impulse.ar(freqBase);
            pd=Phasor.ar(pdbase,2*pi*freqBase/context.server.sampleRate*phasing,0,2pi);
            pdres=Phasor.ar(pdbase,2*pi*freqRes/context.server.sampleRate*phasing,0,2pi);
            pdi=LinLin.ar((2pi-pd).max(0),0,2pi,0,1);
            snd=Lag.ar(SinOsc.ar(0,pdres)*pdi,1/freqBase);
            snd = LPF.ar(snd,Clip.kr(hz*artifacts,20,18000));
            snd = Pan2.ar(snd,Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 5;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("icarus",{
            arg hz=220,amp=1.0,gate=1,sub=1.0,glide=1,bend=0,
            attack=0.1,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=1,lasthz=220;
            var bass,basshz,feedback=0.5,delaytime=0.25, delaytimelag=0.1;
            var ender,snd,local,in,ampcheck,env,detuning=0.1,pwmcenter=0.5,pwmwidth=0.4,pwmfreq=1.5;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            feedback=LinLin.kr(mod1,-1,1,0.1,2);
            delaytime=LinLin.kr(mod2,-1,1,0.05,0.6);
            pwmwidth=LinLin.kr(mod3,-1,1,0.1,0.9);
            detuning=LinExp.kr(mod4,-1,1,0.01,1);

            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            snd=Mix.new({VarSaw.ar(
                hz+(SinOsc.kr(LFNoise0.kr(1),Rand(0,3))*
                    (((hz).cpsmidi+1).midicps-(hz))*detuning),
                width:LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
                mul:0.25
            )}!2);
            snd=snd+Mix.new({VarSaw.ar(
                hz/2+(SinOsc.kr(LFNoise0.kr(1),Rand(0,3))*
                    (((hz/2).cpsmidi+1).midicps-(hz/2))*detuning),
                width:LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
                mul:0.15
            )}!2);

            basshz=hz;
            basshz=Select.kr(basshz>90,[basshz,basshz/2]);
            basshz=Select.kr(basshz>90,[basshz,basshz/2]);
            bass=PulseDPW.ar(basshz,width:SinOsc.kr(1/3).range(0.2,0.4));
            bass=bass+LPF.ar(WhiteNoise.ar(SinOsc.kr(1/rrand(3,4)).range(1,rrand(3,4))),2*basshz);
            bass = Pan2.ar(bass,LFTri.kr(1/6.12).range(-0.2,0.2));
            bass = HPF.ar(bass,20);
            bass = LPF.ar(bass,SinOsc.kr(0.1).range(2,5)*basshz);


            ampcheck = Amplitude.kr(Mix.ar(snd));
            snd = snd * (ampcheck > 0.02); // noise gate
            local = LocalIn.ar(2);
            local = OnePole.ar(local, 0.4);
            local = OnePole.ar(local, -0.08);
            local = Rotate2.ar(local[0], local[1],0.2);
            local = DelayC.ar(local, 0.5,
                Lag.kr(delaytime,0.2)
            );
            local = LeakDC.ar(local);
            local = ((local + snd) * 1.25).softclip;

            LocalOut.ar(local*Lag.kr(feedback,1));


            snd= Balance2.ar(local[0],local[1],pan);
            snd=snd+(SinOsc.kr(0.123,Rand(0,3)).range(0.2,1.0)*bass*sub);

            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);

            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // port of STK's Rhodey (yamaha DX7-style Fender Rhodes) https://sccode.org/1-522
        SynthDef("epiano",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;

            // all of these range from 0 to 1
            var vel = 0.8, modIndex = 0.2, mix = 0.2, lfoSpeed = 0.4, lfoDepth = 0.1;
            var env1, env2, env3, env4;
            var osc1, osc2, osc3, osc4, snd;
            var env;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            lfoDepth=LinExp.kr(mod1,-1,1,0.01,1);
            mix=LinLin.kr(mod2,-1,1,0.0,0.4);
            modIndex=LinExp.kr(mod3,-1,1,0.01,4);
            lfoSpeed=LinLin.kr(mod4,-1,1,0,0.5);

            lfoSpeed = lfoSpeed * 12;

            hz = hz * 2;

            env1 = EnvGen.ar(Env.adsr(0.001, 1.25, 0.5, release, curve: \lin),gate);
            env2 = EnvGen.ar(Env.adsr(0.001, 1.00, 0.5, release, curve: \lin),gate);
            env3 = EnvGen.ar(Env.adsr(0.001, 1.50, 0.5, release, curve: \lin),gate);
            env4 = EnvGen.ar(Env.adsr(0.001, 1.50, 0.5, release, curve: \lin),gate);

            osc4 = SinOsc.ar(hz * 0.5) * 2pi * 2 * 0.535887 * modIndex * env4 * vel;
            osc3 = SinOsc.ar(hz, osc4) * env3 * vel;
            osc2 = SinOsc.ar(hz * 15) * 2pi * 0.05 * env2 * vel;
            osc1 = SinOsc.ar(hz, osc2) * env1 * vel;
            snd = Mix((osc3 * (1 - mix)) + (osc1 * mix));
            snd = snd * (SinOsc.ar(lfoSpeed) * lfoDepth + 1);

            snd = Pan2.ar(snd,Lag.kr(pan,0.1));

            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;


        SynthDef("toshiya",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,note,env,detune,stereo,lowcut,chorus,klanky,klankyvol;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            detune=LinExp.kr(mod1,-1,1,0.001,0.1);
            klankyvol=LinLin.kr(mod2,-1,1,0,2);
            lowcut=LinExp.kr(mod3,-1,1,25,11000);
            chorus=LinExp.kr(mod4,-1,1,0.2,5);

            note=Lag.kr(hz,0.001).cpsmidi + bend;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            sub=Lag.kr(sub,1);
            snd=Pan2.ar(SinOsc.ar((note-12).midicps,LinLin.kr(LFTri.kr(0.5),-1,1,0.2,0.8))/12*amp,SinOsc.kr(0.1,mul:0.2))*sub;
            snd=snd+Mix.ar({
                arg i;
                var snd2;
                snd2=SinOsc.ar((note+(detune*(i*2-1))).midicps);
                snd2=LPF.ar(snd2,LinExp.kr(SinOsc.kr(rrand(1/30,1/10),rrand(0,2*pi)),-1,1,lowcut,12000),2);
                snd2=DelayC.ar(snd2, rrand(0.01,0.03), LFNoise1.kr(Rand(5,10),0.01,0.02)/NRand(10,20,3)*chorus );
                Pan2.ar(snd2,VarLag.kr(LFNoise0.kr(1/3),3,warp:\sine))
            }!2);
            snd=snd+(Amplitude.kr(snd)*VarLag.kr(LFNoise0.kr(1),1,warp:\sine).range(0.1,1.0)*klankyvol*Klank.ar(`[[hz, hz*2+2, hz*4+5, hz*8+2], nil, [1, 1, 1, 1]], PinkNoise.ar([0.007, 0.007])));
            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("malone",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,note,env, basshz,bass, detuning,pw, res,filt,detuningSpeed;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            detuningSpeed=LinExp.kr(mod1,-1,1,0.1,10);
            filt=LinLin.kr(mod2,-1,1,2,10);
            res=LinExp.kr(mod3,-1,1,0.25,4);
            detuning=LinExp.kr(mod4,-1,1,0.002,0.8);
            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            note=hz.cpsmidi;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            snd=Mix.ar(Array.fill(1,{
                arg i;
                var hz_,snd_;
                hz_=((2*hz).cpsmidi+SinOsc.kr(detuningSpeed*Rand(0.1,0.5),Rand(0,pi)).range(detuning.neg,detuning)).midicps;
                snd_=PulseDPW.ar(hz_,0.17);
                snd_=snd_+PulseDPW.ar(hz_/2,0.17);
                snd_=snd_+PulseDPW.ar(hz_*2,0.17);
                snd_=snd_+LFTri.ar(hz_/4);
                snd_=RLPF.ar(snd_,Clip.kr(hz_*filt,hz_*1.5,16000),Clip.kr(LFTri.kr([0.5,0.45]).range(0.3,1)*res,0.2,2));
                Pan2.ar(snd_,VarLag.kr(LFNoise0.kr(1/3),3,warp:\sine))/10
            }));


            basshz=hz;
            basshz=Select.kr(basshz>90,[basshz,basshz/2]);
            basshz=Select.kr(basshz>90,[basshz,basshz/2]);
            bass=PulseDPW.ar(basshz,width:SinOsc.kr(1/3).range(0.2,0.4));
            bass=bass+LPF.ar(WhiteNoise.ar(SinOsc.kr(1/rrand(3,4)).range(1,rrand(3,4))),2*basshz);
            bass = Pan2.ar(bass,LFTri.kr(1/6.12).range(-0.2,0.2));
            bass = HPF.ar(bass,20);
            bass = LPF.ar(bass,SinOsc.kr(0.1).range(2,5)*basshz);
            snd=snd+(SinOsc.kr(0.123).range(0.2,1.0)*bass*sub);

            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // http://sccode.org/1-51n
        SynthDef("kalimba",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=0.8,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=0.5,lasthz=220;
            var snd,env,click,mix;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            env=EnvGen.ar(Env.adsr(attack,0,1.0,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            mix=LinLin.kr(mod4,-1,1,0.01,0.4);

            // Basic tone is a SinOsc
            snd = SinOsc.ar((hz.cpsmidi+mod4).midicps);
            snd = HPF.ar( LPF.ar(snd, 380), 40);
            // The "clicking" sounds are modeled with a bank of resonators excited by enveloped white noise
            click = DynKlank.ar(`[
                // the resonant frequencies are randomized a little to add variation
                // there are two high resonant freqs and one quiet "bass" freq to give it some depth
                [240*ExpRand(0.97, 1.02), 2020*ExpRand(0.97, 1.02), 3151*ExpRand(0.97, 1.02)],
                [-9, 0, -5].dbamp,
                [0.8, 0.07, 0.08]
            ], BPF.ar(PinkNoise.ar, Rand(5500,8500), Rand(0.05,0.2)) * EnvGen.ar(Env.perc(0.001, 0.01)));
            snd = (snd*mix) + (click*(1-mix));
            snd = Splay.ar(snd,center:Rand(-1,1)*LinLin.kr(mod1,-1,1,0,1));

            snd=Vibrato.ar(
                snd,
                rate:LinExp.kr(mod2,-1,1,0.0001,20),
                depth:LinExp.kr(mod3,-1,1,0.0001,1)
            );

            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("mdapiano",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,env,tuning;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz=Lag.kr(hz,0.001);
            env=EnvGen.ar(Env.adsr(attack,0,1.0,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);

            tuning=LinLin.kr(Clip.kr(mod4),0,1,0,1);
            snd=MdaPiano.ar(
                freq:hz,
                gate:gate,
                decay:decay,
                release:release,
                stereo:LinLin.kr(mod1,-1,1,0.3,1),
                vel:Rand(40,80),
                tune:Rand(0.5+tuning.neg,0.5+tuning)
            );
            snd=Vibrato.ar(
                snd,
                rate:LinExp.kr(mod2,-1,1,0.0001,20),
                depth:LinExp.kr(mod3,-1,1,0.0001,1)
            );
            snd = Pan2.ar(snd,Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 6;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // https://github.com/monome/dust/blob/master/lib/sc/Engine_PolyPerc.sc
        SynthDef("polyperc",{
            arg hz=220,amp=1.0,gate=1,sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,filt,env,pw,co,gain,detune,note;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz=Lag.kr(hz,0.001);
            pw=LinLin.kr(mod1,-1,1,0.3,0.7);
            co=LinExp.kr(mod2,-1,1,hz,Clip.kr(10*hz,200,18000));
            gain=LinLin.kr(mod3,-1,1,0.25,3);
            detune=LinExp.kr(mod4,-1,1,0.00001,0.3);
            note=hz.cpsmidi + bend;
            note = Lag.kr(note,glide);
            snd = Pulse.ar([note-detune,note+detune].midicps, pw);
            snd = MoogFF.ar(snd,co,gain);
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            snd = Balance2.ar(snd[0],snd[1],Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 2;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // https://github.com/catfact/zebra/blob/master/lib/Engine_DreadMoon.sc#L20-L41
        SynthDef("dreadpiano",{
            arg hz=220,amp=1.0,pan=0,gate=1,
            sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,duration=600,lasthz=220;
            var snd,note,env, damp;
            var noise, string, delaytime,  noise_env, damp_mul;
            var noise_hz = 4000, noise_attack=0.002, noise_decay=0.06,
            tune_up = 1.0005, tune_down = 0.9996, string_decay=3.0,
            lpf_ratio=2.0, lpf_rq = 4.0, hpf_hz = 40, damp_time=0.1;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            // mods
            string_decay=LinLin.kr(mod1,-1,1,0.1,6);
            noise_hz=LinExp.kr(mod2,-1,1,200,16000);
            lpf_rq=LinLin.kr(mod3,-1,1,0.1,8);
            tune_up=1+LinLin.kr(mod4,-1,1,0.0001,0.0005*4);
            tune_down=1-LinLin.kr(mod4,-1,1,0.00005,0.0004*4);

            hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);

            damp = 0;
            damp_mul = LagUD.ar(K2A.ar(1.0 - damp), 0, damp_time);

            noise_env = Decay2.ar(Impulse.ar(0));
            noise = LFNoise2.ar(noise_hz) * noise_env;

            delaytime = 1.0 / (hz * [tune_up, tune_down]);
            string = Mix.new(CombL.ar(noise, delaytime, delaytime, string_decay * damp_mul));

            snd = RLPF.ar(string, lpf_ratio * hz, lpf_rq);
            snd = HPF.ar(snd, hpf_hz);
            snd = Pan2.ar(snd,Lag.kr(pan,0.1));

            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 5;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("aaaaaa",{
            arg out=0,hz=220,amp=1.0,pan=0,gate=1,
            sub=0,glide=1,bend=0,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,duration=600,lasthz=220;
            var saw, wiggle, snd;
            // frequencies drawn from https://slideplayer.com/slide/15020921/
            var f1a = [290, 420, 580, 720, 690, 550, 400, 280];
            var f2a = [750, 1000, 790, 1100, 1600, 1750, 1900, 2200];
            var f3a = [2300, 2350, 2400, 2500, 2600, 2700, 2800, 3300];
            var f4a = [3500, 3500, 3500, 3500, 3500, 3500, 3500, 3500];
            var f1b = [390, 435, 590, 850, 860, 600, 420, 360];
            var f2b = [900, 1100, 850, 1200, 2200, 2350, 2500, 2750];
            var f3b = [2850, 2900, 3000, 3000, 3100, 3200, 3300, 3800];
            var f4b = [4000, 4000, 4000, 4000, 4000, 4000, 4000, 4000];
            var f1c = [420, 590, 640, 1100, 1000, 700, 575, 375];
            var f2c = [1200, 1300, 1100, 1300, 2500, 2700, 2800, 3200];
            var f3c = [3200, 3250, 3300, 3400, 3500, 3600, 3700, 4200];
            var f4c = [4500, 4500, 4500, 4500, 4500, 4500, 4500, 4500];
            var f1, f2, f3, f4;
            var a1, a2, a3, a4;
            var q1, q2, q3, q4;
            var voice, vowel, tilt, cons, detune, focus, div, reso;
            var env;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            voice=Select.kr( (mod1 > -0.99), [hz.explin(100, 1000, 0, 2), LinLin.kr(mod1, -1, 1, 0, 2)]);
            vowel=LinLin.kr(mod2, -1, 1, 0, 7);
            tilt=LinLin.kr(mod2, -1, 1, 0.3, 0.6) * LinLin.kr(mod4, -1, 1, 0.6, 1.1);
            reso = LinLin.kr(mod4, -1, 1, 0.1, 0.23);
            detune = LinLin.kr(mod3, -1, 1, 0, 0.015);
            focus = -1 * LinLin.kr(mod3, -1, 1, 0, 1);
            div = LinLin.kr(mod3, -1, 1, 1, 7).sqrt;
            cons = mod4.linlin(-1, 1, -0.5, 0.8);

            f1 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f1a, f1b, f1c].flop));
            f2 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f2a, f2b, f2c].flop));
            f3 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f3a, f3b, f3c].flop));
            f4 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f4a, f4b, f4c].flop));
            a1 = 1;
            a2 = tilt;
            a3 = tilt ** 1.5;
            a4 = tilt ** 2;
            q1 = reso;
            q2 = q1/1.5;
            q3 = q2/1.5;
            q4 = reso/10;

                hz=(Lag.kr(hz,0.001).cpsmidi + bend).midicps;
            saw = VarSaw.ar(hz*(1+ (detune * [-1, 0.7, -0.3, 0, 0.3, -0.7, 1])), width: 0).collect({ |item, index|
            Pan2.ar(item, index.linlin(0, 6, -1, 1)*SinOsc.kr(Rand.new(0.1, 0.3))*focus)
            });
            wiggle = EnvGen.kr(Env.perc(attackTime: 0.0, releaseTime: 0.15), doneAction: Done.none);
            saw.postln;
            snd = HPF.ar(
                Mix.new(BBandPass.ar(saw, ([
                f1,
                f2 * (1 + (cons*wiggle)),
                f3,
                f4]!2).flop,
                ([q1, q2, q3, q4]!2).flop) * ([a1, a2, a3, a4]!2).flop),
            20);
            snd.postln;

            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);

            snd = Balance2.ar(snd[0], snd[1], Lag.kr(pan,0.1)).tanh;

            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 5;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        SynthDef("triangles",{
            arg out=0,hz=220,amp=1.0,gate=1,sub=0,glide=1,
            attack=0.01,decay=0.2,sustain=0.9,release=5,
            mod1=0,mod2=0,mod3=0,mod4=0,lpf=18000,pan=0,duration=600,lasthz=220;
            var snd,env,bellow_env,bellow,
            detune_cents,detune_semitones,
            f_cents,freq_a,freq_b,decimation_bits,decimation_rate,
            noise_level,vibrato_rate,vibrato_depth;
            hz=Clip.kr(hz,10,18000);mod1=Lag.kr(mod1);mod2=Lag.kr(mod2);mod3=Lag.kr(mod3);mod4=Lag.kr(mod4);
            //hz=XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz=Lag.kr(hz,0.001);
            env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),(gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            env=env*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);

            bellow=LinLin.kr(mod1,-1,1,0,1);
            decimation_bits = LinLin.kr(mod2, -1, 1, 24, 4);
            decimation_rate = LinLin.kr(mod2, -1, 1, 44100, 8000);
            noise_level = LinLin.kr(mod2, -1, 1, 0, 0.5);
            detune_semitones = LinLin.kr(mod3, -1, 1, -24, 24);
            vibrato_rate = LinLin.kr(mod4, -1, 1, 0, 5);
            vibrato_depth = LinExp.kr(mod4, -1, 1, 0.001, 0.3);

            bellow_env = EnvGen.kr(Env.step([bellow, 1-bellow], [attack, release], 1), gate: gate);
            freq_a = hz;
            freq_b = (hz.cpsmidi + detune_semitones.round).midicps;

            snd = Mix.new([
                SelectX.ar(bellow_env.lag, [
                    DPW3Tri.ar(Vibrato.ar(freq_a, vibrato_rate, vibrato_depth)),
                    DPW3Tri.ar(Vibrato.ar(freq_b, vibrato_rate, vibrato_depth)),
                ]),
                PinkNoise.ar(noise_level),
            ]);
            snd = Decimator.ar(snd, decimation_rate, decimation_bits);

            snd = Pan2.ar(snd,Lag.kr(pan,0.1));
            snd = LPF.ar(snd,Lag.kr(lpf)) * env * amp / 8;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;

        // </mx.synths>

        //passersby/zassersby
        //split the two different envelope modes into two different synthdefs because
        //1. easier to trigger and kill each note via the voice allocation method of zxcvbn
        //2. it saves some cpu power by not defining and running 3 filters at a time. Instead its 1 filter for lpg and 2 for ASR
        SynthDef("passersby_asr",{

           arg out=0,gate=1, killGate, duration=600, hz = 220, lasthz = 220, pitchBendRatio = 1, glide = 0, fm1Ratio = 0.66, fm2Ratio = 3.3, fm1Amount = 0.0, fm2Amount = 0.0,
            vel = 0.7, pressure = 0, timbre = 0, waveShape = 0, waveFolds = 0, envType = 0, attack = 0.04, peak = 10000, decay=0.2,sustain=0.9,release=5, amp = 1, pan=0,
            auxEnvAmt=0,auxEnvRel=40,auxEnvFold=0,auxEnvfm1=0,auxEnvfm2=0,auxEnvhz=0;
            
            var i_nyquist = SampleRate.ir * 0.5, snd, controlLag = 0.005, i_numHarmonics = 44,
            modFreq, mod1, mod2, mod1Index, mod2Index, mod1Freq, mod2Freq, sinOsc,
            filterEnvVel, filterEnvLow, lpgEnvelope, lpgEnvelope2, lpgSignal, asrEnvelope, asrEnvelope2, asrFilterFreq, asrSignal, killEnvelope, maxDecay = 8,auxEnvelope;


            // Lag inputs
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz = Lag.kr(hz * pitchBendRatio, 0.007 + glide);
            fm1Ratio = Lag.kr(fm1Ratio, controlLag);
            fm2Ratio = Lag.kr(fm2Ratio, controlLag);
            fm1Amount = Lag.kr(fm1Amount.squared, controlLag);
            fm2Amount = Lag.kr(fm2Amount.squared, controlLag);

            vel = Lag.kr(vel, controlLag);
            waveShape = Lag.kr(waveShape, controlLag);
            waveFolds = Lag.kr(waveFolds, controlLag);
            attack = Lag.kr(attack, controlLag);
            peak = Lag.kr(peak, controlLag);
            decay = Lag.kr(decay, controlLag);

            //routable auxiliary envelope :)
            auxEnvelope = EnvGen.ar(envelope: Env.new(levels: [0, 1, 0], times: [0.003, auxEnvRel], curve: [4, -20]), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))));
            waveFolds = Clip.kr((waveFolds+(auxEnvelope*3*auxEnvAmt*auxEnvFold)),0,3);
            fm1Amount = Clip.kr((fm1Amount+(auxEnvelope*auxEnvAmt*auxEnvfm1)),0,1);
            fm2Amount = Clip.kr((fm2Amount+(auxEnvelope*auxEnvAmt*auxEnvfm2)),0,1);
            hz = hz + (auxEnvelope*auxEnvAmt*2500*auxEnvhz);

             // Modulators
            mod1Index = fm1Amount * 22;
            mod1Freq = hz * fm1Ratio * LFNoise2.kr(freq: 0.1, mul: 0.001, add: 1);
            mod1 = SinOsc.ar(freq: mod1Freq, phase: 0, mul: mod1Index * mod1Freq, add: 0);
            mod2Index = fm2Amount * 12;
            mod2Freq = hz * fm2Ratio * LFNoise2.kr(freq: 0.1, mul: 0.005, add: 1);
            mod2 = SinOsc.ar(freq: mod2Freq, phase: 0, mul: mod2Index * mod2Freq, add: 0);
            modFreq = hz + mod1 + mod2; 

            // Sine and triangle
            sinOsc = SinOsc.ar(freq: modFreq, phase: 0, mul: 0.5);
            //removed tri and saw, took too much cpu power and caused issues :(

            
			// Fold
			snd = Fold.ar(in: sinOsc * (1 + (timbre * 0.5) + (waveFolds * 2)), lo: -0.5, hi: 0.5);

			// Hack away some aliasing
			snd = LPF.ar(in: snd, freq: 12000);

			// Noise
			snd = snd + PinkNoise.ar(mul: 0.003);

			// LPG
			filterEnvVel = vel.linlin(0, 1, 0.5, 1);
            filterEnvVel = 1; //default velocity to 100% for now
			filterEnvLow = (peak * filterEnvVel).min(300); 

            asrEnvelope = EnvGen.ar(envelope: Env.asr(attackTime: attack, sustainLevel: 1, releaseTime: release, curve: -4), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            asrFilterFreq = asrEnvelope.linlin(0, 1, filterEnvLow, peak * filterEnvVel);

            asrSignal = RLPF.ar(in: snd, freq: asrFilterFreq, rq: 0.95);
            asrSignal = RLPF.ar(in: asrSignal, freq: asrFilterFreq, rq: 0.95);

            asrEnvelope2 = EnvGen.ar(envelope: Env.asr(attackTime: attack, sustainLevel: 1, releaseTime: release, curve: -4), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2); 

            asrSignal = asrSignal * asrEnvelope2; 

            
			snd = asrSignal;
            snd = snd*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
			// Saturation amp
			//snd = tanh(snd * pressure.linlin(0, 1, 1.5, 3) * amp).softclip; //this took up cpu power as well, I guess calculating tanh is expensive :(
            snd = snd*amp/2;
            snd = Pan2.ar(snd,Lag.kr(pan,0.1));

			Out.ar(\outtrack.kr(0),snd);
            Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);
            Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);
            Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);  
        }).add;

        SynthDef("passersby_lpg",{

            arg out=0,gate=1, killGate, duration=600, hz = 220, lasthz = 220, pitchBendRatio = 1, glide = 0, fm1Ratio = 0.66, fm2Ratio = 3.3, fm1Amount = 0.0, fm2Amount = 0.0,
            vel = 0.7, pressure = 0, timbre = 0, waveShape = 0, waveFolds = 0, envType = 0, attack = 0.04, peak = 10000, decay=0.2,sustain=0.9,release=5, amp = 1, pan=0,
            auxEnvAmt=0,auxEnvRel=40,auxEnvFold=0,auxEnvfm1=0,auxEnvfm2=0,auxEnvhz=0;
            
            var i_nyquist = SampleRate.ir * 0.5, snd, controlLag = 0.005, i_numHarmonics = 44,
            modFreq, mod1, mod2, mod1Index, mod2Index, mod1Freq, mod2Freq, sinOsc,
            filterEnvVel, filterEnvLow, lpgEnvelope, lpgEnvelope2, lpgSignal, asrEnvelope, asrEnvelope2, asrFilterFreq, asrSignal, killEnvelope, maxDecay = 8,auxEnvelope;


            // Lag inputs
            hz = XLine.kr(start: lasthz, end: hz, dur: glide + 0.001);
            hz = Lag.kr(hz * pitchBendRatio, 0.007 + glide);
            fm1Ratio = Lag.kr(fm1Ratio, controlLag);
            fm2Ratio = Lag.kr(fm2Ratio, controlLag);
            fm1Amount = Lag.kr(fm1Amount.squared, controlLag);
            fm2Amount = Lag.kr(fm2Amount.squared, controlLag);

            vel = Lag.kr(vel, controlLag);
            waveShape = Lag.kr(waveShape, controlLag);
            waveFolds = Lag.kr(waveFolds, controlLag);
            attack = Lag.kr(attack, controlLag);
            peak = Lag.kr(peak, controlLag);
            decay = Lag.kr(decay, controlLag);

            //routable auxiliary envelope :)
            auxEnvelope = EnvGen.ar(envelope: Env.new(levels: [0, 1, 0], times: [0.003, auxEnvRel], curve: [4, -20]), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))));
            waveFolds = Clip.kr((waveFolds+(auxEnvelope*3*auxEnvAmt*auxEnvFold)),0,3);
            fm1Amount = Clip.kr((fm1Amount+(auxEnvelope*auxEnvAmt*auxEnvfm1)),0,1);
            fm2Amount = Clip.kr((fm2Amount+(auxEnvelope*auxEnvAmt*auxEnvfm2)),0,1);
            hz = hz + (auxEnvelope*auxEnvAmt*2500*auxEnvhz);

             // Modulators
            mod1Index = fm1Amount * 22;
            mod1Freq = hz * fm1Ratio * LFNoise2.kr(freq: 0.1, mul: 0.001, add: 1);
            mod1 = SinOsc.ar(freq: mod1Freq, phase: 0, mul: mod1Index * mod1Freq, add: 0);
            mod2Index = fm2Amount * 12;
            mod2Freq = hz * fm2Ratio * LFNoise2.kr(freq: 0.1, mul: 0.005, add: 1);
            mod2 = SinOsc.ar(freq: mod2Freq, phase: 0, mul: mod2Index * mod2Freq, add: 0);
            modFreq = hz + mod1 + mod2; 
        
            // Sine and triangle
            sinOsc = SinOsc.ar(freq: modFreq, phase: 0, mul: 0.5);

            //removed tri and saw, took too much cpu power and caused issues :(
            
			// Fold
			snd = Fold.ar(in: sinOsc * (1 + (timbre * 0.5) + (waveFolds * 2)), lo: -0.5, hi: 0.5);

			// Hack away some aliasing
			snd = LPF.ar(in: snd, freq: 12000);

			// Noise
			snd = snd + PinkNoise.ar(mul: 0.003);

			// LPG
			filterEnvVel = vel.linlin(0, 1, 0.5, 1);
            filterEnvVel = 1; //default velocity to 100% for now
			filterEnvLow = (peak * filterEnvVel).min(300); 

            lpgEnvelope= EnvGen.ar(envelope: Env.new(levels: [0, 1, 0], times: [0.003, release], curve: [4, -20]), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);
            lpgEnvelope2=EnvGen.ar(envelope: Env.new(levels: [0, 1, 0], times: [0.002, release], curve: [4, -10]), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))),doneAction:2);

            lpgSignal = RLPF.ar(in: snd, freq: lpgEnvelope.linlin(0, 1, filterEnvLow, peak * filterEnvVel), rq: 0.9);
            lpgSignal = lpgSignal * lpgEnvelope2;  
            

			snd = lpgSignal;
            snd = snd*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
			// Saturation amp
			//snd = tanh(snd * pressure.linlin(0, 1, 1.5, 3) * amp).softclip; //this took up cpu power as well, I guess calculating tanh is expensive :(
            snd = snd*amp/2;
            snd = Pan2.ar(snd,Lag.kr(pan,0.1));

			Out.ar(\outtrack.kr(0),snd);
            Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);
            Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);
            Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);  
        }).add;
        //passersby ends


        SynthDef("oilcan",{
            arg  out = 0, lpf=18000, duration= 10, freq = 91, atk = 0, fb = 5, sweep_time = 1, sweep_ix = 0.05, mod_ratio = 1.1, 
            mod_rel = 70, mod_ix = 0.01, car_rel = 0.3, fold = 0, gain = 1, routing = 0, gate = 1,pan=0, level=1;
            var headroom = 0.7, car_env, mod_env, sweep_env, pitch, mod, car, snd;

            car_env = EnvGen.ar(Env.perc(atk,car_rel), doneAction: 2, gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))));
            mod_env = EnvGen.ar(Env.perc(atk,(car_rel*(mod_rel/100))), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0]))));
            sweep_env = EnvGen.ar(Env.perc(atk,(car_rel*(sweep_time/100))), gate: (gate-EnvGen.kr(Env.new([0,0,1],[duration,0])))) * sweep_ix;

            pitch = Clip.ar(freq + (sweep_env*5000),0,10000);

            mod = Fold.ar(SinOscFB.ar(pitch * mod_ratio, fb) * (fold+1), -1, 1) * mod_env * mod_ix;
            car = Fold.ar(SinOsc.ar(pitch + (mod*10000*(1-routing))) * (fold+1), -1, 1) * car_env;

            snd = car + (mod*routing);
            snd = snd*EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2); //this is added so that we can do some sick retriggering!

            snd = Clip.ar(snd, 0-headroom,headroom);
            snd = (snd * gain).tanh;

            snd = SelectX.ar(\decimate.kr(0).lag(0.01), [snd, Latch.ar(snd, Impulse.ar(LFNoise2.kr(0.3).exprange(1000,16e3)))]);

            snd = LPF.ar(snd,Lag.kr(lpf));

            snd = Pan2.ar(snd,Lag.kr(pan,0.1));
            Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);
            Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);
            Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd); 
        }).add;


        (1..2).do({arg ch;
        SynthDef("playerOneShot"++ch,{
            arg bufnum, id=0.0,amp=0.25, rate=1.0,sampleStart=0.0,sampleEnd=1.0, watch=0.0, xfade=0.01,t_free=0,attack=0.005,release=1000000;

            // vars
            var snd,pos,sampleDuration,sampleInit;
            var duration=BufDur.ir(bufnum);
            var frames=BufFrames.ir(bufnum);

            sampleStart=sampleStart-(xfade/2);
            sampleInit=Select.kr(sampleStart<0,[sampleStart,sampleStart+sampleEnd]);
            sampleStart=Select.kr(sampleStart<0,[sampleStart,0]);
            sampleEnd=sampleEnd+(xfade/2);
            sampleEnd=Select.kr(sampleEnd>duration,[sampleEnd,duration]);
            sampleDuration=(sampleEnd-sampleStart)/rate.abs;

            pos=Phasor.ar(
                trig:Impulse.kr(0),
                rate:rate/context.server.sampleRate,
                start:((sampleStart*(rate>0))+(sampleEnd*(rate<0))),
                end:((sampleEnd*(rate>0))+(sampleStart*(rate<0))),
                resetPos:sampleInit
            );

            snd=BufRd.ar(ch,bufnum,pos/duration*frames,
                loop:1,
                interpolation:4
            );
            snd=Pan2.ar(snd,0);

            snd=snd*amp*EnvGen.ar(Env.new([0,1,1,0],[xfade/2,sampleDuration-xfade,xfade/2]),doneAction:2);
            snd=snd*EnvGen.ar(Env.new([1,0],[xfade/2+0.001]),gate:t_free,doneAction:2);
            // envelopes
            // snd=snd*EnvGen.ar(Env.perc(attack,release),doneAction:2);

            SendReply.kr(Impulse.kr(15),'/audition',[A2K.kr(pos)]);

            Out.ar(0,snd);
        }).add;
        });


        SynthDef("defAudioIn",{
            arg ch=0,lpf=20000,lpfqr=0.707,hpf=20,hpfqr=0.909,pan=0,amp=1.0;
            var snd;
            snd=SoundIn.ar(ch);
            snd=Pan2.ar(snd,pan,amp);
            // snd=RHPF.ar(snd,hpf,hpfqr);
            // snd=RLPF.ar(snd,lpf,lpfqr);
            Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;


        (1..2).do({arg ch;
        SynthDef("playerInOut"++ch,{
            arg out=0, buf, id=0,amp=1.0, pan=0, filter=18000, rate=1.0,pitch=0,sampleStart=0.0,sampleEnd=1.0,sampleIn=0.0,sampleOut=1.0, watch=0, gate=1, xfade=0.1,
            duration=10000,attack=0.001,decay=0.3,sustain=1.0,release=2.0,drive=0;

            // vars
            var snd,snd2,pos,trigger,sampleDuration,sampleDurationInOut,imp,aOrB,posA,sndA,posB,sndB,trigA,trigB;
            var durationBuffer=BufDur.ir(buf);
            var frames=BufFrames.ir(buf);

            rate = BufRateScale.ir(buf)*rate*pitch.midiratio;
            sampleDuration=(sampleEnd-sampleStart)/rate.abs;
            sampleDurationInOut=(sampleOut-sampleIn)/rate.abs;

            trigger=DelayN.kr(Impulse.kr(0)+Impulse.kr(1/sampleDurationInOut),sampleDuration*0.9,sampleDuration*0.9);
            aOrB=ToggleFF.kr(trigger);

            posA=Phasor.ar(
                trig:1-aOrB,
                rate:rate,
                start:sampleStart/durationBuffer*frames,
                end:sampleEnd/durationBuffer*frames,
                resetPos:sampleIn/durationBuffer*frames
            );
            sndA=BufRd.ar(ch,buf,posA,
                loop:1,
                interpolation:4
            );
            posB=Phasor.ar(
                trig:aOrB,
                rate:rate.neg,
                start:sampleStart/durationBuffer*frames,
                end:sampleEnd/durationBuffer*frames,
                resetPos:sampleOut/durationBuffer*frames
            );
            sndB=BufRd.ar(ch,buf,posB,
                loop:1,
                interpolation:4
            );

            pos=Select.kr(aOrB,[posA,posB]);
            snd=SelectX.ar(Lag.kr(aOrB,xfade),[sndA,sndB],0)*amp;

            // drive
            snd2 = (snd * 30.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, -10), 3000, 1, -10);
            snd2 = (snd2 * 10.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, 10), 3000, 1, 10);
            snd2 = snd2 * -10.dbamp;
            snd = SelectX.ar(drive,[snd,snd2]);

            snd=LPF.ar(snd,filter);
            snd = snd * Env.asr(attack, 1, release).ar(Done.freeSelf, gate * ToggleFF.kr(1-TDelay.kr(DC.kr(1),duration)) );
            snd = snd * EnvGen.ar(Env.new([1,0],[\gate_release.kr(1)]),Trig.kr(\gate_done.kr(0)),doneAction:2);
            snd=Pan2.ar(snd,0.0);
            snd=Pan2.ar(snd[0],1.neg+(2*pan))+Pan2.ar(snd[1],1+(2*pan));
            snd=Balance2.ar(snd[0],snd[1],pan);

            SendReply.kr(Impulse.kr(10)*watch,'/position',[pos / BufFrames.ir(buf) * BufDur.ir(buf)]);

            Out.ar(\outtrack.kr(0),snd/10);Out.ar(\out.kr(0),\compressible.kr(0)*snd*amp);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd*amp);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).add;
        });


        SynthDef("kick", { |basefreq = 40, ratio = 6, sweeptime = 0.05, preamp = 1, amp = 1,
            decay1 = 0.3, decay1L = 0.8, decay2 = 0.15, clicky=0.0, out|
            var snd;
            var    fcurve = EnvGen.kr(Env([basefreq * ratio, basefreq], [sweeptime], \exp)),
            env = EnvGen.kr(Env([clicky,1, decay1L, 0], [0.0,decay1, decay2], -4), doneAction: Done.freeSelf),
            sig = SinOsc.ar(fcurve, 0.5pi, preamp).distort * env ;
            snd = (sig*amp).tanh!2;
            Out.ar(\outtrack.kr(0),snd);Out.ar(\out.kr(0),\compressible.kr(0)*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);

        SynthDef(\main, {
            arg outBus=0,inBusNSC,inSC,inDelay,lpshelf=60,lpgain=0,sidechain_mult=2,compress_thresh=0.1,compress_level=0.1,compress_attack=0.01,compress_release=1,inBus,
            tape_buf,tape_slow=0,tape_stretch=0,delay_bufs=#[0,1],delay_time=0.25,delay_feedback=0.5;
            var snd,sndSC,sndNSC,sndDelay,tapePosRec,tapePosStretch,local;
            snd=In.ar(inBus,2);
            sndNSC=In.ar(inBusNSC,2);
            sndSC=In.ar(inSC,2);
            sndDelay=In.ar(inDelay,2);

            snd = Compander.ar(snd, (sndSC*sidechain_mult),
                compress_thresh, 1, compress_level,
                compress_attack, compress_release);
            snd = snd + sndNSC;

            // tape delay
            local = LocalIn.ar(2);
            local = OnePole.ar(local,0.4);
            local = OnePole.ar(local, -0.08);
            local = Rotate2.ar(local[0],local[1],0.2);
            local = BufDelayL.ar(delay_bufs,local,Lag.kr(delay_time),mul:EnvGen.ar(Env.new([1,0.1,0.1,1],[0.01,0.01,0.01]),Trig.kr(Changed.kr(delay_time))));
            local = LeakDC.ar(local);
            LocalOut.ar((local + (sndDelay))*Clip.kr((36.neg.dbamp.log/(delay_feedback/delay_time+1)).exp,0,0.99999));
            snd = snd + local;

            snd = LeakDC.ar(snd);
            // snd = RHPF.ar(snd,60,0.707);
            snd=BLowShelf.ar(snd, lpshelf, 1, lpgain);

            // // tape
            tapePosRec=Phasor.ar(end:BufFrames.ir(tape_buf));
            BufWr.ar(snd,tape_buf,tapePosRec);
            // tape slow
            snd = SelectX.ar(VarLag.kr(tape_slow>0,1,warp:\sine),[snd,PlayBuf.ar(2,tape_buf,Lag.kr(1/(tape_slow+1),1),startPos:tapePosRec-10,loop:1,trigger:Trig.kr(tape_slow>0))]);

            // reduce stereo spread in the bass
            snd = BHiPass.ar(snd,200)+Pan2.ar(BLowPass.ar(snd[0]+snd[1],200));

            Out.ar(outBus,snd*EnvGen.ar(Env.new([0,1],[1])));
        }).send(context.server);

        SynthDef(\pad0, {
            var snd;
            snd = Saw.ar(\freq.kr(440) * ((-3..3) * 0.05).midiratio * [1, 2, 1, 4, 1, 2, 1]);
            snd = RLPF.ar(snd, LFNoise2.kr(0.3 ! snd.size).linexp(-1, 1, 100, 8000), 0.3);
            snd = Splay.ar(snd);
            snd = Pan2.ar(snd,\pan.kr(0));
            snd = LPF.ar(snd,\lpf.kr(18000));
            snd = snd * EnvGen.ar(Env.asr(\attack.kr(0.5), 1.0, \release.kr(0.5)),\gate.kr(1) * ToggleFF.kr(1-TDelay.kr(DC.kr(1),\duration.kr(1))),doneAction:2);
            snd = snd * -30.dbamp * \amp.kr(1);
            Out.ar(\out.kr(0), (1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);

        SynthDef(\pad1, {
            var snd;
            snd = SawDPW.ar(\freq.kr(440) * ((-3..2) * 0.05).midiratio * [1, 2, 1, 4, 1, 2]);
            snd = Splay.ar(snd);
            snd = MoogFF.ar(snd, XLine.kr(100,rrand(6000,\lpf.kr(18000)),\duration.kr(1)*(1/\swell.kr(1))), 0);
            snd = snd * EnvGen.ar(Env.asr(\attack.kr(0.5), 1.0, \release.kr(0.5)),\gate.kr(1) * ToggleFF.kr(1-TDelay.kr(DC.kr(1),\duration.kr(1))),doneAction:2);
            snd = Balance2.ar(snd[0], snd[1], \pan.kr(0));
            snd = snd * -10.dbamp * \amp.kr(1);
            Out.ar(\out.kr(0), (1-\sendreverb.kr(0))*snd);
            Out.ar(\outtrack.kr(0),snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);
            Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);
            Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);

        SynthDef(\pad2,{
            var snd;
            snd = CombC.ar(PinkNoise.ar * -10.dbamp, \freq.kr(440).reciprocal, \freq.kr(440).reciprocal, 2.0);
            snd = snd ! 2;
            snd = LeakDC.ar(snd);
            snd = snd * EnvGen.ar(Env.asr(\attack.kr(0.5), 1.0, \release.kr(0.5)),\gate.kr(1) * ToggleFF.kr(1-TDelay.kr(DC.kr(1),\duration.kr(1))),doneAction:2);
            snd = Balance2.ar(snd[0], snd[1], \pan.kr(0));
            snd = snd * -50.dbamp * \amp.kr(1);
            Out.ar(\out.kr(0), (1-\sendreverb.kr(0))*snd);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);

        SynthDef("reverb0", {
            Out.ar(0,Silent.ar());
        }).send(context.server);

        SynthDef("reverb1", {
            arg shimmer=1,predelay=20,input_amount=100,input_lowpass_cutoff=10000,
            input_highpass_cutoff=100,input_diffusion_1=75,input_diffusion_2=62.5,
            tail_density=70,decay=50,damping=5500,modulator_frequency=1,modulator_depth=0.1;
            var snd, snd2;
            snd = In.ar(\in.kr(0), 2);
            snd2 = DelayN.ar(snd, 0.03, 0.03);
            snd2 = snd2 + PitchShift.ar(snd, 0.13, 2,0,1,1*shimmer/2);
            snd2 = snd2 + PitchShift.ar(snd, 0.1, 4,0,1,0.5*shimmer/2);
            snd2 = snd2 + PitchShift.ar(snd, 0.1, 8,0,1,0.125*shimmer/2);
            snd2 = Fverb.ar(snd2[0],snd2[1],
                predelay: predelay,
                input_amount: input_amount,
                input_lowpass_cutoff: input_lowpass_cutoff,
                input_highpass_cutoff: input_highpass_cutoff,
                input_diffusion_1: input_diffusion_1,
                input_diffusion_2: input_diffusion_2,
                tail_density: tail_density,
                decay: decay,
                damping: damping,
                modulator_frequency: modulator_frequency,
                modulator_depth: modulator_depth,
            );
            // snd2 = DelayC.ar(snd2, 0.2, SinOsc.ar(0.3, [0, pi]).linlin(-1,1,0,0.001));
            // snd2 = CombN.ar(snd2, 0.1, {Rand(0.01,0.099)}!32, 0.1+(tail*2));
            // snd2 = SplayAz.ar(2, snd2);
            // 5.do{snd2 = AllpassN.ar(snd2, 0.1, {Rand(0.01,0.099)}!2, 0.1+(tail*1.5))};
            snd = LeakDC.ar(snd2);
            Out.ar(\out.kr(0),\compressible.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*snd);
        }).send(context.server);

        (1..2).do({arg ch;
        SynthDef("slice0"++ch,{
            arg amp=0, buf=0, rate=1, pos=0, drive=1, compression=0, gate=1, duration=100000, pan=0, send_pos=0, filter=18000, attack=0.01,release=0.01;
            var snd,snd2;
            var snd_pos = Phasor.ar(
                trig: Impulse.kr(0),
                rate: rate * BufRateScale.ir(buf),
                resetPos: pos / BufDur.ir(buf) * BufFrames.ir(buf),
                end: BufFrames.ir(buf),
            );
            SendReply.kr(Impulse.kr(10)*send_pos,'/position',[snd_pos / BufFrames.ir(buf) * BufDur.ir(buf)]);
            snd = BufRd.ar(ch,buf,snd_pos,interpolation:4);
            snd = snd * Env.asr(attack, 1, release).ar(Done.freeSelf, gate * ToggleFF.kr(1-TDelay.kr(DC.kr(1),duration)) );
            snd=Pan2.ar(snd,0.0);
            snd=Pan2.ar(snd[0],1.neg+(2*pan))+Pan2.ar(snd[1],1+(2*pan));
            snd=Balance2.ar(snd[0],snd[1],pan);

            // fx
            snd = SelectX.ar(\decimate.kr(0).lag(0.01), [snd, Latch.ar(snd, Impulse.ar(LFNoise2.kr(0.3).exprange(1000,16e3)))]);

            // drive
            snd2 = (snd * 30.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, -10), 3000, 1, -10);
            snd2 = (snd2 * 10.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, 10), 3000, 1, 10);
            snd2 = snd2 * -10.dbamp;

            snd = SelectX.ar(drive,[snd,snd2]);

            snd = Compander.ar(snd,snd,compression,0.5,clampTime:0.01,relaxTime:0.01);

            snd = RLPF.ar(snd,Lag.kr(filter),0.707);

            Out.ar(\outtrack.kr(0),snd/10);Out.ar(\out.kr(0),\compressible.kr(0)*snd*amp);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd*amp);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);
        });


        (1..2).do({arg ch;
        SynthDef("slice1"++ch,{
            arg amp=0, buf=0, rate=1, pos=0, drive=1, compression=0, gate=1, duration=100000, pan=0, send_pos=0, filter=18000, attack=0.01,release=0.01;
            var snd,snd2;
            var snd_pos = Phasor.ar(
                trig: Impulse.kr(0),
                rate: rate * BufRateScale.ir(buf),
                resetPos: pos / BufDur.ir(buf) * BufFrames.ir(buf),
                end: BufFrames.ir(buf),
            );
            SendReply.kr(Impulse.kr(10)*send_pos,'/position',[snd_pos / BufFrames.ir(buf) * BufDur.ir(buf)]);
            snd = WarpZ.ar(ch,buf,snd_pos/BufFrames.ir(buf),windowSize:0.25,overlaps:8,interp:4);
            snd = snd * Env.asr(attack, 1, release).ar(Done.freeSelf, gate * ToggleFF.kr(1-TDelay.kr(DC.kr(1),duration)) );
            snd=Pan2.ar(snd,0.0);
            snd=Pan2.ar(snd[0],1.neg+(2*pan))+Pan2.ar(snd[1],1+(2*pan));
            snd=Balance2.ar(snd[0],snd[1],pan);

            // fx
            snd = SelectX.ar(\decimate.kr(0).lag(0.01), [snd, Latch.ar(snd, Impulse.ar(LFNoise2.kr(0.3).exprange(1000,16e3)))]);

            // drive
            snd2 = (snd * 30.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, -10), 3000, 1, -10);
            snd2 = (snd2 * 10.dbamp).tanh * -10.dbamp;
            snd2 = BHiShelf.ar(BLowShelf.ar(snd2, 500, 1, 10), 3000, 1, 10);
            snd2 = snd2 * -10.dbamp;

            snd = SelectX.ar(drive,[snd,snd2]);

            snd = Compander.ar(snd,snd,compression,0.5,clampTime:0.01,relaxTime:0.01);

            snd = RLPF.ar(snd,Lag.kr(filter),0.707);

            Out.ar(\outtrack.kr(0),snd/10);Out.ar(\out.kr(0),\compressible.kr(0)*snd*amp);
            Out.ar(\outsc.kr(0),\compressing.kr(0)*snd);
            Out.ar(\outnsc.kr(0),(1-\compressible.kr(0))*(1-\sendreverb.kr(0))*snd*amp);
            Out.ar(\outreverb.kr(0),\sendreverb.kr(0)*snd);Out.ar(\outtape.kr(0),\sendtape.kr(0)*snd);Out.ar(\outdelay.kr(0),\senddelay.kr(0)*snd);
        }).send(context.server);
        });


        context.server.sync;
        buses.put("busCompressible",Bus.audio(s,2));
        buses.put("busNotCompressible",Bus.audio(s,2));
        buses.put("busCompressing",Bus.audio(s,2));
        buses.put("busDelay",Bus.audio(s,2));
        buses.put("busReverb",Bus.audio(s,2));
        buses.put("busTape",Bus.audio(s,2));
        10.do({ arg i;
            buses.put("bus"++i,Bus.audio(s,2));
        });
        context.server.sync;
        ouroboro=Ouroboro.new(context.server,buses.at("busTape"));
        context.server.sync;

        mx = MxSamplesZ(Server.default,100,buses.at("busCompressible").index,buses.at("busNotCompressible").index,buses.at("busCompressing"),buses.at("busReverb"),buses.at("busTape"),buses.at("busDelay"));
        context.server.sync;
        syns.put("main",Synth.new(\main,[\tape_buf,bufs.at("tape"),\outBus,0,\sidechain_mult,8,\inBus,buses.at("busCompressible"),\inBusNSC,buses.at("busNotCompressible"),\inSC,buses.at("busCompressing"),\delay_bufs,bufsDelay,\inDelay,buses.at("busDelay")]));
        NodeWatcher.register(syns.at("main"));
        context.server.sync;
        this.reverbOn(0);
        context.server.sync;
        ouroboro=Ouroboro.new(context.server,buses.at("busTape"),syns.at("main"));
        context.server.sync;

        syns.put("audioInL",Synth.new("defAudioIn",[ch:0,
            out: buses.at("busCompressible"),
            outsc: buses.at("busCompressing"),
            outnsc: buses.at("busNotCompressible"),
            outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
            outdelay: buses.at("busDelay"),
            compressible: 0,
            compressing: 0,
            pan:-1], syns.at("reverb"), \addBefore));
        syns.put("audioInR",Synth.new("defAudioIn",[ch:1,
            out: buses.at("busCompressible"),
            outsc: buses.at("busCompressing"),
            outnsc: buses.at("busNotCompressible"),
            outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
            outdelay: buses.at("busDelay"),
            compressible: 0,
            compressing: 0,
            pan:1], syns.at("reverb"), \addBefore));
        NodeWatcher.register(syns.at("audioInR"));
        NodeWatcher.register(syns.at("audioInL"));

        context.server.sync;

        this.addCommand("main_set","sf",{ arg msg;
            var k=msg[1];
            var v=msg[2];
            if (syns.at("main").notNil,{
                if (syns.at("main").isRunning,{
                    ["Main: setting",k,v].postln;
                    syns.at("main").set(k.asString,v);
                });
            });
        });

        this.addCommand("synth_set","ssf",{ arg msg;
            var id=msg[1];
            var k=msg[2];
            var v=msg[3];
            this.synthChange(id,k,v);
        });

        this.addCommand("audionin_set","ssf",{ arg msg;
            var lr=msg[1];
            var key=msg[2];
            var val=msg[3];
            ["audioIn"++lr,key,val].postln;
            syns.at("audioIn"++lr).set(key,val);
        });

        this.addCommand("reverb_set","sf",{ arg msg;
            var key=msg[1];
            var val=msg[2];
            ["padFx: putting",key,val].postln;
            syns.at("reverb").set(key,val);
        });

        this.addCommand("slice_off","s",{ arg msg;
            var id=msg[1];
            if (syns.at(id).notNil,{
                if (syns.at(id).isRunning,{
                    syns.at(id).set(\gate,0);
                });
            });
        });

        this.addCommand("slice_on","ssfffffffffffffffffffffff",{ arg msg;
            var id=msg[1];
            var filename=msg[2];
            var db=msg[3];
            var db_add=msg[4];
            var pan=msg[5];
            var rate=msg[6];
            var pitch=msg[7];
            var pos=msg[8];
            var duration_slice=msg[9];
            var duration_total=msg[10];
            var retrig=msg[11];
            var gate=msg[12];
            var filter=msg[13];
            var decimate=msg[14];
            var compressible=msg[15];
            var compressing=msg[16];
            var send_reverb=msg[17];
            var drive=msg[18];
            var compression=msg[19];
            var send_pos=msg[20];
            var attack=msg[21];
            var release=msg[22];
            var stretch=msg[23];
            var sendTape=msg[24];
            var sendDelay=msg[25];
            var db_first=db+db_add;
            var do_stretch=0;
            var duration = 0.003;
            duration = (duration_slice * gate / (retrig + 1));
            if(duration < 0.003,{
                duration = 0.003;
            });
            if (stretch>0,{
                do_stretch=1;
            });
            if (retrig>0,{
                db_first=db;
                if (db_add>0,{
                    db_first=db-(db_add*retrig);
                    db=db_first;
                })
            });
            // ["duration_slice",duration_slice,"duration_total",duration_total,"retrig",retrig].postln;
            if (bufs.at(filename).notNil,{
                if (syns.at(id).notNil,{
                    if (syns.at(id).isRunning,{
                        syns.at(id).set(\gate,0);
                    });
                });
                syns.put(id,Synth.new("slice"++do_stretch++bufs.at(filename).numChannels, [
                    out: buses.at("busCompressible"),
                    outsc: buses.at("busCompressing"),
                    outnsc: buses.at("busNotCompressible"),
                    outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                    compressible: compressible,
                    compressing: compressing,
                    sendreverb: send_reverb,
                    buf: bufs.at(filename),
                    attack: attack,
                    release: release,
                    amp: db_first.dbamp,
                    pan: pan,
                    filter: filter,
                    rate: rate*pitch.midiratio/(1+stretch),
                    pos: pos,
                    duration: duration,
                    decimate: decimate,
                    drive: drive,
                    compression: compression,
                    send_pos: send_pos,
                    sendtape: sendTape,
                    senddelay: sendDelay,
                    outtrack: buses.at("bus"++id.asString.split($_)[0].asString),
                ], syns.at("reverb"), \addBefore));
                if (retrig>0,{
                    Routine {
                        (retrig).do{ arg i;
                            (duration_total/ (retrig+1) ).wait;
                            syns.put(id,Synth.new("slice"++do_stretch++bufs.at(filename).numChannels, [
                                out: buses.at("busCompressible"),
                                outsc: buses.at("busCompressing"),
                                outnsc: buses.at("busNotCompressible"),
                                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                                sendreverb: send_reverb,
                                compressible: compressible,
                                compressing: compressing,
                                buf: bufs.at(filename),
                                pan: pan,
                                attack: attack,
                                release: release,
                                amp: (db+(db_add*(i+1))).dbamp,
                                rate: rate*((pitch.sign)*(i+1)+pitch).midiratio/(1+stretch),
                                duration: duration,
                                filter: filter,
                                pos: pos,
                                decimate: decimate,
                                drive: drive,
                                compression: compression,
                                send_pos: send_pos,
                                sendtape: sendTape,
                                senddelay: sendDelay,
                                outtrack: buses.at("bus"++id.asString.split($_)[0].asString),
                            ], syns.at("reverb"), \addBefore));
                        };
                        NodeWatcher.register(syns.at(id));
                        this.synthWatch(id.asString.split($_)[0].asString,syns.at(id));
                    }.play;
                 },{
                    NodeWatcher.register(syns.at(id));
                    this.synthWatch(id.asString.split($_)[0].asString,syns.at(id));
                });
            });
        });

        this.addCommand("melodic_off","s",{ arg msg;
            var id=msg[1];
            ["melodic_off",id].postln;
            if (syns.at(id).notNil,{
                if (syns.at(id).isRunning,{
                    ["gating off"].postln;
                    syns.at(id).set(\gate,0);
                });
            });
        });

        this.addCommand("melodic_on","ssffffffffffffffffffffff",{ arg msg;
            var id=msg[1];
            var filename=msg[2];
            var db=msg[3];
            var db_add=msg[4];
            var pan=msg[5];
            var pitch=msg[6];
            var sampleStart=msg[7];
            var sampleIn=msg[8];
            var sampleOut=msg[9];
            var sampleEnd=msg[10];
            var duration=msg[11];
            var filter=msg[12];
            var gate=msg[13];
            var retrig=msg[14];
            var compressible=msg[15];
            var compressing=msg[16];
            var send_reverb=msg[17];
            var watch=msg[18];
            var attack=msg[19];
            var release=msg[20];
            var monophonic_release=msg[21];
            var drive=msg[22];
            var sendTape=msg[23];
            var sendDelay=msg[24];
            var db_first=db+db_add;
            if (retrig>0,{
                db_first=db;
            });
            // ["duration",duration,"release",release,"gate",gate,"retrig",retrig].postln;
            if (bufs.at(filename).notNil,{
                var buf=bufs.at(filename);
                if (monophonic_release>0,{
                    if (syns.at(id).notNil,{
                        if (syns.at(id).isRunning,{
                            syns.at(id).set(\gate_release,monophonic_release);
                            syns.at(id).set(\gate_done,0);
                        });
                    });
                });
                syns.put(id,Synth.new("playerInOut"++buf.numChannels, [
                    out: buses.at("busCompressible"),
                    outsc: buses.at("busCompressing"),
                    outnsc: buses.at("busNotCompressible"),
                    outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                    sendreverb: send_reverb,
                    compressible: compressible,
                    compressing: compressing,
                    buf: buf,
                    amp: db_first.dbamp,
                    pan: pan,
                    filter: filter,
                    pitch: pitch,
                    sampleStart: sampleStart,
                    sampleIn: sampleIn,
                    sampleOut: sampleOut,
                    sampleEnd: sampleEnd,
                    duration: (duration * gate / (retrig + 1)),
                    watch: watch,
                    attack: attack,
                    release: release,
                    drive: drive,
                    sendtape: sendTape,
                    senddelay: sendDelay,
                    outtrack: buses.at("bus"++id.asString.split($_)[0].asString),
                ], syns.at("reverb"), \addBefore));
                if (retrig>0,{
                    if ((duration/ (retrig+1))>0.01, {
                        Routine {
                            (retrig).do{ arg i;
                                (duration/ (retrig+1) ).wait;
                                syns.put(id,Synth.new("playerInOut"++buf.numChannels, [
                                    out: buses.at("busCompressible"),
                                    outsc: buses.at("busCompressing"),
                                    outnsc: buses.at("busNotCompressible"),
                                    outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                                    sendreverb: send_reverb,
                                    compressible: compressible,
                                    compressing: compressing,
                                    buf: buf,
                                    amp: (db+(db_add*(i+1))).dbamp,
                                    duration: (duration * gate / (retrig + 1)),
                                    pan: pan,
                                    filter: filter,
                                    pitch: pitch,
                                    sampleStart: sampleStart,
                                    sampleIn: sampleIn,
                                    sampleOut: sampleOut,
                                    sampleEnd: sampleEnd,
                                    watch: watch,
                                    attack: attack,
                                    release: release,
                                    drive: drive,
                                    sendtape: sendTape,
                                    senddelay: sendDelay,
                                    outtrack: buses.at("bus"++id.asString.split($_)[0].asString),
                                ], syns.at("reverb"), \addBefore));
                            };
                            NodeWatcher.register(syns.at(id));
                            this.synthWatch(id.asString.split($_)[0].asString,syns.at(id));
                        }.play;
                    });
                 },{
                    NodeWatcher.register(syns.at(id));
                    this.synthWatch(id.asString.split($_)[0].asString,syns.at(id));
                });
            });
        });


        this.addCommand("kick","ffffffffffffff",{arg msg;
            var basefreq=msg[1];
            var ratio=msg[2];
            var sweeptime=msg[3];
            var preamp=msg[4];
            var amp=msg[5].dbamp;
            var decay1=msg[6];
            var decay1L=msg[7];
            var decay2=msg[8];
            var clicky=msg[9];
            var compressing=msg[10];
            var compressible=msg[11];
            var send_reverb=msg[12];
            var sendTape=msg[13];
            var sendDelay=msg[14];
            Synth.new("kick",[
                basefreq: basefreq,
                ratio: ratio,
                sweeptime: sweeptime,
                preamp: preamp,
                amp: amp,
                decay1: decay1,
                decay1L: decay1L,
                decay2: decay2,
                clicky: clicky,
                out: buses.at("busCompressible"),
                outsc: buses.at("busCompressing"),
                outnsc: buses.at("busNotCompressible"),
                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                compressible: compressible,
                compressing: compressing,
                sendreverb: send_reverb,
                sendtape: sendTape,
                senddelay: sendDelay,
            ],syns.at("reverb"),\addBefore).onFree({"freed!"});
        });

        //only for infinipad? might be good to rename it...
        this.addCommand("note_on","ffffffffffsf",{ arg msg;
            var note=msg[1];
            var amp=msg[2].dbamp;
            var attack=msg[3];
            var release=msg[4];
            var duration=msg[5];
            var swell=msg[6];
            var sendreverb=msg[7];
            var pan=msg[8];
            var lpf=msg[9].midicps;
            var sendTape=msg[10];
            var track_id=msg[11];
            var sendDelay=msg[12];
            1.do{ arg i;
                var id=note.asString++"_"++i;
                if (syns.at(id).notNil,{
                    if (syns.at(id).isRunning,{
                        syns.at(id).set(\gate,0);
                    });
                });
                syns.put(id,Synth.new("pad1", [
                    freq: note.midicps,
                    amp: amp,
                    attack: attack,
                    release: release,
                    duration: duration,
                    swell: swell,
                    sendreverb: sendreverb,
                    senddelay: sendDelay,
                    pan: pan,
                    lpf: lpf,
                    sendtape: sendTape,
                    out: buses.at("busCompressible"),
                    outtrack: buses.at("bus"++track_id),
                    outreverb: buses.at("busReverb"),
                    outtape: buses.at("busTape"),
                    outdelay: buses.at("busDelay"),
                ],
                syns.at("reverb"),\addBefore));
                NodeWatcher.register(syns.at(id));
                this.synthWatch(track_id,syns.at(id));
                nons.put(track_id++note.floor,syns.at(id));
            };
        });


        this.addCommand("passersby_note_on","ffffffffffffffffffffsffffffffff",{ arg msg;
            var envType=msg[1]-1;
            var note=msg[2];
            var amp=msg[3];
            var peak=msg[4];
            var pan=msg[5];
            var attack=msg[6];
            var decay=msg[7];
            var waveShape=msg[8];
            var waveFolds=msg[9];
            var fm1Ratio=msg[10];
            var fm2Ratio=msg[11];
            var fm1Amount=msg[12];
            var fm2Amount=msg[13];
            var glide = msg[14];
            var duration=msg[15];
            var sendCompressible=msg[16];
            var sendCompressing=msg[17];
            var sendReverb=msg[18];
            var lpf=msg[19].midicps;
            var monophonic_release=msg[20];
            var id=msg[21].asString;
            var sendTape=msg[22];
            var retrig=msg[23];
            var sendDelay=msg[24];
            var aux_amt=msg[25];
            var aux_rel=msg[26];
            var aux_fold=msg[27];
            var aux_fm1=msg[28];
            var aux_fm2=msg[29];
            var aux_hz=msg[30];
            var lastnote=msg[31];
            var killGate;
            var syn;
            var synth;


            if (envType==0,
            {
                synth = "passersby_lpg";
            },
            {
                synth = "passersby_asr";
            }
            );
             if (monophonic_release>0,{
                if (syns.at(id).notNil,{
                    if (syns.at(id).isRunning,{   
                        syns.at(id).set("gate_release",monophonic_release);
                        syns.at(id).set("gate_done",1);
                    });
                });
            });
            syn=Synth.new(synth,[
                envType: envType,
                hz: note.midicps,
                lasthz: lastnote.midicps,
                amp: amp.dbamp,
                peak: peak,
                pan: pan,
                attack: attack,
                release: decay,
                gate_release: decay,
                waveShape: waveShape,
                waveFolds: waveFolds,
                fm1Ratio: fm1Ratio,
                fm2Ratio: fm2Ratio,
                fm1Amount: fm1Amount,
                fm2Amount: fm2Amount,
                auxEnvAmt: aux_amt,
                auxEnvRel: aux_rel,
                auxEnvFold: aux_fold,
                auxEnvfm1: aux_fm1,
                auxEnvfm2: aux_fm2,
                auxEnvhz: aux_hz,
                glide: glide,
                killGate, killGate,
                duration: (duration / (retrig + 1)),
                out: buses.at("busCompressible"),
                outsc: buses.at("busCompressing"),
                outnsc: buses.at("busNotCompressible"),
                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                compressible: sendCompressible,
                compressing: sendCompressing,
                sendreverb: sendReverb,
                senddelay: sendDelay,
                sendtape: sendTape,
                outtrack: buses.at("bus"++id),
            ],syns.at("reverb"),\addBefore).onFree({"freed!"});
            
            

            if (retrig>0,{
                if ((duration/ (retrig+1))>0.01, {
                    Routine {
                        (retrig).do{ arg i;
                            (duration/ (retrig+1) ).wait;
                            syn.set("gate_release",monophonic_release+0.05);
                            syn.set("gate_done",1);
                            syn=Synth.new(synth, [
                                envType: envType,
                                hz: note.midicps,
                                lasthz: lastnote.midicps,
                                amp: amp.dbamp,
                                peak: peak,
                                pan: pan,
                                attack: attack,
                                release: decay,
                                gate_release: decay,
                                waveShape: waveShape,
                                waveFolds: waveFolds,
                                fm1Ratio: fm1Ratio,
                                fm2Ratio: fm2Ratio,
                                fm1Amount: fm1Amount,
                                fm2Amount: fm2Amount,
                                auxEnvAmt: aux_amt,
                                auxEnvRel: aux_rel,
                                auxEnvFold: aux_fold,
                                auxEnvfm1: aux_fm1,
                                auxEnvfm2: aux_fm2,
                                auxEnvhz: aux_hz,
                                glide: glide,
                                killGate, killGate,
                                duration: (duration / (retrig + 1)),
                                out: buses.at("busCompressible"),
                                outsc: buses.at("busCompressing"),
                                outnsc: buses.at("busNotCompressible"),
                                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                                compressible: sendCompressible,
                                compressing: sendCompressing,
                                sendreverb: sendReverb,
                                senddelay: sendDelay,
                                sendtape: sendTape,
                                outtrack: buses.at("bus"++id),
                            ], syns.at("reverb"), \addBefore);
                        };
                    }.play;
                });
            });
            NodeWatcher.register(syn);
            if (monophonic_release>0,{
                syns.put(id,syn);
            });
            this.synthWatch(id,syn);
            nons.put(id++note.floor,syn); 
        }); 

        this.addCommand("oilcan_note_on","fffffffffffffffffffffsfffff",{ arg msg;
            var freq=msg[1];
            var attack=msg[2];
            var fb=msg[3];
            var sweep_time=msg[4];
            var sweep_ix=msg[5];
            var mod_ratio=msg[6];
            var mod_rel=msg[7];
            var mod_ix=msg[8];
            var car_rel=msg[9]; //this is overall release
            var fold=msg[10];
            var gain=msg[11];
            var routing=msg[12];
            var pan=msg[13];
            var headroom=msg[14];
            var level=msg[15];
            var duration=msg[16];
            var sendCompressible=msg[17];
            var sendCompressing=msg[18];
            var sendReverb=msg[19];
            var lpf=msg[20].midicps;
            var monophonic_release=msg[21];
            var id=msg[22].asString;
            var sendTape=msg[23];
            var retrig=msg[24];
            var sendDelay=msg[25];
            var note=msg[26];
            var decimate=msg[27];
            var syn;
     
            if (monophonic_release>0,{
                if (syns.at(id).notNil,{
                    if (syns.at(id).isRunning,{   
                        syns.at(id).set("gate_release",monophonic_release);
                        syns.at(id).set("gate_done",1);
                    });
                });
            });
            syn=Synth.new("oilcan",[
                freq: freq,
                gain: gain,
                headroom: headroom,
                attack: attack,
                pan: pan,
                fb: fb,
                car_rel: car_rel,
                gate_release: car_rel,
                sweep_time: sweep_time,
                sweep_ix: sweep_ix,
                mod_ratio: mod_ratio,
                mod_rel: mod_rel,
                mod_ix: mod_ix,
                fold: fold,
                routing: routing,
                level: level,
                lpf:lpf,
                decimate: decimate,
                duration: (duration / (retrig + 1)),
                out: buses.at("busCompressible"),
                outsc: buses.at("busCompressing"),
                outnsc: buses.at("busNotCompressible"),
                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                compressible: sendCompressible,
                compressing: sendCompressing,
                sendreverb: sendReverb,
                senddelay: sendDelay,
                sendtape: sendTape,
                outtrack: buses.at("bus"++id),
            ],syns.at("reverb"),\addBefore).onFree({"freed!"});
            if (retrig>0,{
                if ((duration/ (retrig+1))>0.01, {
                    Routine {
                        (retrig).do{ arg i;
                            (duration/ (retrig+1) ).wait;
                            syn.set("gate_release",monophonic_release+0.05);
                            syn.set("gate_done",1);
                            syn=Synth.new("oilcan", [
                                freq: freq,
                                gain: gain,
                                headroom: headroom,
                                attack: attack,
                                pan: pan,
                                fb: fb,
                                car_rel: car_rel,
                                gate_release: car_rel,
                                sweep_time: sweep_time,
                                sweep_ix: sweep_ix,
                                mod_ratio: mod_ratio,
                                mod_rel: mod_rel,
                                mod_ix: mod_ix,
                                fold: fold,
                                routing: routing,
                                level: level,
                                lpf,lpf,
                                decimate: decimate,
                                duration: (duration / (retrig + 1)),
                                out: buses.at("busCompressible"),
                                outsc: buses.at("busCompressing"),
                                outnsc: buses.at("busNotCompressible"),
                                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                                compressible: sendCompressible,
                                compressing: sendCompressing,
                                sendreverb: sendReverb,
                                senddelay: sendDelay,
                                sendtape: sendTape,
                                
                                outtrack: buses.at("bus"++id),
                            ], syns.at("reverb"), \addBefore);
                        };
                    }.play;
                });
            });
            NodeWatcher.register(syn);
            if (monophonic_release>0,{
                syns.put(id,syn);
            });
            this.synthWatch(id,syn);
            nons.put(id++note.floor,syn); 
        }); 

        this.addCommand("load_buffer","s",{ arg msg;
            var id=msg[1];
            Buffer.read(context.server, id, action: {arg buf;
                ["loaded"+id].postln;
                bufs.put(id,buf);
            });
        });

        this.addCommand("audition_off","", { arg msg;
            if (syns.at("audition").notNil,{
                if (syns.at("audition").isRunning,{
                    syns.at("audition").set(\t_free,1);
                });
            });
        });


        this.addCommand("audition_on","s", { arg msg;
            var server=context.server;
            var dontLoad=false;
            if (bufs.at("audition").notNil,{
                dontLoad=bufs.at("audition").path==msg[1];
                if (syns.at("audition").notNil,{
                    if (syns.at("audition").isRunning,{
                        syns.at("audition").set(\t_free,1);
                    });
                });
            });
            if (dontLoad==true,{
                syns.put("audition",Synth.head(server,"playerOneShot"++bufs.at("audition").numChannels,[\bufnum,bufs.at("audition"),\sampleStart,0,\sampleEnd,bufs.at("audition").duration,\xfade,0,\watch,15]));
                NodeWatcher.register(syns.at("audition"));
            },{
                Buffer.read(server,msg[1],action:{ arg buf;
                    postln("loaded "++msg[1]++"into buf "++buf.bufnum);
                    bufs.put("audition",buf);
                    syns.put("audition",Synth.head(server,"playerOneShot"++bufs.at("audition").numChannels,[\bufnum,bufs.at("audition"),\sampleStart,0,\sampleEnd,bufs.at("audition").duration,\xfade,0,\watch,15]));
                    NodeWatcher.register(syns.at("audition"));
                });
            });
        });

        this.addCommand("mixer","f", { arg msg;
            if (msg[1]>0,{
                syns.put("mixer", Synth.new("mixer", [
                    bus1: buses.at("bus1"),
                    bus2: buses.at("bus2"),
                    bus3: buses.at("bus3"),
                    bus4: buses.at("bus4"),
                    bus5: buses.at("bus5"),
                    bus6: buses.at("bus6"),
                    bus7: buses.at("bus7"),
                    bus8: buses.at("bus8"),
                    bus9: buses.at("bus9"),
                    bus10: buses.at("bus10"),
                ], syns.at("reverb"), \addAfter));
                NodeWatcher.register(syns.at("mixer"));
            }, {
                if (syns.at("mixer").notNil,{
                    if (syns.at("mixer").isRunning,{
                        syns.at("mixer").free;
                    });
                })
            })
        });

        this.addCommand("mx","sffffffffffff", { arg msg;
            var folder=msg[1].asString;
            var note=msg[2];
            var velocity=msg[3];
            var amp=msg[4];
            var pan=msg[5];
            var attack=msg[6];
            var release=msg[7];
            var duration=msg[8];
            var sendCompressible=msg[9];
            var sendCompressing=msg[10];
            var sendReverb=msg[11];
            var sendTape=msg[12];
            var sendDelay=msg[13];
            mx.note(folder,note,velocity,amp,pan,attack,release,duration,sendCompressible,sendCompressing,sendReverb,sendTape,sendDelay);
        });

        this.addCommand("mx_synths","sffffffffffffffffsffffff", { arg msg;
            var synth=msg[1].asString;
            var note=msg[2];
            var db=msg[3];
            var sub=msg[4];
            var pan=msg[5];
            var attack=msg[6];
            var release=msg[7];
            var mod1=msg[8];
            var mod2=msg[9];
            var mod3=msg[10];
            var mod4=msg[11];
            var duration=msg[12];
            var sendCompressible=msg[13];
            var sendCompressing=msg[14];
            var sendReverb=msg[15];
            var lpf=msg[16].midicps;
            var monophonic_release=msg[17];
            var id=msg[18];
            var sendTape=msg[19];
            var retrig=msg[20];
            var db_add=msg[21];
            var sendDelay=msg[22];
            var lastnote=msg[23];
            var glide=msg[24];
            var syn;
            if (monophonic_release>0,{
                if (syns.at(id).notNil,{
                    if (syns.at(id).isRunning,{
                        syns.at(id).set("gate_release",monophonic_release);
                        syns.at(id).set("gate_done",1);
                    });
                });
            });
            syn=Synth.new(synth,[
                hz: note.midicps,
                lasthz: lastnote.midicps,
                amp: (db+db_add).dbamp,
                sub: (sub+db_add).dbamp,
                pan: pan,
                attack: attack,
                release: release,
                gate_release: release,
                glide: glide,
                mod1: mod1,
                mod2: mod2,
                mod3: mod3,
                mod4: mod4,
                duration: (duration / (retrig + 1)),
                out: buses.at("busCompressible"),
                outsc: buses.at("busCompressing"),
                outnsc: buses.at("busNotCompressible"),
                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                compressible: sendCompressible,
                compressing: sendCompressing,
                sendreverb: sendReverb,
                senddelay: sendDelay,
                lpf: lpf,
                sendtape: sendTape,
                outtrack: buses.at("bus"++id),
            ],syns.at("reverb"),\addBefore).onFree({"freed!"});
            if (retrig>0,{
                if ((duration/ (retrig+1))>0.01, {
                    Routine {
                        (retrig).do{ arg i;
                            (duration/ (retrig+1) ).wait;
                            syn.set("gate_release",monophonic_release+0.05);
                            syn.set("gate_done",1);
                            syn=Synth.new(synth, [
                                hz: note.midicps,
                                lasthz: lastnote.midicps,
                                amp: (db+(db_add*(i+1))).dbamp,
                                sub: (sub+(db_add*(i+1))).dbamp,
                                pan: pan,
                                attack: attack,
                                release: release,
                                gate_release: release,
                                glide: glide,
                                mod1: mod1,
                                mod2: mod2,
                                mod3: mod3,
                                mod4: mod4,
                                duration: (duration / (retrig + 1)),
                                out: buses.at("busCompressible"),
                                outsc: buses.at("busCompressing"),
                                outnsc: buses.at("busNotCompressible"),
                                outreverb: buses.at("busReverb"),outtape: buses.at("busTape"),outdelay: buses.at("busDelay"),
                                compressible: sendCompressible,
                                compressing: sendCompressing,
                                sendreverb: sendReverb,
                                senddelay: sendDelay,
                                lpf: lpf,
                                sendtape: sendTape,
                                outtrack: buses.at("bus"++id),
                            ], syns.at("reverb"), \addBefore);
                        };
                    }.play;
                });
            });
            NodeWatcher.register(syn);
            if (monophonic_release>0,{
                syns.put(id,syn);
            });
            this.synthWatch(id,syn);
            nons.put(id++note.floor,syn);
        });



        this.addCommand("note_off","sf", { arg msg;
            var key=msg[1]++(msg[2].asFloat.floor);
            // ["mx_synths_note_off",key].postln;
            if (nons.at(key).notNil,{
              //   ["mx_synths_note_off",key,"notNil"].postln;
                if (nons.at(key).isRunning,{
                  //  ["mx_synths_note_off",key,"notNil","isRunning"].postln;
                    nons.at(key).set("gate_done",1);
                });
            });
        });

        // ^ Zxcvbn specific


        // <ouroboro>
        this.addCommand("loop_record","iffi", { arg msg;
            var id=msg[1];
            var duration=msg[2];
            var crossfade=msg[3];
            var channel=msg[4];
            var key="ouroboro"++id.asInteger.asString;
            ouroboro.stop(id);
            ouroboro.record(id,duration,crossfade,channel,{
                arg buf;
                ["engine: ouroboro init buffer"+buf].postln;
                if (syns.at(key).notNil,{
                    if (syns.at(key).isRunning,{
                        syns.at(key).set(\t_trig,1);// free current
                    });
                });
                if (bufs.at(key).notNil,{
                    bufs.at(key).free;
                });
                bufs.put(key,buf);
            },{
                arg buf;
                // hotswap with the right version
                bufs.put(key,buf);
                if (syns.at(key).notNil,{
                    if (syns.at(key).isRunning,{
                        ["engine: hot swapping",id].postln;
                        syns.at(key).set(\bufnum,buf,\frames,buf.numFrames);
                    },{
                        ["engine: no running loop to hotswap",key].postln;
                    });
                },{
                    ["engine: no loop to hotswap",key].postln;
                });
         });
        });


        this.addCommand("loop_start","if", { arg msg;
            var id=msg[1];
            var ambisonics=(msg[2]>0).asInteger;
            var key="ouroboro"++id.asString;
            if (syns.at(key).notNil,{
                if (syns.at(key).isRunning,{
                    syns.at(key).set(\t_trig,1);// free current
                });
            });
            if (bufs.at(key).notNil,{
                ["engine: loop starting",key].postln;
                syns.put(key,Synth.new("defLoop"++ambisonics++bufs.at(key).numChannels,[
                    id:id,
                    frames: bufs.at(key).numFrames,
                    bufnum: bufs.at(key),
                    outnsc: buses.at("busNotCompressible"),
                ],syns.at("reverb"),\addBefore).onFree({"freed"+key}));
                NodeWatcher.register(syns.at(key));
            },{
                ["engine: loop not started, no buf",key].postln;
            });
        });

        this.addCommand("loop_stop","i", { arg msg;
            var id=msg[1];
            var key="ouroboro"++id.asString;
            if (syns.at(key).notNil,{
                if (syns.at(key).isRunning,{
                    syns.at(key).set(\t_trig,1);// free current
                });
            });
        });


        this.addCommand("loop_save","is", { arg msg;
            var id=msg[1];
            var path=msg[2];
            var key="ouroboro"++id.asString;
            var filename=path++id.asString++".wav";
            if (bufs.at(key).notNil,{
                bufs.at(key).write(filename,"wav");
            });
        });

        this.addCommand("loop_load","is", { arg msg;
            var id=msg[1];
            var path=msg[2];
            var key="ouroboro"++id.asString;
            var filename=path++id.asString++".wav";
            if (File.exists(filename),{
                Buffer.read(context.server,filename,action:{
                    arg buf;
                    bufs.put(key,buf);
                    ["loaded tape:",filename].postln;
                    NetAddr("127.0.0.1", 10111).sendMsg("recordingDone",id,id);
                });
            });
        });


        this.addCommand("dx7","sffffffffffffff", { arg msg;
            var id=msg[1].asString;
            var preset=msg[2];
            var note=msg[3];
            var db=msg[4];
            var db_add=msg[5];
            var pan=msg[6];
            var attack=msg[7];
            var release=msg[8];
            var duration=msg[9];
            var compressible=msg[10];
            var compressing=msg[11];
            var sendreverb=msg[12];
            var sendtape=msg[13];
            var senddelay=msg[14];
            var filter=msg[15].midicps;
            var out=buses.at("busCompressible");
            var outsc=buses.at("busCompressing");
            var outnsc=buses.at("busNotCompressible");
            var outreverb=buses.at("busReverb");
            var outtape=buses.at("busTape");
            var outdelay=buses.at("busDelay");
            var synBefore=syns.at("reverb");
            var outtrack=buses.at("bus"++id);
            var amp= (db+db_add).dbamp;
            dx7syn.value(0,"",0,preset, note, 110, pan, attack, release, duration, compressible, compressing, sendreverb, sendtape, senddelay,
                out, outsc, outnsc, outreverb, outtape, outdelay, synBefore, amp, filter, outtrack);
        });

        this.addCommand("dx7_set","sf", { arg msg;
            dx7syn.value(1,msg[1],msg[2]);
        });

        this.addCommand("reverb","f", { arg msg;
            this.reverbOn(msg[1]);
        });


        // </ouroboro>
    }

    free {
        // Zxcvbn Specific v0.0.1
        // free dx7
        dx7syn.value(2);
        bufs.keysValuesDo({ arg buf, val;
            val.free;
        });
        syns.keysValuesDo({ arg buf, val;
            val.free;
        });
        buses.keysValuesDo({ arg buf, val;
            val.free;
        });
        oscs.keysValuesDo({ arg buf, val;
            val.free;
        });
        dx7syn.free;
        ouroboro.free;
        bufsDelay.do(_.free);
        // ^ Zxcvbn specific
    }
}
