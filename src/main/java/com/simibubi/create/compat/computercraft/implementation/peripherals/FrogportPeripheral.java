package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FrogportPeripheral extends SyncedPeripheral<FrogportBlockEntity> {

	public FrogportPeripheral(FrogportBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(String address) throws LuaException {
		blockEntity.addressFilter = address;
		blockEntity.filterChanged();
		blockEntity.notifyUpdate();
	}

	@LuaFunction(mainThread = true)
	public final String getAddress() throws LuaException {
		return blockEntity.addressFilter;
	}


	@LuaFunction(mainThread = true)
	public Map<Integer, Map<String, ?>> list() {
		return ComputerUtil.list(blockEntity.inventory);
	}

	@LuaFunction(mainThread = true)
	public Map<String, ?> getItemDetail(int slot) throws LuaException {
		return ComputerUtil.getItemDetail(blockEntity.inventory, slot);
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_Frogport";
	}

}
