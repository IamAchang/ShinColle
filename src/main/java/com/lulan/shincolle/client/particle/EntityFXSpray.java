package com.lulan.shincolle.client.particle;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;


/**SPRAY PARTICLE
 * 從cloud修改來, 用於液體中移動的特效
 */
@SideOnly(Side.CLIENT)
public class EntityFXSpray extends EntityFX {
	
    float Scale;
 
    public EntityFXSpray(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float colorR, float colorG, float colorB, float colorA) {
        super(world, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        this.motionX *= 0.1D;
        this.motionZ *= 0.1D;
        this.motionY *= 0.1D;
        this.motionX += motionX;
        this.motionZ += motionZ;
        this.motionY += motionY;
        this.particleRed = colorR;
        this.particleGreen = colorG;
        this.particleBlue = colorB;
        this.particleAlpha = colorA;
        this.particleScale *= 1.5F;
        this.Scale = this.particleScale;
        this.particleMaxAge = 40;
        this.noClip = false;
        
        if(motionY >= 4D) {
        	this.motionY = 0D;
        	this.Scale = 15F;
        }

    }
    
    @Override
	@SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        return 240;
    }

    public void renderParticle(Tessellator tess, float ticks, float par3, float par4, float par5, float par6, float par7) {
        float f6 = ((float)this.particleAge + ticks) / (float)this.particleMaxAge * 32.0F;

        if(f6 < 0.0F) {
            f6 = 0.0F;
        }

        if(f6 > 1.0F) {
            f6 = 1.0F;
        }

        this.particleScale = this.Scale * f6;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);	//防止water block蓋過particle
//        GL11.glEnable(GL11.GL_BLEND);
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        super.renderParticle(tess, ticks, par3, par4, par5, par6, par7);
//        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
//        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate() {
    	//this particle is CLIENT ONLY
    	if(this.worldObj.isRemote) {
    		this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            if(this.particleAge++ > this.particleMaxAge) {
                this.setDead();
                return;
            }
            
        	this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge); 
            this.motionX *= 0.96D;
            this.motionY *= 0.96D;
            this.motionZ *= 0.96D;

            if(this.onGround) {
                this.motionX *= 0.7D;
                this.motionZ *= 0.7D;
            }
            
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
    	}  
    }
}