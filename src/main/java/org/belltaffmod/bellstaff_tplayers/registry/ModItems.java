package org.belltaffmod.bellstaff_tplayers.registry;

import net.minecraft.world.item.Rarity;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.belltaffmod.bellstaff_tplayers.item.BellStaffItem;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS=
            DeferredRegister.create(ForgeRegistries.ITEMS, Bellstaff_tplayers.MODID);

    public static final RegistryObject<Item> BELL_SHARD = ITEMS.register(
            "bell_shard",
            () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> BELL_METAL_BLOCK_ITEM = ITEMS.register(
            "bell_metal_block",
            () -> new BlockItem(ModBlocks.BELL_METAL_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> BELLSTAFF = ITEMS.register(
            "bellstaff",
            () -> {
                return new BellStaffItem(new
                        Item.Properties().rarity(Rarity.RARE));
            }
    );
}
