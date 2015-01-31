package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.Names;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**Process client side packet by Jabelar
 * this class is intended to be sent from server to client to keep custom entities synced
 * 
 * SYNC PACKET: for ExtendEntityProps, client should not send sync-packet back to server
 */
public class ProcessPacketClientSide { 
	//for entity sync
	private static int packetTypeID;
	private static int entityID;
	private static Entity foundEntity;
	//for particle position
	private static byte particleType;
	private static float posX;
	private static float posY;
	private static float posZ;
	private static float lookX;
	private static float lookY;
	private static float lookZ;
	
	public ProcessPacketClientSide() {}

	@SideOnly(Side.CLIENT)
	public static void processPacketOnClient(ByteBuf parBB, Side parSide) throws IOException {
			
		if (parSide == Side.CLIENT) {
			LogHelper.info("DEBUG : recv packet (client side)");

			World theWorld = Minecraft.getMinecraft().theWorld;
			ByteBufInputStream bbis = new ByteBufInputStream(parBB);
   
			//read packet ID
			packetTypeID = bbis.readByte();
			
			switch (packetTypeID) {
			case Names.Packets.ENTITY_SYNC:  //entity sync packet
				//read entity ID
				entityID = bbis.readInt();
				foundEntity = getEntityByID(entityID, theWorld);

				if (foundEntity instanceof BasicEntityShip) {
					BasicEntityShip foundEntityShip = (BasicEntityShip)foundEntity;
					//read packet data
					foundEntityShip.ShipLevel = bbis.readShort();
					foundEntityShip.Kills = bbis.readInt();
					
					foundEntityShip.ArrayEquip[AttrID.HP] = bbis.readFloat();
					foundEntityShip.ArrayEquip[AttrID.ATK] = bbis.readFloat();
					foundEntityShip.ArrayEquip[AttrID.DEF] = bbis.readFloat();
					foundEntityShip.ArrayEquip[AttrID.SPD] = bbis.readFloat();
					foundEntityShip.ArrayEquip[AttrID.MOV] = bbis.readFloat();
					foundEntityShip.ArrayEquip[AttrID.HIT] = bbis.readFloat();
					
					foundEntityShip.ArrayFinal[AttrID.HP] = bbis.readFloat();
					foundEntityShip.ArrayFinal[AttrID.ATK] = bbis.readFloat();
					foundEntityShip.ArrayFinal[AttrID.DEF] = bbis.readFloat();
					foundEntityShip.ArrayFinal[AttrID.SPD] = bbis.readFloat();
					foundEntityShip.ArrayFinal[AttrID.MOV] = bbis.readFloat();
					foundEntityShip.ArrayFinal[AttrID.HIT] = bbis.readFloat();
					
					foundEntityShip.EntityState[AttrID.State] = bbis.readByte();
					foundEntityShip.EntityState[AttrID.Emotion] = bbis.readByte();
					foundEntityShip.EntityState[AttrID.SwimType] = bbis.readByte();
					
					foundEntityShip.BonusPoint[0] = bbis.readByte();
					foundEntityShip.BonusPoint[1] = bbis.readByte();
					foundEntityShip.BonusPoint[2] = bbis.readByte();
					foundEntityShip.BonusPoint[3] = bbis.readByte();
					foundEntityShip.BonusPoint[4] = bbis.readByte();
					foundEntityShip.BonusPoint[5] = bbis.readByte();
				}
				break;
				
			case Names.Packets.PARTICLE_ATK:  //attack particle
				//read entity ID
				entityID = bbis.readInt();
				foundEntity = getEntityByID(entityID, theWorld);
				//read particle type
				particleType = bbis.readByte();
				//spawn particle
				ParticleHelper.spawnAttackParticle(foundEntity, particleType);			
				break;
				
			case Names.Packets.PARTICLE_ATK2:  //attack particle at custom position
				//read position + look vector
				posX = bbis.readFloat();
				posY = bbis.readFloat();
				posZ = bbis.readFloat();
				lookX = bbis.readFloat();
				lookY = bbis.readFloat();
				lookZ = bbis.readFloat();
				//read particle type
				particleType = bbis.readByte();
				//spawn particle
				ParticleHelper.spawnAttackParticleCustomVector((double)posX, (double)posY, (double)posZ, (double)lookX, (double)lookY, (double)lookZ, particleType);			
				break;
				
			}//end switch
		bbis.close();   
		}
	}
 
	//get entity by ID
	public static Entity getEntityByID(int entityID, World world) {         
		for(Object o: world.getLoadedEntityList()) {                        
			if(((Entity)o).getEntityId() == entityID) {                                
				LogHelper.info("Found the entity by ID");                                
				return ((Entity)o);                        
			}                
		}                
		return null;        
	} 
	
	
}
