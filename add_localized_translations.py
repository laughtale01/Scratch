#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add properly localized translations for menu items and commands
Uses official Minecraft terminology where available
"""

import re

# Properly localized translations for each language
# Format: {lang_code: {translation_key: translated_value}}

TRANSLATIONS = {
    "ko": {
        # Game modes (Korean uses official Minecraft terms)
        "minecraft.menu.survival": "생존",
        "minecraft.menu.creative": "크리에이티브",
        "minecraft.menu.adventure": "모험",
        "minecraft.menu.spectator": "관전",
        # Directions
        "minecraft.menu.north": "북쪽",
        "minecraft.menu.south": "남쪽",
        "minecraft.menu.east": "동쪽",
        "minecraft.menu.west": "서쪽",
        # Time
        "minecraft.menu.day": "낮",
        "minecraft.menu.noon": "정오",
        "minecraft.menu.sunset": "일몰",
        "minecraft.menu.night": "밤",
        "minecraft.menu.midnight": "자정",
        # Placement
        "minecraft.menu.bottom": "일반 (아래)",
        "minecraft.menu.top": "뒤집기 (위)",
        "minecraft.menu.double": "이중",
        "minecraft.menu.default": "기본값",
        # Game rules
        "minecraft.menu.mobSpawning": "몹 스폰",
        "minecraft.menu.daylightCycle": "낮/밤 주기",
        "minecraft.menu.weatherCycle": "날씨 주기",
        # Commands
        "minecraft.connect": "Minecraft에 연결 호스트 [HOST] 포트 [PORT]",
        "minecraft.disconnect": "연결 끊기",
        "minecraft.place": "블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeBuilding": "건축 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeLighting": "조명 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeDecoration": "장식 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeNature": "자연 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeFunctional": "기능 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeOre": "광석 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.placeSpecial": "특수 블록 [BLOCK]을(를) [X] [Y] [Z]에 배치",
        "minecraft.fill": "[X1] [Y1] [Z1]에서 [X2] [Y2] [Z2]까지 [BLOCK](으)로 채우기",
        "minecraft.clone": "[X1] [Y1] [Z1]에서 [X2] [Y2] [Z2]를 [X3] [Y3] [Z3]로 복제",
        "minecraft.destroyBlock": "[X] [Y] [Z]의 블록 파괴",
        "minecraft.teleport": "[X] [Y] [Z](으)로 텔레포트",
        "minecraft.summon": "[ENTITY]을(를) [X] [Y] [Z]에 소환",
        "minecraft.entityPassive": "수동적 엔티티 [ENTITY]",
        "minecraft.entityNeutral": "중립적 엔티티 [ENTITY]",
        "minecraft.entityHostile": "적대적 엔티티 [ENTITY]",
        "minecraft.entityBoss": "보스 엔티티 [ENTITY]",
        "minecraft.entityAquatic": "수생 엔티티 [ENTITY]",
        "minecraft.entityVillager": "주민 엔티티 [ENTITY]",
        "minecraft.entityOther": "기타 엔티티 [ENTITY]",
        "minecraft.setWeather": "날씨를 [WEATHER](으)로 설정",
        "minecraft.setTime": "시간을 [TIME](으)로 설정",
        "minecraft.getPlayerFacing": "플레이어가 바라보는 방향",
        "minecraft.getBlockType": "[X] [Y] [Z]의 블록 유형",
        "minecraft.isConnected": "연결됨?",
        "minecraft.blockBuilding": "건축 블록 [BLOCK]",
        "minecraft.blockLighting": "조명 블록 [BLOCK]",
        "minecraft.blockDecoration": "장식 블록 [BLOCK]",
        "minecraft.blockNature": "자연 블록 [BLOCK]",
        "minecraft.blockFunctional": "기능 블록 [BLOCK]",
        "minecraft.blockOre": "광석 블록 [BLOCK]",
        "minecraft.blockSpecial": "특수 블록 [BLOCK]",
        "minecraft.chat": "[MESSAGE] 말하기",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "블록 배치 X:[X] Y:[Y] Z:[Z] 블록:[BLOCK] 배치:[PLACEMENT] 방향:[FACING]",
        "minecraft.clearArea": "영역 정리 X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "모든 엔티티 정리 X:[X] Z:[Z]",
        "minecraft.setGameRule": "게임 규칙 [RULE]을(를) [VALUE](으)로 설정",
        "minecraft.setGameMode": "게임 모드를 [MODE](으)로 설정",
        "minecraft.summonEntity": "엔티티 [ENTITY]을(를) X:[X] Y:[Y] Z:[Z]에 소환",
    },
    "de": {
        # Game modes (German)
        "minecraft.menu.survival": "Überleben",
        "minecraft.menu.creative": "Kreativ",
        "minecraft.menu.adventure": "Abenteuer",
        "minecraft.menu.spectator": "Zuschauer",
        # Directions
        "minecraft.menu.north": "Norden",
        "minecraft.menu.south": "Süden",
        "minecraft.menu.east": "Osten",
        "minecraft.menu.west": "Westen",
        # Time
        "minecraft.menu.day": "Tag",
        "minecraft.menu.noon": "Mittag",
        "minecraft.menu.sunset": "Sonnenuntergang",
        "minecraft.menu.night": "Nacht",
        "minecraft.menu.midnight": "Mitternacht",
        # Placement
        "minecraft.menu.bottom": "Normal (Unten)",
        "minecraft.menu.top": "Umgedreht (Oben)",
        "minecraft.menu.double": "Doppelt",
        "minecraft.menu.default": "Standard",
        # Game rules
        "minecraft.menu.mobSpawning": "Mob-Spawning",
        "minecraft.menu.daylightCycle": "Tageszyklus",
        "minecraft.menu.weatherCycle": "Wetterzyklus",
        # Commands
        "minecraft.connect": "Mit Minecraft verbinden Host [HOST] Port [PORT]",
        "minecraft.disconnect": "Trennen",
        "minecraft.place": "Block [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeBuilding": "Baublock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeLighting": "Lichtblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeDecoration": "Dekoblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeNature": "Naturblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeFunctional": "Funktionsblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeOre": "Erzblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.placeSpecial": "Spezialblock [BLOCK] bei [X] [Y] [Z] platzieren",
        "minecraft.fill": "Von [X1] [Y1] [Z1] bis [X2] [Y2] [Z2] mit [BLOCK] füllen",
        "minecraft.clone": "Von [X1] [Y1] [Z1] bis [X2] [Y2] [Z2] nach [X3] [Y3] [Z3] klonen",
        "minecraft.destroyBlock": "Block bei [X] [Y] [Z] zerstören",
        "minecraft.teleport": "Zu [X] [Y] [Z] teleportieren",
        "minecraft.summon": "[ENTITY] bei [X] [Y] [Z] beschwören",
        "minecraft.entityPassive": "Passives Wesen [ENTITY]",
        "minecraft.entityNeutral": "Neutrales Wesen [ENTITY]",
        "minecraft.entityHostile": "Feindliches Wesen [ENTITY]",
        "minecraft.entityBoss": "Boss [ENTITY]",
        "minecraft.entityAquatic": "Wasserwesen [ENTITY]",
        "minecraft.entityVillager": "Dorfbewohner [ENTITY]",
        "minecraft.entityOther": "Anderes Wesen [ENTITY]",
        "minecraft.setWeather": "Wetter auf [WEATHER] setzen",
        "minecraft.setTime": "Zeit auf [TIME] setzen",
        "minecraft.getPlayerFacing": "Blickrichtung des Spielers",
        "minecraft.getBlockType": "Blocktyp bei [X] [Y] [Z]",
        "minecraft.isConnected": "Verbunden?",
        "minecraft.blockBuilding": "Baublock [BLOCK]",
        "minecraft.blockLighting": "Lichtblock [BLOCK]",
        "minecraft.blockDecoration": "Dekoblock [BLOCK]",
        "minecraft.blockNature": "Naturblock [BLOCK]",
        "minecraft.blockFunctional": "Funktionsblock [BLOCK]",
        "minecraft.blockOre": "Erzblock [BLOCK]",
        "minecraft.blockSpecial": "Spezialblock [BLOCK]",
        "minecraft.chat": "[MESSAGE] sagen",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "Block platzieren X:[X] Y:[Y] Z:[Z] Block:[BLOCK] Platzierung:[PLACEMENT] Richtung:[FACING]",
        "minecraft.clearArea": "Bereich räumen X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "Alle Wesen räumen X:[X] Z:[Z]",
        "minecraft.setGameRule": "Spielregel [RULE] auf [VALUE] setzen",
        "minecraft.setGameMode": "Spielmodus auf [MODE] setzen",
        "minecraft.summonEntity": "Wesen [ENTITY] bei X:[X] Y:[Y] Z:[Z] beschwören",
    },
    "fr": {
        # Game modes (French)
        "minecraft.menu.survival": "Survie",
        "minecraft.menu.creative": "Créatif",
        "minecraft.menu.adventure": "Aventure",
        "minecraft.menu.spectator": "Spectateur",
        # Directions
        "minecraft.menu.north": "Nord",
        "minecraft.menu.south": "Sud",
        "minecraft.menu.east": "Est",
        "minecraft.menu.west": "Ouest",
        # Time
        "minecraft.menu.day": "Jour",
        "minecraft.menu.noon": "Midi",
        "minecraft.menu.sunset": "Coucher de soleil",
        "minecraft.menu.night": "Nuit",
        "minecraft.menu.midnight": "Minuit",
        # Placement
        "minecraft.menu.bottom": "Normal (Bas)",
        "minecraft.menu.top": "Inversé (Haut)",
        "minecraft.menu.double": "Double",
        "minecraft.menu.default": "Par défaut",
        # Game rules
        "minecraft.menu.mobSpawning": "Apparition des mobs",
        "minecraft.menu.daylightCycle": "Cycle jour/nuit",
        "minecraft.menu.weatherCycle": "Cycle météo",
        # Commands
        "minecraft.connect": "Se connecter à Minecraft hôte [HOST] port [PORT]",
        "minecraft.disconnect": "Déconnecter",
        "minecraft.place": "Placer bloc [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeBuilding": "Placer bloc de construction [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeLighting": "Placer bloc d'éclairage [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeDecoration": "Placer bloc de décoration [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeNature": "Placer bloc naturel [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeFunctional": "Placer bloc fonctionnel [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeOre": "Placer bloc de minerai [BLOCK] à [X] [Y] [Z]",
        "minecraft.placeSpecial": "Placer bloc spécial [BLOCK] à [X] [Y] [Z]",
        "minecraft.fill": "Remplir de [X1] [Y1] [Z1] à [X2] [Y2] [Z2] avec [BLOCK]",
        "minecraft.clone": "Cloner de [X1] [Y1] [Z1] à [X2] [Y2] [Z2] vers [X3] [Y3] [Z3]",
        "minecraft.destroyBlock": "Détruire bloc à [X] [Y] [Z]",
        "minecraft.teleport": "Téléporter à [X] [Y] [Z]",
        "minecraft.summon": "Invoquer [ENTITY] à [X] [Y] [Z]",
        "minecraft.entityPassive": "Entité passive [ENTITY]",
        "minecraft.entityNeutral": "Entité neutre [ENTITY]",
        "minecraft.entityHostile": "Entité hostile [ENTITY]",
        "minecraft.entityBoss": "Boss [ENTITY]",
        "minecraft.entityAquatic": "Entité aquatique [ENTITY]",
        "minecraft.entityVillager": "Villageois [ENTITY]",
        "minecraft.entityOther": "Autre entité [ENTITY]",
        "minecraft.setWeather": "Définir météo sur [WEATHER]",
        "minecraft.setTime": "Définir heure sur [TIME]",
        "minecraft.getPlayerFacing": "Direction du joueur",
        "minecraft.getBlockType": "Type de bloc à [X] [Y] [Z]",
        "minecraft.isConnected": "Connecté ?",
        "minecraft.blockBuilding": "Bloc de construction [BLOCK]",
        "minecraft.blockLighting": "Bloc d'éclairage [BLOCK]",
        "minecraft.blockDecoration": "Bloc de décoration [BLOCK]",
        "minecraft.blockNature": "Bloc naturel [BLOCK]",
        "minecraft.blockFunctional": "Bloc fonctionnel [BLOCK]",
        "minecraft.blockOre": "Bloc de minerai [BLOCK]",
        "minecraft.blockSpecial": "Bloc spécial [BLOCK]",
        "minecraft.chat": "Dire [MESSAGE]",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "Placer bloc X:[X] Y:[Y] Z:[Z] bloc:[BLOCK] placement:[PLACEMENT] direction:[FACING]",
        "minecraft.clearArea": "Effacer zone X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "Effacer toutes les entités X:[X] Z:[Z]",
        "minecraft.setGameRule": "Définir règle [RULE] sur [VALUE]",
        "minecraft.setGameMode": "Définir mode de jeu sur [MODE]",
        "minecraft.summonEntity": "Invoquer entité [ENTITY] à X:[X] Y:[Y] Z:[Z]",
    },
    "es": {
        # Game modes (Spanish)
        "minecraft.menu.survival": "Supervivencia",
        "minecraft.menu.creative": "Creativo",
        "minecraft.menu.adventure": "Aventura",
        "minecraft.menu.spectator": "Espectador",
        # Directions
        "minecraft.menu.north": "Norte",
        "minecraft.menu.south": "Sur",
        "minecraft.menu.east": "Este",
        "minecraft.menu.west": "Oeste",
        # Time
        "minecraft.menu.day": "Día",
        "minecraft.menu.noon": "Mediodía",
        "minecraft.menu.sunset": "Atardecer",
        "minecraft.menu.night": "Noche",
        "minecraft.menu.midnight": "Medianoche",
        # Placement
        "minecraft.menu.bottom": "Normal (Abajo)",
        "minecraft.menu.top": "Invertido (Arriba)",
        "minecraft.menu.double": "Doble",
        "minecraft.menu.default": "Por defecto",
        # Game rules
        "minecraft.menu.mobSpawning": "Generación de mobs",
        "minecraft.menu.daylightCycle": "Ciclo día/noche",
        "minecraft.menu.weatherCycle": "Ciclo del clima",
        # Commands
        "minecraft.connect": "Conectar a Minecraft host [HOST] puerto [PORT]",
        "minecraft.disconnect": "Desconectar",
        "minecraft.place": "Colocar bloque [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeBuilding": "Colocar bloque de construcción [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeLighting": "Colocar bloque de iluminación [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeDecoration": "Colocar bloque de decoración [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeNature": "Colocar bloque natural [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeFunctional": "Colocar bloque funcional [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeOre": "Colocar bloque de mineral [BLOCK] en [X] [Y] [Z]",
        "minecraft.placeSpecial": "Colocar bloque especial [BLOCK] en [X] [Y] [Z]",
        "minecraft.fill": "Rellenar de [X1] [Y1] [Z1] a [X2] [Y2] [Z2] con [BLOCK]",
        "minecraft.clone": "Clonar de [X1] [Y1] [Z1] a [X2] [Y2] [Z2] a [X3] [Y3] [Z3]",
        "minecraft.destroyBlock": "Destruir bloque en [X] [Y] [Z]",
        "minecraft.teleport": "Teletransportar a [X] [Y] [Z]",
        "minecraft.summon": "Invocar [ENTITY] en [X] [Y] [Z]",
        "minecraft.entityPassive": "Entidad pasiva [ENTITY]",
        "minecraft.entityNeutral": "Entidad neutral [ENTITY]",
        "minecraft.entityHostile": "Entidad hostil [ENTITY]",
        "minecraft.entityBoss": "Jefe [ENTITY]",
        "minecraft.entityAquatic": "Entidad acuática [ENTITY]",
        "minecraft.entityVillager": "Aldeano [ENTITY]",
        "minecraft.entityOther": "Otra entidad [ENTITY]",
        "minecraft.setWeather": "Establecer clima a [WEATHER]",
        "minecraft.setTime": "Establecer hora a [TIME]",
        "minecraft.getPlayerFacing": "Dirección del jugador",
        "minecraft.getBlockType": "Tipo de bloque en [X] [Y] [Z]",
        "minecraft.isConnected": "¿Conectado?",
        "minecraft.blockBuilding": "Bloque de construcción [BLOCK]",
        "minecraft.blockLighting": "Bloque de iluminación [BLOCK]",
        "minecraft.blockDecoration": "Bloque de decoración [BLOCK]",
        "minecraft.blockNature": "Bloque natural [BLOCK]",
        "minecraft.blockFunctional": "Bloque funcional [BLOCK]",
        "minecraft.blockOre": "Bloque de mineral [BLOCK]",
        "minecraft.blockSpecial": "Bloque especial [BLOCK]",
        "minecraft.chat": "Decir [MESSAGE]",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "Colocar bloque X:[X] Y:[Y] Z:[Z] bloque:[BLOCK] colocación:[PLACEMENT] dirección:[FACING]",
        "minecraft.clearArea": "Limpiar área X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "Eliminar todas las entidades X:[X] Z:[Z]",
        "minecraft.setGameRule": "Establecer regla [RULE] a [VALUE]",
        "minecraft.setGameMode": "Establecer modo de juego a [MODE]",
        "minecraft.summonEntity": "Invocar entidad [ENTITY] en X:[X] Y:[Y] Z:[Z]",
    },
    "zh-cn": {
        # Game modes (Simplified Chinese)
        "minecraft.menu.survival": "生存",
        "minecraft.menu.creative": "创造",
        "minecraft.menu.adventure": "冒险",
        "minecraft.menu.spectator": "旁观",
        # Directions
        "minecraft.menu.north": "北",
        "minecraft.menu.south": "南",
        "minecraft.menu.east": "东",
        "minecraft.menu.west": "西",
        # Time
        "minecraft.menu.day": "白天",
        "minecraft.menu.noon": "正午",
        "minecraft.menu.sunset": "日落",
        "minecraft.menu.night": "夜晚",
        "minecraft.menu.midnight": "午夜",
        # Placement
        "minecraft.menu.bottom": "正常 (底部)",
        "minecraft.menu.top": "倒置 (顶部)",
        "minecraft.menu.double": "双层",
        "minecraft.menu.default": "默认",
        # Game rules
        "minecraft.menu.mobSpawning": "生物生成",
        "minecraft.menu.daylightCycle": "昼夜更替",
        "minecraft.menu.weatherCycle": "天气变化",
        # Commands
        "minecraft.connect": "连接到Minecraft 主机 [HOST] 端口 [PORT]",
        "minecraft.disconnect": "断开连接",
        "minecraft.place": "在 [X] [Y] [Z] 放置方块 [BLOCK]",
        "minecraft.placeBuilding": "在 [X] [Y] [Z] 放置建筑方块 [BLOCK]",
        "minecraft.placeLighting": "在 [X] [Y] [Z] 放置照明方块 [BLOCK]",
        "minecraft.placeDecoration": "在 [X] [Y] [Z] 放置装饰方块 [BLOCK]",
        "minecraft.placeNature": "在 [X] [Y] [Z] 放置自然方块 [BLOCK]",
        "minecraft.placeFunctional": "在 [X] [Y] [Z] 放置功能方块 [BLOCK]",
        "minecraft.placeOre": "在 [X] [Y] [Z] 放置矿石方块 [BLOCK]",
        "minecraft.placeSpecial": "在 [X] [Y] [Z] 放置特殊方块 [BLOCK]",
        "minecraft.fill": "从 [X1] [Y1] [Z1] 到 [X2] [Y2] [Z2] 填充 [BLOCK]",
        "minecraft.clone": "从 [X1] [Y1] [Z1] 到 [X2] [Y2] [Z2] 克隆到 [X3] [Y3] [Z3]",
        "minecraft.destroyBlock": "破坏 [X] [Y] [Z] 的方块",
        "minecraft.teleport": "传送到 [X] [Y] [Z]",
        "minecraft.summon": "在 [X] [Y] [Z] 召唤 [ENTITY]",
        "minecraft.entityPassive": "被动生物 [ENTITY]",
        "minecraft.entityNeutral": "中立生物 [ENTITY]",
        "minecraft.entityHostile": "敌对生物 [ENTITY]",
        "minecraft.entityBoss": "Boss [ENTITY]",
        "minecraft.entityAquatic": "水生生物 [ENTITY]",
        "minecraft.entityVillager": "村民 [ENTITY]",
        "minecraft.entityOther": "其他生物 [ENTITY]",
        "minecraft.setWeather": "设置天气为 [WEATHER]",
        "minecraft.setTime": "设置时间为 [TIME]",
        "minecraft.getPlayerFacing": "玩家朝向",
        "minecraft.getBlockType": "[X] [Y] [Z] 的方块类型",
        "minecraft.isConnected": "已连接？",
        "minecraft.blockBuilding": "建筑方块 [BLOCK]",
        "minecraft.blockLighting": "照明方块 [BLOCK]",
        "minecraft.blockDecoration": "装饰方块 [BLOCK]",
        "minecraft.blockNature": "自然方块 [BLOCK]",
        "minecraft.blockFunctional": "功能方块 [BLOCK]",
        "minecraft.blockOre": "矿石方块 [BLOCK]",
        "minecraft.blockSpecial": "特殊方块 [BLOCK]",
        "minecraft.chat": "说 [MESSAGE]",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "放置方块 X:[X] Y:[Y] Z:[Z] 方块:[BLOCK] 放置:[PLACEMENT] 朝向:[FACING]",
        "minecraft.clearArea": "清除区域 X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "清除所有实体 X:[X] Z:[Z]",
        "minecraft.setGameRule": "设置游戏规则 [RULE] 为 [VALUE]",
        "minecraft.setGameMode": "设置游戏模式为 [MODE]",
        "minecraft.summonEntity": "在 X:[X] Y:[Y] Z:[Z] 召唤实体 [ENTITY]",
    },
    "zh-tw": {
        # Game modes (Traditional Chinese)
        "minecraft.menu.survival": "生存",
        "minecraft.menu.creative": "創造",
        "minecraft.menu.adventure": "冒險",
        "minecraft.menu.spectator": "旁觀",
        # Directions
        "minecraft.menu.north": "北",
        "minecraft.menu.south": "南",
        "minecraft.menu.east": "東",
        "minecraft.menu.west": "西",
        # Time
        "minecraft.menu.day": "白天",
        "minecraft.menu.noon": "正午",
        "minecraft.menu.sunset": "日落",
        "minecraft.menu.night": "夜晚",
        "minecraft.menu.midnight": "午夜",
        # Placement
        "minecraft.menu.bottom": "正常 (底部)",
        "minecraft.menu.top": "倒置 (頂部)",
        "minecraft.menu.double": "雙層",
        "minecraft.menu.default": "預設",
        # Game rules
        "minecraft.menu.mobSpawning": "生物生成",
        "minecraft.menu.daylightCycle": "晝夜更替",
        "minecraft.menu.weatherCycle": "天氣變化",
        # Commands
        "minecraft.connect": "連接到Minecraft 主機 [HOST] 連接埠 [PORT]",
        "minecraft.disconnect": "斷開連接",
        "minecraft.place": "在 [X] [Y] [Z] 放置方塊 [BLOCK]",
        "minecraft.placeBuilding": "在 [X] [Y] [Z] 放置建築方塊 [BLOCK]",
        "minecraft.placeLighting": "在 [X] [Y] [Z] 放置照明方塊 [BLOCK]",
        "minecraft.placeDecoration": "在 [X] [Y] [Z] 放置裝飾方塊 [BLOCK]",
        "minecraft.placeNature": "在 [X] [Y] [Z] 放置自然方塊 [BLOCK]",
        "minecraft.placeFunctional": "在 [X] [Y] [Z] 放置功能方塊 [BLOCK]",
        "minecraft.placeOre": "在 [X] [Y] [Z] 放置礦石方塊 [BLOCK]",
        "minecraft.placeSpecial": "在 [X] [Y] [Z] 放置特殊方塊 [BLOCK]",
        "minecraft.fill": "從 [X1] [Y1] [Z1] 到 [X2] [Y2] [Z2] 填充 [BLOCK]",
        "minecraft.clone": "從 [X1] [Y1] [Z1] 到 [X2] [Y2] [Z2] 複製到 [X3] [Y3] [Z3]",
        "minecraft.destroyBlock": "破壞 [X] [Y] [Z] 的方塊",
        "minecraft.teleport": "傳送到 [X] [Y] [Z]",
        "minecraft.summon": "在 [X] [Y] [Z] 召喚 [ENTITY]",
        "minecraft.entityPassive": "被動生物 [ENTITY]",
        "minecraft.entityNeutral": "中立生物 [ENTITY]",
        "minecraft.entityHostile": "敵對生物 [ENTITY]",
        "minecraft.entityBoss": "Boss [ENTITY]",
        "minecraft.entityAquatic": "水生生物 [ENTITY]",
        "minecraft.entityVillager": "村民 [ENTITY]",
        "minecraft.entityOther": "其他生物 [ENTITY]",
        "minecraft.setWeather": "設置天氣為 [WEATHER]",
        "minecraft.setTime": "設置時間為 [TIME]",
        "minecraft.getPlayerFacing": "玩家朝向",
        "minecraft.getBlockType": "[X] [Y] [Z] 的方塊類型",
        "minecraft.isConnected": "已連接？",
        "minecraft.blockBuilding": "建築方塊 [BLOCK]",
        "minecraft.blockLighting": "照明方塊 [BLOCK]",
        "minecraft.blockDecoration": "裝飾方塊 [BLOCK]",
        "minecraft.blockNature": "自然方塊 [BLOCK]",
        "minecraft.blockFunctional": "功能方塊 [BLOCK]",
        "minecraft.blockOre": "礦石方塊 [BLOCK]",
        "minecraft.blockSpecial": "特殊方塊 [BLOCK]",
        "minecraft.chat": "說 [MESSAGE]",
        "minecraft.getPosition": "[COORD]",
        "minecraft.setBlock": "放置方塊 X:[X] Y:[Y] Z:[Z] 方塊:[BLOCK] 放置:[PLACEMENT] 朝向:[FACING]",
        "minecraft.clearArea": "清除區域 X:[X] Z:[Z]",
        "minecraft.clearAllEntities": "清除所有實體 X:[X] Z:[Z]",
        "minecraft.setGameRule": "設置遊戲規則 [RULE] 為 [VALUE]",
        "minecraft.setGameMode": "設置遊戲模式為 [MODE]",
        "minecraft.summonEntity": "在 X:[X] Y:[Y] Z:[Z] 召喚實體 [ENTITY]",
    },
    "pt": {
        # Game modes (Portuguese)
        "minecraft.menu.survival": "Sobrevivência",
        "minecraft.menu.creative": "Criativo",
        "minecraft.menu.adventure": "Aventura",
        "minecraft.menu.spectator": "Espectador",
        # Directions
        "minecraft.menu.north": "Norte",
        "minecraft.menu.south": "Sul",
        "minecraft.menu.east": "Leste",
        "minecraft.menu.west": "Oeste",
        # Time
        "minecraft.menu.day": "Dia",
        "minecraft.menu.noon": "Meio-dia",
        "minecraft.menu.sunset": "Pôr do sol",
        "minecraft.menu.night": "Noite",
        "minecraft.menu.midnight": "Meia-noite",
        # Placement
        "minecraft.menu.bottom": "Normal (Baixo)",
        "minecraft.menu.top": "Invertido (Cima)",
        "minecraft.menu.double": "Duplo",
        "minecraft.menu.default": "Padrão",
        # Game rules
        "minecraft.menu.mobSpawning": "Geração de mobs",
        "minecraft.menu.daylightCycle": "Ciclo dia/noite",
        "minecraft.menu.weatherCycle": "Ciclo do clima",
    },
    "pt-br": {
        # Game modes (Brazilian Portuguese)
        "minecraft.menu.survival": "Sobrevivência",
        "minecraft.menu.creative": "Criativo",
        "minecraft.menu.adventure": "Aventura",
        "minecraft.menu.spectator": "Espectador",
        # Directions
        "minecraft.menu.north": "Norte",
        "minecraft.menu.south": "Sul",
        "minecraft.menu.east": "Leste",
        "minecraft.menu.west": "Oeste",
        # Time
        "minecraft.menu.day": "Dia",
        "minecraft.menu.noon": "Meio-dia",
        "minecraft.menu.sunset": "Pôr do sol",
        "minecraft.menu.night": "Noite",
        "minecraft.menu.midnight": "Meia-noite",
        # Placement
        "minecraft.menu.bottom": "Normal (Baixo)",
        "minecraft.menu.top": "Invertido (Cima)",
        "minecraft.menu.double": "Duplo",
        "minecraft.menu.default": "Padrão",
        # Game rules
        "minecraft.menu.mobSpawning": "Geração de mobs",
        "minecraft.menu.daylightCycle": "Ciclo dia/noite",
        "minecraft.menu.weatherCycle": "Ciclo do clima",
    },
    "it": {
        # Game modes (Italian)
        "minecraft.menu.survival": "Sopravvivenza",
        "minecraft.menu.creative": "Creativa",
        "minecraft.menu.adventure": "Avventura",
        "minecraft.menu.spectator": "Spettatore",
        # Directions
        "minecraft.menu.north": "Nord",
        "minecraft.menu.south": "Sud",
        "minecraft.menu.east": "Est",
        "minecraft.menu.west": "Ovest",
        # Time
        "minecraft.menu.day": "Giorno",
        "minecraft.menu.noon": "Mezzogiorno",
        "minecraft.menu.sunset": "Tramonto",
        "minecraft.menu.night": "Notte",
        "minecraft.menu.midnight": "Mezzanotte",
        # Placement
        "minecraft.menu.bottom": "Normale (Basso)",
        "minecraft.menu.top": "Capovolto (Alto)",
        "minecraft.menu.double": "Doppio",
        "minecraft.menu.default": "Predefinito",
        # Game rules
        "minecraft.menu.mobSpawning": "Generazione mob",
        "minecraft.menu.daylightCycle": "Ciclo giorno/notte",
        "minecraft.menu.weatherCycle": "Ciclo meteo",
    },
    "ru": {
        # Game modes (Russian)
        "minecraft.menu.survival": "Выживание",
        "minecraft.menu.creative": "Творческий",
        "minecraft.menu.adventure": "Приключение",
        "minecraft.menu.spectator": "Наблюдатель",
        # Directions
        "minecraft.menu.north": "Север",
        "minecraft.menu.south": "Юг",
        "minecraft.menu.east": "Восток",
        "minecraft.menu.west": "Запад",
        # Time
        "minecraft.menu.day": "День",
        "minecraft.menu.noon": "Полдень",
        "minecraft.menu.sunset": "Закат",
        "minecraft.menu.night": "Ночь",
        "minecraft.menu.midnight": "Полночь",
        # Placement
        "minecraft.menu.bottom": "Обычный (Низ)",
        "minecraft.menu.top": "Перевёрнутый (Верх)",
        "minecraft.menu.double": "Двойной",
        "minecraft.menu.default": "По умолчанию",
        # Game rules
        "minecraft.menu.mobSpawning": "Спаун мобов",
        "minecraft.menu.daylightCycle": "Смена дня и ночи",
        "minecraft.menu.weatherCycle": "Смена погоды",
    },
    "pl": {
        # Game modes (Polish)
        "minecraft.menu.survival": "Przetrwanie",
        "minecraft.menu.creative": "Kreatywny",
        "minecraft.menu.adventure": "Przygoda",
        "minecraft.menu.spectator": "Obserwator",
        # Directions
        "minecraft.menu.north": "Północ",
        "minecraft.menu.south": "Południe",
        "minecraft.menu.east": "Wschód",
        "minecraft.menu.west": "Zachód",
        # Time
        "minecraft.menu.day": "Dzień",
        "minecraft.menu.noon": "Południe",
        "minecraft.menu.sunset": "Zachód słońca",
        "minecraft.menu.night": "Noc",
        "minecraft.menu.midnight": "Północ",
        # Placement
        "minecraft.menu.bottom": "Normalny (Dół)",
        "minecraft.menu.top": "Odwrócony (Góra)",
        "minecraft.menu.double": "Podwójny",
        "minecraft.menu.default": "Domyślny",
        # Game rules
        "minecraft.menu.mobSpawning": "Pojawianie się mobów",
        "minecraft.menu.daylightCycle": "Cykl dnia i nocy",
        "minecraft.menu.weatherCycle": "Cykl pogody",
    },
    "nl": {
        # Game modes (Dutch)
        "minecraft.menu.survival": "Overleven",
        "minecraft.menu.creative": "Creatief",
        "minecraft.menu.adventure": "Avontuur",
        "minecraft.menu.spectator": "Toeschouwer",
        # Directions
        "minecraft.menu.north": "Noord",
        "minecraft.menu.south": "Zuid",
        "minecraft.menu.east": "Oost",
        "minecraft.menu.west": "West",
        # Time
        "minecraft.menu.day": "Dag",
        "minecraft.menu.noon": "Middag",
        "minecraft.menu.sunset": "Zonsondergang",
        "minecraft.menu.night": "Nacht",
        "minecraft.menu.midnight": "Middernacht",
        # Placement
        "minecraft.menu.bottom": "Normaal (Onder)",
        "minecraft.menu.top": "Omgekeerd (Boven)",
        "minecraft.menu.double": "Dubbel",
        "minecraft.menu.default": "Standaard",
        # Game rules
        "minecraft.menu.mobSpawning": "Mob spawning",
        "minecraft.menu.daylightCycle": "Dag/nacht cyclus",
        "minecraft.menu.weatherCycle": "Weercyclus",
    },
    "tr": {
        # Game modes (Turkish)
        "minecraft.menu.survival": "Hayatta Kalma",
        "minecraft.menu.creative": "Yaratıcı",
        "minecraft.menu.adventure": "Macera",
        "minecraft.menu.spectator": "İzleyici",
        # Directions
        "minecraft.menu.north": "Kuzey",
        "minecraft.menu.south": "Güney",
        "minecraft.menu.east": "Doğu",
        "minecraft.menu.west": "Batı",
        # Time
        "minecraft.menu.day": "Gündüz",
        "minecraft.menu.noon": "Öğle",
        "minecraft.menu.sunset": "Gün batımı",
        "minecraft.menu.night": "Gece",
        "minecraft.menu.midnight": "Gece yarısı",
        # Placement
        "minecraft.menu.bottom": "Normal (Alt)",
        "minecraft.menu.top": "Ters (Üst)",
        "minecraft.menu.double": "Çift",
        "minecraft.menu.default": "Varsayılan",
        # Game rules
        "minecraft.menu.mobSpawning": "Yaratık Doğması",
        "minecraft.menu.daylightCycle": "Gündüz/Gece Döngüsü",
        "minecraft.menu.weatherCycle": "Hava Döngüsü",
    },
}


def main():
    print("=== Adding properly localized translations ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    updated_count = 0

    for lang, translations in TRANSLATIONS.items():
        # Find the language section
        pattern = rf'  "{re.escape(lang)}": \{{'
        match = re.search(pattern, content)

        if not match:
            print(f"  Warning: Could not find section for {lang}")
            continue

        section_start = match.start()

        # Find end of section (next language or end)
        next_lang = re.search(r'\n  \},\n  "[a-z]', content[section_start + 10:])
        if next_lang:
            section_end = section_start + 10 + next_lang.start() + 4
        else:
            section_end = content.find('\n  }\n}', section_start) + 4

        section = content[section_start:section_end]

        # Replace each translation
        for key, value in translations.items():
            # Escape for regex
            escaped_key = re.escape(key)
            escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')

            # Pattern to find existing translation
            old_pattern = rf'"{escaped_key}": "[^"]*"'
            new_value = f'"{key}": "{escaped_value}"'

            if re.search(old_pattern, section):
                section = re.sub(old_pattern, new_value, section)
            else:
                print(f"    Key {key} not found in {lang}")

        content = content[:section_start] + section + content[section_end:]
        updated_count += 1
        print(f"  Updated {lang} with {len(translations)} translations")

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"\n=== Done - updated {updated_count} languages ===")


if __name__ == '__main__':
    main()
