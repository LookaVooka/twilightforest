package twilightforest.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import twilightforest.entity.TFEntities;
import twilightforest.entity.projectile.EntityIceArrow;

public class ItemTFIceBow extends ItemTFBowBase {

	public ItemTFIceBow(Properties props) {
		super(props);
	}

	@Override
	public AbstractArrowEntity customeArrow(AbstractArrowEntity arrow) {
		if (arrow.getShooter() instanceof LivingEntity) {
			return new EntityIceArrow(TFEntities.ice_arrow.get(), arrow.world, (LivingEntity) arrow.getShooter());
		}
		return arrow;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repairWith) {
		return repairWith.getItem() == Item.getItemFromBlock(Blocks.ICE) || super.getIsRepairable(toRepair, repairWith);
	}
}
