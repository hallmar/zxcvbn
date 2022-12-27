---
title: Random or ordered command values
clades:
    - drum
    - melodic
    - mx.samples
    - mx.synths
    - softcut
    - midi
    - crow
---

Ordered command values can be applied by separating values by `.` (no spaces). For example if you want to pan alternating between left and right you can do `w-50,50` which will select `-50%` and then `50%`.

Random command values can be applied by separating values by `,` (no spaces). For example, if you want to pan randomly between the left and right side you can do `w-50,50` which will select either `-50%` or `50%`. You can also randomize with whole sequential arrays by using the `:`. You can randomize a note between the first twelve semitones using the command `n1:12`. You can also combine these two, so you can select several numbers or within a range. For example, if you want to decimate sometimes between 50% and 75%, or sometimes not at all you can do `j0,0,0,50:75`. There are multiple `0` here, so that those values have more weight when selected randomly.

Ordered (using `.`) and random (using `,` or `:`) commands cannot be mixed together, doing so will likely result in an error.

