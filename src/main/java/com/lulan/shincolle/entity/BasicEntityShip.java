package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIOwnerHurtByTarget;
import net.minecraft.entity.ai.EntityAIOwnerHurtTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipGuarding;
import com.lulan.shincolle.ai.EntityAIShipInRangeTarget;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable implements IShipCannonAttack, IShipGuardian, IShipFloating {

	protected ExtendShipProps ExtProps;			//entity額外NBT紀錄
	protected ShipPathNavigate shipNavigator;	//水空移動用navigator
	protected ShipMoveHelper shipMoveHelper;
	protected Entity guardedEntity;
	
	//for attribute calc
	protected byte ShipType;			//ship type
	protected byte ShipID;
	
	//for AI calc
	protected double ShipDepth;			//水深, 用於水中高度判定
	protected double ShipPrevX;			//ship posX 5 sec ago
	protected double ShipPrevY;			//ship posY 5 sec ago
	protected double ShipPrevZ;			//ship posZ 5 sec ago
	/**equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateEquip;
	/**final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateFinal;
	/**minor states: 0:ShipLevel 1:Kills 2:ExpCurrent 3:ExpNext 4:NumAmmoLight 
	 * 5:NumAmmoHeavy 6:NumGrudge 7:NumAirLight 8:NumAirHeavy 9:immunity time 
	 * 10:followMin 11:followMax 12:FleeHP 13:TargetAIType 14:guardX 15:guardY 16:guardZ 17:guardDim
	 * 18:guardID 19:ownerID */
	protected int[] StateMinor;
	/**equip effect: 0:critical 1:doubleHit 2:tripleHit 3:baseMiss*/
	protected float[] EffectEquip;
	/**EntityState: 0:State 1:Emotion 2:Emotion2 3:HP State 4:State2 5:AttackPhase*/
	protected byte[] StateEmotion;
	/**EntityFlag: 0:canFloatUp 1:isMarried 2:noFuel 3:canMelee 4:canAmmoLight 5:canAmmoHeavy 
	 * 6:canAirLight 7:canAirHeavy 8:headTilt(client only) 9:canRingEffect 10:canDrop 11:canFollow
	 * 12:onSightChase */
	protected boolean[] StateFlag;
	/**BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected byte[] BonusPoint;
	/**TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected float[] TypeModify;
	/**ModelPos: posX, posY, posZ, scale (in ship inventory) */
	protected float[] ModelPos;
	
	//for model render
	protected int StartEmotion;			//表情1開始時間
	protected int StartEmotion2;		//表情2開始時間
	protected float[] rotateAngle;		//模型旋轉角度, 用於手持物品render
	
	//for GUI display, no use
	protected String ownerName;
	
	
	public BasicEntityShip(World world) {
		super(world);
		ignoreFrustumCheck = true;	//即使不在視線內一樣render
		//init value
		isImmuneToFire = true;	//set ship immune to lava
		StateEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateFinal = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateMinor = new int[] {1, 0, 0, 40, 0, 0, 0, 0, 0, 0, 2, 14, 35, 1, -1, -1, -1, 0, -1, -1};
		EffectEquip = new float[] {0F, 0F, 0F, 0F};
		StateEmotion = new byte[] {0, 0, 0, 0, 0, 0};
		StateFlag = new boolean[] {false, false, true, false, true, true, true, true, false, true, true, false, false};
		BonusPoint = new byte[] {0, 0, 0, 0, 0, 0};
		TypeModify = new float[] {1F, 1F, 1F, 1F, 1F, 1F};
		ModelPos = new float[] {0F, 0F, 0F, 50F};
		
		//for AI
		ShipDepth = 0D;			//water block above ship (within ship position)
		ShipPrevX = posX;		//ship position 5 sec ago
		ShipPrevY = posY;
		ShipPrevZ = posZ;
		ownerName = "";
		stepHeight = 1F;
		shipNavigator = new ShipPathNavigate(this, worldObj);
		shipMoveHelper = new ShipMoveHelper(this);
		
		//for render
		StartEmotion = -1;		//emotion start time
		StartEmotion2 = -1;		//head tile cooldown
		rotateAngle = new float[] {0F, 0F, 0F};		//model rotate angle (right hand)
	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public boolean isEntityInvulnerable() {
        return StateMinor[ID.N.ImmuneTime] > 0;
    }
	
	@Override
	public boolean isBurning() {	//display fire effect
		return this.getStateEmotion(ID.S.HPState) == ID.HPState.HEAVY;
	}
	
	@Override
	public float getEyeHeight() {
		return this.height * 1F;
	}
	
	@Override
	public boolean hasCustomNameTag() {
		if(ConfigHandler.showTag) {
			return this.dataWatcher.getWatchableObjectString(10).length() > 0;
		}
        return false;
    }
	
	//平常音效
	protected String getLivingSound() {
		if(this.getStateFlag(ID.F.IsMarried)) {
			if(rand.nextInt(5) == 0) {
				return Reference.MOD_ID+":ship-love";
			}
			else {
				return Reference.MOD_ID+":ship-say";
			}
		}
		else {
			return Reference.MOD_ID+":ship-say";
		}
    }
	
	//受傷音效
    protected String getHurtSound() {
    	
        return Reference.MOD_ID+":ship-hurt";
    }

    //死亡音效
    protected String getDeathSound() {
    	return Reference.MOD_ID+":ship-death";
    }

    //音效大小
    protected float getSoundVolume() {
        return ConfigHandler.shipVolume;
    }
    
    //get model rotate angle, par1 = 0:X, 1:Y, 2:Z
    @Override
    public float getModelRotate(int par1) {
    	switch(par1) {
    	default:
    		return this.rotateAngle[0];
    	case 1:
    		return this.rotateAngle[1];
    	case 2:
    		return this.rotateAngle[2];
    	}
    }
    
    //set model rotate angle, par1 = 0:X, 1:Y, 2:Z
    @Override
	public void setModelRotate(int par1, float par2) {
		switch(par1) {
    	default:
    		rotateAngle[0] = par2;
    	case 1:
    		rotateAngle[1] = par2;
    	case 2:
    		rotateAngle[2] = par2;
    	}
	}
	
	//get owner name (for player owner only)
	public String getOwnerName() {
		return this.ownerName;
	}
	
	//for client host check, owner must be online
	public int getOwnerID() {
		return this.getStateMinor(ID.N.OwnerID);
	}

	@Override
	public EntityAgeable createChild(EntityAgeable p_90011_1_) {
		return null;
	}
	
	//setup AI
	protected void setAIList() {
		this.getNavigator().setEnterDoors(true);
		this.getNavigator().setAvoidsWater(false);
		this.getNavigator().setCanSwim(true);
		
		//high priority
		this.tasks.addTask(1, new EntityAIShipSit(this));	   		//0101
		this.tasks.addTask(2, new EntityAIShipFlee(this));			//0111
		this.tasks.addTask(3, new EntityAIShipGuarding(this));		//0111
		this.tasks.addTask(4, new EntityAIShipFollowOwner(this));	//0111
		
		//use melee attack
		if(this.getStateFlag(ID.F.UseMelee)) {
			this.tasks.addTask(15, new EntityAIShipAttackOnCollide(this, 1D, true));//0011
			this.tasks.addTask(16, new EntityAIMoveTowardsTarget(this, 1D, 48F));	//0001
		}
		
		//idle AI
		//moving
		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));	//0000
		this.tasks.addTask(23, new EntityAIShipFloating(this));		//0101
		this.tasks.addTask(24, new EntityAIShipWatchClosest(this, EntityPlayer.class, 6F, 0.08F)); //0010
		this.tasks.addTask(25, new EntityAIWander(this, 0.8D));		//0001
		this.tasks.addTask(25, new EntityAILookIdle(this));			//0011
	}
	
	//setup target AI: par1: 0:passive 1:active
	public void setAITargetList(int par1) {	
		//passive target AI
		if(par1 == 0) {
			this.targetTasks.addTask(1, new EntityAIOwnerHurtByTarget(this));
			this.targetTasks.addTask(2, new EntityAIOwnerHurtTarget(this));
			this.targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
		}
		//active target AI
		else {
			this.targetTasks.addTask(1, new EntityAIOwnerHurtByTarget(this));
			this.targetTasks.addTask(2, new EntityAIOwnerHurtTarget(this));
			this.targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
			this.targetTasks.addTask(4, new EntityAIShipInRangeTarget(this, 0.4F, 1));
		}	
	}

	//clear AI
	protected void clearAITasks() {
		tasks.taskEntries.clear();
	}
	
	//clear target AI
	protected void clearAITargetTasks() {
		this.setAttackTarget(null);
		targetTasks.taskEntries.clear();
	}
	
	//getter
	public ExtendShipProps getExtProps() {
		return ExtProps;
	}
	
	public ShipPathNavigate getShipNavigate() {
		return shipNavigator;
	}
	
	public ShipMoveHelper getShipMoveHelper() {
		return shipMoveHelper;
	}
	
	abstract public int getEquipType();
	
	/** 0=small 1=large */
	abstract public int getKaitaiType();	//0 = small, 1 = large
	
	@Override
	public int getLevel() {
		return StateMinor[ID.N.ShipLevel];
	}
	
	public byte getShipType() {
		return ShipType;
	}
	
	public byte getShipID() {
		return ShipID;
	}
	
	@Override
	public EntityLivingBase getPlayerOwner() {
		return this.getOwner();
	}
	
	@Override
	public int getAmmoLight() {
		return this.StateMinor[ID.N.NumAmmoLight];
	}

	@Override
	public int getAmmoHeavy() {
		return this.StateMinor[ID.N.NumAmmoHeavy];
	}
	
	@Override
	public int getStartEmotion() {
		return StartEmotion;
	}
	
	@Override
	public int getStartEmotion2() {
		return StartEmotion2;
	}
	
	@Override
	public int getTickExisted() {
		return this.ticksExisted;
	}
	
	@Override
	public int getAttackTime() {
		return this.attackTime;
	}
	
	@Override
	public boolean getIsRiding() {
		return this.isRiding();
	}
	
	@Override
	public boolean getIsLeashed() {
		return this.getLeashed();
	}

	@Override
	public boolean getIsSprinting() {
		return this.isSprinting();
	}

	@Override
	public boolean getIsSitting() {
		return this.isSitting();
	}

	@Override
	public boolean getIsSneaking() {
		return this.isSneaking();
	}
	
	@Override
	public EntityLivingBase getTarget() {
		return this.getAttackTarget();
	}
	
	@Override
	public float getAttackDamage() {	//NO USE for ship entity
		return 0;
	}
	
	@Override
	public float getAttackSpeed() {
		return this.StateFinal[ID.SPD];
	}
	
	@Override
	public float getAttackRange() {
		return this.StateFinal[ID.HIT];
	}
	
	@Override
	public float getDefValue() {
		return this.StateFinal[ID.DEF];
	}
	
	@Override
	public float getMoveSpeed() {
		return this.StateFinal[ID.MOV];
	}
	
	@Override
	public boolean hasAmmoLight() {
		return StateMinor[ID.N.NumAmmoLight] > 0;
	}
	
	@Override
	public boolean hasAmmoHeavy() {
		return StateMinor[ID.N.NumAmmoHeavy] > 0;
	}

	@Override
	public boolean useAmmoLight() {
		return StateFlag[ID.F.UseAmmoLight];
	}

	@Override
	public boolean useAmmoHeavy() {
		return StateFlag[ID.F.UseAmmoHeavy];
	}
	
	@Override
	public double getShipDepth() {
		return ShipDepth;
	}
	
	@Override
	public boolean getStateFlag(int flag) {	//get flag (boolean)
		return StateFlag[flag];		
	}
	
	public byte getStateFlagI(int flag) {	//get flag (byte)
		if(StateFlag[flag]) {
			return 1;
		}
		else {
			return 0;
		}
	}
	public float getStateEquip(int id) {
		return StateEquip[id];
	}
	public float getStateFinal(int id) {
		return StateFinal[id];
	}
	public int getStateMinor(int id) {
		return StateMinor[id];
	}
	@Override
	public float getEffectEquip(int id) {
		return EffectEquip[id];
	}
	@Override
	public byte getStateEmotion(int id) {
		return StateEmotion[id];
	}
	public byte getBonusPoint(int id) {
		return BonusPoint[id];
	}
	public float getTypeModify(int id) {
		return TypeModify[id];
	}
	/** get model pos in GUI */
	public float[] getModelPos() {
		return ModelPos;
	}
	
	//called when entity equip changed
	//this method get equip state from slots, no input parm
	public void calcEquipAndUpdateState() {
		ItemStack itemstack = null;
		float[] equipStat = {0F,0F,0F,0F,0F,0F,0F,0F,0F,0F};
		
		//init value
		StateEquip[ID.HP] = 0F;
		StateEquip[ID.DEF] = 0F;
		StateEquip[ID.SPD] = 0F;
		StateEquip[ID.MOV] = 0F;
		StateEquip[ID.HIT] = 0F;
		StateEquip[ID.ATK] = 0F;
		StateEquip[ID.ATK_H] = 0F;
		StateEquip[ID.ATK_AL] = 0F;
		StateEquip[ID.ATK_AH] = 0F;
		EffectEquip[ID.EF_CRI] = 0;
		EffectEquip[ID.EF_DHIT] = 0;
		EffectEquip[ID.EF_THIT] = 0;
		EffectEquip[ID.EF_MISS] = 0;
		
		//calc equip slots
		for(int i=0; i<ContainerShipInventory.SLOTS_SHIPINV; i++) {
			itemstack = this.ExtProps.slots[i];
			if(itemstack != null) {
				equipStat = EquipCalc.getEquipStat(this, itemstack);
				StateEquip[ID.HP] += equipStat[ID.HP];
				StateEquip[ID.DEF] += equipStat[ID.DEF];
				StateEquip[ID.SPD] += equipStat[ID.SPD];
				StateEquip[ID.MOV] += equipStat[ID.MOV];
				StateEquip[ID.HIT] += equipStat[ID.HIT];
				StateEquip[ID.ATK] += equipStat[ID.ATK];
				StateEquip[ID.ATK_H] += equipStat[ID.ATK_H];
				StateEquip[ID.ATK_AL] += equipStat[ID.ATK_AL];
				StateEquip[ID.ATK_AH] += equipStat[ID.ATK_AH];
				
				EffectEquip[ID.EF_CRI] += equipStat[ID.CRI];
				EffectEquip[ID.EF_DHIT] += equipStat[ID.DHIT];
				EffectEquip[ID.EF_THIT] += equipStat[ID.THIT];
				EffectEquip[ID.EF_MISS] += equipStat[ID.MISS];
			}	
		}
		//update value
		calcShipAttributes(this.ShipID);
	}
	
	//setter	
	//setting attributes, called at load nbt data & init mob
	public void calcShipAttributes(byte id) {
		//init or renew bonus value, for short value: discard decimal
		//HP = (base + equip + (point + 1) * level * typeModify) * config scale
		StateFinal[ID.HP] = (Values.BaseHP[id] + StateEquip[ID.HP] + (float)(BonusPoint[ID.HP]+1) * (float)StateMinor[ID.N.ShipLevel] * TypeModify[ID.HP]) * (float)ConfigHandler.scaleShip[ID.HP]; 
		//DEF = base + ((point + 1) * level / 3 * 0.4 + equip) * typeModify
		StateFinal[ID.DEF] = (Values.BaseDEF[id] + StateEquip[ID.DEF] + ((float)(BonusPoint[ID.DEF]+1) * ((float)StateMinor[ID.N.ShipLevel])/3F) * 0.4F * TypeModify[ID.DEF]) * (float)ConfigHandler.scaleShip[ID.DEF];
		//SPD = base + ((point + 1) * level / 10 * 0.02 + equip) * typeModify
		StateFinal[ID.SPD] = (Values.BaseSPD[id] + StateEquip[ID.SPD] + ((float)(BonusPoint[ID.SPD]+1) * ((float)StateMinor[ID.N.ShipLevel])/10F) * 0.02F * TypeModify[ID.SPD]) * (float)ConfigHandler.scaleShip[ID.SPD];
		//MOV = base + ((point + 1) * level / 10 * 0.01 + equip) * typeModify
		StateFinal[ID.MOV] = (Values.BaseMOV[id] + StateEquip[ID.MOV] + ((float)(BonusPoint[ID.MOV]+1) * ((float)StateMinor[ID.N.ShipLevel])/10F) * 0.01F * TypeModify[ID.MOV]) * (float)ConfigHandler.scaleShip[ID.MOV];
		//HIT = base + ((point + 1) * level / 10 * 0.4 + equip) * typeModify
		StateFinal[ID.HIT] = (Values.BaseHIT[id] + StateEquip[ID.HIT] + ((float)(BonusPoint[ID.HIT]+1) * ((float)StateMinor[ID.N.ShipLevel])/10F) * 0.3F * TypeModify[ID.HIT]) * (float)ConfigHandler.scaleShip[ID.HIT];
		//ATK = (base + equip + ((point + 1) * level / 3) * typeModify) * config scale
		float atk = Values.BaseATK[id] + ((float)(BonusPoint[ID.ATK]+1) * ((float)StateMinor[ID.N.ShipLevel])/3F) * 0.5F * TypeModify[ID.ATK];
		StateFinal[ID.ATK] = (atk + StateEquip[ID.ATK]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_H] = (atk * 4F + StateEquip[ID.ATK_H]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AL] = (atk + StateEquip[ID.ATK_AL]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AH] = (atk * 4F + StateEquip[ID.ATK_AH]) * (float)ConfigHandler.scaleShip[ID.ATK];
		//KB Resistance = Level / 10 * 0.04
		float resisKB = (((float)StateMinor[ID.N.ShipLevel])/10F) * 0.067F;

		//max cap balue
		for(int i = 0; i < ConfigHandler.limitShip.length; i++) {
			if(ConfigHandler.limitShip[i] >= 0D && StateFinal[i] > ConfigHandler.limitShip[i]) {
				StateFinal[i] = (float) ConfigHandler.limitShip[i];
			}
		}
		
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_H] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_H] = (float) ConfigHandler.limitShip[ID.ATK];
		}
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_AL] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_AL] = (float) ConfigHandler.limitShip[ID.ATK];
		}
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_AH] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_AH] = (float) ConfigHandler.limitShip[ID.ATK];
		}

		//min cap value
		if(StateFinal[ID.SPD] < 0F) {
			StateFinal[ID.SPD] = 0F;
		}
		if(StateFinal[ID.MOV] < 0F) {
			StateFinal[ID.MOV] = 0F;
		}
		
		//set attribute by final value
		/**
		 * DO NOT SET ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(StateFinal[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(StateFinal[ID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(StateFinal[ID.HIT]+16); //此為找目標, 路徑的範圍
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
//		this.jumpMovementFactor = (1F + StateFinal[ID.MOV]) * 0.05F;
		
		//for server side
		if(!worldObj.isRemote) {
			sendSyncPacket();		//sync nbt data
		}
	}
	
	//set next exp value, no sync or update (for client load nbt data, gui display)
	public void setExpNext() {
		StateMinor[ID.N.ExpNext] = StateMinor[ID.N.ShipLevel] * 20 + 20;
	}
		
	//called when entity exp++
	public void addShipExp(int exp) {
		int CapLevel = getStateFlag(ID.F.IsMarried) ? 150 : 100;
		
		if(StateMinor[ID.N.ShipLevel] != CapLevel && StateMinor[ID.N.ShipLevel] < 150) {	//level is not cap level
			StateMinor[ID.N.ExpCurrent] += exp;
			if(StateMinor[ID.N.ExpCurrent] >= StateMinor[ID.N.ExpNext]) {
				//level up sound
				this.worldObj.playSoundAtEntity(this, "random.levelup", 0.75F, 1.0F);
				StateMinor[ID.N.ExpCurrent] -= StateMinor[ID.N.ExpNext];	//level up
				StateMinor[ID.N.ExpNext] = (StateMinor[ID.N.ShipLevel] + 1) * 20 + 20;
				setShipLevel(++StateMinor[ID.N.ShipLevel], true);
			}
		}	
	}
	
	@Override
	public void setShipDepth(double par1) {
		this.ShipDepth = par1;
	}
	
	//called when entity level up
	public void setShipLevel(int par1, boolean update) {
		//set level
		if(par1 < 151) {
			StateMinor[ID.N.ShipLevel] = par1;
		}
		//update attributes
		if(update) {
			LogHelper.info("DEBUG : set ship level with update");
			calcEquipAndUpdateState();
			this.setHealth(this.getMaxHealth());
		}
	}
	
	//set owner name (for player owner only)
	public void setOwnerName(String name) {
		this.ownerName = name;
	}
	
	public void setOwnerID(int par1) {
		this.setStateMinor(ID.N.OwnerID, par1);
	}
	
	//prevent player use name tag
	@Override
	public void setCustomNameTag(String str) {}
	
	//custom name tag method
	public void setNameTag(String str) {
        this.dataWatcher.updateObject(10, str);
    }
	
	//called when a mob die near the entity (used in event handler)
	public void addKills() {
		StateMinor[ID.N.Kills]++;
	}
	
	@Override
	public void setAmmoLight(int num) {
		this.StateMinor[ID.N.NumAmmoLight] = num;
	}
	
	@Override
	public void setAmmoHeavy(int num) {
		this.StateMinor[ID.N.NumAmmoHeavy] = num;
	}
	
	//ship attribute setter, sync packet in method: calcShipAttributes 
	public void setStateFinal(int state, float par1) {
		StateFinal[state] = par1;
	}
	
	public void setStateMinor(int state, int par1) {
		StateMinor[state] = par1;
		
		//若修改melee flag, 則reload AI
		if(state == ID.N.TargetAI) {
			clearAITargetTasks();
    		setAITargetList(par1);
		}
	}
	
	//called when GUI update
	public void setEffectEquip(int state, float par1) {
		EffectEquip[state] = par1;
	}
	
	public void setBonusPoint(int state, byte par1) {
		BonusPoint[state] = par1;
	}
	
	@Override
	public void setEntitySit() {
		this.setSitting(!this.isSitting());
        this.isJumping = false;
        this.getShipNavigate().clearPathEntity();
        this.setPathToEntity((PathEntity)null);
        this.setTarget((Entity)null);
        this.setAttackTarget((EntityLivingBase)null);
        
        if(this.ridingEntity instanceof BasicEntityMount) {
        	((BasicEntityMount) this.ridingEntity).getShipNavigate().clearPathEntity();
        	((BasicEntityMount) this.ridingEntity).getNavigator().clearPathEntity();
        	((BasicEntityMount) this.ridingEntity).setAttackTarget(null);
        }
	}
	
	//called when load nbt data or GUI click
	@Override
	public void setStateFlag(int id, boolean par1) {
		this.StateFlag[id] = par1;
		
		//若修改melee flag, 則reload AI
		if(!this.worldObj.isRemote && id == ID.F.UseMelee) {
			clearAITasks();
    		setAIList();
    		
    		//設定mount的AI
			if(this.ridingEntity instanceof BasicEntityMount) {
				((BasicEntityMount) this.ridingEntity).clearAITasks();
				((BasicEntityMount) this.ridingEntity).setAIList();
			}
		}
	}
	
	//called when load nbt data or GUI click
	public void setEntityFlagI(int flag, int par1) {
		if(par1 == 1) {
			this.StateFlag[flag] = true;
		}
		else {
			this.StateFlag[flag] = false;
		}
		
		//若修改melee flag, 則reload AI
		if(!this.worldObj.isRemote && flag == ID.F.UseMelee) {
			clearAITasks();
    		setAIList();
    		
    		//設定mount的AI
			if(this.ridingEntity instanceof BasicEntityMount) {
				((BasicEntityMount) this.ridingEntity).clearAITasks();
				((BasicEntityMount) this.ridingEntity).setAIList();
			}
		}
	}
	
	//called when entity spawn, set the type modify
	public void initTypeModify() {
		TypeModify[ID.HP] = Values.ModHP[ShipID];
		TypeModify[ID.ATK] = Values.ModATK[ShipID];
		TypeModify[ID.DEF] = Values.ModDEF[ShipID];
		TypeModify[ID.SPD] = Values.ModSPD[ShipID];
		TypeModify[ID.MOV] = Values.ModMOV[ShipID];
		TypeModify[ID.HIT] = Values.ModHIT[ShipID];
	}

	@Override
	public void setStateEmotion(int id, int value, boolean sync) {
		StateEmotion[id] = (byte)value;
		if(sync && !worldObj.isRemote) {
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 1), point);
		}
	}
	
	//emotion start time (CLIENT ONLY), called from model class
	@Override
	public void setStartEmotion(int par1) {
		StartEmotion = par1;
	}
	
	@Override
	public void setStartEmotion2(int par1) {
		StartEmotion2 = par1;
	}
	
	//manual send sync packet
	public void sendSyncPacket() {
		if(!worldObj.isRemote) {
			//for owner, send all data
			if(this.getOwner() != null) {
				EntityPlayerMP player = EntityHelper.getOnlinePlayer(this.getOwner());
				//owner在附近才需要sync
				if(player != null && this.getDistanceToEntity(player) < 48F) {
					CommonProxy.channelE.sendTo(new S2CEntitySync(this, 0), player);
				}	
			}
			
			//for other player, send ship state for display
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 30D);
			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 1), point);
		}
	}
	
	//sync data for GUI display
	public void sendGUISyncPacket() {
		if(!this.worldObj.isRemote) {
			if(this.getOwner() != null) {
				EntityPlayerMP player = EntityHelper.getOnlinePlayer(this.getOwner());
				//owner在附近才需要sync
				if(player != null && this.getDistanceToEntity(player) < 64F) {
					CommonProxy.channelG.sendTo(new S2CGUIPackets(this), player);
				}	
			}
		}
	}
	
	//right click on ship
	@Override
	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand

		//use item
		if(itemstack != null) {
			//use name tag, owner only
			if(itemstack.getItem() == Items.name_tag && EntityHelper.checkSameOwner(player, this)) {
	            //若該name tag有取名過, 則將名字貼到entity上
				if(itemstack.hasDisplayName()) {
					this.setNameTag(itemstack.getDisplayName());
					return false;
	            } 
			}
			
			//use repair bucket
			if(itemstack.getItem() == ModItems.BucketRepair) {
				//hp不到max hp時可以使用bucket
				if(this.getHealth() < this.getMaxHealth()) {
					if(!player.capabilities.isCreativeMode) {  //item-1 in non-creative mode
						--itemstack.stackSize;
	                }
	
	                if(this instanceof BasicEntityShipSmall) {
	                	this.heal(this.getMaxHealth() * 0.1F + 5F);	//1 bucket = 10% hp for small ship
	                }
	                else {
	                	this.heal(this.getMaxHealth() * 0.05F + 10F);	//1 bucket = 5% hp for large ship
	                }
	                
	                if (itemstack.stackSize <= 0) {  
	                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                }
	                
	                return true;
	            }			
			}
			
			//use kaitai hammer
			if(itemstack.getItem() == ModItems.KaitaiHammer && !worldObj.isRemote && 
			   ((this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) ||
				 EntityHelper.checkOP(player))) {
				//創造模式不消耗物品
                if(!player.capabilities.isCreativeMode) {  //damage +1 in non-creative mode
 	                itemstack.setItemDamage(itemstack.getItemDamage() + 1);
                    
                    //set item amount
                    ItemStack item0, item1, item2, item3;
                                
                    //large ship -> more materials
                    if(this.getKaitaiType() == 1) {
                    	if(ConfigHandler.easyMode) {	//easy mode
                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 1);
                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 1);
                        	item2 = new ItemStack(ModItems.Ammo, 1, 1);
                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 1);
                    	}
                    	else {						
                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 8 + rand.nextInt(3));
                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 8 + rand.nextInt(3));
                        	item2 = new ItemStack(ModItems.Ammo, 8 + rand.nextInt(3), 1);
                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 8 + rand.nextInt(3));
                    	}
                        
                    }
                    //small ship
                    else {
                        item0 = new ItemStack(ModItems.Grudge, 10 + rand.nextInt(8));
                    	item1 = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 0);
                    	item2 = new ItemStack(ModItems.Ammo, 10 + rand.nextInt(8), 0);
                    	item3 = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 1);
                    }
                    
                    EntityItem entityItem0 = new EntityItem(worldObj, posX+0.5D, posY+0.8D, posZ+0.5D, item0);
                    EntityItem entityItem1 = new EntityItem(worldObj, posX+0.5D, posY+0.8D, posZ-0.5D, item1);
                    EntityItem entityItem2 = new EntityItem(worldObj, posX-0.5D, posY+0.8D, posZ+0.5D, item2);
                    EntityItem entityItem3 = new EntityItem(worldObj, posX-0.5D, posY+0.8D, posZ-0.5D, item3);

                    worldObj.spawnEntityInWorld(entityItem0);
                    worldObj.spawnEntityInWorld(entityItem1);
                    worldObj.spawnEntityInWorld(entityItem2);
                    worldObj.spawnEntityInWorld(entityItem3);
                    
                    //drop inventory item
                    if(ExtProps != null) {
                    	for(int i = 0; i < ExtProps.slots.length; i++) {
            				ItemStack invitem = ExtProps.slots[i];

            				if(invitem != null) {
            					//設定要隨機噴出的range
            					float f = rand.nextFloat() * 0.8F + 0.1F;
            					float f1 = rand.nextFloat() * 0.8F + 0.1F;
            					float f2 = rand.nextFloat() * 0.8F + 0.1F;

            					while(invitem.stackSize > 0) {
            						int j = rand.nextInt(21) + 10;
            						//如果物品超過一個隨機數量, 會分更多疊噴出
            						if(j > invitem.stackSize) {  
            							j = invitem.stackSize;
            						}

            						invitem.stackSize -= j;
            						//將item做成entity, 生成到世界上
            						EntityItem item = new EntityItem(this.worldObj, this.posX+f, this.posY+f1, this.posZ+f2, new ItemStack(invitem.getItem(), j, invitem.getItemDamage()));
            						//如果有NBT tag, 也要複製到物品上
            						if(invitem.hasTagCompound()) {
            							item.getEntityItem().setTagCompound((NBTTagCompound)invitem.getTagCompound().copy());
            						}
            						
            						worldObj.spawnEntityInWorld(item);	//生成item entity
            					}
            				}
            			}
                    }//end drop inventory item
                    
                    playSound(Reference.MOD_ID+":ship-kaitai", ConfigHandler.shipVolume, 1.0F);
                    playSound(Reference.MOD_ID+":ship-death", ConfigHandler.shipVolume, 1.0F);
                }
                
                //物品用完時要設定為null清空該slot
                if(itemstack.getItemDamage() >= itemstack.getMaxDamage()) {  //物品耐久度用完時要設定為null清空該slot
                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
                }
                 
                this.setDead();
                
                return true;			
			}
			
			//use marriage ring
			if(itemstack.getItem() == ModItems.MarriageRing && !this.getStateFlag(ID.F.IsMarried) && 
			   player.isSneaking() && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {
				//stack-1 in non-creative mode
				if(!player.capabilities.isCreativeMode) {
                    --itemstack.stackSize;
                }

				//set marriage flag
                this.setStateFlag(ID.F.IsMarried, true);
                
                //player marriage num +1
    			ExtendPlayerProps extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);		
    			if(extProps != null) {
    				extProps.setMarriageNum(extProps.getMarriageNum() + 1);
    			}
                
                //play hearts effect
                TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
    			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 3, false), point);
                
    			//play marriage sound
    	        this.playSound(Reference.MOD_ID+":ship-love", ConfigHandler.shipVolume, 1.0F);
    	        
    	        //add 3 random bonus point
    	        for(int i = 0; i < 3; ++i) {
    	        	addRandomBonusPoint();
    	        }
    	        
    	        this.calcEquipAndUpdateState();
    	        
                if(itemstack.stackSize <= 0) {  //物品用完時要設定為null清空該slot
                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
                }
                
                return true;
			}
			
			//use modernization kit
			if(itemstack.getItem() == ModItems.ModernKit) {
				if(addRandomBonusPoint()) {	//add 1 random bonus
					if(!player.capabilities.isCreativeMode) {
	                    --itemstack.stackSize;
	                    
	                    if(itemstack.stackSize <= 0) {  //物品用完時要設定為null清空該slot
	                    	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                    }
	                }
					
					//play marriage sound
	    	        this.playSound(Reference.MOD_ID+":ship-love", ConfigHandler.shipVolume, 1.0F);
					
					return true;
				}	
			}
			
			//use modernization kit, owner only
			if(itemstack.getItem() == ModItems.OwnerPaper && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {
				NBTTagCompound nbt = itemstack.getTagCompound();
				boolean changeOwner = false;
				
				//check sign A and B is owner and another player
				if(itemstack.hasTagCompound()) {
					if(nbt.getString("signA").equals(player.getUniqueID().toString())) {
						if(nbt.getString("signB").length() > 0) {
							this.func_152115_b(nbt.getString("signB"));
							changeOwner = true;
						}
					}
					
					if(nbt.getString("signB").equals(player.getUniqueID().toString())) {
						if(nbt.getString("signA").length() > 0) {
							this.func_152115_b(nbt.getString("signA"));
							changeOwner = true;
						}
					}
				}
				
				if(changeOwner) {
					//play marriage sound
	    	        this.playSound(Reference.MOD_ID+":ship-death", ConfigHandler.shipVolume, 1.0F);
	    	        
					player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
					return true;
				}
			}
			
			//use lead
			if(itemstack.getItem() == Items.lead && this.allowLeashing()) {
				this.getShipNavigate().clearPathEntity();
				this.setLeashedToEntity(player, true);
				return true;
	        }
			
		}
		
		//如果已經被綑綁, 再點一下可以解除綑綁
		if(this.getLeashed() && this.getLeashedToEntity() == player) {
            this.clearLeashed(true, !player.capabilities.isCreativeMode);
            return true;
        }
	
		//shift+right click時打開GUI
		if(player.isSneaking() && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {  
    		FMLNetworkHandler.openGui(player, ShinColle.instance, ID.G.SHIPINVENTORY, this.worldObj, this.getEntityId(), 0, 0);
    		return true;
		}
		
		//click ship without shift = sit
		if(!this.worldObj.isRemote && !player.isSneaking() && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {			
			this.setEntitySit();
            return true;
        }

		return false;
	}
	
	//add random bonus point, NO SYNC, server only!
	private boolean addRandomBonusPoint() {
		int bonusChoose = rand.nextInt(6);
		
		//bonus point +1 if bonus point < 3
		if(BonusPoint[bonusChoose] < 3) {
			BonusPoint[bonusChoose] = (byte) (BonusPoint[bonusChoose] + 1);
			return true;
		}
		else {	//select other bonus point
			for(int i = 0; i < BonusPoint.length; ++i) {
				if(BonusPoint[i] < 3) {
					BonusPoint[i] = (byte) (BonusPoint[i] + 1);
					return true;
				}
			}
		}
		
		return false;
	}

	/**修改移動方法, 使其water跟lava中移動時像是flying entity
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

        if(EntityHelper.checkEntityIsInLiquid(this)) { //判定為液體中時, 不會自動下沉
            d0 = this.posY;
            this.moveFlying(movX, movZ, this.getStateFinal(ID.MOV)*0.4F); //水中的速度計算(含漂移效果)
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //水中阻力
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
            //水中撞到東西會上升
            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D - this.posY + d0, this.motionZ)) {
                this.motionY = 0.3D;
            }
        }
        else {									//其他移動狀態
            float f2 = 0.91F;
            
            if(this.onGround) {					//在地面移動
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float f4;
            
            if(this.onGround) {
                f4 = this.getAIMoveSpeed() * f3;
            }
            else {								//跳躍中
                f4 = this.jumpMovementFactor;
            }
            this.moveFlying(movX, movZ, f4);
            f2 = 0.91F;
            
            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            if(this.isOnLadder()) {				//爬樓梯中
                float f5 = 0.15F;
                //限制爬樓梯時的橫向移動速度
                if(this.motionX < (double)(-f5)) {
                    this.motionX = (double)(-f5);
                }
                if(this.motionX > (double)f5) {
                    this.motionX = (double)f5;
                }
                if(this.motionZ < (double)(-f5)) {
                    this.motionZ = (double)(-f5);
                }
                if(this.motionZ > (double)f5) {
                    this.motionZ = (double)f5;
                }

                this.fallDistance = 0.0F;
                //限制爬樓梯的落下速度
                if (this.motionY < -0.15D) {
                    this.motionY = -0.15D;
                }

                boolean flag = this.isSneaking();
                //若是爬樓梯時為sneaking, 則不會落下(卡在樓梯上)
                if(flag && this.motionY < 0D) {
                    this.motionY = 0D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //往樓梯推擠, 則會往上爬
            if(this.isCollidedHorizontally && this.isOnLadder()) {
                this.motionY = 0.4D;
            }
            //自然掉落
            if(this.worldObj.isRemote && (!this.worldObj.blockExists((int)this.posX, 0, (int)this.posZ) || !this.worldObj.getChunkFromBlockCoords((int)this.posX, (int)this.posZ).isChunkLoaded)) {
                if (this.posY > 0.0D) {
                    this.motionY = -0.1D;	//空氣中的gravity為0.1D
                }
                else {
                    this.motionY = 0.0D;
                }
            }
            else {
                this.motionY -= 0.08D;
            }
            //空氣中的三方向阻力
            this.motionY *= 0.98D;			
            this.motionX *= (double)f2;
            this.motionZ *= (double)f2;
//            LogHelper.info("DEBUG : f2 "+f2+" ");
        }
        //計算四肢擺動值
        this.prevLimbSwingAmount = this.limbSwingAmount;
        d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f6 > 1.0F)
        {
            f6 = 1.0F;
        }

        this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }
	
	//update ship move helper
	@Override
	protected void updateAITasks() {
		super.updateAITasks();
		
        EntityHelper.updateShipNavigator(this);
    }
	
	//update entity position
	@Override
	public void onUpdate() {
		super.onUpdate();
//		LogHelper.info("DEBUG : entity id "+this.worldObj.isRemote+" "+this.getEntityId());
		EntityHelper.checkDepth(this);
//		LogHelper.info("DEBUG : mount depth "+this.ShipDepth+" "+this.worldObj.isRemote);
		//client side
		if(this.worldObj.isRemote) {
			//有移動時, 產生水花特效
			if(this.getShipDepth() > 0D) {
				//(注意此entity因為設為非高速更新, client端不會更新motionX等數值, 需自行計算)
				double motX = this.posX - this.prevPosX;
				double motZ = this.posZ - this.prevPosZ;
				double parH = this.posY - (int)this.posY;
				
				if(motX != 0 || motZ != 0) {
					ParticleHelper.spawnAttackParticleAt(this.posX + motX*1.5D, this.posY + 0.4D, this.posZ + motZ*1.5D, 
							-motX*0.5D, 0D, -motZ*0.5D, (byte)15);
				}
			}
			
			if(this.ticksExisted % 10 == 0) {
				//generate HP state effect, use parm:lookX to send width size
				switch(getStateEmotion(ID.S.HPState)) {
				case ID.HPState.MINOR:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)4);
					break;
				case ID.HPState.MODERATE:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)5);
					break;
				case ID.HPState.HEAVY:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)7);
					break;
				default:
					break;
				}
			}//end every 10 ticks
			
			if(this.ticksExisted % 40 == 0) {
				//show guard position
				if(!this.getStateFlag(ID.F.CanFollow)) {
					//set guard entity
					if(this.getStateMinor(ID.N.GuardID) > 0) {
						Entity getEnt = EntityHelper.getEntityByID(this.getStateMinor(ID.N.GuardID), 0, true);
						
						if(getEnt != null) {
							this.setGuarded(getEnt);
						}
					}
					else {
						this.setGuarded(null);
					}
					
					//owner have to hold pointer item
//					EntityPlayer player = (EntityPlayer) this.getPlayerOwner();
					EntityPlayer player = null;
					if(this.getStateMinor(ID.N.OwnerID) > 0) {
						Entity getEnt = EntityHelper.getEntityByID(this.getStateMinor(ID.N.OwnerID), this.dimension, true);
						
						if(getEnt != null && getEnt instanceof EntityPlayer) {
							player = (EntityPlayer) getEnt;
						}
						//找不到owner (可能超出距離或者不同世界), reset owner = -1
						else {
							this.setStateMinor(ID.N.OwnerID, -1);
						}
					}
					
					if(player != null) {
						ItemStack item = player.inventory.getCurrentItem();
						
						if(item != null && item.getItem() instanceof PointerItem) {
							//標記在entity上
							if(this.getGuarded() != null) {
								ParticleHelper.spawnAttackParticleAtEntity(this.getGuarded(), 0.3D, 6D, 0D, (byte)2);
							}
							//標記載block上
							else if(this.getGuardedPos(1) >= 0) {
								ParticleHelper.spawnAttackParticleAt(this.getGuardedPos(0), this.getGuardedPos(1), this.getGuardedPos(2), 0.3D, 6D, 0D, (byte)25);
							}
						}
					}
				}
			}//end every 40 ticks
		}//end client side
	}

	//check entity state every tick
	@Override
	public void onLivingUpdate() {
        super.onLivingUpdate();

        //server side check
        if((!worldObj.isRemote)) {
//        	if(this.getAttackTarget()!=null) {
//    			LogHelper.info("DEBUG : ship target "+this.getAttackTarget().isDead+" "+this.getAttackTarget());
//    		}
        	
        	//clear dead target for vanilla AI bug
  			if(this.getAttackTarget() != null && !this.getAttackTarget().isEntityAlive()) {
  				this.setAttackTarget(null);
  			}
 			
  			//clear target if target is self/host
			if(getAttackTarget() == this.ridingEntity) {
				this.setAttackTarget(null);
			}
			
        	//decr immune time
        	if(this.StateMinor[ID.N.ImmuneTime] > 0) {
        		this.StateMinor[ID.N.ImmuneTime]--;
        	}
        	
        	//sync client and reset AI after server start 10 ticks
        	if(ticksExisted == 10) {
        		this.setStateFlag(ID.F.CanDrop, true);
        		clearAITasks();
        		clearAITargetTasks();	//reset AI for get owner after loading NBT data
        		setAIList();
        		setAITargetList(getStateMinor(ID.N.TargetAI));
        		decrGrudgeNum(0);		//check grudge
        		sendSyncPacket();		//sync packet to client
        	}
        	
        	//check every 10 ticks
        	if(ticksExisted % 10 == 0) {
        		//clear dead or same team target 
      			if(this.getAttackTarget() != null) {
      				if(!this.getAttackTarget().isEntityAlive() || EntityHelper.checkSameOwner(this, getAttackTarget())) {
      					this.setAttackTarget(null);
      				}
      			}
        		
        		//use bucket automatically
        		if((getMaxHealth() - getHealth()) > (getMaxHealth() * 0.1F + 5F)) {
	                if(decrSupplies(7)) {
	                	if(this instanceof BasicEntityShipSmall) {
		                	this.heal(this.getMaxHealth() * 0.1F + 5F);	//1 bucket = 10% hp for small ship
		                }
		                else {
		                	this.heal(this.getMaxHealth() * 0.05F + 10F);	//1 bucket = 5% hp for large ship
		                }
	                }
	            }
//        		LogHelper.info("DEBUG : check spawn: "+this.worldObj.getChunkProvider().getPossibleCreatures(EnumCreatureType.waterCreature, (int)posX, (int)posY, (int)posZ));
        	}//end every 10 ticks
        	
        	//check every 20 ticks
        	if(ticksExisted % 20 == 0) {
        		//cancel mounts
        		if(this.canSummonMounts()) {
        			if(getStateEmotion(ID.S.State) < ID.State.EQUIP00) {
      	  	  			//cancel riding
      	  	  			if(this.isRiding() && this.ridingEntity instanceof BasicEntityMount) {
      	  	  				BasicEntityMount mount = (BasicEntityMount) this.ridingEntity;
      	  	  				
      	  	  				if(mount.seat2 != null ) {
      	  	  					mount.seat2.setRiderNull();	
      	  	  				}
      	  	  				
      	  	  				mount.setRiderNull();
      	  	  				this.ridingEntity = null;
      	  	  			}
      	  	  		}
        		}
        		
//        		//debug: check disappear ship
//        		if(this.worldObj.provider.dimensionId == 0) {	//main world
//        			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//        		}
//        		else {	//other world
//        			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//        		}
        	}//end every 20 ticks
        	
        	//check every 40 ticks
        	if(ticksExisted % 40 == 0) {
        		if(this.getPlayerOwner() != null && this.dimension == this.getPlayerOwner().dimension && this.getPlayerOwner().getDistanceToEntity(this) < 48F) {
//        			LogHelper.info("DEBUG : ship onupdate sync 3: "+getPlayerOwner().getEntityId());
        			//update owner entity id (could be changed when owner change dimension or dead)
        			this.setStateMinor(ID.N.OwnerID, this.getPlayerOwner().getEntityId());
        			
        			//sync guard
    				CommonProxy.channelE.sendTo(new S2CEntitySync(this, 3), (EntityPlayerMP) this.getPlayerOwner());
        		}
	        }//end every 40 ticks
        	
        	//check every 100 ticks
        	if(ticksExisted % 100 == 0) {
        		float hpState = this.getHealth() / this.getMaxHealth();
        		
        		//check hp state
        		if(hpState > 0.75F) {		//normal
        			this.setStateEmotion(ID.S.HPState, ID.HPState.NORMAL, false);
        		}
        		else if(hpState > 0.5F){	//minor damage
        			this.setStateEmotion(ID.S.HPState, ID.HPState.MINOR, false);
        		}
				else if(hpState > 0.25F){	//moderate damage
					this.setStateEmotion(ID.S.HPState, ID.HPState.MODERATE, false);   			
				}
				else {						//heavy damage
					this.setStateEmotion(ID.S.HPState, ID.HPState.HEAVY, false);
				}
        		
        		//roll emtion: hungry > T_T > bored > O_O
        		if(getStateFlag(ID.F.NoFuel)) {
        			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.HUNGRY) {
//        				LogHelper.info("DEBUG : set emotion HUNGRY");
	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.HUNGRY, false);
	    			}
        		}
        		else {
        			if(hpState < 0.5F) {
    	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.T_T) {
//    	    				LogHelper.info("DEBUG : set emotion T_T");
    	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, false);
    	    			}			
    	    		}
        			else {
        				if(this.isSitting() && this.getRNG().nextInt(2) == 0) {	//50% for bored
        	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.BORED) {
//        	    				LogHelper.info("DEBUG : set emotion BORED");
        	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, false);
        	    			}
        	    		}
        	    		else {	//back to normal face
        	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.NORMAL) {
//        	    				LogHelper.info("DEBUG : set emotion NORMAL");
        	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
        	    			}
        	    		}
        			}
        		}
        		
        		//sync emotion every 5 sec
        		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 1), point);

        		//set air value
        		if(this.getAir() < 300) {
                	setAir(300);
                }
        		
        		//get ammo if no ammo
        		if(!this.hasAmmoLight()) { this.decrAmmoNum(2); }
        		if(!this.hasAmmoHeavy()) { this.decrAmmoNum(3); }
        		
        		//calc move distance and eat grudge (check position 5 sec ago)
        		double distX = posX - ShipPrevX;
        		double distY = posY - ShipPrevY;
        		double distZ = posZ - ShipPrevZ;
        		ShipPrevX = posX;
        		ShipPrevY = posY;
        		ShipPrevZ = posZ;
	        	int distSqrt = (int) MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
	        	decrGrudgeNum(distSqrt+5);	//eat grudge or change movement speed
        		
	        	//summon mounts
	        	if(this.canSummonMounts()) {
	        		//summon mount if emotion state >= equip00
	  	  	  		if(getStateEmotion(ID.S.State) >= ID.State.EQUIP00) {
	  	  	  			if(!this.isRiding()) {
	  	  	  				//summon mount entity
	  	  	  	  			BasicEntityMount mount = this.summonMountEntity();
	  	  	  	  			this.worldObj.spawnEntityInWorld(mount);
	  	  	  	  			
	  	  	  	  			//clear rider
	  	  	  	  			if(this.riddenByEntity != null) {
	  	  	  	  				this.riddenByEntity.mountEntity(null);
	  	  	  	  			}
	  	  	  	  			
	  	  	  	  			//set riding entity
		  	  	  			this.mountEntity(mount);
		  	  	  			
		  	  	  			//sync rider
		  	  	  			mount.sendSyncPacket();
	  	  	  			}
	  	  	  		}
	        	}
        	}//end every 100 ticks
        	
        	//auto recovery every 20 sec
        	if(this.ticksExisted % 400 == 0) {
        		if(this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.01F);
        		}
        	}
        	
        	//play timekeeping sound
        	if(ConfigHandler.timeKeeping) {
        		long time = this.worldObj.provider.getWorldTime();
            	int checkTime = (int)(time % 1000L);
            	if(checkTime == 0) {
            		playTimeSound((int)(time / 1000L) % 24);
            	}
        	}
        }//end if(server side)
        //client side
//        else {
//        	//set client side owner
//        	if(this.ticksExisted % 20 == 0) {
//        		LogHelper.info("DEBUG : entity sync 3: get owner id "+this.getStateMinor(ID.N.OwnerID));
//        	}
//        }
        
//        //both side
//        if(this.ridingEntity instanceof BasicEntityMount) {
//    		((BasicEntityMount)this.ridingEntity).prevRotationYaw = this.prevRotationYaw;
//    		((BasicEntityMount)this.ridingEntity).rotationYaw = this.rotationYaw;
//    		((BasicEntityMount)this.ridingEntity).renderYawOffset = this.renderYawOffset;
//		}
    }

	//timekeeping method
	protected void playTimeSound(int i) {
		//play entity attack sound
        this.playSound(Reference.MOD_ID+":ship-time"+i, ConfigHandler.timeKeepingVolume, 1.0F);
	}
	
	//melee attack method, no ammo cost, no attack speed, damage = 12.5% atk
	@Override
	public boolean attackEntityAsMob(Entity target) {
		//get attack value
		float atk = StateFinal[ID.ATK] * 0.125F;
		//set knockback value (testing)
		float kbValue = 0.15F;
		
		//experience++
		addShipExp(1);
		
		//grudge--
		decrGrudgeNum(1);
				
	    //將atk跟attacker傳給目標的attackEntityFrom方法, 在目標class中計算傷害
	    //並且回傳是否成功傷害到目標
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //play entity attack sound
        if(this.getRNG().nextInt(10) > 6) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
	    
	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }

	        //send packet to client for display partical effect   
	        if (!worldObj.isRemote) {
	        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
	    		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
			}
	    }

	    return isTargetHurt;
	}
	
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	public boolean attackEntityWithAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK];
		
		//set knockback value (testing)
		float kbValue = 0.05F;
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(1);
        
        //light ammo -1
        if(!decrAmmoNum(0)) {		//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc dist to target
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
        
        //play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //發射者煙霧特效
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY, this.posZ, distX, distY, distZ, true), point);

        //calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.N.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];		//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance
        
        //calc miss -> crit -> double -> tripple
  		if(rand.nextFloat() < missChance) {
          	atk = 0F;	//still attack, but no damage
          	//spawn miss particle
      		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
  		}
  		else {
  			//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
  			//calc critical
          	if(rand.nextFloat() < this.getEffectEquip(ID.EF_CRI)) {
          		atk *= 1.5F;
          		//spawn critical particle
          		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
          	}
          	else {
          		//calc double hit
              	if(rand.nextFloat() < this.getEffectEquip(ID.EF_DHIT)) {
              		atk *= 2F;
              		//spawn double hit particle
              		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, false), point);
              	}
              	else {
              		//calc double hit
                  	if(rand.nextFloat() < this.getEffectEquip(ID.EF_THIT)) {
                  		atk *= 3F;
                  		//spawn triple hit particle
                  		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, false), point);
                  	}
              	}
          	}
  		}
  		
  		//calc damage to player
  		if(target instanceof EntityPlayer) {
  			atk *= 0.25F;
    			
  			//check friendly fire
      		if(!ConfigHandler.friendlyFire) {
      			atk = 0F;
      		}
      		else if(atk > 59F) {
      			atk = 59F;	//same with TNT
      		}
  		}
      		
	    //將atk跟attacker傳給目標的attackEntityFrom方法, 在目標class中計算傷害
	    //並且回傳是否成功傷害到目標
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }
	        
        	//display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point1);
        }

	    return isTargetHurt;
	}

	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK_H];
		
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
		addShipExp(16);
		
		//grudge--
		decrGrudgeNum(1);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmoNum(1)) {	//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.N.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
       
        if(this.rand.nextFloat() < missChance) {
        	tarX = tarX - 5F + this.rand.nextFloat() * 10F;
        	tarY = tarY + this.rand.nextFloat() * 5F;
        	tarZ = tarZ - 5F + this.rand.nextFloat() * 10F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        
        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, -1F);
        this.worldObj.spawnEntityInWorld(missile);
        
        return true;
	}
	
	//be attacked method, 包括其他entity攻擊, anvil攻擊, arrow攻擊, fall damage都使用此方法 
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//set hurt face
    	if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.O_O) {
    		this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
    	}

		//進行def計算
        float reduceAtk = atk * (1F - (StateFinal[ID.DEF] - (float)rand.nextInt(20) + 10F) / 100F);    
        if(atk < 0) { atk = 0; }
        
        //若掉到世界外, 則傳送回y=4
        if(attacker.getDamageType().equals("outOfWorld")) {
        	this.posY = 4D;
        	return false;
        }
        
        //無敵的entity傷害無效
		if(this.isEntityInvulnerable()) {
            return false;
        }
		else if(attacker.getSourceOfDamage() != null) {	//不為null才算傷害, 可免疫毒/掉落/窒息等傷害
			Entity entity = attacker.getSourceOfDamage();
			
			//不會對自己造成傷害, 可免疫毒/掉落/窒息等傷害 (此為自己對自己造成傷害)
			if(entity.equals(this)) {  
				return false;
			}
			
			//若攻擊方同樣為ship, 則傷害-95% (使ship vs ship能打久一點)
			if(entity instanceof BasicEntityShip || entity instanceof BasicEntityAirplane || 
			   entity instanceof EntityRensouhou || entity instanceof BasicEntityMount) {
				reduceAtk *= 0.05F;
			}
			
			//若攻擊方為player, 則修正傷害
			if(entity instanceof EntityPlayer) {
				//若攻擊者為owner, 傷害 = 25%
				if(this.getOwner() != null && entity.getUniqueID().equals(this.getOwner().getUniqueID())) {
					reduceAtk *= 0.25F;
				}
				else {
					//若禁止friendlyFire, 則傷害設為0
					if(!ConfigHandler.friendlyFire) {
						return false;
					}
				}
			}

			//取消坐下動作
			this.setSitting(false);
			
			//若傷害力可能致死, 則尋找物品中有無repair goddess來取消掉此攻擊
			if(reduceAtk >= (this.getHealth() - 1F)) {
				if(this.decrSupplies(8)) {
					this.setHealth(this.getMaxHealth());
					this.StateMinor[ID.N.ImmuneTime] = 60;
					return false;
				}
			}
   
            //執行父class的被攻擊判定, 包括重置love時間, 計算火毒抗性, 計算鐵砧/掉落傷害, 
            //hurtResistantTime(0.5sec無敵時間)計算, 
            return super.attackEntityFrom(attacker, reduceAtk);
        }
		
		return false;
    }
	
	//decrese ammo number with type, or find ammo item from inventory
	protected boolean decrAmmoNum(int type) {
		switch(type) {
		case 0:  //use 1 light ammo
			if(hasAmmoLight()) { 
				--StateMinor[ID.N.NumAmmoLight];
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoLight] += 299;
					}
					else {
						StateMinor[ID.N.NumAmmoLight] += 29;
					}
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoLight] += 2699;
					}
					else {
						StateMinor[ID.N.NumAmmoLight] += 269;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 1:  //use 1 heavy ammo
			if(hasAmmoHeavy()) { 
				--StateMinor[ID.N.NumAmmoHeavy];
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoHeavy] += 149;
					}
					else {
						StateMinor[ID.N.NumAmmoHeavy] += 14;
					}
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoHeavy] += 1349;
					}
					else {
						StateMinor[ID.N.NumAmmoHeavy] += 134;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 2:	//no ammo light, use item
			if(decrSupplies(0)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumAmmoLight] += 300;
				}
				else {
					StateMinor[ID.N.NumAmmoLight] += 30;
				}
				return true;
			}
			else if(decrSupplies(2)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumAmmoLight] += 2700;
				}
				else {
					StateMinor[ID.N.NumAmmoLight] += 270;
				}
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 3:	//no ammo heavy, use item
			if(decrSupplies(1)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumAmmoHeavy] += 150;
				}
				else {
					StateMinor[ID.N.NumAmmoHeavy] += 15;
				}
				return true;
			}
			else if(decrSupplies(3)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumAmmoHeavy] += 1350;
				}
				else {
					StateMinor[ID.N.NumAmmoHeavy] += 135;
				}
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 4:  //use 4 light ammo
			if(StateMinor[ID.N.NumAmmoLight] > 3) {
				StateMinor[ID.N.NumAmmoLight] -= 4;
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoLight] += 296;
					}
					else {
						StateMinor[ID.N.NumAmmoLight] += 26;
					}
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoLight] += 2696;
					}
					else {
						StateMinor[ID.N.NumAmmoLight] += 266;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 5:  //use 2 heavy ammo
			if(StateMinor[ID.N.NumAmmoHeavy] > 1) { 
				StateMinor[ID.N.NumAmmoHeavy] -= 2;
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoHeavy] += 148;
					}
					else {
						StateMinor[ID.N.NumAmmoHeavy] += 13;
					}
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.N.NumAmmoHeavy] += 1348;
					}
					else {
						StateMinor[ID.N.NumAmmoHeavy] += 133;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		}
		
		return false;	//unknow attack type
	}
	
	//eat grudge and change movement speed
	protected void decrGrudgeNum(int par1) {
//		LogHelper.info("DEBUG : check grudge num");
		
		if(par1 > 215) {	//max cost = 215 (calc from speed 1 moving 5 sec)
			par1 = 215;
		}
		
		if(StateMinor[ID.N.NumGrudge] >= (int)par1) { //has enough fuel
			StateMinor[ID.N.NumGrudge] -= (int)par1;
		}
		else {
			if(decrSupplies(4)) {		//find grudge
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumGrudge] += 3600;
				}
				else {
					StateMinor[ID.N.NumGrudge] += 1200;
				}
				StateMinor[ID.N.NumGrudge] -= (int)par1;
			}
			else if(decrSupplies(5)) {	//find grudge block
				if(ConfigHandler.easyMode) {
					StateMinor[ID.N.NumGrudge] += 32400;
				}
				else {
					StateMinor[ID.N.NumGrudge] += 10800;
				}
				StateMinor[ID.N.NumGrudge] -= (int)par1;
			}
//避免吃掉含有儲存資訊的方塊, 因此停用此方塊作為grudge補充道具
//			else if(decrSupplies(6)) {	//find grudge heavy block
//				NumGrudge += 97200;
//				NumGrudge -= (int)par1;
//			}
		}
		
		if(StateMinor[ID.N.NumGrudge] <= 0) {
			setStateFlag(ID.F.NoFuel, true);
		}
		else {
			setStateFlag(ID.F.NoFuel, false);
		}
		
		//no fuel, clear AI
		if(getStateFlag(ID.F.NoFuel)) {
			//原本有AI, 則清除之
			if(this.targetTasks.taskEntries.size() > 0) {
				LogHelper.info("DEBUG : No fuel, clear AI");
				clearAITasks();
				clearAITargetTasks();
				sendSyncPacket();
				
				//設定mount的AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
				}
			}	
		}
		//has fuel, set AI
		else {
			if(this.targetTasks.taskEntries.size() < 2) {
				LogHelper.info("DEBUG : Get fuel, set AI");
				clearAITasks();
				clearAITargetTasks();
				setAIList();
				setAITargetList(getStateMinor(ID.N.TargetAI));
				sendSyncPacket();
				
				//設定mount的AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
					((BasicEntityMount) this.ridingEntity).setAIList();
				}
			}
		}
	}
	
	//decrese ammo/grudge/repair item, return true or false(not enough item)
	protected boolean decrSupplies(int type) {
		boolean isEnoughItem = true;
		int itemNum = 1;
		ItemStack itemType = null;
		
		//find ammo
		switch(type) {
		case 0:	//use 1 light ammo
			itemType = new ItemStack(ModItems.Ammo,1,0);
			break;
		case 1: //use 1 heavy ammo
			itemType = new ItemStack(ModItems.Ammo,1,2);
			break;
		case 2:	//use 1 light ammo container
			itemType = new ItemStack(ModItems.Ammo,1,1);
			break;
		case 3: //use 1 heavy ammo container
			itemType = new ItemStack(ModItems.Ammo,1,3);
			break;
		case 4: //use 1 grudge
			itemType = new ItemStack(ModItems.Grudge,1);
			break;
		case 5: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudge,1);
			break;
		case 6: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudgeHeavy,1);
			break;
		case 7:	//use 1 repair bucket
			itemType = new ItemStack(ModItems.BucketRepair,1);
			break;
		case 8:	//use 1 repair goddess
			itemType = new ItemStack(ModItems.RepairGoddess,1);
			break;	
		}
		
		//search item in ship inventory
		int i = findItemInSlot(itemType);
		if(i == -1) {		//item not found
			return false;
		}
		
		//decr item stacksize
		ItemStack getItem = this.ExtProps.slots[i];

		if(getItem.stackSize >= itemNum) {
			getItem.stackSize -= itemNum;
		}
		else {	//not enough item
			getItem.stackSize = 0;
			isEnoughItem = false;
		}
				
		if(getItem.stackSize == 0) {
			getItem = null;
		}
		
		//save back itemstack
		//no need to sync because no GUI opened
		this.ExtProps.slots[i] = getItem;	
		
		return isEnoughItem;	
	}

	//find item in ship inventory
	protected int findItemInSlot(ItemStack parItem) {
		ItemStack slotitem = null;

		//search ship inventory (except equip slots)
		for(int i = ContainerShipInventory.SLOTS_SHIPINV; i < ContainerShipInventory.SLOTS_PLAYERINV; i++) {
			slotitem = this.ExtProps.slots[i];
			if(slotitem != null && 
			    slotitem.getItem().equals(parItem.getItem()) && 
			    slotitem.getItemDamage() == parItem.getItemDamage()) {
				return i;	//found item
			}		
		}	
		
		return -1;	//item not found
	}
	
	@Override
	public boolean canFly() {
		return false;
	}
	
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}
	
	//true if use mounts
	public boolean canSummonMounts() {
		return false;
	}
	
	public BasicEntityMount summonMountEntity() {
		return null;
	}
	
	@Override
	public Entity getGuarded() {
		return this.guardedEntity;
	}

	@Override
	public void setGuarded(Entity entity) {
		if(entity != null && entity.isEntityAlive()) {
			this.guardedEntity = entity;
			this.setStateFlag(ID.F.CanFollow, false);
			this.setStateMinor(ID.N.GuardID, entity.getEntityId());
		}
		else {
			this.guardedEntity = null;
			this.setStateFlag(ID.F.CanFollow, true);
			this.setStateMinor(ID.N.GuardID, -1);
		}
	}

	@Override
	public int getGuardedPos(int vec) {
		switch(vec) {
		default:
			return this.getStateMinor(ID.N.GuardX);
		case 1:
			return this.getStateMinor(ID.N.GuardY);
		case 2:
			return this.getStateMinor(ID.N.GuardZ);
		case 3:
			return this.getStateMinor(ID.N.GuardDim);
		}
	}

	@Override
	public void setGuardedPos(int x, int y, int z, int dim) {
		this.setStateMinor(ID.N.GuardX, x);
		this.setStateMinor(ID.N.GuardY, y);
		this.setStateMinor(ID.N.GuardZ, z);
		this.setStateMinor(ID.N.GuardDim, dim);
		
		if(y <= 0) {
			this.setStateFlag(ID.F.CanFollow, true);
		}
		else {
			this.setStateFlag(ID.F.CanFollow, false);
		}
	}
	
	@Override
	public double getMountedYOffset() {
		return (double)this.height;
	}
	
//	//set slot 6 as held item 
//	@Override
//	public ItemStack getHeldItem() {
//		if(ExtProps != null && ExtProps.slots != null) {
//			return ExtProps.slots[6];
//		}
//		
//		return super.getHeldItem();
//	}

	
}
