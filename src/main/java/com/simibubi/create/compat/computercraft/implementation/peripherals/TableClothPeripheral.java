package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;

import dan200.computercraft.api.lua.LuaFunction;
import org.jetbrains.annotations.Nullable;

public class TableClothPeripheral extends SyncedPeripheral<TableClothBlockEntity> {

    public TableClothPeripheral(TableClothBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "Create_TableCloth";
    }

    @LuaFunction(mainThread = true)
    public final boolean isShop() {
        return blockEntity.isShop();
    }

    @Override
    public @Nullable Object getTarget() {
        return isShop() ? ComputerUtil.NOOP_HANDLER : blockEntity.manuallyAddedItems;
    }
}
