package com.example.fortifications.mixin;

import com.gametechbc.gtbcs_geomancy_plus.spells.geo.PetriviseSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = PetriviseSpell.class, remap = false)
public abstract class PetriviseSpellMixin {

    @ModifyConstant(method = "spawnPillarWalls", constant = @Constant(intValue = 20), remap = false)
    private int fortifications$reducePillarWallCount(int original) {
        return 10;
    }

    @ModifyConstant(method = "getPillars", constant = @Constant(floatValue = 20.0F), remap = false)
    private float fortifications$displayReducedPillarCount(float original) {
        return 10.0F;
    }
}
