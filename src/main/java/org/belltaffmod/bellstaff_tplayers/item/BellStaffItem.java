package org.belltaffmod.bellstaff_tplayers.item;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.server.BellStaffHitHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class BellStaffItem extends SwordItem {

    public BellStaffItem(Item.Properties properties) {
        super(BellStaffTiers.INSTANCE,
                BellstaffConstants.PHYSICAL_DAMAGE - 1,       // -1: a mão do jogador já dá 1
                BellstaffConstants.ATTACK_SPEED_MODIFIER,
                properties);
    }

    // chamado depois que o golpe físico acertou com sucesso.
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        // a lógica de verdade só roda no SERVIDOR.
        if (!attacker.level().isClientSide) {
            boolean critical = BellStaffHitHandler.consumeCritical(attacker);
            BellStaffHitHandler.onHit(attacker, target, critical);
        }
        return result;
    }

    // aceita TODOS os encantamentos do mod Malum na mesa de encantamento.
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (id != null && id.getNamespace().equals(BellstaffConstants.MALUM_MOD_ID)) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
}