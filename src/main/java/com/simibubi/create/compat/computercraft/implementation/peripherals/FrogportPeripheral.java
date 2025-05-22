package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

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
	public final Map<Integer, Map<String, ?>> list() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		for (int i = 0; i < blockEntity.inventory.getSlots(); i++) {
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getBasicDetails(blockEntity.inventory.getStackInSlot(i)));
			result.put(i + 1, details);
		}
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<String, ?> getItemDetail(int slot) throws LuaException {
		if (slot < 1 || slot > blockEntity.inventory.getSlots()) {
			throw new LuaException("Slot out of range (must be between 1 and " + blockEntity.inventory.getSlots() + ")");
		}

		ItemStack itemStack = blockEntity.inventory.getStackInSlot(slot - 1);
		if (itemStack.isEmpty()) {
			return null;
		}
		return VanillaDetailRegistries.ITEM_STACK.getDetails(itemStack);
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_Frogport";
	}

}
