package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class FrogportPeripheral extends SyncedPeripheral<FrogportBlockEntity> {

	public FrogportPeripheral(FrogportBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) throws LuaException {
		if (argument.isPresent()) {
			blockEntity.addressFilter = argument.get();
			blockEntity.filterChanged();
			blockEntity.notifyUpdate();
		} else {
			blockEntity.addressFilter = "";
			blockEntity.filterChanged();
			blockEntity.notifyUpdate();
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
