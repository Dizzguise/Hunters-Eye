package com.enochmods.hunterseye;

import java.util.Set;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;

/** Full-mode combat guards. Eligibility is read fresh without damage-hook state mutations. */
public final class PhantomCombat {
    private static final Set<String> ITEMS=Set.of("trident","snowball","egg","wind_charge","splash_potion",
            "lingering_potion","firework_rocket","fire_charge","flint_and_steel","lava_bucket","tnt","end_crystal");
    private static final Set<String> PROJECTILES=Set.of("arrow","spectral_arrow","trident","firework_rocket",
            "snowball","egg","wind_charge","breeze_wind_charge","fireball","small_fireball","dragon_fireball",
            "wither_skull","llama_spit","shulker_bullet","splash_potion","lingering_potion");
    private PhantomCombat() {}
    public static boolean isOffensiveItem(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem||ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }
    public static boolean blockedOwner(Entity entity) {
        for(int depth=0;entity!=null&&depth<8;depth++) {
            if(HunterEyePower.blocksOutgoingDamage(entity)) return true;
            if(!(entity instanceof TraceableEntity traceable)) break;
            Entity owner=traceable.getOwner(); if(owner==entity) break; entity=owner;
        }
        return false;
    }
    public static boolean blocksDamage(Entity victim,DamageSource source) {
        return blockedOwner(source.getEntity())||blockedOwner(source.getDirectEntity());
    }
    public static boolean blockedProjectile(Projectile projectile) {
        return PROJECTILES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).getPath())&&blockedOwner(projectile.getOwner());
    }
    /** A previously thrown survival trident is equipment, not disposable ammunition. */
    public static void removeActiveProjectile(Projectile projectile) {
        if(projectile.isRemoved()) return;
        if(projectile instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident trident
                &&trident.pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED
                &&trident.getOwner() instanceof ServerPlayer owner) {
            ItemStack recovered=trident.getPickupItemStackOrigin().copy();
            projectile.discard();
            owner.getInventory().placeItemBackInInventory(recovered);
        } else projectile.discard();
    }
    public static void registerInteractions() {
        AttackEntityCallback.EVENT.register((player,level,hand,target,hit)->HunterEyePower.blocksOutgoingDamage(player)?deny(player):InteractionResult.PASS);
        UseItemCallback.EVENT.register((player,level,hand)->HunterEyePower.blocksOutgoingDamage(player)&&isOffensiveItem(player.getItemInHand(hand))?deny(player):InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player,level,hand,target,hit)->HunterEyePower.blocksOutgoingDamage(player)&&isOffensiveItem(player.getItemInHand(hand))?deny(player):InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!HunterEyePower.blocksOutgoingDamage(player)) return InteractionResult.PASS;
            var block=level.getBlockState(hit.getBlockPos()).getBlock();
            boolean explosiveBed=block instanceof BedBlock
                    &&level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE,hit.getBlockPos()).explodes();
            boolean explosiveAnchor=block instanceof RespawnAnchorBlock&&level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    &&!RespawnAnchorBlock.canSetSpawn(serverLevel,hit.getBlockPos());
            if(!isOffensiveItem(player.getItemInHand(hand))&&!explosiveBed&&!explosiveAnchor) return InteractionResult.PASS;
            if(player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level,hit.getBlockPos()));
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level,hit.getBlockPos().relative(hit.getDirection())));
            }
            return deny(player);
        });
    }
    private static InteractionResult deny(Entity player) {
        HunterEyePower.offensiveAction(player);
        if(player instanceof ServerPlayer serverPlayer) serverPlayer.inventoryMenu.sendAllDataToRemote();
        return InteractionResult.FAIL;
    }
}
