package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.client.ConcealedRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements ConcealedRenderState {
    @Unique private boolean huntersEye$concealed;
    public boolean huntersEye$isConcealed() { return huntersEye$concealed; }
    public void huntersEye$setConcealed(boolean value) { huntersEye$concealed = value; }
}
