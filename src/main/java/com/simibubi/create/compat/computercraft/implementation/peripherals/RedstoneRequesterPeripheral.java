package com.simibubi.create.compat.computercraft.implementation.peripherals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;

public class RedstoneRequesterPeripheral extends SyncedPeripheral<RedstoneRequesterBlockEntity> {

	// private final ScrollValueBehaviour targetSpeed;

	public RedstoneRequesterPeripheral(RedstoneRequesterBlockEntity blockEntity) {
		super(blockEntity);
		// this.targetSpeed = targetSpeed;
	}

	/*
	 * for every item in the netowrk, this will compare that item to the CC args
	 * filter, a table that looks something like this:
	 * {
	 * name = "minecraft:jungle_log",
	 * tags = {
	 * ["minecraft:logs"] = true
	 * },
	 * count = 5
	 * },
	 * and the second optional String arg which is the address:
	 * "home_address"
	 * (default value "")
	 *
	 * It then adds items that match the name if provided, nbt if provided, have all
	 * of the tags if provided, has all the enchants if provided and
	 * stops looking after adding items equal to count or finishing
	 * going through the summary.
	 * filter of {} requests all items from the network trollface.jpeg
	 */
	@LuaFunction(mainThread = true)
	public final void request() throws LuaException {
		blockEntity.triggerRequest();
	}

	@LuaFunction(mainThread = true)
	public final void configure(IArguments arguments) throws LuaException {

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

		this.blockEntity.encodedRequest = new PackageOrder(list);
		this.blockEntity.encodedRequestContext = new PackageOrder(list);
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

	@NotNull
	@Override
	public String getType() {
		return "Create_RedstoneRequester";
	}

}
