package com.github.minecraftedu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
 * 縦スラブ（縦向きのハーフブロック）
 *
 * バニラの Minecraft には無い、縦向きのハーフブロック。
 * バニラのハーフブロックは上・下（と 2 枚重ね）にしか置けないが、
 * 縦スラブは東西南北の 4 つの向きに置ける。
 *
 * プロパティ:
 * - facing: 向き（north / south / east / west）
 * - waterlogged: 水に浸かっているか
 *
 * 当たり判定:
 * - 向きの方向に厚さ半分（8 ピクセル）
 * - 高さはまるごと（16 ピクセル）
 * - 横幅はまるごと（16 ピクセル）
 *
 * 例:
 * - 北向き: ブロックの北半分をうめる
 * - 東向き: ブロックの東半分をうめる
 */
public class VerticalSlabBlock extends Block implements SimpleWaterloggedBlock {

    /**
     * 向き（東西南北）
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * 水に浸かっているか（水の中にも置けるようにするため）
     */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * 向きごとの形（当たり判定と、選んだときの枠）
     */
    // 北向き: 北半分（Z: 0〜8）
    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);

    // 南向き: 南半分（Z: 8〜16）
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);

    // 西向き: 西半分（X: 0〜8）
    protected static final VoxelShape SHAPE_WEST = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);

    // 東向き: 東半分（X: 8〜16）
    protected static final VoxelShape SHAPE_EAST = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    /**
     * コンストラクタ
     *
     * @param properties ブロックの性質（ふつうは、似たバニラのブロックからコピーする）
     */
    public VerticalSlabBlock(Properties properties) {
        super(properties);

        // はじめの状態: 北向き、水に浸かっていない
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, Boolean.FALSE)
        );
    }

    /**
     * このブロックが持つプロパティ（向き・水に浸かっているか）を決める
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    /**
     * 向きに合った形（当たり判定）を返す
     *
     * @param state ブロックの状態
     * @param level ワールド
     * @param pos ブロックの位置
     * @param context 当たり判定の場面
     * @return 今の向きに合った形
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
                return SHAPE_NORTH; // 念のため（横向き以外は来ない）
        }
    }

    /**
     * 置くときの状態（向き）を決める
     *
     * 向きは、次で決める:
     * 1. クリックしたブロックの面
     * 2. プレイヤーの向いている方向
     *
     * 決め方:
     * - 横の面（東西南北）をクリックしたら、クリックしたブロックにくっつく向きにする
     *   （まるごとのブロックの南の面をクリックすると、新しいブロックはその南に置かれる。そこで北半分をうめると、
     *    すき間なくくっつく。縦スラブの平らな面をクリックしたときは、そのブロックの中にすき間が半マス残る）
     * - 上か下の面をクリックしたら、プレイヤーの向いている方向にする
     *
     * @param context 置くときの情報（プレイヤーの向き、クリックした面など）
     * @return 置く状態（置けないときは null）
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

        // 向きを決める
        Direction facing;

        if (clickedFace.getAxis().isHorizontal()) {
            // 横の面（東西南北）をクリックした
            // クリックした面の反対向きに置く（その面の向きにすると、半マスはなれて すき間ができるため）
            facing = clickedFace.getOpposite();
        } else {
            // 上か下の面をクリックした
            // プレイヤーの向いている方向にする
            facing = context.getHorizontalDirection();
        }

        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    /**
     * このブロックの中の液体を返す（水の中に置けるようにするため）
     *
     * @param state ブロックの状態
     * @return 水に浸かっていれば水、そうでなければ空
     */
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * となりのブロックが変わったときに呼ばれる
     *
     * 水に浸かっていたら、水が流れ直すように知らせる
     * （知らせないと、となりを空けても水が流れ出さず、見た目が乱れるため。バニラのハーフブロックと同じ）
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * 回したときの状態（ストラクチャーブロックなどで建物を回して写すときに、向きもいっしょに回す）
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * 裏返したときの状態（ストラクチャーブロックなどで建物を反転して写すときに、向きもいっしょに裏返す）
     */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // これからの案: 縦スラブを 2 枚重ねて、まるごとのブロックにできるようにする
    // （バニラのハーフブロックと同じように、反対向きの同じ縦スラブを重ねたら入れ替える）
    //
    // 書き方の案:
    // @Override
    // public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    //     ItemStack itemStack = context.getItemInHand();
    //     // 同じ種類の縦スラブを、反対向きに置こうとしているか確かめる
    //     // そうなら入れ替えて、まるごとのブロックにする
    //     return itemStack.getItem() instanceof BlockItem blockItem
    //         && blockItem.getBlock() == this
    //         && /* 反対向きか確かめる */;
    // }
}
