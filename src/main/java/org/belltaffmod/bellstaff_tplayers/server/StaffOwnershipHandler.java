package org.belltaffmod.bellstaff_tplayers.server;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.event.TickEvent;
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event){
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean lossTick = player.tickCount % BellstaffConstants.NON_OWNER_LOSS_INTERVAL_TICKS == 0;
        boolean recoveryTick = player.tickCount % BellstaffConstants.NON_OWNER_RECOVERY_INTERVAL_TICKS == 0;
        if (!lossTick && !recoveryTick) return;

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        AttributeModifier existing = maxHealth.getModifier(PENALTY_ID);
        double lost = (existing == null) ? 0.0D : -existing.getAmount();
        double step = BellstaffConstants.NON_OWNER_HEALTH_STEP;
        double newLost = lost;

        if (isPenalized(player)){
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

        if (player.getHealth() > player.getMaxHealth()){
            player.setHealth(player.getMaxHealth());
        }
    }

    private static boolean isPenalized(ServerPlayer player) {
        UUID owner = StaffOwnerData.get(player.server).getOwner();
        if (owner == null || owner.equals(player.getUUID())) return false;

        if (!BellstaffConstants.PENALIZE_CREATIVE_PLAYERS
                && (player.isCreative() || player.isSpectator())){
            return false;
        }

        return carriesStaff(player);
    }

    private static boolean carriesStaff(ServerPlayer player){
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof BellStaffItem) return true;
        }

        return player.containerMenu.getCarried().getItem() instanceof BellStaffItem;
    }
}
