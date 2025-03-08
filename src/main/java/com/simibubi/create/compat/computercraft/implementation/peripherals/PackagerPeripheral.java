package com.simibubi.create.compat.computercraft.implementation.peripherals;

import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;

import com.simibubi.create.content.logistics.BigItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

public class PackagerPeripheral extends SyncedPeripheral<PackagerBlockEntity> {

	public PackagerPeripheral(PackagerBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final int getItemCount() {
		return blockEntity.getAvailableItems().getTotalCount();
	}

	@LuaFunction(mainThread = true)
	public final boolean makePackage() {
		if (!blockEntity.heldBox.isEmpty())
			return false;
		blockEntity.activate(); // activate() doesn't return a value so i'm walking around it
		if (blockEntity.heldBox.isEmpty())
			return false;
		return true;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> list() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAvailableItems().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getBasicDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
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
	public final Map<Integer, Map<String, ?>> listDetailed() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAvailableItems().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> checkPackage() throws LuaException {
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
		return "Create_Packager";
	}

}
