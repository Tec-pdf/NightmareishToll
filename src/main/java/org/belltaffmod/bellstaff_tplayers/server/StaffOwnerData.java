package org.belltaffmod.bellstaff_tplayers.server;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public class StaffOwnerData extends SavedData {

    private static final String DATA_NAME = Bellstaff_tplayers.MODID + "_owner";
    private static final String KEY_OWNER = "Owner";
    private static final String KEY_NAME = "OwnerName";

    private UUID owner;
    private String ownerName = "";

    public StaffOwnerData(){}

    public static StaffOwnerData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(StaffOwnerData::load, StaffOwnerData::new, DATA_NAME);
    }

    public static StaffOwnerData load(CompoundTag tag){
        StaffOwnerData data = new StaffOwnerData();
        if (tag.hasUUID(KEY_OWNER)){
            data.owner = tag.getUUID(KEY_OWNER);
            data.ownerName = tag.getString(KEY_NAME);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag){
        if (owner != null){
            tag.putUUID(KEY_OWNER, owner);
            tag.putString(KEY_NAME, ownerName);
        }

        return tag;
    }

    public UUID getOwner() { return owner; }

    public String getOwnerName() { return ownerName; }

    public void setOwner(UUID id, String name) {
        this.owner = id;
        this.ownerName = name;
        setDirty();
    }

    public void clearOwner() {
        this.owner = null;
        this.ownerName = "";
        setDirty();
    }
}
