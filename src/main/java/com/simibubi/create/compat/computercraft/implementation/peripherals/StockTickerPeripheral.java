package com.simibubi.create.compat.computercraft.implementation.peripherals;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
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

public class StockTickerPeripheral extends SyncedPeripheral<StockTickerBlockEntity> {

	// private final ScrollValueBehaviour targetSpeed;

	public StockTickerPeripheral(StockTickerBlockEntity blockEntity) {
		super(blockEntity);
		// this.targetSpeed = targetSpeed;
	}

	// tldr: the computercraft api lets you parse items into lua-like-tables that cc
	// uses for all it's items. to keep consistency with the rest of the inventory
	// api in other parts of the mod i must do this terribleness. i am sorry.
	private int checkFilter(BigItemStack entry, Map<?, ?> filter) throws LuaException {
		Map<String, Object> details = new HashMap<>(
				VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
		details.put("count", entry.count);
		if (filter.containsKey("name"))
			if (filter.get("name") instanceof String) {
				String filterName = (String) filter.get("name");
				if (!filterName.contains(":"))
					filterName = "minecraft:" + filterName;
				if (!filterName.equals(details.get("name")))
					return 0;
			} else {
				throw new LuaException("Name must be a string");
			}
		// check the easy types
		Map<String, Class<?>> expectedTypes = new HashMap<>();
		expectedTypes.put("displayName", String.class);
		expectedTypes.put("nbt", String.class);
		expectedTypes.put("damage", Double.class);
		expectedTypes.put("durability", Double.class);
		expectedTypes.put("maxDamage", Double.class);
		expectedTypes.put("maxCount", Double.class);
		for (String key : expectedTypes.keySet()) {
			if (filter.containsKey(key)) {
				Object filterValue = filter.get(key);
				Class<?> expectedType = expectedTypes.get(key);
				if (expectedType.isInstance(filterValue)) {
					Object detailsValue = details.get(key);
					// some of these values are ints sometimes :tf:
					if (expectedType == Double.class && detailsValue instanceof Number) {
						detailsValue = ((Number) detailsValue).doubleValue();
					}
					if (!details.containsKey(key) || !filterValue.equals(detailsValue)) {
						return 0;
					}
				} else {
					throw new LuaException(key + " must be a " + expectedType.getSimpleName());
				}
			}
		}
		// java types dont mix well with lua tables at all
		if (filter.containsKey("tags")) {
			Object filterTagsObject = filter.get("tags");
			Object itemTagsObject = details.get("tags");
			if (filterTagsObject instanceof Map<?, ?> && itemTagsObject instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Boolean> filterTags = (Map<String, Boolean>) filterTagsObject;
				@SuppressWarnings("unchecked")
				Map<String, Boolean> itemTags = (Map<String, Boolean>) itemTagsObject;
				for (Map.Entry<String, Boolean> filterTagEntry : filterTags.entrySet()) {
					if (!(filterTagEntry.getValue() instanceof Boolean)) {
						throw new LuaException(
								"Tags filter must be a table of tags like: \n{tags = { \n	[\"minecraft:logs\"] = true} \n	{diamonds = true}\n}}");
					}
					int filterMatches = 0;
					for (Map.Entry<String, Boolean> itemTagEntry : itemTags.entrySet()) {
						if (itemTagEntry.getKey().equals(filterTagEntry.getKey())) {
							filterMatches++;
						}
					}
					if (filterMatches == 0) {
						return 0;
					}
				}
			} else {
				return 0;
			}
		}
		// avert your eyes, it only gets worse. When the computercraft api fetches
		// enchants of an item, it's an array list. when you submit a table from within
		// computercraft as an argument, it's always a hash map. Handling the mix of
		// both instead of converting because i felt like it's a good idea
		if (filter.containsKey("enchantments")) {
			Object filterEnchantmentsObject = filter.get("enchantments"); // HashMap
			Object itemEnchantmentsObject = details.get("enchantments"); // ArrayList
			// i might be doing a major skill issue here, idk i mainly do development in lua
			if (filterEnchantmentsObject instanceof Map<?, ?> && itemEnchantmentsObject instanceof ArrayList<?>) {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, ?>> filterEnchantments = (Map<String, Map<String, ?>>) filterEnchantmentsObject;
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String, ?>> itemEnchantments = (ArrayList<HashMap<String, ?>>) itemEnchantmentsObject;
				for (Map.Entry<String, ?> filterEnchantmentNode : filterEnchantments.entrySet()) {
					int filterMatches = 0;
					if (!(filterEnchantmentNode.getValue() instanceof Map<?, ?>)) {
						throw new LuaException(
								"Enchantments filter must be a table of enchant information like: \n{enchantments = { \n	{name = \"minecraft:sharpness\"} \n	{name = \"minecraft:protection\" \n level = 1\n	}\n}}");
					}
					@SuppressWarnings("unchecked")
					Map<String, ?> filterEnchantmentEntry = (Map<String, ?>) (filterEnchantmentNode.getValue());
					String filterEnchantmentName = (String) (filterEnchantmentEntry.get("name"));
					Double filterEnchantmentLevel = 0.0;
					boolean CheckEnchantmentLevel = false;
					if (filterEnchantmentEntry.get("level") instanceof Double) {
						filterEnchantmentLevel = (Double) (filterEnchantmentEntry.get("level"));
						CheckEnchantmentLevel = true;
					}
					for (HashMap<String, ?> itemEnchantmentEntry : itemEnchantments) {
						String itemEnchantmentName = (String) itemEnchantmentEntry.get("name");
						Integer itemEnchantmentLevel = (Integer) (itemEnchantmentEntry.get("level"));

						if (itemEnchantmentName.equals(filterEnchantmentName)
								&& (!CheckEnchantmentLevel
										|| (itemEnchantmentLevel.doubleValue()) == filterEnchantmentLevel)) {
							filterMatches++;
						}
					}
					if (filterMatches == 0) {
						return 0;
					}
				}
			} else {
				return 0;
			}
		}
		return entry.count;
	}

	@LuaFunction(mainThread = true)
	public final int getItemCount() {
		return blockEntity.getAccurateSummary().getTotalCount();
		// this.targetSpeed.setValue(speed);
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
		// return this.targetSpeed.getValue();
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
		// return this.targetSpeed.getValue();C
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
	 * of the tags if provided, excluisvely match all the enchants if provided and
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
			if (checkFilter(entry, filter) > 0) {
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

	@NotNull
	@Override
	public String getType() {
		return "Create_StockTicker";
	}

}
