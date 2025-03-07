package com.simibubi.create.compat.computercraft.implementation.peripherals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import java.util.Map;
import java.util.List;
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
	public final int request(IArguments arguments) throws LuaException {
		if (!(arguments.get(0) instanceof Map<?, ?>))
			return 0;
		Map<?, ?> filter = (Map<?, ?>) arguments.get(0);
		String address;
		// Computercraft has forced my hand to make this dollar store filter algo
		List<BigItemStack> validItems = new ArrayList<>();
		int totalItemCount = 0;
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
			if (ComputerUtil.bigItemStackToLuaTableFilter(entry, filter) > 0) {
				// limit the number of items pulled from the system equals to the requested
				// count parameter
				if (filter.containsKey("count")) {
					Object count = filter.get("count");
					if (count instanceof Double) {
						int maxCount = ((Double) count).intValue();
						int remainingCount = maxCount - totalItemCount;

						if (remainingCount > 0) {
							int itemsToAdd = Math.min(remainingCount, entry.count);
							entry.count = itemsToAdd;
							totalItemCount += itemsToAdd;
						} else
							break;
					}
				} else {
					totalItemCount += entry.count;
				}
				validItems.add(entry);
			}
		}
		if (arguments.get(1) instanceof String)
			address = arguments.getString(1);
		else
			address = "";

		PackageOrder order = new PackageOrder(validItems);
		blockEntity.broadcastPackageRequest(RequestType.RESTOCK, order, null, address);

		/*
		 * CatnipServices.NETWORK
		 * .sendToServer(new PackageOrderRequestPacket(blockEntity.getBlockPos(), new
		 * PackageOrder(itemsToOrder),
		 * address, false, new PackageOrder(stacks);
		 */
		return totalItemCount;
	}

	@LuaFunction(mainThread = true)
	public final void configure(IArguments arguments) throws LuaException {

		ArrayList<BigItemStack> list = new ArrayList<>();

		for (int i = 0; i <= 8; i++) {
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
			}
			ResourceLocation resourceLocation = ResourceLocation.parse(itemName);
			ItemLike item = BuiltInRegistries.ITEM.get(resourceLocation);
			ItemStack itemStack = new ItemStack(item);
			list.add(new BigItemStack(itemStack, 20));
		}

		this.blockEntity.encodedRequest = new PackageOrder(list);
		this.blockEntity.encodedRequestContext = new PackageOrder(list);
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(IArguments arguments) throws LuaException {
		Object argument = arguments.get(0);
		if (argument instanceof String) {
			blockEntity.encodedTargetAdress = (String) argument;
		} else if (argument == null) {
			blockEntity.encodedTargetAdress = "";
		} else {
			throw new LuaException("Argument must be string or nil");
		}
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
