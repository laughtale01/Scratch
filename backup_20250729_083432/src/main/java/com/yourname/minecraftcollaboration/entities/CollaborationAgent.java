package com.yourname.minecraftcollaboration.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Agent entity that can be controlled by Scratch
 * Represents a helper/companion for educational activities
 */
public class CollaborationAgent extends PathfinderMob {
    
    private static final String AGENT_NAME_TAG = "AgentName";
    private static final String OWNER_UUID_TAG = "OwnerUUID";
    private static final String AGENT_ID_TAG = "AgentID";
    
    private String agentName = "Agent";
    private UUID ownerUUID;
    private String agentId;
    private BlockPos targetPosition;
    private boolean isFollowing = false;
    
    public CollaborationAgent(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.agentId = UUID.randomUUID().toString();
    }
    
    @Override
    protected void registerGoals() {
        // Basic movement goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        
        // Custom follow player goal
        this.goalSelector.addGoal(1, new FollowOwnerGoal());
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            // Show agent information
            player.sendSystemMessage(Component.literal(
                String.format("§a[Agent] §f%s (ID: %s)", agentName, agentId.substring(0, 8))
            ));
            
            if (ownerUUID != null && ownerUUID.equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§7This is your agent!"));
            }
        }
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Agents are invulnerable to damage (educational safety)
        return false;
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(AGENT_NAME_TAG, agentName);
        compound.putString(AGENT_ID_TAG, agentId);
        if (ownerUUID != null) {
            compound.putUUID(OWNER_UUID_TAG, ownerUUID);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(AGENT_NAME_TAG)) {
            agentName = compound.getString(AGENT_NAME_TAG);
        }
        if (compound.contains(AGENT_ID_TAG)) {
            agentId = compound.getString(AGENT_ID_TAG);
        }
        if (compound.hasUUID(OWNER_UUID_TAG)) {
            ownerUUID = compound.getUUID(OWNER_UUID_TAG);
        }
    }
    
    // Agent control methods
    
    public void setAgentName(String name) {
        this.agentName = name;
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }
    
    public String getAgentName() {
        return agentName;
    }
    
    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void moveToPosition(BlockPos pos) {
        this.targetPosition = pos;
        this.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }
    
    public void moveInDirection(String direction, int distance) {
        BlockPos currentPos = this.blockPosition();
        BlockPos newPos = currentPos;
        
        switch (direction.toLowerCase()) {
            case "forward":
            case "north":
                newPos = currentPos.north(distance);
                break;
            case "backward":
            case "south":
                newPos = currentPos.south(distance);
                break;
            case "left":
            case "west":
                newPos = currentPos.west(distance);
                break;
            case "right":
            case "east":
                newPos = currentPos.east(distance);
                break;
            case "up":
                newPos = currentPos.above(distance);
                break;
            case "down":
                newPos = currentPos.below(distance);
                break;
        }
        
        moveToPosition(newPos);
    }
    
    public void followPlayer(boolean follow) {
        this.isFollowing = follow;
    }
    
    public void performAction(String action) {
        switch (action.toLowerCase()) {
            case "jump":
                this.jumpFromGround();
                break;
            case "spin":
                this.setYRot(this.getYRot() + 180);
                break;
            case "dance":
                // Simple dance animation
                this.jumpFromGround();
                this.setYRot(this.getYRot() + 90);
                break;
        }
    }
    
    // Custom goal for following owner
    private class FollowOwnerGoal extends Goal {
        private static final double FOLLOW_DISTANCE = 3.0;
        private static final double MAX_DISTANCE = 10.0;
        
        @Override
        public boolean canUse() {
            if (!isFollowing || ownerUUID == null) {
                return false;
            }
            
            Player owner = level().getPlayerByUUID(ownerUUID);
            if (owner == null || owner.isSpectator()) {
                return false;
            }
            
            double distance = distanceToSqr(owner);
            return distance > FOLLOW_DISTANCE * FOLLOW_DISTANCE && distance < MAX_DISTANCE * MAX_DISTANCE;
        }
        
        @Override
        public void tick() {
            if (ownerUUID != null) {
                Player owner = level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
                    getNavigation().moveTo(owner, 1.0);
                }
            }
        }
    }
}