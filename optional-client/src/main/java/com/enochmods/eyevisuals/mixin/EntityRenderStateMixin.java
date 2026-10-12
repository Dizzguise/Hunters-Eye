package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.OwnerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements OwnerRenderState {
    @Unique private boolean eyeVisuals$localOwner;
    public boolean eyeVisuals$isLocalOwner() { return eyeVisuals$localOwner; }
    public void eyeVisuals$setLocalOwner(boolean localOwner) { eyeVisuals$localOwner = localOwner; }
}
