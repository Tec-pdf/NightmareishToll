package org.belltaffmod.bellstaff_tplayers.server;


import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import org.belltaffmod.bellstaff_tplayers.item.BellStaffItem;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Bellstaff_tplayers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StaffOwnershipHandler {

    private static final UUID PENALTY_ID = UUID.fromString("7d3e5a10-2c4b-4f8e-b1a6-9e0c52d7f348");
    private static final String PENALTY_NAME = "bellstaff_tplayers:not_owner_penalty";

    private StaffOwnershipHandler() {}

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event){
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;

        boolean lossTick = entity.tickCount % BellstaffConstants.NON_OWNER_LOSS_INTERVAL_TICKS == 0;
        boolean recoveryTick = entity.tickCount % BellstaffConstants.NON_OWNER_RECOVERY_INTERVAL_TICKS == 0;
        if (!lossTick && !recoveryTick) return;

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        AttributeModifier existing = maxHealth.getModifier(PENALTY_ID);
        boolean carries = carriesStaff(entity);
        if(!carries && existing == null) return;

        double lost = (existing == null) ? 0.0D : -existing.getAmount();
        double step = BellstaffConstants.NON_OWNER_HEALTH_STEP;
        double newLost = lost;

        if (carries && isSubjectToPenalty(entity, level.getServer())){
            if (lossTick){
                double limit = Math.max(0.0D,
                        (maxHealth.getValue() + lost) - BellstaffConstants.NON_OWNER_MIN_MAX_HEALTH);
                if (lost < limit){
                    newLost = Math.min(lost + step, limit);
                }
            }
        } else if (recoveryTick && lost > 0.0D){
            newLost = Math.max(0.0D, lost - step);
        }

        if (newLost == lost) return;

        maxHealth.removeModifier(PENALTY_ID);
        if (newLost > 0.0D) {
            maxHealth.addPermanentModifier(new AttributeModifier(
                    PENALTY_ID, PENALTY_NAME, -newLost, AttributeModifier.Operation.ADDITION));
        }

        if (entity.getHealth() > entity.getMaxHealth()){
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private static boolean isSubjectToPenalty(LivingEntity entity, MinecraftServer server){
        UUID owner = StaffOwnerData.get(server).getOwner();
        if (owner == null || owner.equals(entity.getUUID())) return false;

        if (entity instanceof Player player){
            return BellstaffConstants.PENALIZE_CREATIVE_PLAYERS
                    || !(player.isCreative() || player.isSpectator());
        }

        return BellstaffConstants.PENALIZE_ENTITIES;
    }

    private static boolean carriesStaff(LivingEntity entity){
        if (entity instanceof Player player){
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++){
                if (inventory.getItem(i).getItem() instanceof BellStaffItem) return true;
            }

            return player.containerMenu.getCarried().getItem() instanceof BellStaffItem;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()){
            if (entity.getItemBySlot(slot).getItem() instanceof BellStaffItem) return true;
        }

        return false;
    }
}