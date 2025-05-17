package com.simibubi.create.compat.computercraft.implementation.peripherals;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

import org.jetbrains.annotations.Nullable;

public class StockTickerPeripheral extends SyncedPeripheral<StockTickerBlockEntity> {

	// private final ScrollValueBehaviour targetSpeed;

	public StockTickerPeripheral(StockTickerBlockEntity blockEntity) {
		super(blockEntity);
		// this.targetSpeed = targetSpeed;
	}

	@LuaFunction(mainThread = true)
	public final int getItemCount() {
		return blockEntity.getAccurateSummary().getTotalCount();
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> list() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getBasicDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> listDetailed() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

	// Looks for a list of items and only makes the request if ALL requests are satisfied
	// Accepts a table structured as:
	// {
	// 	{
	// 		name = "minecraft:stick",
	// 		count = 16
	// 	},
	// 	{
	// 		name = "minecraft:diamond",
	// 		count = 4
	// 	}
	// }
	// Second argument is optional and specifies the address to send the items to
	// The address defaults to ""
	@LuaFunction(mainThread = true)
	public final int request(IArguments arguments) throws LuaException {
		if (!(arguments.get(0) instanceof Map<?, ?> filterTable))
			throw new LuaException("Filter must be an array of items");

		for (Object key : filterTable.keySet())
			if (!(key instanceof Double)) throw new LuaException("Filter keys must be doubles (array indecies)");

		Map<Double, Map<String, Object>> filters = (Map<Double, Map<String, Object>>) filterTable;
		List<BigItemStack> validItems = new ArrayList<>();
		int totalItems = 0;

		// For each filter, check if the stack is valid and update counts accordingly
		List<BigItemStack> stacks = blockEntity.getAccurateSummary().getStacks();
		for (Map<String, Object> filter : filters.values()) {
			boolean findAll = !filter.containsKey("count");
			Integer targetCount = !findAll ? ((Number)filter.get("count")).intValue() : Integer.MAX_VALUE;
			for (BigItemStack stack : stacks) {
				int foundItems = ComputerUtil.bigItemStackToLuaTableFilter(stack, filter);
				if (foundItems > 0) {
					int toTake = Math.min(foundItems, targetCount);
					targetCount -= toTake;
					totalItems += toTake;
					filter.put("count", targetCount);
					stack.count = toTake;
					validItems.add(stack);
					if (targetCount <= 0) break;
				}
			}
			if (targetCount > 0 && !findAll) return 0; // Target count failed for a filter, so exit the process
		}

		String address = arguments.get(1) instanceof String ? arguments.getString(1) : "";
		PackageOrder order = new PackageOrder(validItems);
		blockEntity.broadcastPackageRequest(RequestType.RESTOCK, order, null, address);
		return totalItems;
	}

	@LuaFunction(mainThread = true)
	public Map<Integer, Map<String, ?>> listPaymentInventory() {
		return ComputerUtil.list(blockEntity.getReceivedPaymentsHandler());
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_StockTicker";
	}

	@Override
	public @Nullable Object getTarget() {
		return blockEntity.getReceivedPaymentsHandler();
	}
}
