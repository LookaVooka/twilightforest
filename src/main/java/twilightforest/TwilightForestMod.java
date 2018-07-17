package twilightforest;

import net.minecraft.item.EnumRarity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twilightforest.advancements.TFAdvancements;
import twilightforest.compat.TFCompat;
import twilightforest.item.TFItems;
import twilightforest.loot.TFTreasure;
import twilightforest.network.TFPacketHandler;
import twilightforest.structures.hollowtree.TFHollowTreePieces;
import twilightforest.structures.start.StructureStartNothing;
import twilightforest.tileentity.*;
import twilightforest.world.WorldProviderTwilightForest;

@Mod( modid = TwilightForestMod.ID,
		name = "The Twilight Forest",
		version = TwilightForestMod.VERSION,
		acceptedMinecraftVersions = "[1.12.2]",
		dependencies = "after:ctm@[MC1.12-0.3.0.15,);before:immersiveengineering@[0.12-83,);before:tconstruct;required-after:forge@[14.23.3.2655,)",
		updateJSON = "https://raw.githubusercontent.com/TeamTwilight/twilightforest/1.12.x/update.json"
)
public class TwilightForestMod {
	public static final String ID = "twilightforest";
	public static final String VERSION = "@VERSION@";

	public static final String MODEL_DIR = "twilightforest:textures/model/";
	public static final String GUI_DIR = "twilightforest:textures/gui/";
	public static final String ENVRIO_DIR = "twilightforest:textures/environment/";
	public static final String ARMOR_DIR = "twilightforest:textures/armor/";
	public static final String ENFORCED_PROGRESSION_RULE = "tfEnforcedProgression";

	public static final int GUI_ID_UNCRAFTING = 1;
	public static final int GUI_ID_FURNACE = 2;

	public static DimensionType dimType;
	public static int backupdimensionID = -777;

	public static final Logger LOGGER = LogManager.getLogger(ID);

	private static final EnumRarity rarity = EnumHelper.addRarity("TWILIGHT", TextFormatting.DARK_GREEN, "Twilight");

	private static boolean compat = true;

	@Instance(ID)
	public static TwilightForestMod instance;

	@SidedProxy(clientSide = "twilightforest.client.TFClientProxy", serverSide = "twilightforest.TFCommonProxy")
	public static TFCommonProxy proxy;

	@SuppressWarnings("unused")
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (Loader.isModLoaded("sponge"))
			LOGGER.info("It looks like you have Sponge installed! You may notice Hydras spawning incorrectly with floating heads.\n" +
					"If so, please update Sponge to resolve this issue. Have fun!");

		registerTileEntities();
		dimType = DimensionType.register("twilight_forest", "_twilightforest", TFConfig.dimension.dimensionID, WorldProviderTwilightForest.class, false);

		// sounds on client, and whatever else needs to be registered pre-load
		proxy.preInit();

		TFTreasure.init();

		// just call this so that we register structure IDs correctly
		LOGGER.debug("There are " + TFFeature.values().length + " entries in TTFeature enum. Maximum structure size is " + TFFeature.getMaxSize());

		MapGenStructureIO.registerStructure(StructureStartNothing.class, "TFNothing");
		TFHollowTreePieces.registerPieces();

		compat = TFConfig.doCompat;

		if (compat) {
			try {
				TFCompat.preInitCompat();
			} catch (Exception e) {
				compat = false;
				TwilightForestMod.LOGGER.error(ID + " had an error loading preInit compatibility!");
				TwilightForestMod.LOGGER.catching(e.fillInStackTrace());
			}
		} else {
			TwilightForestMod.LOGGER.warn(ID + " is skipping! compatibility!");
		}
	}

	@SuppressWarnings("unused")
	@EventHandler
	public void init(FMLInitializationEvent evt) {
		TFItems.initRepairMaterials();
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
		TFPacketHandler.init();
		proxy.init();
		TFAdvancements.init();

		if (compat) {
			try {
				TFCompat.initCompat();
			} catch (Exception e) {
				compat = false;
				TwilightForestMod.LOGGER.error(ID + " had an error loading init compatibility!");
				TwilightForestMod.LOGGER.catching(e.fillInStackTrace());
			}
		}

		TFDataFixers.init();
	}

	@SuppressWarnings("unused")
	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		if (!DimensionManager.isDimensionRegistered(TFConfig.dimension.dimensionID)) {
			DimensionManager.registerDimension(TFConfig.dimension.dimensionID, TwilightForestMod.dimType);
		} else {
			TwilightForestMod.LOGGER.warn("Detected that the configured dimension id '{}' is being used. Using backup ID. It is recommended that you configure this mod to use a unique dimension ID.", TFConfig.dimension.dimensionID);
			DimensionManager.registerDimension(TwilightForestMod.backupdimensionID, TwilightForestMod.dimType);
			TFConfig.dimension.dimensionID = TwilightForestMod.backupdimensionID;
		}

		if (compat) {
			try {
				TFCompat.postInitCompat();
			} catch (Exception e) {
				TwilightForestMod.LOGGER.error(ID + " had an error loading postInit compatibility!");
				TwilightForestMod.LOGGER.catching(e.fillInStackTrace());
			}
		}
	}

	@SuppressWarnings("unused")
	@EventHandler
	public void startServer(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandTFFeature());
	}

	private void registerTileEntities() {
		proxy.registerCritterTileEntities();

		GameRegistry.registerTileEntity(TileEntityTFNagaSpawner.class, "twilightforest:naga_spawner");
		GameRegistry.registerTileEntity(TileEntityTFLichSpawner.class, "twilightforest:lich_spawner");
		GameRegistry.registerTileEntity(TileEntityTFHydraSpawner.class, "twilightforest:hydra_spawner");
		GameRegistry.registerTileEntity(TileEntityTFSmoker.class, "twilightforest:smoker");
		GameRegistry.registerTileEntity(TileEntityTFPoppingJet.class, "twilightforest:popping_jet");
		GameRegistry.registerTileEntity(TileEntityTFFlameJet.class, "twilightforest:flame_jet");
		GameRegistry.registerTileEntity(TileEntityTFTowerBuilder.class, "twilightforest:tower_builder");
		GameRegistry.registerTileEntity(TileEntityTFAntibuilder.class, "twilightforest:tower_reverter");
		GameRegistry.registerTileEntity(TileEntityTFTrophy.class, "twilightforest:trophy");
		GameRegistry.registerTileEntity(TileEntityTFTowerBossSpawner.class, "twilightforest:tower_boss_spawner");
		GameRegistry.registerTileEntity(TileEntityTFGhastTrapInactive.class, "twilightforest:ghast_trap_inactive");
		GameRegistry.registerTileEntity(TileEntityTFGhastTrapActive.class, "twilightforest:ghast_trap_active");
		GameRegistry.registerTileEntity(TileEntityTFCReactorActive.class, "twilightforest:carminite_reactor_active");
		GameRegistry.registerTileEntity(TileEntityTFKnightPhantomsSpawner.class, "twilightforest:knight_phantom_spawner");
		GameRegistry.registerTileEntity(TileEntityTFSnowQueenSpawner.class, "twilightforest:snow_queen_spawner");
		GameRegistry.registerTileEntity(TileEntityTFCinderFurnace.class, "twilightforest:cinder_furnace");
		GameRegistry.registerTileEntity(TileEntityTFMinoshroomSpawner.class, "twilightforest:minoshroom_spawner");
		GameRegistry.registerTileEntity(TileEntityTFAlphaYetiSpawner.class, "twilightforest:alpha_yeti_spawner"); //*/
	}

	public static EnumRarity getRarity() {
		return rarity != null ? rarity : EnumRarity.EPIC;
	}
}
