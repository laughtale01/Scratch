package com.yourname.minecraftcollaboration.blockpacks;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * 教育目的に応じたカスタムブロックパックの管理システム
 */
public class BlockPackManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static BlockPackManager instance;
    private final LanguageManager languageManager;
    
    // ブロックパック管理
    private final Map<String, BlockPack> blockPacks = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerActivePacks = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> packSubscribers = new ConcurrentHashMap<>();
    
    // 定義済みブロックパック
    private static final String BASIC_PACK = "basic";
    private static final String EDUCATIONAL_PACK = "educational";
    private static final String CREATIVE_PACK = "creative";
    private static final String ARCHITECTURAL_PACK = "architectural";
    private static final String REDSTONE_PACK = "redstone";
    private static final String NATURE_PACK = "nature";
    private static final String BEGINNER_PACK = "beginner";
    private static final String ADVANCED_PACK = "advanced";
    
    private BlockPackManager() {
        this.languageManager = LanguageManager.getInstance();
        initializeDefaultPacks();
    }
    
    public static BlockPackManager getInstance() {
        if (instance == null) {
            instance = new BlockPackManager();
        }
        return instance;
    }
    
    /**
     * デフォルトブロックパックの初期化
     */
    private void initializeDefaultPacks() {
        // 基本パック - 最も基本的なブロック
        createBasicPack();
        
        // 教育パック - 教育に適したブロック
        createEducationalPack();
        
        // クリエイティブパック - 創造性を重視したブロック
        createCreativePack();
        
        // 建築パック - 建築に特化したブロック
        createArchitecturalPack();
        
        // レッドストーンパック - プログラミング学習用
        createRedstonePack();
        
        // 自然パック - 自然素材のブロック
        createNaturePack();
        
        // 初心者パック - 学習開始に適したブロック
        createBeginnerPack();
        
        // 上級者パック - 高度な建築用ブロック
        createAdvancedPack();
        
        LOGGER.info("Initialized {} default block packs", blockPacks.size());
    }
    
    /**
     * 基本パックの作成
     */
    private void createBasicPack() {
        List<Block> blocks = Arrays.asList(
            Blocks.STONE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.WOOD,
            Blocks.COBBLESTONE, Blocks.SAND, Blocks.GRAVEL, Blocks.GLASS,
            Blocks.WATER, Blocks.LAVA
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "基本ブロック");
        names.put("en_US", "Basic Blocks");
        names.put("zh_CN", "基础方块");
        names.put("zh_TW", "基礎方塊");
        names.put("ko_KR", "기본 블록");
        names.put("es_ES", "Bloques Básicos");
        names.put("fr_FR", "Blocs de Base");
        names.put("de_DE", "Grundblöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "最も基本的な建築ブロックのセットです。初心者に適しています。");
        descriptions.put("en_US", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("zh_CN", "最基本的建筑方块集合，适合初学者使用。");
        descriptions.put("zh_TW", "最基本的建築方塊集合，適合初學者使用。");
        descriptions.put("ko_KR", "가장 기본적인 건축 블록 세트입니다. 초보자에게 적합합니다.");
        descriptions.put("es_ES", "El conjunto de bloques de construcción más fundamental. Perfecto para principiantes.");
        descriptions.put("fr_FR", "L'ensemble de blocs de construction le plus fondamental. Parfait pour les débutants.");
        descriptions.put("de_DE", "Die grundlegendste Sammlung von Bausteinen. Perfekt für Anfänger.");
        
        BlockPack pack = new BlockPack(BASIC_PACK, names, descriptions, blocks, 
            BlockPackCategory.BASIC, DifficultyLevel.BEGINNER, true);
        blockPacks.put(BASIC_PACK, pack);
    }
    
    /**
     * 教育パックの作成
     */
    private void createEducationalPack() {
        List<Block> blocks = Arrays.asList(
            Blocks.WHITE_WOOL, Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL,
            Blocks.YELLOW_WOOL, Blocks.ORANGE_WOOL, Blocks.PURPLE_WOOL, Blocks.PINK_WOOL,
            Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.SMOOTH_STONE, Blocks.CHISELED_STONE_BRICKS,
            Blocks.WHITE_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.GREEN_CONCRETE,
            Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.REDSTONE_LAMP
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "教育用ブロック");
        names.put("en_US", "Educational Blocks");
        names.put("zh_CN", "教育方块");
        names.put("zh_TW", "教育方塊");
        names.put("ko_KR", "교육용 블록");
        names.put("es_ES", "Bloques Educativos");
        names.put("fr_FR", "Blocs Éducatifs");
        names.put("de_DE", "Bildungsblöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "色彩豊かで教育目的に最適化されたブロックセットです。");
        descriptions.put("en_US", "Colorful blocks optimized for educational purposes.");
        descriptions.put("zh_CN", "色彩丰富，专为教育目的优化的方块集合。");
        descriptions.put("zh_TW", "色彩豐富，專為教育目的優化的方塊集合。");
        descriptions.put("ko_KR", "교육 목적에 최적화된 다채로운 블록 세트입니다.");
        descriptions.put("es_ES", "Bloques coloridos optimizados para propósitos educativos.");
        descriptions.put("fr_FR", "Blocs colorés optimisés à des fins éducatives.");
        descriptions.put("de_DE", "Farbenfrohe Blöcke, die für Bildungszwecke optimiert sind.");
        
        BlockPack pack = new BlockPack(EDUCATIONAL_PACK, names, descriptions, blocks,
            BlockPackCategory.EDUCATIONAL, DifficultyLevel.BEGINNER, true);
        blockPacks.put(EDUCATIONAL_PACK, pack);
    }
    
    /**
     * クリエイティブパックの作成
     */
    private void createCreativePack() {
        List<Block> blocks = Arrays.asList(
            Blocks.DIAMOND_BLOCK, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK, Blocks.EMERALD_BLOCK,
            Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.COAL_BLOCK,
            Blocks.PURPUR_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.PRISMARINE,
            Blocks.NETHER_BRICKS, Blocks.END_STONE_BRICKS, Blocks.BLACKSTONE,
            Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.BEACON,
            Blocks.CONDUIT, Blocks.DRAGON_EGG
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "クリエイティブブロック");
        names.put("en_US", "Creative Blocks");
        names.put("zh_CN", "创意方块");
        names.put("zh_TW", "創意方塊");
        names.put("ko_KR", "창의적 블록");
        names.put("es_ES", "Bloques Creativos");
        names.put("fr_FR", "Blocs Créatifs");
        names.put("de_DE", "Kreative Blöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "創造性を刺激する特殊で美しいブロックのコレクションです。");
        descriptions.put("en_US", "A collection of special and beautiful blocks to inspire creativity.");
        descriptions.put("zh_CN", "激发创造力的特殊美丽方块集合。");
        descriptions.put("zh_TW", "激發創造力的特殊美麗方塊集合。");
        descriptions.put("ko_KR", "창의성을 자극하는 특별하고 아름다운 블록 컬렉션입니다.");
        descriptions.put("es_ES", "Una colección de bloques especiales y hermosos para inspirar creatividad.");
        descriptions.put("fr_FR", "Une collection de blocs spéciaux et beaux pour inspirer la créativité.");
        descriptions.put("de_DE", "Eine Sammlung besonderer und schöner Blöcke zur Inspiration der Kreativität.");
        
        BlockPack pack = new BlockPack(CREATIVE_PACK, names, descriptions, blocks,
            BlockPackCategory.CREATIVE, DifficultyLevel.INTERMEDIATE, true);
        blockPacks.put(CREATIVE_PACK, pack);
    }
    
    /**
     * 建築パックの作成
     */
    private void createArchitecturalPack() {
        List<Block> blocks = Arrays.asList(
            Blocks.STONE_STAIRS, Blocks.BRICK_STAIRS, Blocks.QUARTZ_STAIRS,
            Blocks.STONE_SLAB, Blocks.BRICK_SLAB, Blocks.QUARTZ_SLAB,
            Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.OAK_FENCE,
            Blocks.OAK_DOOR, Blocks.IRON_DOOR, Blocks.OAK_TRAPDOOR,
            Blocks.PILLAR_QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.SMOOTH_QUARTZ, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE,
            Blocks.POLISHED_GRANITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_ANDESITE
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "建築用ブロック");
        names.put("en_US", "Architectural Blocks");
        names.put("zh_CN", "建筑方块");
        names.put("zh_TW", "建築方塊");
        names.put("ko_KR", "건축용 블록");
        names.put("es_ES", "Bloques Arquitectónicos");
        names.put("fr_FR", "Blocs Architecturaux");
        names.put("de_DE", "Architektonische Blöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "詳細で美しい建築物を作るための専門的なブロックセットです。");
        descriptions.put("en_US", "Professional blocks for creating detailed and beautiful architecture.");
        descriptions.put("zh_CN", "用于创建精细美观建筑的专业方块集合。");
        descriptions.put("zh_TW", "用於創建精細美觀建築的專業方塊集合。");
        descriptions.put("ko_KR", "세밀하고 아름다운 건축물을 만들기 위한 전문 블록 세트입니다.");
        descriptions.put("es_ES", "Bloques profesionales para crear arquitectura detallada y hermosa.");
        descriptions.put("fr_FR", "Blocs professionnels pour créer une architecture détaillée et belle.");
        descriptions.put("de_DE", "Professionelle Blöcke für die Erstellung detaillierter und schöner Architektur.");
        
        BlockPack pack = new BlockPack(ARCHITECTURAL_PACK, names, descriptions, blocks,
            BlockPackCategory.ARCHITECTURAL, DifficultyLevel.ADVANCED, true);
        blockPacks.put(ARCHITECTURAL_PACK, pack);
    }
    
    /**
     * レッドストーンパックの作成
     */
    private void createRedstonePack() {
        List<Block> blocks = Arrays.asList(
            Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_BLOCK,
            Blocks.REPEATER, Blocks.COMPARATOR, Blocks.PISTON, Blocks.STICKY_PISTON,
            Blocks.REDSTONE_LAMP, Blocks.DAYLIGHT_DETECTOR, Blocks.TRIPWIRE_HOOK,
            Blocks.PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE, Blocks.LEVER,
            Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.DISPENSER, Blocks.DROPPER,
            Blocks.HOPPER, Blocks.OBSERVER, Blocks.TARGET
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "レッドストーンブロック");
        names.put("en_US", "Redstone Blocks");
        names.put("zh_CN", "红石方块");
        names.put("zh_TW", "紅石方塊");
        names.put("ko_KR", "레드스톤 블록");
        names.put("es_ES", "Bloques de Redstone");
        names.put("fr_FR", "Blocs de Redstone");
        names.put("de_DE", "Redstone-Blöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "プログラミング思考と論理回路を学ぶためのレッドストーンブロックです。");
        descriptions.put("en_US", "Redstone blocks for learning programming logic and circuits.");
        descriptions.put("zh_CN", "用于学习编程逻辑和电路的红石方块。");
        descriptions.put("zh_TW", "用於學習編程邏輯和電路的紅石方塊。");
        descriptions.put("ko_KR", "프로그래밍 논리와 회로를 학습하기 위한 레드스톤 블록입니다.");
        descriptions.put("es_ES", "Bloques de redstone para aprender lógica de programación y circuitos.");
        descriptions.put("fr_FR", "Blocs de redstone pour apprendre la logique de programmation et les circuits.");
        descriptions.put("de_DE", "Redstone-Blöcke zum Erlernen von Programmierlogik und Schaltkreisen.");
        
        BlockPack pack = new BlockPack(REDSTONE_PACK, names, descriptions, blocks,
            BlockPackCategory.PROGRAMMING, DifficultyLevel.ADVANCED, true);
        blockPacks.put(REDSTONE_PACK, pack);
    }
    
    /**
     * 自然パックの作成
     */
    private void createNaturePack() {
        List<Block> blocks = Arrays.asList(
            Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG,
            Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.MYCELIUM,
            Blocks.STONE, Blocks.ANDESITE, Blocks.GRANITE, Blocks.DIORITE,
            Blocks.WATER, Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY,
            Blocks.MOSS_BLOCK, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "自然ブロック");
        names.put("en_US", "Nature Blocks");
        names.put("zh_CN", "自然方块");
        names.put("zh_TW", "自然方塊");
        names.put("ko_KR", "자연 블록");
        names.put("es_ES", "Bloques de Naturaleza");
        names.put("fr_FR", "Blocs de Nature");
        names.put("de_DE", "Natur-Blöcke");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "自然環境や風景を作るための天然素材ブロックです。");
        descriptions.put("en_US", "Natural material blocks for creating environments and landscapes.");
        descriptions.put("zh_CN", "用于创建自然环境和景观的天然材料方块。");
        descriptions.put("zh_TW", "用於創建自然環境和景觀的天然材料方塊。");
        descriptions.put("ko_KR", "자연 환경과 풍경을 만들기 위한 천연 소재 블록입니다.");
        descriptions.put("es_ES", "Bloques de materiales naturales para crear entornos y paisajes.");
        descriptions.put("fr_FR", "Blocs de matériaux naturels pour créer des environnements et des paysages.");
        descriptions.put("de_DE", "Natürliche Materialblöcke zum Erstellen von Umgebungen und Landschaften.");
        
        BlockPack pack = new BlockPack(NATURE_PACK, names, descriptions, blocks,
            BlockPackCategory.NATURE, DifficultyLevel.BEGINNER, true);
        blockPacks.put(NATURE_PACK, pack);
    }
    
    /**
     * 初心者パックの作成
     */
    private void createBeginnerPack() {
        List<Block> blocks = Arrays.asList(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.WOOD,
            Blocks.WHITE_WOOL, Blocks.RED_WOOL, Blocks.BLUE_WOOL,
            Blocks.GLASS, Blocks.GLOWSTONE, Blocks.WATER
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "初心者パック");
        names.put("en_US", "Beginner Pack");
        names.put("zh_CN", "初学者包");
        names.put("zh_TW", "初學者包");
        names.put("ko_KR", "초보자 팩");
        names.put("es_ES", "Paquete para Principiantes");
        names.put("fr_FR", "Pack Débutant");
        names.put("de_DE", "Anfänger-Paket");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "Minecraftを始めたばかりの学習者に最適な限定ブロックセットです。");
        descriptions.put("en_US", "Perfect limited block set for learners just starting with Minecraft.");
        descriptions.put("zh_CN", "专为刚开始学习Minecraft的学习者设计的限定方块集合。");
        descriptions.put("zh_TW", "專為剛開始學習Minecraft的學習者設計的限定方塊集合。");
        descriptions.put("ko_KR", "Minecraft를 막 시작한 학습자에게 최적인 제한된 블록 세트입니다.");
        descriptions.put("es_ES", "Conjunto limitado de bloques perfecto para estudiantes que empiezan con Minecraft.");
        descriptions.put("fr_FR", "Ensemble de blocs limité parfait pour les apprenants qui commencent avec Minecraft.");
        descriptions.put("de_DE", "Perfektes begrenztes Blockset für Lernende, die gerade mit Minecraft anfangen.");
        
        BlockPack pack = new BlockPack(BEGINNER_PACK, names, descriptions, blocks,
            BlockPackCategory.BEGINNER, DifficultyLevel.BEGINNER, true);
        blockPacks.put(BEGINNER_PACK, pack);
    }
    
    /**
     * 上級者パックの作成
     */
    private void createAdvancedPack() {
        List<Block> blocks = Arrays.asList(
            Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.BARRIER,
            Blocks.BEDROCK, Blocks.END_PORTAL_FRAME, Blocks.SPAWNER,
            Blocks.DRAGON_EGG, Blocks.BEACON, Blocks.CONDUIT,
            Blocks.SHULKER_BOX, Blocks.ENDER_CHEST, Blocks.ENCHANTING_TABLE,
            Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.CAULDRON
        );
        
        Map<String, String> names = new HashMap<>();
        names.put("ja_JP", "上級者パック");
        names.put("en_US", "Advanced Pack");
        names.put("zh_CN", "高级包");
        names.put("zh_TW", "高級包");
        names.put("ko_KR", "고급자 팩");
        names.put("es_ES", "Paquete Avanzado");
        names.put("fr_FR", "Pack Avancé");
        names.put("de_DE", "Fortgeschrittenen-Paket");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "高度な機能と特殊ブロックを含む上級者向けのブロックセットです。");
        descriptions.put("en_US", "Advanced block set with special functions for experienced users.");
        descriptions.put("zh_CN", "包含高级功能和特殊方块的高级用户方块集合。");
        descriptions.put("zh_TW", "包含高級功能和特殊方塊的高級用戶方塊集合。");
        descriptions.put("ko_KR", "고급 기능과 특수 블록을 포함한 숙련자용 블록 세트입니다.");
        descriptions.put("es_ES", "Conjunto de bloques avanzado con funciones especiales para usuarios experimentados.");
        descriptions.put("fr_FR", "Ensemble de blocs avancé avec des fonctions spéciales pour les utilisateurs expérimentés.");
        descriptions.put("de_DE", "Fortgeschrittenes Blockset mit besonderen Funktionen für erfahrene Benutzer.");
        
        BlockPack pack = new BlockPack(ADVANCED_PACK, names, descriptions, blocks,
            BlockPackCategory.ADVANCED, DifficultyLevel.EXPERT, false); // 教師のみ利用可能
        blockPacks.put(ADVANCED_PACK, pack);
    }
    
    /**
     * プレイヤーにブロックパックを適用
     */
    public boolean applyBlockPack(UUID playerUUID, String packId) {
        BlockPack pack = blockPacks.get(packId);
        if (pack == null) {
            LOGGER.warn("Block pack not found: {}", packId);
            return false;
        }
        
        playerActivePacks.put(playerUUID, packId);
        packSubscribers.computeIfAbsent(packId, k -> new HashSet<>()).add(playerUUID);
        
        LOGGER.info("Applied block pack '{}' to player {}", packId, playerUUID);
        return true;
    }
    
    /**
     * プレイヤーの現在のブロックパックを取得
     */
    public BlockPack getPlayerBlockPack(UUID playerUUID) {
        String packId = playerActivePacks.get(playerUUID);
        return packId != null ? blockPacks.get(packId) : blockPacks.get(BASIC_PACK);
    }
    
    /**
     * 利用可能なブロックパック一覧を取得
     */
    public List<BlockPack> getAvailableBlockPacks(boolean isTeacher) {
        return blockPacks.values().stream()
            .filter(pack -> isTeacher || pack.isStudentAccessible())
            .sorted(Comparator.comparing(pack -> pack.getCategory().getOrder()))
            .toList();
    }
    
    /**
     * カスタムブロックパックを作成
     */
    public BlockPack createCustomBlockPack(String id, Map<String, String> names, 
                                         Map<String, String> descriptions, 
                                         List<Block> blocks, BlockPackCategory category,
                                         DifficultyLevel difficulty, boolean studentAccessible) {
        BlockPack pack = new BlockPack(id, names, descriptions, blocks, category, difficulty, studentAccessible);
        blockPacks.put(id, pack);
        
        LOGGER.info("Created custom block pack: {}", id);
        return pack;
    }
    
    /**
     * ブロックパックを削除
     */
    public boolean removeBlockPack(String packId) {
        if (isDefaultPack(packId)) {
            LOGGER.warn("Cannot remove default pack: {}", packId);
            return false;
        }
        
        BlockPack removed = blockPacks.remove(packId);
        if (removed != null) {
            // このパックを使用しているプレイヤーを基本パックに戻す
            Set<UUID> subscribers = packSubscribers.remove(packId);
            if (subscribers != null) {
                subscribers.forEach(uuid -> {
                    playerActivePacks.put(uuid, BASIC_PACK);
                    packSubscribers.computeIfAbsent(BASIC_PACK, k -> new HashSet<>()).add(uuid);
                });
            }
            
            LOGGER.info("Removed custom block pack: {}", packId);
            return true;
        }
        
        return false;
    }
    
    /**
     * デフォルトパックかどうかを確認
     */
    private boolean isDefaultPack(String packId) {
        return Set.of(BASIC_PACK, EDUCATIONAL_PACK, CREATIVE_PACK, ARCHITECTURAL_PACK,
                     REDSTONE_PACK, NATURE_PACK, BEGINNER_PACK, ADVANCED_PACK).contains(packId);
    }
    
    /**
     * ブロックパック使用統計を取得
     */
    public Map<String, Integer> getPackUsageStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (String packId : blockPacks.keySet()) {
            Set<UUID> subscribers = packSubscribers.get(packId);
            stats.put(packId, subscribers != null ? subscribers.size() : 0);
        }
        return stats;
    }
    
    /**
     * プレイヤーがブロックを使用可能かチェック
     */
    public boolean canPlayerUseBlock(UUID playerUUID, Block block) {
        BlockPack pack = getPlayerBlockPack(playerUUID);
        return pack != null && pack.getAllowedBlocks().contains(block);
    }
    
    /**
     * ブロックパック情報をエクスポート
     */
    public Map<String, Object> exportBlockPackData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalPacks", blockPacks.size());
        data.put("defaultPacks", 8);
        data.put("customPacks", blockPacks.size() - 8);
        data.put("activeUsers", playerActivePacks.size());
        data.put("usageStatistics", getPackUsageStatistics());
        data.put("lastUpdated", LocalDateTime.now().toString());
        
        return data;
    }
    
    /**
     * 全ブロックパックの情報を取得
     */
    public Map<String, BlockPack> getAllBlockPacks() {
        return new HashMap<>(blockPacks);
    }
    
    /**
     * ブロックパックをIDで取得
     */
    public BlockPack getBlockPack(String packId) {
        return blockPacks.get(packId);
    }
    
    /**
     * プレイヤーの現在のパックIDを取得
     */
    public String getPlayerActivePackId(UUID playerUUID) {
        return playerActivePacks.getOrDefault(playerUUID, BASIC_PACK);
    }
}