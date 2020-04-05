package com.chaosthedude.endermail.blocks;

import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import com.chaosthedude.endermail.EnderMail;
import com.chaosthedude.endermail.blocks.te.TileEntityPackage;
import com.chaosthedude.endermail.config.ConfigHandler;
import com.chaosthedude.endermail.entity.EntityEnderMailman;
import com.chaosthedude.endermail.gui.GuiHandler;
import com.chaosthedude.endermail.items.ItemPackageController;
import com.chaosthedude.endermail.registry.EnderMailBlocks;
import com.chaosthedude.endermail.registry.EnderMailItems;
import com.chaosthedude.endermail.util.EnumControllerState;
import com.chaosthedude.endermail.util.ItemUtils;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockPackage extends BlockContainer {

	public static final String NAME = "package";

	public static final int INVENTORY_SIZE = 5;

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final PropertyBool STAMPED = PropertyBool.create("stamped");

	public BlockPackage() {
		super(Material.WOOD);
		setUnlocalizedName(EnderMail.MODID + "." + NAME);
		setCreativeTab(CreativeTabs.DECORATIONS);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(STAMPED, false));
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!isStamped(state) && ItemUtils.isHolding(player, EnderMailItems.stamp)) {
			player.openGui(EnderMail.instance, GuiHandler.STAMP_ID, world, pos.getX(), pos.getY(), pos.getZ());
		} else if (!isStamped(state)) {
			player.openGui(EnderMail.instance, GuiHandler.PACKAGE_ID, world, pos.getX(), pos.getY(), pos.getZ());
		} else if (!world.isRemote) {
			if (isStamped(state) && player.isSneaking()) {
				setState(false, world, pos);
			} else if (isStamped(state) && ItemUtils.isHolding(player, EnderMailItems.packageController)) {
				ItemStack stack = ItemUtils.getHeldItem(player, EnderMailItems.packageController);
				ItemPackageController packageController = (ItemPackageController) stack.getItem();
				BlockPos deliveryPos = getDeliveryPos(world, pos);
				if (deliveryPos != null) {
					packageController.setDeliveryPos(stack, deliveryPos);
					int distanceToDelivery = (int) pos.getDistance(deliveryPos.getX(), deliveryPos.getY(), deliveryPos.getZ());
					if (ConfigHandler.maxDeliveryDistance > -1 && distanceToDelivery > ConfigHandler.maxDeliveryDistance) {
						packageController.setState(stack, EnumControllerState.TOOFAR);
						packageController.setDeliveryDistance(stack, distanceToDelivery);
						packageController.setMaxDistance(stack, ConfigHandler.maxDeliveryDistance);
					} else {
						packageController.setState(stack, EnumControllerState.DELIVERING);
						EntityEnderMailman enderMailman = new EntityEnderMailman(world, pos, deliveryPos, stack);
						world.spawnEntity(enderMailman);
					}
				}
			}
		}

		return true;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		if (stack.hasDisplayName()) {
			TileEntity tileentity = world.getTileEntity(pos);

			if (tileentity instanceof TileEntityPackage) {
				((TileEntityPackage) tileentity).setCustomName(stack.getDisplayName());
			}
		}
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
		player.addStat(StatList.getBlockStats(this));
		player.addExhaustion(0.005F);

		if (te != null && te instanceof TileEntityPackage) {
			TileEntityPackage tePackage = (TileEntityPackage) te;
			ItemStack stackPackage = new ItemStack(Item.getItemFromBlock(this));
			NBTTagCompound tag1 = new NBTTagCompound();
			NBTTagCompound tag2 = new NBTTagCompound();
			tag1.setTag("BlockEntityTag", ((TileEntityPackage) te).saveToNBT(tag2));
			stackPackage.setTagCompound(tag1);

			if (tePackage.hasCustomName()) {
				stackPackage.setStackDisplayName(tePackage.getName());
				tePackage.setCustomName("");
			}

			spawnAsEntity(world, pos, stackPackage);

			world.updateComparatorOutputLevel(pos, state.getBlock());
		}
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		ItemStack stack = super.getItem(world, pos, state);
		TileEntityPackage tileEntityPackage = (TileEntityPackage) world.getTileEntity(pos);
		NBTTagCompound tag = tileEntityPackage.saveToNBT(new NBTTagCompound());
		if (!tag.hasNoTags()) {
			stack.setTagInfo("BlockEntityTag", tag);
		}

		return stack;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			NBTTagCompound temp = stack.getTagCompound();
			if (temp != null && temp.hasKey("BlockEntityTag", 10)) {
				NBTTagCompound tag = temp.getCompoundTag("BlockEntityTag");
				if (tag.hasKey("Items", 9)) {
					NonNullList<ItemStack> content = NonNullList.<ItemStack>withSize(INVENTORY_SIZE, ItemStack.EMPTY);
					ItemStackHelper.loadAllItems(tag, content);
					for (ItemStack contentStack : content) {
						if (!contentStack.isEmpty()) {
							tooltip.add(String.format("%s x%d", new Object[] { contentStack.getDisplayName(),
									Integer.valueOf(contentStack.getCount()) }));
						}
					}
				}
			}
		} else {
			tooltip.add(I18n.format("string.endermail.holdShift"));
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		setDefaultFacing(world, pos, state);
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
			float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.getFront(meta);

		if (facing.getAxis() == EnumFacing.Axis.Y) {
			facing = EnumFacing.NORTH;
		}

		return this.getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return ((EnumFacing) state.getValue(FACING)).getIndex();
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate((EnumFacing) state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation((EnumFacing) state.getValue(FACING)));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { FACING, STAMPED });
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityPackage();
	}

	public IBlockState getStampedState() {
		return getDefaultState().withProperty(STAMPED, true);
	}

	public boolean isStamped(IBlockState state) {
		return state.getValue(STAMPED).booleanValue();
	}

	public static void stampPackage(World world, BlockPos packagePos, BlockPos deliveryPos) {
		setState(true, world, packagePos);
		TileEntity te = world.getTileEntity(packagePos);
		if (te != null && te instanceof TileEntityPackage) {
			TileEntityPackage tePackage = (TileEntityPackage) te;
			tePackage.setDeliveryPos(deliveryPos);
		}
	}

	public static BlockPos getDeliveryPos(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof TileEntityPackage) {
			TileEntityPackage tePackage = (TileEntityPackage) te;
			return tePackage.getDeliveryPos();
		}

		return null;
	}

	public static void setState(boolean stamped, World world, BlockPos pos) {
		IBlockState iblockstate = world.getBlockState(pos);
		TileEntity tileentity = world.getTileEntity(pos);
		world.setBlockState(pos, EnderMailBlocks.package_block.getDefaultState()
				.withProperty(FACING, iblockstate.getValue(FACING)).withProperty(STAMPED, stamped), 3);
		if (tileentity != null) {
			tileentity.validate();
			world.setTileEntity(pos, tileentity);
		}
	}

	private void setDefaultFacing(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			IBlockState northState = world.getBlockState(pos.north());
			IBlockState southState = world.getBlockState(pos.south());
			IBlockState westState = world.getBlockState(pos.west());
			IBlockState eastState = world.getBlockState(pos.east());
			EnumFacing facing = (EnumFacing) state.getValue(FACING);

			if (facing == EnumFacing.NORTH && northState.isFullBlock() && !southState.isFullBlock()) {
				facing = EnumFacing.SOUTH;
			} else if (facing == EnumFacing.SOUTH && southState.isFullBlock() && !northState.isFullBlock()) {
				facing = EnumFacing.NORTH;
			} else if (facing == EnumFacing.WEST && westState.isFullBlock() && !eastState.isFullBlock()) {
				facing = EnumFacing.EAST;
			} else if (facing == EnumFacing.EAST && eastState.isFullBlock() && !westState.isFullBlock()) {
				facing = EnumFacing.WEST;
			}

			world.setBlockState(pos, state.withProperty(FACING, facing), 2);
		}
	}

}