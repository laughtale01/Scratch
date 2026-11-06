package com.github.minecraftedu.commands;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Scratch の summonEntity で出した生き物に、みんなの作品（ブロックと飾り）を壊させない見張り。
 * 出した生き物には印（TAG）を付け、印の付いたもの（と、それが撃った弾・生んだ子ども・変身した先）について、Forge のイベントで止める。
 * プレイヤーや生き物へのダメージは、そのまま。自然にわいた生き物は、ワールドの決まり mobGriefing のまま。
 * （出したとたんに爆発・火がつくもの（TNT・雷など）と弾そのものは、CommandExecutor で出さない）
 * MinecraftEduMod で Forge のイベントバスにつなぐ。
 */
public final class SummonedEntityGuard {

    // 印（ワールドに保存されるので、入り直しても残る）
    public static final String TAG = "minecraftedu_summoned";

    private SummonedEntityGuard() {
    }

    /**
     * summonEntity で出した生き物に印を付ける（ワールドに足す前に呼ぶ）
     */
    public static void mark(Entity entity) {
        entity.addTag(TAG);
    }

    /**
     * 印が付いているか。弾は、撃ったものに印があっても同じに扱う
     * （弾には撃ったときに印を写すが、ワールドに足すより前に聞かれたときのため）
     */
    public static boolean isMarked(Entity entity) {
        if (entity == null) return false;
        if (entity.getTags().contains(TAG)) return true;
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            return owner != null && owner.getTags().contains(TAG);
        }
        return false;
    }

    /**
     * ワールドを荒らさせない（クリーパーの爆発・ガストの火の玉・エンダーマンがブロックを持つ、など）。
     * ただし、アレイ・ピグリン・村人は外す（同じイベントで、物を拾う・物々交換・畑仕事も止まってしまうため）。
     * 外した村人は、畑の育った作物を刈って植え直す。畑の土を踏み荒らすのは onFarmlandTrample で止める
     */
    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (isMarked(entity)
                && !(entity instanceof Allay || entity instanceof Piglin || entity instanceof AbstractVillager)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * 印の付いたもの（上で外した村人なども）が、落ちて畑の土を踏み荒らさないようにする
     */
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (isMarked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * 印の付いた生き物が撃った弾にも、印を写す。
     * ガストの火の玉は、打ち返されると持ち主がプレイヤーに替わり、撃ったガストが倒されると持ち主が分からなくなるので、
     * 弾そのものに印がないと止められない
     */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;  // 保存から読んだものは、印も保存されている
        if (event.getEntity() instanceof Projectile projectile && isMarked(projectile)) {
            projectile.addTag(TAG);
        }
    }

    /**
     * 印の付いたもの（クリーパー・火の玉など）の爆発では、ブロックを壊さず、火も置かない。
     * 爆発でダメージを受けるのは生き物だけにする（額縁・絵・防具立てを壊さず、TNT 付きトロッコに火をつけない）。
     * ガストの火の玉は、火がつくかを撃ったものについて、ブロックを壊すかを火の玉について聞くので、
     * 打ち返されたあとの火も止めるには、ここで止める
     */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!isMarked(event.getExplosion().getDirectSourceEntity())) return;
        event.getAffectedBlocks().clear();  // 壊すブロックの一覧。火を置く場所もここから選ばれる
        event.getAffectedEntities().removeIf(SummonedEntityGuard::isDecoration);
    }

    /**
     * 印の付いた弾が当たったとき（爆発とは別に、当たった相手やブロックに直接働く）。
     * - 飾り（額縁・絵・防具立て・TNT 付きトロッコ・エンドクリスタルなど）に当たったら、当たったときの動きをさせずに消す
     * - ブロックに当たったら、弾の火を消してから当てる（燃えている弾は TNT・たき火に火をつけるが、
     *   打ち返された・持ち主がいない弾は、それを Forge に聞かずに決めるため）
     * - ブレイズの火の玉は、ブロックに当たったら消す（持ち主がいないと、イベントを聞かずに火を置くため）
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!isMarked(projectile)) return;
        HitResult hit = event.getRayTraceResult();
        boolean stop = false;
        if (hit instanceof EntityHitResult entityHit) {
            stop = isDecoration(entityHit.getEntity());
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            projectile.clearFire();
            stop = projectile instanceof SmallFireball;
        }
        if (stop) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);  // 当たったときの動きをさせない
            projectile.discard();
        }
    }

    /**
     * 生き物が別の生き物に変わったら（ゾンビ → ドラウンド、ピグリン → ゾンビピグリン など）、印を写す
     * （スケルトン → ストレイはこのイベントが出ないが、ストレイはブロックを壊さない）
     */
    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        if (event.getEntity().getTags().contains(TAG)) {
            mark(event.getOutcome());
        }
    }

    /**
     * 印の付いた生き物から生まれた子どもにも、印を写す（子ヒツジが草を食べて土に変える、などを止める）
     * （村人の子どもは、このイベントが出ないので写らない。自然の村人と同じになる）
     */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        if (event.getChild() != null && (isMarked(parentA) || isMarked(parentB))) {
            mark(event.getChild());
        }
    }

    /**
     * 飾り（生き物でないもの、と防具立て）か。爆発や弾から守る。
     * エンダードラゴンの頭などの当たり判定の部品は、生き物ではないが、本体（生き物）で決める
     */
    private static boolean isDecoration(Entity entity) {
        Entity target = entity instanceof PartEntity<?> part ? part.getParent() : entity;
        return !(target instanceof LivingEntity) || target instanceof ArmorStand;
    }
}
