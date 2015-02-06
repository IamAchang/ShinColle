package com.lulan.shincolle.entity;

import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**Extend Entity NBT data
 * IExtendedEntityProperties穦NBT穝tag: SHIP_EXTPROP_NAME
 * IInventory糤肂珇逆, tag: ShipInv
 */
public class ExtendShipProps implements IExtendedEntityProperties, IInventory {

	public static final String SHIP_EXTPROP_NAME = "ShipExtProps";
	public static final String tagName = "ShipInv";	//ship inventory nbt tag
    public ItemStack[] slots = new ItemStack[ContainerShipInventory.SLOTS_TOTAL];
    private BasicEntityShip entity;
    private World world;

  
	//init extend entity prop
	@Override
	public void init(Entity entity, World world) {
		this.world = world;
		this.entity = (BasicEntityShip) entity;

	}
	
	//save extend entity prop
	@Override
	public void saveNBTData(NBTTagCompound nbt) {
		NBTTagCompound nbtExt = new NBTTagCompound();		
		NBTTagCompound nbtExt_add1 = new NBTTagCompound();
		NBTTagCompound nbtExt_add2 = new NBTTagCompound();
		NBTTagCompound nbtExt_add3 = new NBTTagCompound();

		//save values to NBT
		nbtExt.setShort("Level", this.entity.getShipLevel());
		nbtExt.setInteger("Kills", this.entity.getKills());
		nbtExt.setInteger("Exp", this.entity.getExpCurrent());
		nbtExt.setInteger("NumAmmoL", this.entity.getNumAmmoLight());
		nbtExt.setInteger("NumAmmoH", this.entity.getNumAmmoHeavy());
		//save AttrFinal
		nbtExt.setTag("Final", nbtExt_add1);
		nbtExt_add1.setFloat("HP", this.entity.getFinalHP());
		nbtExt_add1.setFloat("ATK", this.entity.getFinalATK());
		nbtExt_add1.setFloat("DEF", this.entity.getFinalDEF());
		nbtExt_add1.setFloat("SPD", this.entity.getFinalSPD());
		nbtExt_add1.setFloat("MOV", this.entity.getFinalMOV());
		nbtExt_add1.setFloat("HIT", this.entity.getFinalHIT());
		//save EntityState
		nbtExt.setTag("State", nbtExt_add2);	
		nbtExt_add2.setByte("State", this.entity.getEntityState());
		nbtExt_add2.setByte("Emotion", this.entity.getEntityEmotion());
		nbtExt_add2.setByte("SwimType", this.entity.getEntitySwinType());
		//save BonusPoint
		nbtExt.setTag("Point", nbtExt_add3);	
		nbtExt_add3.setByte("HP", this.entity.getBonusHP());
		nbtExt_add3.setByte("ATK", this.entity.getBonusATK());
		nbtExt_add3.setByte("DEF", this.entity.getBonusDEF());
		nbtExt_add3.setByte("SPD", this.entity.getBonusSPD());
		nbtExt_add3.setByte("MOV", this.entity.getBonusMOV());
		nbtExt_add3.setByte("HIT", this.entity.getBonusHIT());
		
		//save inventory
		NBTTagList list = new NBTTagList();
		nbt.setTag(tagName, list);	//slots戈场tag: ShipInv
		
		for(int i=0; i<slots.length; i++) {
			if (slots[i] != null) {
				NBTTagCompound item = new NBTTagCompound();
				item.setByte("Slot", (byte)i);
				slots[i].writeToNBT(item);
				list.appendTag(item);
			}
		}		
		
		nbt.setTag(SHIP_EXTPROP_NAME, nbtExt);	
		LogHelper.info("DEBUG : save entity ExtNBT data on id: "+entity.getEntityId());
	}

	//load extend entity prop
	@Override
	public void loadNBTData(NBTTagCompound nbt) {
		NBTTagCompound nbt_tag = (NBTTagCompound) nbt.getTag(SHIP_EXTPROP_NAME);
		NBTTagCompound nbt_load = new NBTTagCompound();

		//load values to entity
		entity.setShipLevel(nbt_tag.getShort("Level"), false);
		entity.setKills(nbt_tag.getInteger("Kills"));
		entity.setExpCurrent(nbt_tag.getInteger("Exp"));
		entity.setNumAmmoLight(nbt_tag.getInteger("NumAmmoL"));
		entity.setNumAmmoHeavy(nbt_tag.getInteger("NumAmmoH"));
		//load Attr Final
		nbt_load = (NBTTagCompound) nbt_tag.getTag("Final");
		entity.setFinalHP(nbt_load.getFloat("HP"));
		entity.setFinalATK(nbt_load.getFloat("ATK"));
		entity.setFinalDEF(nbt_load.getFloat("DEF"));
		entity.setFinalSPD(nbt_load.getFloat("SPD"));
		entity.setFinalMOV(nbt_load.getFloat("MOV"));
		entity.setFinalHIT(nbt_load.getFloat("HIT"));
		//load entity state
		nbt_load = (NBTTagCompound) nbt_tag.getTag("State");
		entity.setEntityState(nbt_load.getByte("State"), false);
		entity.setEntityEmotion(nbt_load.getByte("Emotion"), false);
		entity.setEntitySwimType(nbt_load.getByte("SwimType"), false);
		//load bonus point
		nbt_load = (NBTTagCompound) nbt_tag.getTag("Point");
		entity.setBonusHP(nbt_load.getByte("HP"));
		entity.setBonusATK(nbt_load.getByte("ATK"));
		entity.setBonusDEF(nbt_load.getByte("DEF"));
		entity.setBonusSPD(nbt_load.getByte("SPD"));
		entity.setBonusMOV(nbt_load.getByte("MOV"));
		entity.setBonusHIT(nbt_load.getByte("HIT"));
		
		//load inventory
		NBTTagList list = nbt.getTagList(tagName, 10);

		for(int i=0; i<list.tagCount(); i++) {
			NBTTagCompound item = (NBTTagCompound) list.getCompoundTagAt(i);
			byte sid = item.getByte("Slot");

			if (sid>=0 && sid<slots.length) {
				slots[sid] = ItemStack.loadItemStackFromNBT(item);
			}
		}
		
		//calc equip and attribute
		entity.setExpNext();	//for gui display
		entity.setEquipAndUpdateState();	
		LogHelper.info("DEBUG : nbt exp"+entity.getShipLevel()+" "+entity.getExpNext());
		LogHelper.info("DEBUG : load entity ExtNBT data on id: "+entity.getEntityId());
	}

	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            if (itemStack.stackSize <= j) {			  //璝计秖<=j
                setInventorySlotContents(i, null);	  //玥赣slot睲
            }
            else {									  //璝计秖 >j
                itemStack = itemStack.splitStack(j);  //赣itemstack计秖-j
                if (itemStack.stackSize == 0) {
                    setInventorySlotContents(i, null);//场, slot睲
                }
            }
        }
        return itemStack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            setInventorySlotContents(i, null);
        }
        return itemStack;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		slots[i] = itemstack;
		//璝も珇禬筁赣计秖, 玥秈计秖
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}
		if(!world.isRemote) {
			this.entity.setEquipAndUpdateState();;  //update equip and attribute value
		}
	}

	@Override
	public String getInventoryName() {
		return "Ship Inventory";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
		return true;
	}
	
	@Override
	public void markDirty() {}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
		return true;
	}


}
