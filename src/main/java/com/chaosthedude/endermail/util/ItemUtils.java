package com.chaosthedude.endermail.util;

import com.chaosthedude.endermail.registry.EnderMailItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemUtils {
	
	public static boolean verifyNBT(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() != EnderMailItems.packageController) {
			return false;
		} else if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}

		return true;
	}

	public static ItemStack getHeldItem(EntityPlayer player, Item item) {
		if (!player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() == item) {
			return player.getHeldItemMainhand();
		} else if (!player.getHeldItemOffhand().isEmpty() && player.getHeldItemOffhand().getItem() == item) {
			return player.getHeldItemOffhand();
		}

		return ItemStack.EMPTY;
	}
	
	public static boolean isHolding(EntityPlayer player, Item item) {
		return player.getHeldItemMainhand().getItem() == item || player.getHeldItemOffhand().getItem() == item;
	}

}
