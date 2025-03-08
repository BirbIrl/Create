package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class TableClothPeripheral extends SyncedPeripheral<TableClothBlockEntity> {

	public TableClothPeripheral(TableClothBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction(mainThread = true)
	public final boolean isShop() {
		return blockEntity.isShop();
	}

	@LuaFunction(mainThread = true)
	public final String getAddress() throws LuaException {
		return blockEntity.requestData.encodedTargetAdress;
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) throws LuaException {
		if (argument.isPresent())
			blockEntity.requestData.encodedTargetAdress = argument.get();
		else
			blockEntity.requestData.encodedTargetAdress = "";

	}

	@LuaFunction(mainThread = true)
	public final Map<String, ?> getPriceTagItem() throws LuaException {
		return VanillaDetailRegistries.ITEM_STACK.getDetails(blockEntity.priceTag.getFilter());
	}

	@LuaFunction(mainThread = true)
	public final boolean setPriceTagItem(Optional<String> itemName) throws LuaException {
		ResourceLocation resourceLocation = ResourceLocation.tryParse("minecraft:air");
		if (itemName.isPresent())
			resourceLocation = ResourceLocation.tryParse(itemName.get());
		ItemLike item = BuiltInRegistries.ITEM.get(resourceLocation);
		blockEntity.priceTag.setFilter(new ItemStack(item));
		return true;
	}

	@LuaFunction(mainThread = true)
	public final int getPriceTagCount() throws LuaException {
		return blockEntity.priceTag.count;
	}

	@LuaFunction(mainThread = true)
	public final void setPriceTagCount(Optional<Double> argument) throws LuaException {
		if (argument.isPresent())
			blockEntity.priceTag.count = (Math.max(1, Math.min(100, argument.get().intValue())));
		else
			blockEntity.priceTag.count = 1;
		this.blockEntity.notifyUpdate();
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> getWares() throws LuaException {
		List<BigItemStack> wares = blockEntity.requestData.encodedRequest.stacks();
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		for (int i = 0; i < wares.size(); i++) {
			ItemStack stack = wares.get(i).stack;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
			details.put("count", wares.get(i).count);
			result.put(i + 1, details); // +1 because lua
		}
		return result;
	}

	/*
	 * this functionally works, but none of us have been able to figure out how to
	 * visually update the store's wares without reloading the chunk. The render
	 * pipeline is difficult :(
	 *
	 * @LuaFunction(mainThread = true)
	 * public final void setWares(IArguments arguments) throws LuaException {
	 * if (!blockEntity.manuallyAddedItems.isEmpty())
	 * throw new LuaException("Tablecloth isn't empty.");
	 * ArrayList<BigItemStack> list = new ArrayList<>();
	 * for (int i = 0; i <= 8; i++) {
	 * if (arguments.get(i) != null) {
	 * Map<?, ?> itemData = arguments.getTable(i);
	 *
	 * if (!(itemData instanceof Map)) {
	 * throw new LuaException("Table or nil expected for each item entry");
	 * }
	 * String itemName = "minecraft:air";
	 * if (itemData.get("name") instanceof String) {
	 * itemName = (String) itemData.get("name");
	 * }
	 * int count = 1;
	 * if (itemData.get("count") instanceof Number) {
	 * Object countObj = itemData.get("count");
	 * count = (countObj instanceof Number) ? ((Number) countObj).intValue() : 1;
	 * if (count > 256)
	 * throw new LuaException("Count for item " + itemName + " exceeds 256");
	 * }
	 * ResourceLocation resourceLocation = ResourceLocation.tryParse(itemName);
	 * ItemLike item = BuiltInRegistries.ITEM.get(resourceLocation);
	 * list.add(new BigItemStack(new ItemStack(item), count));
	 * }
	 * }
	 * blockEntity.requestData.encodedRequest = new PackageOrder(list);
	 * blockEntity.requestData.encodedRequestContext = new PackageOrder(list);
	 * blockEntity.notifyUpdate();
	 * }
	 */

	@Override
	public String getType() {
		return "Create_TableCloth";
	}

	@Override
	public @Nullable Object getTarget() {
		return isShop() ? ComputerUtil.NOOP_HANDLER : blockEntity.manuallyAddedItems;
	}
}
