package com.simibubi.create.compat.computercraft.implementation.peripherals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;

public class RedstoneRequesterPeripheral extends SyncedPeripheral<RedstoneRequesterBlockEntity> {

	// private final ScrollValueBehaviour targetSpeed;

	public RedstoneRequesterPeripheral(RedstoneRequesterBlockEntity blockEntity) {
		super(blockEntity);
		// this.targetSpeed = targetSpeed;
	}

	@LuaFunction(mainThread = true)
	public final void request() throws LuaException {
		blockEntity.triggerRequest();
	}

	@LuaFunction(mainThread = true)
	public final void configure(IArguments arguments) throws LuaException {
		List<BigItemStack> orderStacks = generateOrder(arguments);
		this.blockEntity.encodedRequest = PackageOrderWithCrafts.simple(orderStacks);
		this.blockEntity.notifyUpdate();
	}

	@LuaFunction(mainThread = true)
	public final void configureCraft(IArguments arguments) throws LuaException {
		int count = arguments.getInt(0);
		arguments = arguments.drop(1);

		List<BigItemStack> orderStacks = generateOrder(arguments);

		PackageOrder order = new PackageOrder(orderStacks);
		CraftingEntry orderContext = new CraftingEntry(new PackageOrder(orderStacks.stream()
				.map(stack -> new BigItemStack(stack.stack.copyWithCount(1)))
				.toList()), count);

		this.blockEntity.encodedRequest = new PackageOrderWithCrafts(order, List.of(orderContext));
		this.blockEntity.notifyUpdate();
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) throws LuaException {
		if (argument.isPresent()) {
			blockEntity.encodedTargetAdress = argument.get();
		} else {
			blockEntity.encodedTargetAdress = "";
		}
		this.blockEntity.notifyUpdate();
	}

	@LuaFunction(mainThread = true)
	public final String getAddress() throws LuaException {
		return blockEntity.encodedTargetAdress;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> getConfiguration() throws LuaException {
		List<BigItemStack> stacks = blockEntity.encodedRequest.stacks();
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		// Loop through the packageOrder get each bigItem stack
		//
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i).stack;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
			if (!details.get("name").equals("minecraft:air")) {
				details.put("count", stacks.get(i).count);
				result.put(i + 1, details); // +1 because lua
			}
		}
		return result;
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_RedstoneRequester";
	}

	private List<BigItemStack> generateOrder(IArguments arguments) throws LuaException {
		ArrayList<BigItemStack> list = new ArrayList<>();

		for (int i = 0; i <= 8; i++) {
			if (arguments.get(i) == null) {
				list.add(new BigItemStack(ItemStack.EMPTY, 1));
			} else {
				Map<?, ?> itemData = arguments.getTable(i);

				if (!(itemData instanceof Map)) {
					throw new LuaException("Table or nil expected for each item entry");
				}
				String itemName = "minecraft:air";
				if (itemData.get("name") instanceof String) {
					itemName = (String) itemData.get("name");
				}
				int count = 1;
				if (itemData.get("count") instanceof Number) {
					Object countObj = itemData.get("count");
					count = (countObj instanceof Number) ? ((Number) countObj).intValue() : 1;
					if (count > 256)
						throw new LuaException("Count for item " + itemName + " exceeds 256");
				}
				ResourceLocation resourceLocation = ResourceLocation.tryParse(itemName);
				ItemLike item = BuiltInRegistries.ITEM.get(resourceLocation);
				list.add(new BigItemStack(new ItemStack(item), count));
			}
		}

		return list;
	}
}
