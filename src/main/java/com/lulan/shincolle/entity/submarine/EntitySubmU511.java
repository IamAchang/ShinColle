package com.lulan.shincolle.entity.submarine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntitySubmU511 extends BasicEntityShipSmall {

	public EntitySubmU511(World world) {
		super(world);
		this.setSize(0.7F, 1.4F);	//碰撞大小 跟模型大小無關
		this.ShipType = ID.ShipType.SUBMARINE;
		this.ShipID = ID.S_SubmarineU511;
		this.ModelPos = new float[] {0F, 10F, 0F, 45F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		
		this.initTypeModify();
	}
	
	//for morph
	@Override
	public float getEyeHeight() {
		return this.height * 1.2F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}
	
	@Override
	public void setAIList() {
		super.setAIList();
		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));			   //0011
	}

    //check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(!worldObj.isRemote) {
  			//add aura to master every 100 ticks
  			if(this.ticksExisted % 100 == 0) {
  				if(getStateFlag(ID.F.UseRingEffect)) {
  					//apply ability to player
  					EntityPlayerMP player = (EntityPlayerMP) this.getOwner();
  	  				if(getStateFlag(ID.F.IsMarried) && getStateMinor(ID.N.NumGrudge) > 0 && player != null && getDistanceSqToEntity(player) < 256D) {
  	  					//potion effect: id, time, level
  	  	  	  			player.addPotionEffect(new PotionEffect(Potion.invisibility.id, 60 + getLevel() * 10));
  	  				}
  				}	
  			}
  			
  			if(this.ticksExisted % 300 == 0) {
  				if(getStateFlag(ID.F.UseRingEffect)) {
  					//apply ability to ship
  					this.addPotionEffect(new PotionEffect(Potion.invisibility.id, 100 + getLevel()));
  				}
  			}
  		}    
  	}
  	
  	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				switch(getStateEmotion(ID.S.State)) {
				case ID.State.NORMAL:
					setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
					break;
				case ID.State.EQUIP00:
					setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					break;
				default:
					setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					break;
				}
				return true;
			}
		}
		
		super.interact(player);
		return false;
  	}
  	
  	@Override
	public int getKaitaiType() {
		return 0;
	}
  	
  	//潛艇的輕攻擊一樣使用飛彈
  	@Override
  	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
  	public boolean attackEntityWithAmmo(Entity target) {	
  		//get attack value
  		float atk = StateFinal[ID.ATK];
  		
  		//set knockback value (testing)
  		float kbValue = 0.15F;
  		//飛彈是否採用直射
  		boolean isDirect = false;
  		//計算目標距離
  		float tarX = (float)target.posX;	//for miss chance calc
  		float tarY = (float)target.posY;
  		float tarZ = (float)target.posZ;
  		float distX = tarX - (float)this.posX;
  		float distY = tarY - (float)this.posY;
  		float distZ = tarZ - (float)this.posZ;
          float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
          float launchPos = (float)posY + height * 0.7F;
          
          //超過一定距離/水中 , 則採用拋物線,  在水中時發射高度較低
          if((distX*distX+distY*distY+distZ*distZ) < 36F) {
          	isDirect = true;
          }
          if(getShipDepth() > 0D) {
          	isDirect = true;
          	launchPos = (float)posY;
          }
  		
  		//experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(1);
  	
  		//play cannon fire sound at attacker
          this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
          //play entity attack sound
          if(this.getRNG().nextInt(10) > 7) {
          	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
          }
          
          //heavy ammo -1
          if(!decrAmmoNum(0)) {	//not enough ammo
          	atk = atk * 0.125F;	//reduce damage to 12.5%
          }
          
          //calc miss chance, miss: add random offset(0~6) to missile target 
          float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.N.ShipLevel];
          missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
          if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
         
          if(this.rand.nextFloat() < missChance) {
        	  tarX = tarX - 3F + this.rand.nextFloat() * 6F;
        	  tarY = tarY + this.rand.nextFloat() * 3F;
        	  tarZ = tarZ - 3F + this.rand.nextFloat() * 6F;
        	  //spawn miss particle
        	  TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	  CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
          }

          //spawn missile
          EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
          		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, 0.08F);
          this.worldObj.spawnEntityInWorld(missile);
          
          return true;
  	}
  	
  	@Override
	public double getMountedYOffset() {
  		if(this.isSitting()) {
			if(getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
				return (double)this.height * 0.0F;
  			}
  			else {
  				return (double)this.height * 0.0F;
  			}
  		}
  		else {
  			return (double)this.height * 0.5F;
  		}
	}
  	

}



