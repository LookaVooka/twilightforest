package twilightforest.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.item.Rarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twilightforest.entity.EntityTFLoyalZombie;
import twilightforest.entity.TFEntities;
import twilightforest.util.EntityUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemTFZombieWand extends ItemTF {

	protected ItemTFZombieWand(Rarity rarity, Properties props) {
		super(rarity, props.maxDamage(9));
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, @Nonnull Hand hand) {

		ItemStack stack = player.getHeldItem(hand);

		if (stack.getDamage() == stack.getMaxDamage()) {
			return new ActionResult<>(ActionResultType.FAIL, stack);
		}

		if (!world.isRemote) {
			// what block is the player pointing at?
			RayTraceResult ray = EntityUtil.rayTrace(player, 20.0);
			BlockRayTraceResult blockray = (BlockRayTraceResult) ray;

			if (ray != null && blockray.getPos() != null) {
				EntityTFLoyalZombie zombie = new EntityTFLoyalZombie(TFEntities.loyal_zombie.get(), world);
				zombie.setPositionAndRotation(blockray.getPos().getX(), blockray.getPos().getY(), blockray.getPos().getZ(), 1.0F, 1.0F);
				zombie.setTamed(true);
				zombie.setOwnerId(player.getUniqueID());
				zombie.addPotionEffect(new EffectInstance(Effects.STRENGTH, 1200, 1));
				world.addEntity(zombie);

				stack.damageItem(1, player, (user) -> user.sendBreakAnimation(hand));
			}
		}

		return new ActionResult<>(ActionResultType.SUCCESS, stack);
	}

	@Override
	public float getXpRepairRatio(ItemStack stack) {
		return 0.1f;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flags) {
		super.addInformation(stack, world, tooltip, flags);
		tooltip.add(new TranslationTextComponent("twilightforest.scepter_charges", stack.getMaxDamage() - stack.getDamage()));
	}
}
