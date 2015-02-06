package com.lulan.shincolle.block;

import net.minecraft.block.material.Material;

public class BlockGrudge extends BasicBlock {
	public BlockGrudge() {
		super(Material.iron);
		this.setBlockName("BlockGrudge");
		this.setHarvestLevel("pickaxe", 0);
	    this.setHardness(1F);
	    this.setLightLevel(1F);
		
	}
}
