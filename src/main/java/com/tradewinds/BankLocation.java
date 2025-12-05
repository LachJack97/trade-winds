package com.tradewinds;

import net.runelite.api.coords.WorldPoint;

public enum BankLocation
{
    LUMBRIDGE,
    VARROCK_WEST,
    VARROCK_EAST,
    FALADOR_EAST,
    FALADOR_WEST,
    GRAND_EXCHANGE,
    AL_KHARID,
    EMIRS_ARENA,
    NARDAH,
    RUINS_OF_UNKAH,
    SHANTAY_PASS,
    TUTORIAL_ISLAND,
    THE_NODE,
    DRAYNOR_VILLAGE,
    EDGEVILLE,
    CANIFIS,
    PORT_PHASMATYS,
    DARKMEYER,
    VER_SINHAZA,
    BURG_DE_ROTT,
    MOS_LE_HARMLESS,
    TROUBLE_BREWING,
    FOSSIL_ISLAND_MUSEUM_CAMP,
    VOLCANIC_MINE,
    FOSSIL_ISLAND_SMALL_ISLAND,
    THE_PANDEMONIUM,
    THE_GREAT_CONCH,
    APE_ATOLL,
    SHILO_VILLAGE,
    CRAFTING_GUILD,
    PORT_KHAZARD,
    ARDOUGNE_SOUTH,
    ARDOUGNE_NORTH,
    FISHING_GUILD,
    CATHERBY,
    ROUGES_DEN,
    SEERS_VILLAGE,
    ETCETERIA,
    JATIZSO,
    NEITIZNOT,
    LUNAR_ISLE,
    PISCATORIS_FISHING_COLONY,
    TREE_GNOME_STRONGHOLD,
    GRAND_TREE,
    BARBARIAN_OUTPOST,
    CASTLE_WARS,
    YANILLE,
    CORSAIR_COVE,
    MYTHS_GUILD,
    VOID_KNIGHTS_OUTPOST,
    SOUL_WARS_LOBBY,
    LLETYA,
    INSTANCED_LLEYTYA,
    PRIFDDINAS_SOUTH_EAST,
    PRIFDDINAS_NORTH_WEST,
    ZANARIS,
    MAGE_ARENA_BANK,
    CAMDOZAAL,
    FIGHT_CAVES,
    MOR_UL_REK,
    PORT_PISCARILIUS,
    ARCEUUS,
    KOUREND_CASTLE,
    HOSIDIUS,
    HOSIDIUS_VINERY,
    HOSIDIUS_KITCHEN,
    WOODCUTTING_GUILD,
    SHAYZIEN,
    LANDS_END,
    LOVAKENGJ,
    LOVAKENGJ_MINE,
    BLAST_MINE,
    WINTERTODT,
    MOUNT_KARUULM,
    FARMING_GUILD,
    MOUNT_QUIIDAMORTEM,
    AUBURNVALE,
    NEMUS_RETREAT,
    TAL_TEKLAN,
    ALDARIN,
    MISTROCK,
    HUNTER_GUILD,
    CIVITAS_ILLA_FORTIS_WEST,
    CIVITAS_ILLA_FORTIS_EAST,
    QUETZACALLI_GORGE,
    THE_DARKFROST,
    FEROX_ENCLAVE,
    MOTHERLODE_MINE,
    UNKNOWN;



    public static BankLocation fromWorldPoint(WorldPoint wp)
    {
        if (wp == null)
            return UNKNOWN;

        int region = wp.getRegionID();

        switch (region)
        {
            case 12850: return LUMBRIDGE;
            case 12597: return VARROCK_WEST;
            case 12853: return VARROCK_EAST;
            case 11828: return FALADOR_EAST;
            case 12084: return FALADOR_WEST;
            case 12598: return GRAND_EXCHANGE;
            case 13105: return AL_KHARID;
            case 13363: return EMIRS_ARENA;
            case 13613: return NARDAH;
            case 12588: return RUINS_OF_UNKAH;
            case 13104: return SHANTAY_PASS;
            case 12336: return TUTORIAL_ISLAND;
            case 12335: return THE_NODE;
            case 12338: return DRAYNOR_VILLAGE;
            case 12342: return EDGEVILLE;
            case 13878: return CANIFIS;
            case 14646: return PORT_PHASMATYS;
            case 14388: return DARKMEYER;
            case 14386: return VER_SINHAZA;
            case 13874: return BURG_DE_ROTT;
            case 14638: return MOS_LE_HARMLESS;
            case 15151: return TROUBLE_BREWING;
            case 14907: return FOSSIL_ISLAND_MUSEUM_CAMP;
            case 15163: return VOLCANIC_MINE;
            case 14908: return FOSSIL_ISLAND_SMALL_ISLAND;
            case 12078: return THE_PANDEMONIUM;
            case 12581: return THE_GREAT_CONCH;
            case 11051: return APE_ATOLL;
            case 11310: return SHILO_VILLAGE;
            case 11571: return CRAFTING_GUILD;
            case 10545: return PORT_KHAZARD;
            case 10547: return ARDOUGNE_SOUTH;
            case 10292: return ARDOUGNE_NORTH;
            case 10293: return FISHING_GUILD;
            case 11061: return CATHERBY;
            case 11575: return ROUGES_DEN;
            case 10806: return SEERS_VILLAGE;
            case 10300: return ETCETERIA;
            case 9531: return JATIZSO;
            case 9275: return NEITIZNOT;
            case 8253: return LUNAR_ISLE;
            case 9273: return PISCATORIS_FISHING_COLONY;
            case 9781: return TREE_GNOME_STRONGHOLD;
            case 9782: return GRAND_TREE;
            case 10039: return BARBARIAN_OUTPOST;
            case 9776: return CASTLE_WARS;
            case 10288: return YANILLE;
            case 10284: return CORSAIR_COVE;
            case 9772: return MYTHS_GUILD;
            case 10537: return VOID_KNIGHTS_OUTPOST;
            case 8748: return SOUL_WARS_LOBBY;
            case 9265: return LLETYA;
            case 9011: return PRIFDDINAS_SOUTH_EAST;
            case 12895: return PRIFDDINAS_NORTH_WEST;
            case 9541: return ZANARIS;
            case 10057: return MAGE_ARENA_BANK;
            case 11866: return CAMDOZAAL;
            case 9808: return FIGHT_CAVES;
            case 10064: return MOR_UL_REK;
            case 7227: return PORT_PISCARILIUS;
            case 6458: return ARCEUUS;
            case 6457: return KOUREND_CASTLE;
            case 6968: return HOSIDIUS;
            case 7223: return HOSIDIUS_VINERY;
            case 6712: return HOSIDIUS_KITCHEN;
            case 6198: return WOODCUTTING_GUILD;
            case 5944: return SHAYZIEN;
            case 5941: return LANDS_END;
            case 5946: return LOVAKENGJ;
            case 5691: return LOVAKENGJ_MINE;
            case 5948: return BLAST_MINE;
            case 6461: return WINTERTODT;
            case 5179: return MOUNT_KARUULM;
            case 4922: return FARMING_GUILD;
            case 4919: return MOUNT_QUIIDAMORTEM;
            case 5428: return AUBURNVALE;
            case 4912: return TAL_TEKLAN;
            case 5421: return ALDARIN;
            case 5420: return MISTROCK;
            case 6191: return HUNTER_GUILD;
            case 6448: return CIVITAS_ILLA_FORTIS_WEST;
            case 6960: return CIVITAS_ILLA_FORTIS_EAST;
            case 5938: return QUETZACALLI_GORGE;
            case 5939: return THE_DARKFROST;
            case 5427: return NEMUS_RETREAT;
            case 12344: return FEROX_ENCLAVE;
            case 14936: return MOTHERLODE_MINE;

            default:    return UNKNOWN;
        }
    }
}
