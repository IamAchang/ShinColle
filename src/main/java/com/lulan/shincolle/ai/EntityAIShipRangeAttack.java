package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;

/**ENTITY RANGE ATTACK AI
 * �q���}���g�bAI�ק�Ө�
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIShipRangeAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityShip host;  	//entity with AI
    private EntityLivingBase attackTarget;  //entity of target
    private int delayLight = 0;			//light attack delay (attack when time 0)
    private int maxDelayLight;	    //light attack max delay (calc from ship attack speed)
    private int delayHeavy = 0;			//heavy attack delay
    private int maxDelayHeavy;	    //heavy attack max delay (= light delay x5)    
    private double entityMoveSpeed;	//move speed when finding attack path
    private int onSightTime;		//target on sight time
    private float attackRange;		//attack range
    private float rangeSq;			//attack range square
    private int aimTime;			//time before fire
    
    //���u�e�i���\��
    private double distSq, distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipRangeAttack(BasicEntityShip host) {

        if (!(host instanceof BasicEntityShip)) {
            throw new IllegalArgumentException("RangeAttack AI requires BasicEntityShip with attackEntityWithAmmo");
        }
        else {
            this.host = host;
            this.setMutexBits(3);
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
    	EntityLivingBase target = this.host.getAttackTarget();
    	
        if (target != null && 
        	(this.host.getEntityFlag(AttrID.F_UseAmmoLight) && this.host.hasAmmoLight()) || 
        	(this.host.getEntityFlag(AttrID.F_UseAmmoHeavy) && this.host.hasAmmoHeavy())) {   
        	this.attackTarget = target;
        	LogHelper.info("DEBUG : try to range attack");
            return true;
        }       
        
        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
    	this.maxDelayLight = (int)(20F / (this.host.getFinalState(AttrID.SPD)));
    	this.maxDelayHeavy = (int)(100F / (this.host.getFinalState(AttrID.SPD)));    	
    	this.aimTime = (int) (20F * (float)( 150 - this.host.getShipLevel() ) / 150F) + 10;        
    	
    	//if target changed, check the delay time from prev attack
    	if(this.delayLight <= this.aimTime) {
    		this.delayLight = this.aimTime;
    	}
    	if(this.delayHeavy <= this.aimTime * 2) {
    		this.delayHeavy = this.aimTime * 2;
    	}
    	
        this.attackRange = this.host.getFinalState(AttrID.HIT) + 1F;
        this.rangeSq = this.attackRange * this.attackRange;
        
        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
       
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    public boolean continueExecuting() {
        return this.shouldExecute() || !this.host.getNavigator().noPath();
    }

    //���mAI��k
    public void resetTask() {
        this.attackTarget = null;
        this.onSightTime = 0;
        this.delayLight = this.aimTime;
        this.delayHeavy = this.aimTime;
    }

    //�i��AI
    public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	  	
    	if(this.attackTarget != null) {  //for lots of NPE issue-.-	
    		this.distX = this.attackTarget.posX - this.host.posX;
    		this.distY = this.attackTarget.posY - this.host.posY;
    		this.distZ = this.attackTarget.posZ - this.host.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;
    		    		
            onSight = this.host.getEntitySenses().canSee(this.attackTarget);  

	        //�i����, �hparf++, �_�h���m��0
	        if(onSight) {
	            ++this.onSightTime;
	        }
	        else {
	            this.onSightTime = 0;
	        }
	        
	        //�Y�����Ӥ[(�C12���ˬd�@��), ��50%���v�M���ؼ�, �Ϩ䭫�s��@���ؼ�
	        if((this.onSightTime > 240) && (this.onSightTime % 240 == 0)) {
	        	if(rand.nextInt(2) > 0)  {
	        		this.resetTask();
	        		return;
	        	}
	        }
	
	        //�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
	        if(distSq < (double)this.rangeSq && onSight) {
	            this.host.getNavigator().clearPathEntity();
	        }
	        else {	//�ؼв���, �h�~��l��	
	        	//�b�G�餤, �Ī��u�e�i
	        	if(this.host.getShipDepth() > 0D) {
	        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
	        		if(this.distY > 1.5D && this.host.getShipDepth() > 1.5D) {  //�קK�����u��
	        			this.motY = 0.2F;
	        		}
	        		else if(this.distY < -1D) {
	        			this.motY = -0.2F;
	        		}
	  		
	        		//�Y��������F��, �h���ո���
	        		if(this.host.isCollidedHorizontally) {
	        			this.host.setPosition(this.host.posX, this.host.posY + 0.3D, this.host.posZ);
	        		}
	        		
	        		//�Y���u�i��, �h�������u����
	        		if(this.host.getEntitySenses().canSee(this.attackTarget)) {
	        			double PetSpeed = this.host.getFinalState(AttrID.MOV);
	        			this.motX = (this.distX / this.distSq) * PetSpeed * 6D;
	        			this.motZ = (this.distZ / this.distSq) * PetSpeed * 6D;

	        			this.host.motionY = this.motY;
	        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
	        		}
	           	}
            	else {	//�D�G�餤, �ĥΤ@��M����|�k
            		this.host.getNavigator().tryMoveToEntityLiving(this.attackTarget, 1D);
            	}
            }
	
	        //�]�w������, �Y���[�ݪ�����
	        this.host.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
	        
	        //delay time decr
	        this.delayLight--;
	        this.delayHeavy--;

	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
	        if(this.delayHeavy <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoHeavy() && this.host.getEntityFlag(AttrID.F_UseAmmoHeavy)) {
	        	//�Y�ؼж]�X�d�� or �ؼгQ���� or �Z���Ӫ�, �h�������, �i��U�@��ai�P�w
	            if(distSq > (double)this.rangeSq || distSq < 4D || !onSight) { return; }
	            
	            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
	            this.host.attackEntityWithHeavyAmmo(this.attackTarget);
	            this.delayHeavy = this.maxDelayHeavy;
	        } 
	        
	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
	        if(this.delayLight <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoLight() && this.host.getEntityFlag(AttrID.F_UseAmmoLight)) {
	        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
	            if(distSq > (double)this.rangeSq || !onSight) { return; }
	            
	            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
	            this.host.attackEntityWithAmmo(this.attackTarget);
	            this.delayLight = this.maxDelayLight;
	        }
	        
	        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
	        if(this.delayHeavy < -200 || this.delayLight < -200) {
	        	this.resetTask();
	        	return;
	        }
    	}
    }
}