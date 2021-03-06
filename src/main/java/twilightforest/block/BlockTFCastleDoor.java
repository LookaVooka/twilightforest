package twilightforest.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twilightforest.world.ChunkGeneratorTFBase;
import twilightforest.world.TFWorld;

import java.util.Random;

public class BlockTFCastleDoor extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty VANISH = BooleanProperty.create("vanish");
	private static int lockIndex;

	private static final VoxelShape REAPPEARING_BB = VoxelShapes.create(new AxisAlignedBB(0.375F, 0.375F, 0.375F, 0.625F, 0.625F, 0.625F));

	public BlockTFCastleDoor(int lock) {
		super(Properties.create(Material.ROCK, MaterialColor.CYAN).hardnessAndResistance(100.0F, 35.0F));

		//this.setBlockUnbreakable();
		//this.setResistance(Float.MAX_VALUE);

		lockIndex = lock;
		//this.lightOpacity = isVanished ? 0 : 255;

		this.setDefaultState(stateContainer.getBaseState().with(ACTIVE, false).with(VANISH, false));
	}

	@Override
	@Deprecated
	public Material getMaterial(BlockState state) {
		return state.get(VANISH) ? Material.GLASS : Material.ROCK;
	}

	@Override
	@Deprecated
	public MaterialColor getMaterialColor(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return state.get(VANISH) ? MaterialColor.AIR : MaterialColor.CYAN;
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE, VANISH);
	}

	//TODO: Find out what this does now
//	@Override
//	public boolean isSolid(BlockState state) {
//		return !state.get(VANISH);
//	}

	@Override
	@Deprecated
	public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return state.get(VANISH) ? VoxelShapes.empty() : super.getCollisionShape(state, world, pos, context);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return state.get(VANISH) ? REAPPEARING_BB : super.getShape(state, world, pos, context);
	}

	@Override
	@Deprecated
	public ActionResultType onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		return onActivation(world, pos, state);
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		if (!(block instanceof BlockTFCastleDoor) && world.isBlockPowered(pos)) {
			onActivation(world, pos, state);
		}
	}

	private ActionResultType onActivation(World world, BlockPos pos, BlockState state) {

		if (state.get(VANISH) || state.get(ACTIVE)) return ActionResultType.FAIL;

		if (isBlockLocked(world, pos)) {
			world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0F, 0.3F);
		} else {
			changeToActiveBlock(world, pos, state);
		}
		return ActionResultType.PASS;
	}

	private static void changeToActiveBlock(World world, BlockPos pos, BlockState originState) {
		changeActiveState(world, pos, true, originState);
		playVanishSound(world, pos);

		//world.scheduleUpdate(pos, originState.getBlock(), 2 + world.rand.nextInt(5));
	}

	private static void changeActiveState(World world, BlockPos pos, boolean active, BlockState originState) {
		if (originState.getBlock() instanceof BlockTFCastleDoor) {
			world.setBlockState(pos, originState.with(ACTIVE, active), 3);
			//world.markBlockRangeForRenderUpdate(pos, pos);
			//world.notifyNeighborsRespectDebug(pos, originState.getBlock(), false);
		}
	}

	private static boolean isBlockLocked(World world, BlockPos pos) {
		// check if we are in a structure, and if that structure says that we are locked
		if (!world.isRemote) {
			ChunkGeneratorTFBase generator = TFWorld.getChunkGenerator(world);
			return generator != null /*&& generator.isStructureLocked(pos, lockIndex)*/;
		}
		return false;
	}

	@Override
	public int tickRate(IWorldReader world) {
		return 5;
	}

	@Override // todo 1.10 recheck all of this
	@Deprecated
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.isRemote) return;

		if (state.get(VANISH)) {
			if (state.get(ACTIVE)) {
				world.setBlockState(pos, world.getBlockState(pos).with(VANISH, true));
				playVanishSound(world, pos);
			} else {
				changeToActiveBlock(world, pos, state);
			}
		} else {
			// if we have an active castle door, turn it into a vanished door block
			if (state.get(ACTIVE)) {
				world.setBlockState(pos, world.getBlockState(pos).with(VANISH, false));
				//world.scheduleUpdate(pos, getOtherBlock(this), 80);

				playReappearSound(world, pos);

				//this.sendAnnihilateBlockPacket(world, pos);

				// activate all adjacent inactive doors
				for (Direction e : Direction.values()) {
					checkAndActivateCastleDoor(world, pos.offset(e));
				}
			}
			// inactive solid door blocks we don't care about updates
		}
	}

	//TODO: Move to loot table
	//	@Override
//	public Item getItemDropped(BlockState state, Random rand, int fortune) {
//		return TFItems.castle_door;
//	}
//
//	@Override
//	public int damageDropped(BlockState state) {
//		return state.getValue(LOCK_INDEX);
//	}

	//TODO: How to packet
//	private void sendAnnihilateBlockPacket(World world, BlockPos pos) {
//		IMessage message = new PacketAnnihilateBlock(pos);
//		NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64);
//		TFPacketHandler.CHANNEL.sendToAllAround(message, targetPoint);
//	}

	private static void playVanishSound(World world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.125f, world.rand.nextFloat() * 0.25F + 1.75F);
	}

	private static void playReappearSound(World world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.125f, world.rand.nextFloat() * 0.25F + 1.25F);
	}

	/**
	 * If the targeted block is a vanishing block, activate it
	 */
	public static void checkAndActivateCastleDoor(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		if (state.getBlock() instanceof BlockTFCastleDoor && !state.get(ACTIVE) && !isBlockLocked(world, pos)) {
			changeToActiveBlock(world, pos, state);
		}
//		if (state.getBlock() == TFBlocks.castle_door_vanished && !state.getValue(ACTIVE) && !isBlockLocked(world, pos)) {
//    		changeToActiveBlock(world, x, y, z, meta);
//    	}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(ACTIVE)) {
			for (int i = 0; i < 1; ++i) {
				//this.sparkle(world, x, y, z, random);
			}
		}
	}

	// [VanillaCopy] BlockRedStoneOre.spawnParticles with own rand
	//@SuppressWarnings("unused")
//	private void sparkle(World worldIn, BlockPos pos, Random rand) {
//		Random random = rand;
//		double d0 = 0.0625D;
//
//		for (int i = 0; i < 6; ++i) {
//			double d1 = (double) ((float) pos.getX() + random.nextFloat());
//			double d2 = (double) ((float) pos.getY() + random.nextFloat());
//			double d3 = (double) ((float) pos.getZ() + random.nextFloat());
//
//			if (i == 0 && !worldIn.getBlockState(pos.up()).isOpaqueCube()) {
//				d2 = (double) pos.getY() + 0.0625D + 1.0D;
//			}
//
//			if (i == 1 && !worldIn.getBlockState(pos.down()).isOpaqueCube()) {
//				d2 = (double) pos.getY() - 0.0625D;
//			}
//
//			if (i == 2 && !worldIn.getBlockState(pos.south()).isOpaqueCube()) {
//				d3 = (double) pos.getZ() + 0.0625D + 1.0D;
//			}
//
//			if (i == 3 && !worldIn.getBlockState(pos.north()).isOpaqueCube()) {
//				d3 = (double) pos.getZ() - 0.0625D;
//			}
//
//			if (i == 4 && !worldIn.getBlockState(pos.east()).isOpaqueCube()) {
//				d1 = (double) pos.getX() + 0.0625D + 1.0D;
//			}
//
//			if (i == 5 && !worldIn.getBlockState(pos.west()).isOpaqueCube()) {
//				d1 = (double) pos.getX() - 0.0625D;
//			}
//
//			if (d1 < (double) pos.getX() || d1 > (double) (pos.getX() + 1) || d2 < 0.0D || d2 > (double) (pos.getY() + 1) || d3 < (double) pos.getZ() || d3 > (double) (pos.getZ() + 1)) {
//				worldIn.spawnParticle(ParticleTypes.REDSTONE, d1, d2, d3, 0.0D, 0.0D, 0.0D, new int[0]);
//			}
//		}
//	}

	//TODO: Move to client
//	@OnlyIn(Dist.CLIENT)
//	@Override
//	public BlockRenderLayer getRenderLayer() {
//		//return isVanished ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
//		return BlockRenderLayer.TRANSLUCENT;
//	}

	//TODO: Removed. Check this
//	@Override
//	@Deprecated
//	public boolean doesSideBlockRendering(BlockState blockState, IEnviromentBlockReader blockAccess, BlockPos pos, Direction side) {
//		return !(blockAccess.getBlockState(pos.offset(side)).getBlock() instanceof BlockTFCastleDoor) && shouldSideBeRendered(blockState, blockAccess, pos, side);
//	}

	//TODO: Move to loot table
	//	@Override
//	public ItemStack getItem(World world, BlockPos pos, BlockState state) {
//		return new ItemStack(TFItems.castle_door, 1, damageDropped(state));
//	}
//
//	@Override
//	protected ItemStack getSilkTouchDrop(BlockState state) {
//		return new ItemStack(TFItems.castle_door, 1, damageDropped(state));
//	}
}
