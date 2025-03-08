package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.Optional;

import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;

import org.jetbrains.annotations.NotNull;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public class RepackagerPeripheral extends SyncedPeripheral<RepackagerBlockEntity> {

	public RepackagerPeripheral(RepackagerBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) throws LuaException {
		if (argument.isPresent()) {
			blockEntity.CustomComputerAddress = argument.get();
			blockEntity.signBasedAddress = argument.get();
			blockEntity.hasCustomComputerAddress = true;
		} else {
			blockEntity.hasCustomComputerAddress = false;
		}
	}

	@LuaFunction(mainThread = true)
	public final String getAddress() throws LuaException {
		return blockEntity.signBasedAddress;
	}

	@LuaFunction(mainThread = true)
	public final boolean makePackage() {
		if (!blockEntity.heldBox.isEmpty())
			return false;
		blockEntity.activate();
		if (blockEntity.heldBox.isEmpty())
			return false;
		return true;
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_Repackager";
	}

}
