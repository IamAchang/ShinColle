package com.lulan.shincolle.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

public class EquipArmor extends BasicEquip {
	
	IIcon[] icons = new IIcon[1];
	
	public EquipArmor() {
		super();
		this.setUnlocalizedName("EquipArmor");
		this.types = 2;
	}
	
	@Override
	public void registerIcons(IIconRegister iconRegister) {	
		icons[0] = iconRegister.registerIcon(this.getUnlocalizedName().substring(this.getUnlocalizedName().indexOf(".")+1));
	}
	
	@Override
	public IIcon getIconFromDamage(int meta) {
	    return icons[0];
	}

}


