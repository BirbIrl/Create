package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import org.jetbrains.annotations.NotNull;

public class FrogportPeripheral extends SyncedPeripheral<FrogportBlockEntity> {

	public FrogportPeripheral(FrogportBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(IArguments arguments) throws LuaException {
		Object argument = arguments.get(0);
		if (argument instanceof String) {
			blockEntity.addressFilter = (String) argument;
			blockEntity.filterChanged();
			blockEntity.notifyUpdate();
		} else if (argument == null) {
			blockEntity.addressFilter = "";
			blockEntity.filterChanged();
			blockEntity.notifyUpdate();
		} else {
			throw new LuaException("Argument must be string or nil");
		}
	}

	@LuaFunction(mainThread = true)
	public final String getAddress() throws LuaException {
		return blockEntity.addressFilter;
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_Frogport";
	}

}
