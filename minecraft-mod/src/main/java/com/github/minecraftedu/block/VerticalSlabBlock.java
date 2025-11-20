package com.github.minecraftedu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Vertical Slab Block
 *
 * A custom block that provides vertical slab functionality not present in vanilla Minecraft.
 * Unlike vanilla slabs which only have vertical positioning (bottom/top/double), vertical slabs
 * can be placed facing any of the 4 horizontal directions (north, south, east, west).
 *
 * Properties:
 * - facing: The horizontal direction the slab faces (north, south, east, west)
 * - waterlogged: Whether the block is waterlogged
 *
 * Collision Box:
 * - Half-width (8 pixels) in the direction perpendicular to facing
 * - Full height (16 pixels)
 * - Full depth (16 pixels in the facing direction)
 *
 * Example:
 * - Facing NORTH: Occupies the northern half of the block space
 * - Facing EAST: Occupies the eastern half of the block space
 */
public class VerticalSlabBlock extends Block implements SimpleWaterloggedBlock {

    /**
     * Horizontal facing direction property (north, south, east, west)
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Waterlogged property for compatibility with water
     */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * VoxelShapes for each facing direction
     * These define the collision and selection boxes for the block
     */
    // North facing: occupies northern half (Z: 0-8)
    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);

    // South facing: occupies southern half (Z: 8-16)
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);

    // West facing: occupies western half (X: 0-8)
    protected static final VoxelShape SHAPE_WEST = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);

    // East facing: occupies eastern half (X: 8-16)
    protected static final VoxelShape SHAPE_EAST = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    /**
     * Constructor
     *
     * @param properties Block properties (typically copied from a similar vanilla block)
     */
    public VerticalSlabBlock(Properties properties) {
        super(properties);

        // Set default state: facing north, not waterlogged
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, Boolean.FALSE)
        );
    }

    /**
     * Create the block state definition
     * Defines which properties this block has
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    /**
     * Get the VoxelShape (collision box) for this block based on its facing direction
     *
     * @param state The block state
     * @param level The world/level
     * @param pos The block position
     * @param context Collision context
     * @return The appropriate VoxelShape for the current facing direction
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);

        switch (facing) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case WEST:
                return SHAPE_WEST;
            case EAST:
                return SHAPE_EAST;
            default:
                return SHAPE_NORTH; // Fallback (should never happen)
        }
    }

    /**
     * Get the block state for placement
     *
     * This method determines the facing direction based on:
     * 1. The side of the block that was clicked
     * 2. The player's look direction
     *
     * Logic:
     * - If clicking on a horizontal face (north/south/east/west), place facing that direction
     * - If clicking on top/bottom, use the player's horizontal facing direction
     *
     * @param context The placement context (includes player direction, clicked face, etc.)
     * @return The block state to place, or null if placement should fail
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

        // Determine facing direction
        Direction facing;

        if (clickedFace.getAxis().isHorizontal()) {
            // Clicked on a horizontal face (north/south/east/west)
            // Place the slab facing that direction
            facing = clickedFace;
        } else {
            // Clicked on top or bottom
            // Use player's horizontal facing direction
            facing = context.getHorizontalDirection();
        }

        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    /**
     * Get the fluid state for this block
     * Required for waterlogging support
     *
     * @param state The block state
     * @return The fluid state (water if waterlogged, empty otherwise)
     */
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // Future enhancement: Allow combining two vertical slabs into a full block
    // This would require additional logic to detect adjacent vertical slabs
    // and potentially replace them with a full block (similar to vanilla slab behavior)
    //
    // Potential implementation:
    // @Override
    // public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    //     ItemStack itemStack = context.getItemInHand();
    //     // Check if placing same type of vertical slab in opposite direction
    //     // If so, allow replacement to create full block
    //     return itemStack.getItem() instanceof BlockItem blockItem
    //         && blockItem.getBlock() == this
    //         && /* check if opposite direction */;
    // }
}
