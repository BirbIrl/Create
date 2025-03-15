package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;

import dan200.computercraft.api.peripheral.IComputerAccess;

import org.jetbrains.annotations.NotNull;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

public class RepackagerPeripheral extends SyncedPeripheral<RepackagerBlockEntity> {

	public RepackagerPeripheral(RepackagerBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void attach(@NotNull IComputerAccess computer) {
		super.attach(computer);
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}

	@Override
	public void detach(@NotNull IComputerAccess computer) {
		super.detach(computer);
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) {
		if (argument.isPresent()) {
			blockEntity.customComputerAddress = argument.get();
			blockEntity.signBasedAddress = argument.get();
			blockEntity.hasCustomComputerAddress = true;
		} else {
			blockEntity.customComputerAddress = "";
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

	@LuaFunction(mainThread = true)
	public final String getPackageAddress() throws LuaException {
		if (!blockEntity.heldBox.isEmpty())
			return PackageItem.getAddress(blockEntity.heldBox);
		return null;
	}

	@LuaFunction(mainThread = true)
	public final boolean setPackageAddress(Optional<String> argument) {
		if (!blockEntity.heldBox.isEmpty()) {
			if (argument.isPresent()) {
				PackageItem.addAddress(blockEntity.heldBox, argument.get());
			} else {
				PackageItem.addAddress(blockEntity.heldBox, "");
			}
			return true;
		}
		return false;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> getPackageItems() throws LuaException {
		ItemStack box = blockEntity.heldBox;
		if (box.isEmpty() && !PackageItem.isPackage(box))
			return null;
		ItemStackHandler results = PackageItem.getContents(box);
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		for (int i = 0; i < results.getSlots(); i++) {
			ItemStack stack = results.getStackInSlot(i);
			if (!stack.isEmpty()) {
				Map<String, Object> details = new HashMap<>(
						VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
				result.put(i + 1, details); // +1 because lua
			}
		}
		return result;
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_Repackager";
	}

}
