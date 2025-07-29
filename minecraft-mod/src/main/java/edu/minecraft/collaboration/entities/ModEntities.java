package edu.minecraft.collaboration.entities;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom entities for the mod
 */
public class ModEntities {
    
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinecraftCollaborationMod.MOD_ID);
    
    public static final RegistryObject<EntityType<CollaborationAgent>> COLLABORATION_AGENT = 
        ENTITY_TYPES.register("collaboration_agent",
            () -> EntityType.Builder.of(CollaborationAgent::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .build(new ResourceLocation(MinecraftCollaborationMod.MOD_ID, "collaboration_agent").toString())
        );
    
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}