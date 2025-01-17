# TODO

* Portal block
* Frames

So, each portal is a block entity
Thats not really slow because a) they can be artificially culled at ~32 blocks, and allows for fancy render tricks:

* Each portal consists of 4 6^3 px blocks with runes.
* They randomly rotate (full 90°) and feel energetic (1px noise movement and minor rotation)
* When the player moves closer, they move closer and dent inwards, the runes glow more consistent
* When the player moves out of range they rotate-fade-out and disappear


https://www.curseforge.com/minecraft/mc-mods/structure-essentials-forge-fabric