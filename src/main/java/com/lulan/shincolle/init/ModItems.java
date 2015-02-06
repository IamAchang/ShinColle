package com.lulan.shincolle.init;

import net.minecraft.item.Item;

import com.lulan.shincolle.item.*;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(Reference.MOD_ID)	//登錄object holder使mod的物件容易流通 其他人可以直接讀取該物件
public class ModItems {

	//spawn egg
	public static final ShipSpawnEgg ShipSpawnEgg = new ShipSpawnEgg();
	//materials
	public static final BasicItem Abyssium = new Abyssium();
	public static final BasicItem Ammo = new Ammo();
	public static final BasicItem AmmoContainer = new AmmoContainer();
	public static final BasicItem BucketRepair = new BucketRepair();
	public static final BasicItem Grudge = new Grudge();
	public static final BasicItem HeavyAmmo = new HeavyAmmo();
	public static final BasicItem HeavyAmmoContainer = new HeavyAmmoContainer();
	public static final BasicItem Polymetal = new Polymetal();
	//equip	
	public static final BasicItem EquipCannon = new EquipCannon();
	

	//登錄item到遊戲中 (在pre init階段登錄)
	public static void init() {
		//spawn egg
		GameRegistry.registerItem(ShipSpawnEgg, "ShipSpawnEgg");
		//materials
		GameRegistry.registerItem(Abyssium, "Abyssium");
		GameRegistry.registerItem(Ammo, "Ammo");
		GameRegistry.registerItem(AmmoContainer, "AmmoContainer");
		GameRegistry.registerItem(BucketRepair, "BucketRepair");
		GameRegistry.registerItem(Grudge, "Grudge");
		GameRegistry.registerItem(HeavyAmmo, "HeavyAmmo");
		GameRegistry.registerItem(HeavyAmmoContainer, "HeavyAmmoContainer");
		GameRegistry.registerItem(Polymetal, "Polymetal");
		//equip		
		GameRegistry.registerItem(EquipCannon, "EquipCannon");
		
	}
	
}
