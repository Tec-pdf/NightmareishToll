package org.belltaffmod.bellstaff_tplayers.item;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class BellStaffTiers implements Tier{
    public static final BellStaffTiers INSTANCE = new BellStaffTiers();

    @Override
    public int getUses() {
        return BellstaffConstants.DURABILITY;
    }

    @Override
    public float getSpeed(){
        return BellstaffConstants.MINING_SPEED;
    }

    @Override
    public float getAttackDamageBonus(){
        return 0.0F;
    }

    @Override
    public int getLevel(){
        return BellstaffConstants.HARVEST_LEVEL;
    }

    @Override
    public int getEnchantmentValue(){
        return BellstaffConstants.ENCHANTABILITY;
    }

    @Override
    public Ingredient getRepairIngredient(){
        return Ingredient.of(ModItems.BELL_SHARD.get()); //Concertar na bigorna
    }
}
