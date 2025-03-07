package com.simibubi.create.foundation.events;

import com.simibubi.create.Create;
import com.simibubi.create.compat.trainmap.TrainMapSync;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler;
import com.simibubi.create.content.contraptions.minecart.CouplingPhysics;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.equipment.zapper.ZapperInteractionHandler;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import com.simibubi.create.content.kinetics.drill.CobbleGenOptimisation;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.content.logistics.crate.CreativeCrateBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import com.simibubi.create.content.trains.entity.CarriageEntityHandler;
import com.simibubi.create.foundation.data.RuntimeDataGenerator;
import com.simibubi.create.foundation.pack.DynamicPack;
import com.simibubi.create.foundation.pack.DynamicPackSource;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.TickBasedCache;
import com.simibubi.create.infrastructure.command.AllCommands;

import net.createmod.catnip.data.WorldAttached;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent event) {
		if (event.phase == Phase.START)
			return;
		Create.SCHEMATIC_RECEIVER.tick();
		Create.LAGGER.tick();
		ServerSpeedProvider.serverTick();
		Create.RAILWAYS.sync.serverTick();
		TrainMapSync.serverTick(event);
		ServerChainConveyorHandler.tick();
		TickBasedCache.tick();
	}

	@SubscribeEvent
	public static void onChunkUnloaded(ChunkEvent.Unload event) {
		CapabilityMinecartController.onChunkUnloaded(event);
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		ToolboxHandler.playerLogin(player);
		Create.RAILWAYS.playerLogin(player);
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		Create.RAILWAYS.playerLogout(player);
	}

	@SubscribeEvent
	public static void onServerWorldTick(LevelTickEvent event) {
		if (event.phase == Phase.START)
			return;
		if (event.side == LogicalSide.CLIENT)
			return;
		Level world = event.level;
		ContraptionHandler.tick(world);
		CapabilityMinecartController.tick(world);
		CouplingPhysics.tick(world);
		LinkedControllerServerHandler.tick(world);
		ControlsServerHandler.tick(world);
		Create.RAILWAYS.tick(world);
		Create.LOGISTICS.tick(world);
	}

	@SubscribeEvent
	public static void onUpdateLivingEntity(LivingTickEvent event) {
		LivingEntity entityLiving = event.getEntity();
		Level world = entityLiving.level();
		if (world == null)
			return;
		ContraptionHandler.entitiesWhoJustDismountedGetSentToTheRightLocation(entityLiving, world);
		ToolboxHandler.entityTick(entityLiving, world);
	}

	@SubscribeEvent
	public static void onEntityAdded(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		Level world = event.getLevel();
		ContraptionHandler.addSpawnedContraptionsToCollisionList(entity, world);
	}

	@SubscribeEvent
	public static void onEntityAttackedByPlayer(AttackEntityEvent event) {
		WrenchItem.wrenchInstaKillsMinecarts(event);
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		AllCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onEntityEnterSection(EntityEvent.EnteringSection event) {
		CarriageEntityHandler.onEntityEnterSection(event);
	}

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent event) {
		event.addListener(RecipeFinder.LISTENER);
		event.addListener(BeltHelper.LISTENER);
	}

	@SubscribeEvent
	public static void serverStopping(ServerStoppingEvent event) {
		Create.SCHEMATIC_RECEIVER.shutdown();
	}

	@SubscribeEvent
	public static void onLoadWorld(LevelEvent.Load event) {
		LevelAccessor world = event.getLevel();
		Create.REDSTONE_LINK_NETWORK_HANDLER.onLoadWorld(world);
		Create.TORQUE_PROPAGATOR.onLoadWorld(world);
		Create.RAILWAYS.levelLoaded(world);
		Create.LOGISTICS.levelLoaded(world);
	}

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		LevelAccessor world = event.getLevel();
		Create.REDSTONE_LINK_NETWORK_HANDLER.onUnloadWorld(world);
		Create.TORQUE_PROPAGATOR.onUnloadWorld(world);
		WorldAttached.invalidateWorld(world);
		CobbleGenOptimisation.invalidateWorld(world);
	}

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		CapabilityMinecartController.attach(event);
	}

	@SubscribeEvent
	public static void startTracking(PlayerEvent.StartTracking event) {
		CapabilityMinecartController.startTracking(event);
	}

	public static void leftClickEmpty(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof ZapperItem) {
			ZapperInteractionHandler.trySelect(stack, player);
		}
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

		@SubscribeEvent
		public static void registerCapabilities(RegisterCapabilitiesEvent event) {
			event.register(CapabilityMinecartController.class);
		}

		@SubscribeEvent
		public static void addPackFinders(AddPackFindersEvent event) {
			// Uncomment and rename pack to add built in resource packs
//			if (event.getPackType() == PackType.CLIENT_RESOURCES) {
//				IModFileInfo modFileInfo = ModList.get().getModFileById(Create.ID);
//				if (modFileInfo == null) {
//					Create.LOGGER.error("Could not find Create mod file info; built-in resource packs will be missing!");
//					return;
//				}
//				IModFile modFile = modFileInfo.getFile();
//				event.addRepositorySource(consumer -> {
//                    Pack pack = Pack.readMetaAndCreate(Create.asResource("legacy_copper").toString(), Component.literal("Create Legacy Copper"), false, id -> new ModFilePackResources(id, modFile, "resourcepacks/legacy_copper"), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
//					if (pack != null) {
//						consumer.accept(pack);
//					}
//				});
//			}

			if (event.getPackType() == PackType.SERVER_DATA) {
				DynamicPack dynamicPack = new DynamicPack("create:dynamic_data", PackType.SERVER_DATA);
				RuntimeDataGenerator.insertIntoPack(dynamicPack);
				event.addRepositorySource(new DynamicPackSource("create:dynamic_data", PackType.SERVER_DATA, Pack.Position.BOTTOM, dynamicPack));
			}
		}

		@net.neoforged.bus.api.SubscribeEvent
		public static void onRegisterMapDecorationRenderers(RegisterMapDecorationRenderersEvent event) {
			event.register(AllMapDecorationTypes.STATION_MAP_DECORATION.value(), new StationMapDecorationRenderer());
		}

		@net.neoforged.bus.api.SubscribeEvent
		public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
			ChuteBlockEntity.registerCapabilities(event);
			SmartChuteBlockEntity.registerCapabilities(event);
			BeltBlockEntity.registerCapabilities(event);
			BasinBlockEntity.registerCapabilities(event);
			BeltTunnelBlockEntity.registerCapabilities(event);
			BrassTunnelBlockEntity.registerCapabilities(event);
			CreativeCrateBlockEntity.registerCapabilities(event);
			CrushingWheelControllerBlockEntity.registerCapabilities(event);
			ToolboxBlockEntity.registerCapabilities(event);
			DeployerBlockEntity.registerCapabilities(event);
			DepotBlockEntity.registerCapabilities(event);
			PortableFluidInterfaceBlockEntity.registerCapabilities(event);
			SpoutBlockEntity.registerCapabilities(event);
			PortableItemInterfaceBlockEntity.registerCapabilities(event);
			SawBlockEntity.registerCapabilities(event);
			EjectorBlockEntity.registerCapabilities(event);
			FluidTankBlockEntity.registerCapabilities(event);
			CreativeFluidTankBlockEntity.registerCapabilities(event);
			HosePulleyBlockEntity.registerCapabilities(event);
			ItemDrainBlockEntity.registerCapabilities(event);
			ItemVaultBlockEntity.registerCapabilities(event);
			MechanicalCrafterBlockEntity.registerCapabilities(event);
			MillstoneBlockEntity.registerCapabilities(event);
			StressGaugeBlockEntity.registerCapabilities(event);
			SpeedGaugeBlockEntity.registerCapabilities(event);
			StationBlockEntity.registerCapabilities(event);
			SpeedControllerBlockEntity.registerCapabilities(event);
			SequencedGearshiftBlockEntity.registerCapabilities(event);
			DisplayLinkBlockEntity.registerCapabilities(event);
			StockTickerBlockEntity.registerCapabilities(event);
			RedstoneRequesterBlockEntity.registerCapabilities(event);
			PackagerBlockEntity.registerCapabilities(event);
			RepackagerBlockEntity.registerCapabilities(event);
			PostboxBlockEntity.registerCapabilities(event);
			FrogportBlockEntity.registerCapabilities(event);
			TableClothBlockEntity.registerCapabilities(event);
		}
	}
}
