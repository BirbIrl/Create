package com.simibubi.create.compat.computercraft.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.logistics.BigItemStack;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;

import net.neoforged.neoforge.items.IItemHandler;

public class ComputerUtil {

	// tldr: the computercraft api lets you parse items into lua-like-tables that cc
	// uses for all it's items. to keep consistency with the rest of the inventory
	// api in other parts of the mod i must do this terribleness. i am sorry.
	public static int bigItemStackToLuaTableFilter(BigItemStack entry, Map<?, ?> filter) throws LuaException {
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
				if (filterEnchantments.size() != itemEnchantments.size())
					return 0;
				Set<HashMap<String, ?>> matchedItemEnchantments = new HashSet<>();
				for (Map.Entry<String, ?> filterEnchantmentNode : filterEnchantments.entrySet()) {
					int filterMatches = 0;
					if (!(filterEnchantmentNode.getValue() instanceof Map<?, ?>)) {
						throw new LuaException(
							"Enchantments filter must be a table of enchant information like: \n{enchantments = { \n	{name = \"minecraft:sharpness\"} \n	{name = \"minecraft:protection\" \n level = 1\n	}\n}}");
					}
					@SuppressWarnings("unchecked")
					Map<String, ?> filterEnchantmentEntry = (Map<String, ?>) (filterEnchantmentNode.getValue());
					String filterEnchantmentName = (String) (filterEnchantmentEntry.get("name"));
					String filterEnchantmentDisplayName = (String) (filterEnchantmentEntry.get("displayName"));
					Double filterEnchantmentLevel = 0.0;
					boolean CheckEnchantmentName = false;
					boolean CheckEnchantmentDisplayName = false;
					boolean CheckEnchantmentLevel = false;
					if (filterEnchantmentEntry.get("level") instanceof Double) {
						filterEnchantmentLevel = (Double) (filterEnchantmentEntry.get("level"));
						CheckEnchantmentLevel = true;
					}
					if (filterEnchantmentEntry.get("name") instanceof String) {
						filterEnchantmentName = (String) (filterEnchantmentEntry.get("name"));
						CheckEnchantmentName = true;
					}
					if (filterEnchantmentEntry.get("displayName") instanceof String) {
						filterEnchantmentDisplayName = (String) (filterEnchantmentEntry.get("displayName"));
						CheckEnchantmentDisplayName = true;
					}
					for (HashMap<String, ?> itemEnchantmentEntry : itemEnchantments) {
						String itemEnchantmentName = (String) itemEnchantmentEntry.get("name");
						String itemEnchantmentDisplayName = (String) (itemEnchantmentEntry.get("displayName"));
						Integer itemEnchantmentLevel = (Integer) (itemEnchantmentEntry.get("level"));

						if (!matchedItemEnchantments.contains(itemEnchantmentEntry)
							&& (!CheckEnchantmentName || itemEnchantmentName.equals(filterEnchantmentName))
							&& (!CheckEnchantmentDisplayName
							|| (itemEnchantmentDisplayName.equals(filterEnchantmentDisplayName)))
							&& (!CheckEnchantmentLevel
							|| (itemEnchantmentLevel
							.doubleValue()) == filterEnchantmentLevel)) {
							matchedItemEnchantments.add(itemEnchantmentEntry); // one itemenchant per filter
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

	public static Map<Integer, Map<String, ?>> list(IItemHandler inventory) {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		var size = inventory.getSlots();
		for (var i = 0; i < size; i++) {
			var stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
		}

		return result;
	}
}
