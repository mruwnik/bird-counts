# Bird counter

A simple web app that simulates birds singing to each other, with the option of reacting to a different birds singing. Counts can be downloaded - reload the page to restart the simulation.

There is also the possibility of simulating observers (i.e. researchers trying to count birds) which can move around (or not) on the basis of various counting methods. Observers can also be dragged around to manually position them.

## Demo

Check [here](https://media.ahiru.pl/birds/) to see a working version.

## Getting started

Shadow-cljs is used for compiling, so make sure it's [available](https://shadow-cljs.github.io/docs/UsersGuide.html#_command_line). If so, then `shadow-cljs server` should be enough to get things running.
I personally use Emacs + Cider for development, but a quick test of the above allowed me to run things locally.

## Simulation configuration

Simulations can be configured either via the web GUI, or by passing in appropriate URL parameters. The following options are supported (with default values):

### Bird options

 * num-of-birds          - 10 - the number of birds to simulate
 * volume                - 25 - the radius of each birds volume (assume pixels)

 * spontaneous-sing-prob - 0.01 - the probability of a bird spontaneously singing in a given tick
 * motivated-sing-prob   - 0.9  - the probability of a bird singing back if it hears a different bird singing

 * motivated-sing-after  - 30 - how many ticks to wait before starting motivated singing - the bird will start singing this many ticks after it hears a different bird start singing
 * sing-rest-time        - 100 - how long to wait between singing periods. This is a minimal time - during this time the bird won't attempt to sing, after this many ticks it will start trying again
 * song-length           - 20 - the song duration, in ticks
 * audio-sensitivity     - 50 - the radius in which birds can hear other birds (assume pixels)

### Bird display options

 * show-bird-hear?       - true - whether to show the radius in which a bird hears other singing
 * show-birds?           - true - whether to show where birds are
 * show-observers?       - true - whether to show where observers are
 * show-observer-hear?   - true - whether to show the radius in which observers can hear bird singing

 * bird-colour    - #000000 - the colour with which birds are shown
 * song-colour    - #0000FF - the colour of the audible radius of a bird's song
 * resing-colour  - #009600 - the colour of the audible radius of a bird's song if it's motivated singing
 * hearing-colour - #FF0000 - the colour of the radius in which a bird can hear other birds singing
 * resting-colour - #960000 - the colour of the radius in which a bird can hear other birds singing when the bird is resting

### Observer options

 * observers         - 1 - the number of observers to start off with
 * observer-strategy - the strategy used by observers to count birds. Should be one of `no-movement`, `wander`, `follow-singing`

#### Strategy specific parameters
##### wander
 * observer-prob-change-direction - 0.05 - the probability of changing wander direction in a given tick
##### follow-singing
 * observer-should-wander? - true - whether the observer should wander when no bird heard
 * observer-ignore-after   - 100 - after how many ticks after hearing a bird song should the observer continue moving

### Speed options

 * frame-rate  - 30  - Render frame rate - this is just for presentation and has no effect on the actual simulation
 * speed       - 10  - Speed factor, i.e. by how many times to speed up the simulation
 * tick-length - 100 - How many ms per a single simulation tick
