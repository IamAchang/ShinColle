package com.lulan.shincolle.entity;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIOwnerHurtByTarget;
import net.minecraft.entity.ai.EntityAIOwnerHurtTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIInRangeTarget;
import com.lulan.shincolle.ai.EntityAIRangeAttack;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.inventory.ContainerShipInventory;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.AttrValues;
import com.lulan.shincolle.reference.GUIs;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityHeavyCruiserRi extends BasicEntitySmallShip {
	
	public EntityHeavyCruiserRi(World world) {
		super(world);
		this.setSize(0.9F, 1.9F);	//�I���j�p ��ҫ��j�p�L��
		this.setCustomNameTag(StatCollector.translateToLocal("entity.shincolle:EntityHeavyCruiserRi.name"));
		this.ShipType = AttrValues.ShipType.HEAVY_CRUISER;
		this.ShipID = AttrID.HeavyCruiserRI;
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		
		this.initTypeModify();	
		this.setAIList();
		this.setAITargetList();
		
	}
	
	public void setAIList() {
		this.getNavigator().setEnterDoors(true);
		
		//use range attack (light)
		this.tasks.addTask(2, new EntityAIRangeAttack(this));
		
		//use melee attack
		this.tasks.addTask(3, new EntityAIAttackOnCollide(this, 1D, true));
		this.tasks.addTask(4, new EntityAIMoveTowardsTarget(this, 1D, 64F));
		
		//idle AI
		//moving
        this.tasks.addTask(21, new EntityAIRestrictOpenDoor(this));
		this.tasks.addTask(22, new EntityAIOpenDoor(this, true));
		this.tasks.addTask(23, new EntityAIWatchClosest(this, EntityPlayer.class, 5F));
		this.tasks.addTask(24, new EntityAIWander(this, 0.8D));
		this.tasks.addTask(25, new EntityAILookIdle(this));
		
/* 		//switch AI method
		this.tasks.removeTask(this.aiAttackOnCollide);
        this.tasks.removeTask(this.aiArrowAttack);
        ItemStack itemstack = this.getHeldItem();

        if (itemstack != null && itemstack.getItem() == Items.bow)
        {
            this.tasks.addTask(4, this.aiArrowAttack);
        }
        else
        {
            this.tasks.addTask(4, this.aiAttackOnCollide);
        }*/
	}
	
	public void setAITargetList() {	
		//target AI
	//NYI:	this.targetTasks.addTask(1, new EntityAIOwnerPointTarget(this));
		this.targetTasks.addTask(2, new EntityAIOwnerHurtByTarget(this));
		this.targetTasks.addTask(3, new EntityAIInRangeTarget(this, 0.4F, 1));
	}

	//���`����
	protected String getLivingSound() {
        return Reference.MOD_ID+":ship-say";
    }
	
	//���˭���
    protected String getHurtSound() {
    	
        return Reference.MOD_ID+":ship-hurt";
    }

    //���`����
    protected String getDeathSound() {
    	return Reference.MOD_ID+":ship-death";
    }

    //���Ĥj�p
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void onLivingUpdate() {
    	//check server side
    	if(!this.worldObj.isRemote) {
    		if(this.ticksExisted % 40 == 0) {
	    		if(this.getHealth()/this.getMaxHealth() < 0.5F) {
	    			this.setEntityEmotion(AttrValues.Emotion.T_T, true);  			
	    		}
	    		else {	//back to normal face
	    			if(this.getEntityEmotion() == AttrValues.Emotion.T_T) {
	    				this.setEntityEmotion(AttrValues.Emotion.NORMAL, true);
	    			}
	    		}
    		}
    		//check every 5 sec
    		if(this.ticksExisted % 100 == 0) {
	    		//apply potion effect in the night
	        	if (!this.worldObj.isDaytime()) {	
        			this.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 300, 2));
        			this.addPotionEffect(new PotionEffect(Potion.jump.id, 300, 2));
        		} 		
        	}
    	}
    	
    	super.onLivingUpdate();
    }
    
    //this method work in server side
    @Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {		
    	//set hurt face
		this.setEntityEmotion(AttrValues.Emotion.O_O, true);
  	
    	return super.attackEntityFrom(attacker, atk);
    }
	
	@Override
	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use item on entity
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {  //change equip mode
				if(getEntityState() == 1) {
					setEntityState(0, true);
				}
				else {
					setEntityState(1, true);
				}
			}
		}
		
		
		//debug test
		setShipLevel((short) (ShipLevel+1), true);

		
		//shift+right click�ɥ��}GUI
		if (player.isSneaking() && player.getDisplayName().equals(this.getOwnerName())) {  
			int eid = this.getEntityId();
			//player.openGui vs FMLNetworkHandler ?
		//	player.openGui(ShinColle.instance, GUIs.SHIPINVENTORY, this.worldObj, eid, 0, 0);
    		FMLNetworkHandler.openGui(player, ShinColle.instance, GUIs.SHIPINVENTORY, this.worldObj, this.getEntityId(), 0, 0);
    		return true;
		}
		
		super.interact(player);
    	return false;	
	}
	
	//get block under entity
	public String getBlockUnder(Entity entity) {
	    int blockX = MathHelper.floor_double(entity.posX);
	    int blockY = MathHelper.floor_double(entity.boundingBox.minY)-1;
	    int blockZ = MathHelper.floor_double(entity.posZ);
	    
	    BlockUnderName = entity.worldObj.getBlock(blockX, blockY, blockZ).getLocalizedName();   
	    
	    return BlockUnderName;
	}

	

}
