package com.lulan.shincolle.entity.other;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**ENTITY ABYSS MISSILE
 * @parm world, host entity, tarX, tarY, tarZ, damage, knockback value
 * 
 * Parabola Orbit(for distance 7~65)
 * 形狀設定:
 * 在經過距離中點之前, 加上額外motionY向上以及accY向下
 * 到中點時, Vy = 0
 * 
 */
public class EntityAbyssMissile extends Entity {
	
    private IShipAttackBase host;	//main host type
    private EntityLiving host2;		//second host type: entity living
    private EntityLivingBase owner;
    
    //missile motion
    private float distX;			//target distance
    private float distY;
    private float distZ;
    private boolean isDirect;		//false:parabola  true:direct
  
    //for parabola y position
    private float accParaY;			//額外y軸加速度
    private int midFlyTime;			//一半的飛行時間
   
    //for direct only
    private float ACCE = 0.02F;		//預設加速度
    private float accX;				//三軸加速度
    private float accY;
    private float accZ;
    
    //missile attributes
    public int type;				//missile type
    private float atk;				//missile damage
    private float kbValue;			//knockback value
    private float missileHP;		//if hp = 0 -> onImpact
    private boolean isTargetHurt;	//knockback flag
    private World world;

    
    //基本constructor, size必須在此設定
    public EntityAbyssMissile(World world) {
    	super(world);
    	this.setSize(1.0F, 1.0F);
    }
    
    public EntityAbyssMissile(World world, IShipAttackBase host, float tarX, float tarY, float tarZ, float launchPos, float atk, float kbValue, boolean isDirect, float customAcc) {
        super(world);
        this.world = world;
        
        //設定host跟owner
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.owner = this.host.getPlayerOwner();
        
        //set basic attributes
        this.atk = atk;
        this.kbValue  = kbValue;
        this.posX = this.host2.posX;
        this.posZ = this.host2.posZ;
        this.posY = launchPos;
             
        //計算距離, 取得方向vector, 並且初始化速度, 使飛彈方向朝向目標
        this.distX = (float) (tarX - this.posX);
        this.distY = (float) (tarY - this.posY);
        this.distZ = (float) (tarZ - this.posZ);
        
        if(MathHelper.abs(this.distX) < 0.001F) this.distX = 0F;
        if(MathHelper.abs(this.distY) < 0.001F) this.distY = 0F;
        if(MathHelper.abs(this.distZ) < 0.001F) this.distZ = 0F;
        
        //設定直射或者拋物線
        this.isDirect = isDirect;
        this.type = 0;
        
        //設定飛彈速度
        if(customAcc > 0F) {
        	this.ACCE = customAcc;
        	if(customAcc > 0.09F) {
        		this.type = 2;
        	}
        	else if(customAcc > 0.05F) {
        		this.type = 1;
        	}
        }
        else {
        	this.ACCE = 0.02F;
        }
        
        //直射彈道, no gravity
    	float dist = MathHelper.sqrt_float(this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ);
  	    this.accX = (float) (this.distX / dist * this.ACCE);
	    this.accY = (float) (this.distY / dist * this.ACCE);
	    this.accZ = (float) (this.distZ / dist * this.ACCE);
	    this.motionX = this.accX;
	    this.motionY = this.accY;
	    this.motionZ = this.accZ;
 
	    //拋物線軌道計算, y軸初速加上 (一半飛行時間 * 額外y軸加速度)
	    if(!this.isDirect) {
	    	this.midFlyTime = (int) (0.5F * MathHelper.sqrt_float(2F * dist / this.ACCE));
	    	this.accParaY = this.ACCE;
	    	this.motionY = this.motionY + (double)this.midFlyTime * this.accParaY;
	    }
    }

    protected void entityInit() {}

    /**
     * Checks if the entity is in range to render by using the past in distance and 
     * comparing it to its average bounding box edge length * 64 * renderDistanceWeight 
     * Args: distance
     * 
     * 由於entity可能不為正方體, 故取平均邊長大小來計算距離, 此方法預設為256倍邊長大小
     */
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d1 = this.boundingBox.getAverageEdgeLength() * 256D;
        return distanceSq < d1 * d1;
    }

    //update entity
    //注意: 移動要在server+client都做畫面才能顯示平順, particle則只能在client做
    public void onUpdate() {
    	/**********both side***********/
    	//將位置更新 (包含server, client間同步位置, 才能使bounding box運作正常)
        this.setPosition(this.posX, this.posY, this.posZ);

        //計算發射體的高度
    	if(!this.isDirect) {  //直射軌道計算  	
			this.motionY = this.motionY + this.accY - this.accParaY;                   
    	}
    	else {
    		this.motionY += this.accY;
    	}
    	
    	//計算next tick的速度
        this.motionX += this.accX;
        this.motionZ += this.accZ;
        
    	//設定發射體的下一個位置
		this.posX += this.motionX;
		this.posY += this.motionY;
        this.posZ += this.motionZ;
           	
    	//計算模型要轉的角度 (RAD, not DEG)
        float f1 = MathHelper.sqrt_double(this.motionX*this.motionX + this.motionZ*this.motionZ);
        this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f1));
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ));    
        
        //依照x,z軸正負向修正角度(轉180)
        if(this.distX > 0) {
        	this.rotationYaw -= Math.PI;
        }
        else {
        	this.rotationYaw += Math.PI;
        }
        
        //更新位置等等基本資訊, 同時更新prePosXYZ
        super.onUpdate();
        
        /**********server side***********/
    	if(!this.worldObj.isRemote) {
    		//沒有host資料, 消除此飛彈
    		if(this.host == null) {
    			this.setDead();	//直接抹消, 不觸發爆炸
    			return;
    		}
    		
    		//發射超過20 sec, 設定為死亡(消失), 注意server restart後此值會歸零
    		if(this.ticksExisted > 300) {
    			this.setDead();	//直接抹消, 不觸發爆炸
    			return;
    		}
    		//sync missile type at start
    		else if(this.ticksExisted == 1) {
    			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, 8), point);
    		}
    		
    		//該位置碰到方塊, 則設定爆炸 (方法1: 直接用座標找方塊) 此方法由於把座標取int, 很多時候看起來有撞到但是依然抓不到方塊
    		if(!this.worldObj.blockExists((int)this.posX, (int)this.posY, (int)this.posZ)) {
    			this.onImpact(null);
    		}
    		
    		//該位置碰到方塊, 則設定爆炸 (方法2: 用raytrace找方塊)
    		Vec3 vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            Vec3 vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec3, vec31);          
            vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

            if(movingobjectposition != null) {
                vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
                this.onImpact(null);
            }
            
            //判定bounding box內是否有可以觸發爆炸的entity
            EntityLivingBase hitEntity = null;
            List hitList = null;
            hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(1.0D, 1.0D, 1.0D));
           
            //搜尋list, 找出第一個可以判定的目標, 即傳給onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) { 
                	hitEntity = (EntityLivingBase)hitList.get(i);
                	
                	/**不會對自己主人觸發爆炸
            		 * isEntityEqual() is NOT working
            		 * use entity id to check entity  */
                	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity.getEntityId()) && 
                	   !EntityHelper.checkSameOwner(host2, hitEntity)) {
                		break;	//get target entity
                	}
                	else {
            			hitEntity = null;
            		}
                }
            }
            
            //call onImpact
            if(hitEntity != null) {
            	this.onImpact(hitEntity);
            } 
            
    	}//end server side
    	/**********client side***********/
    	else {
    		//spawn particle by speed type
    		byte smokeType = 15;
    		
    		switch(this.type) {
    		default:
    			break;
    		case 1:
    			smokeType = 16;
    			break;
    		case 2:
    			smokeType = 27;
    			break;
    		}
    		
    		for (int j = 0; j < 3; ++j) {
            	ParticleHelper.spawnAttackParticleAt(this.posX-this.motionX*1.5D*j, this.posY+1D-this.motionY*1.5D*j, this.posZ-this.motionZ*1.5D*j, 
                		-this.motionX*0.1D, -this.motionY*0.1D, -this.motionZ*0.1D, smokeType);
    		}
    	}//end client side
    	   	
    }

    //check entity is not host itself
    private boolean isNotHost(int eid) {
		if(host2 != null && host2.getEntityId() == eid) {
			return false;
		} 	
		return true;
	}

	//撞擊判定時呼叫此方法
    protected void onImpact(EntityLivingBase target) {
    	isTargetHurt = false;
    	
    	//play sound
    	playSound(Reference.MOD_ID+":ship-explode", ConfigHandler.fireVolume * 1.5F, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
    	
    	//server side
    	if(!this.worldObj.isRemote) {
    		float missileAtk = atk;
    		EntityLivingBase host3 = null;
    		
    		if(host2 instanceof BasicEntityMount || host2 instanceof BasicEntityAirplane) {
        		host3 = host.getOwner();
    		}
    		
            if(target != null) {	//撞到entity引起爆炸
            	//若攻擊到同陣營entity (ex: owner), 則傷害設為0 (但是依然觸發擊飛特效)
            	if(EntityHelper.checkSameOwner(host2, target)) {
            		missileAtk = 0F;
            	}
            	
            	//calc critical, only for type:ship
            	if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
            		missileAtk *= 3F;
            		//spawn critical particle
            		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
                	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host2, 11, false), point);
            	}
            	
            	//若攻擊到玩家, 最大傷害固定為TNT傷害 (non-owner)
            	if(target instanceof EntityPlayer) {
            		missileAtk *= 0.25F;
            		
            		if(missileAtk > 59F) {
            			missileAtk = 59F;	//same with TNT
            		}
            		
            		//check friendly fire if host is not mob
            		if(EntityHelper.checkOwnerIsPlayer(host2) && !ConfigHandler.friendlyFire) {
            			missileAtk = 0F;
            		}
            	}

            	//設定該entity受到的傷害
        		if(host3 != null) {
        			isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(host3).setExplosion(), missileAtk);
        		}
            	else {
            		isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), missileAtk);
            	}
            	
            	//if attack success
        	    if(isTargetHurt) {
        	    	//calc kb effect
        	        if(this.kbValue > 0) {
        	        	target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
        	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
        	            motionX *= 0.6D;
        	            motionZ *= 0.6D;
        	        }             	 
        	    }
            }
            
            //計算範圍爆炸傷害: 判定bounding box內是否有可以吃傷害的entity
            EntityLivingBase hitEntity = null;
            AxisAlignedBB impactBox = this.boundingBox.expand(3.5D, 3.5D, 3.5D); 
            List hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
            
            //搜尋list, 找出第一個可以判定的目標, 即傳給onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) {
                	missileAtk = this.atk;
                	hitEntity = (EntityLivingBase)hitList.get(i);
                	
                	//目標不能是自己 or 主人
                	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity.getEntityId())) {

            			//calc critical, only for type:ship
                		if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
                    		missileAtk *= 3F;
                    		//spawn critical particle
                    		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
                        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host2, 11, false), point);
                    	}
                		
                		//若攻擊到同陣營entity (ex: owner), 則傷害設為0 (但是依然觸發擊飛特效)
                		if(EntityHelper.checkSameOwner(host2, hitEntity)) {
                    		missileAtk = 0F;
                    	}
                		
                		//若攻擊到玩家, 最大傷害固定為TNT傷害 (non-owner)
                    	if(hitEntity instanceof EntityPlayer) {
                    		missileAtk *= 0.25F;
                    		
                    		if(missileAtk > 59F) {
                    			missileAtk = 59F;	//same with TNT
                    		}
                    		
                    		//check friendly fire
                    		if(EntityHelper.checkOwnerIsPlayer(host2) && !ConfigHandler.friendlyFire) {
                    			missileAtk = 0F;
                    		}
                    	}
                		//對entity造成傷害
                    	if(host3 != null) {
                    		isTargetHurt = hitEntity.attackEntityFrom(DamageSource.causeMobDamage(host3).setExplosion(), missileAtk);
                    	}
                    	else {
                    		isTargetHurt = hitEntity.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), missileAtk);
                    	}
                		//if attack success
                	    if(isTargetHurt) {
                	    	//calc kb effect
                	        if(this.kbValue > 0) {
                	        	hitEntity.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
                	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
                	            motionX *= 0.6D;
                	            motionZ *= 0.6D;
                	        }             	 
                	    }
                	}//end can be collided with
                }//end hit target list for loop
            }//end hit target list != null
  
            //send packet to client for display partical effect
            TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 2, false), point);
            
            this.setDead();
        }//end if server side
    }

	//儲存entity的nbt
    public void writeEntityToNBT(NBTTagCompound nbt) {
    	nbt.setTag("direction", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));  
    	nbt.setFloat("atk", this.atk);
    }

    //讀取entity的nbt
    public void readEntityFromNBT(NBTTagCompound nbt) {
        if(nbt.hasKey("direction", 9)) {	//9為tag list
            NBTTagList nbttaglist = nbt.getTagList("direction", 6);	//6為tag double
            this.motionX = nbttaglist.func_150309_d(0);	//此為get double
            this.motionY = nbttaglist.func_150309_d(1);
            this.motionZ = nbttaglist.func_150309_d(2);
        }
        else {
            this.setDead();
        }
        
        this.atk = nbt.getFloat("atk");
    }

    //設定true可使其他生物判定是否要閃開此entity
    public boolean canBeCollidedWith() {
        return true;
    }

    //取得此entity的bounding box大小
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    //entity被攻擊到時呼叫此方法
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if(this.isEntityInvulnerable()) {	//對無敵目標回傳false
            return false;
        }
        
        //攻擊到飛彈會導致立刻爆炸
        this.onImpact(null);
        return true;
    }

    //render用, 陰影大小
    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    //計算光線用
    public float getBrightness(float p_70013_1_) {
        return 1.0F;
    }

    //render用, 亮度值屬於亮紫色
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }
    
    public void setMissileType(int par1) {
    	this.type = par1;
    }
    
}
