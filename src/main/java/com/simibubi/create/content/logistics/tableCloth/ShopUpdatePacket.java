package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.AllPackets;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;

import net.minecraft.core.BlockPos;

public class ShopUpdatePacket extends BlockEntityDataPacket<TableClothBlockEntity> {

	public static final StreamCodec<ByteBuf, ShopUpdatePacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(ShopUpdatePacket::new, p -> p.pos);

	public ShopUpdatePacket(BlockPos pos) {
		super(pos);
	}

	@Override
	protected void handlePacket(TableClothBlockEntity be) {
		if (!be.hasLevel()) {
			return;
		}

		be.invalidateItemsForRender();
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.SHOP_UPDATE;
	}
}
