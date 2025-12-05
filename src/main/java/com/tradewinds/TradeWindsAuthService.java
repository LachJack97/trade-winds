package com.tradewinds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import net.runelite.client.config.ConfigManager; // ConfigManager is required for account-keyed configuration

import okhttp3.*;

@Slf4j
@Singleton
public class TradeWindsAuthService
{
    // -----------------------------
    // SUPABASE ENDPOINTS
    // -----------------------------
    private static final String SUPABASE_URL = "https://llgmcppmuqwqbezynski.supabase.co";
    private static final String REGISTER_PATH = "/functions/v1/tradewinds-register";
    private static final String STATUS_PATH   = "/functions/v1/tradewinds-status";
    private static final String PRESENCE_PATH = "/functions/v1/tradewinds-presence";
    private static final String PRESENCE_GET  = "/functions/v1/tradewinds-get-presence";

    // Replace this ↓
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxsZ21jcHBtdXF3cWJlenluc2tpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ4MjIxNDQsImV4cHAiOjIwODAzOTgxNDR9._5BahsJXpLKOa0j9mUMZmAwogJkE_4fCeDJAvl2E9-M";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new GsonBuilder().create();

    // ---------------------------------------------------
    // TIMERS
    // ---------------------------------------------------
    private static final long STATUS_POLL_SECONDS   = 30;
    private static final long PRESENCE_HEARTBEAT    = 10;
    private static final long PRESENCE_LOOKUP       = 10;
    private static final long PRESENCE_EXPIRY_SEC   = 20;

    private Instant lastStatusPoll   = Instant.EPOCH;
    private Instant lastPresenceSend = Instant.EPOCH;
    private Instant lastPresencePull = Instant.EPOCH;

    // ---------------------------------------------------
    // Presence cache: username → status (other players)
    // ---------------------------------------------------
    private final Map<String, PresenceEntry> presenceCache = new ConcurrentHashMap<>();

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private TradeWindsConfig config;
    @Inject private ConfigManager configManager;

    // ---------------------------------------------------
    // ACCOUNT-KEYED CONFIGURATION HELPERS
    // ---------------------------------------------------

    private String getAccountConfig(String key)
    {
        if (client.getAccountHash() == -1) return null;

        // Key is prefixed with the account hash for per-account storage
        String accountKey = String.valueOf(client.getAccountHash()) + "." + key;
        return configManager.getConfiguration(TradeWindsConfig.GROUP, accountKey);
    }

    private void setAccountConfig(String key, String value)
    {
        if (client.getAccountHash() == -1) return;

        String accountKey = String.valueOf(client.getAccountHash()) + "." + key;
        configManager.setConfiguration(TradeWindsConfig.GROUP, accountKey, value);
    }

    // ---------------------------------------------------
    // AUTH STATE
    // ---------------------------------------------------

    public boolean isAuthenticated()
    {
        // An account is authenticated if it has a stored character ID
        String characterId = getCharacterId();
        return characterId != null && !characterId.isEmpty();
    }

    public String getCharacterId()
    {
        return getAccountConfig("characterId");
    }

    public TradeWindsAccountStatus getAccountStatus()
    {
        // Get status dynamically from the account-keyed config
        return TradeWindsAccountStatus.fromString(getAccountConfig("accountStatus"));
    }

    // ---------------------------------------------------
    // REGISTRATION
    // ---------------------------------------------------
    public void startFirstTimeRegistration()
    {
        if (isAuthenticated())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: This character is already registered.", null);
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // --- NEW: DEBUG MODE BYPASS ---
        if (!config.debugMode())
        {
            // --- NEW: Tutorial Island lock ---
            if (!isOnTutorialIsland())
            {

                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: Registration can only be completed on Tutorial Island.", null);
                return;
            }

            if (!passesNewAccountChecks())
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: This account is too progressed to register (must be fresh from Tutorial Island).", null);
                return;
            }
        } else {
            log.warn("TradeWinds: Debug mode active. Bypassing registration restrictions.");
        }


        ensureClientUuid();
        new Thread(this::doRegistration, "tw-register").start();
    }


    private void doRegistration()
    {
        try
        {
            RegistrationRequest reqObj = buildRegistrationPayload();
            String json = gson.toJson(reqObj);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + REGISTER_PATH)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .build();

            try (Response r = http.newCall(request).execute())
            {
                String resp = r.body() != null ? r.body().string() : "";
                if (!r.isSuccessful())
                {
                    fallbackRegister();
                    return;
                }

                RegistrationResponse out = gson.fromJson(resp, RegistrationResponse.class);
                if (out == null || out.characterId == null)
                {
                    fallbackRegister();
                    return;
                }

                // Store character ID and status per-account
                setAccountConfig("characterId", out.characterId);
                setAccountConfig("accountStatus", out.status != null ? out.status : "CLEAN");
            }
        }
        catch (Exception e)
        {
            log.error("Registration failed", e);
            fallbackRegister();
        }
    }

    private void fallbackRegister()
    {
        // Store character ID and status per-account
        setAccountConfig("characterId", "local-" + config.clientUuid());
        setAccountConfig("accountStatus", "CLEAN");
    }

    // ---------------------------------------------------
    // STATUS POLLING
    // ---------------------------------------------------
    public void pollStatusIfDue()
    {
        if (!isAuthenticated()) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;

        Instant now = Instant.now();
        if (Duration.between(lastStatusPoll, now).getSeconds() < STATUS_POLL_SECONDS) return;

        lastStatusPoll = now;
        new Thread(this::doStatusPoll, "tw-status-poll").start();
    }

    private void doStatusPoll()
    {
        try
        {
            StatusRequest reqObj = new StatusRequest();
            reqObj.clientUuid = config.clientUuid();
            reqObj.accountHash = client.getAccountHash();

            String json = gson.toJson(reqObj);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + STATUS_PATH)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .build();

            try (Response r = http.newCall(request).execute())
            {
                String resp = r.body() != null ? r.body().string() : "";
                if (!r.isSuccessful()) return;

                StatusResponse out = gson.fromJson(resp, StatusResponse.class);
                if (out != null && out.status != null)
                {
                    // Store account status per-account
                    setAccountConfig("accountStatus", out.status);
                }
            }
        }
        catch (Exception e)
        {
            log.error("Status poll failure", e);
        }
    }

    // ---------------------------------------------------
    // PRESENCE HEARTBEAT (YOU → server)
    // ---------------------------------------------------
    public void sendPresenceHeartbeatIfDue()
    {
        if (!isAuthenticated()) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;

        Instant now = Instant.now();
        if (Duration.between(lastPresenceSend, now).getSeconds() < PRESENCE_HEARTBEAT) return;

        lastPresenceSend = now;
        new Thread(this::sendPresenceHeartbeat, "tw-presence-heartbeat").start();
    }

    private void sendPresenceHeartbeat()
    {
        try
        {
            String rawName = client.getLocalPlayer().getName();
            String normalized = normalizeUsername(rawName);

            PresenceHeartbeat reqObj = new PresenceHeartbeat();
            reqObj.username = normalized;
            reqObj.world = client.getWorld();
            reqObj.status = getAccountStatus().toConfigValue(); // Get status dynamically

            String json = gson.toJson(reqObj);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + PRESENCE_PATH)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .build();

            http.newCall(request).execute().close();
        }
        catch (Exception e)
        {
            log.error("Presence heartbeat failed", e);
        }
    }

    // ---------------------------------------------------
    // PRESENCE LOOKUP (OTHERS → you)
    // ---------------------------------------------------
    public void refreshNearbyPlayersIfDue()
    {
        if (!isAuthenticated()) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;

        Instant now = Instant.now();
        if (Duration.between(lastPresencePull, now).getSeconds() < PRESENCE_LOOKUP) return;

        lastPresencePull = now;
        new Thread(this::refreshNearbyPlayers, "tw-presence-fetch").start();
    }

    private void refreshNearbyPlayers()
    {
        try
        {
            List<String> names = new ArrayList<>();

            for (Player p : client.getPlayers())
            {
                if (p == null || p.getName() == null) continue;

                names.add(normalizeUsername(p.getName()));
            }

            if (names.isEmpty()) return;

            PresenceQuery reqObj = new PresenceQuery();
            reqObj.world = client.getWorld();
            reqObj.names = names;

            String json = gson.toJson(reqObj);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + PRESENCE_GET)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .build();

            try (Response r = http.newCall(request).execute())
            {
                String resp = r.body() != null ? r.body().string() : "";
                if (!r.isSuccessful()) return;

                Map<String, String> map =
                        gson.fromJson(resp, Map.class);

                Instant now = Instant.now();

                for (String key : map.keySet())
                {
                    String val = map.get(key);
                    presenceCache.put(
                            key,
                            new PresenceEntry(TradeWindsAccountStatus.fromString(val), now)
                    );
                }
            }

            // Remove expired (older than 20 sec)
            Instant now = Instant.now();
            presenceCache.entrySet().removeIf(e ->
                    Duration.between(e.getValue().updated, now).getSeconds() > PRESENCE_EXPIRY_SEC
            );

        }
        catch (Exception e)
        {
            log.error("Presence fetch failure", e);
        }
    }

    // ---------------------------------------------------
    // ACCESS FOR OVERLAY
    // ---------------------------------------------------
    public TradeWindsAccountStatus getStatusOf(String normalizedUsername)
    {
        PresenceEntry entry = presenceCache.get(normalizedUsername);
        if (entry != null)
        {
            return entry.status;
        }

        if (isAuthenticated())
        {
            Player local = client.getLocalPlayer();
            if (local != null)
            {
                String self = normalizeUsername(local.getName());
                if (self.equals(normalizedUsername))
                {
                    return getAccountStatus();
                }
            }
        }

        return null;
    }

    // ---------------------------------------------------
    // HELPERS
    // ---------------------------------------------------
    private void ensureClientUuid()
    {
        // clientUuid remains a client-wide setting
        if (config.clientUuid() == null || config.clientUuid().isEmpty())
        {
            config.clientUuid(UUID.randomUUID().toString());
        }
    }

    public static String normalizeUsername(String raw)
    {
        if (raw == null) return "";
        raw = Text.removeTags(raw);
        raw = raw.toLowerCase();
        raw = raw.trim().replaceAll("\\s+", "_");
        return raw;
    }

    // ---------------------------------------------------
    // DATA CLASSES
    // ---------------------------------------------------
    private static class PresenceEntry
    {
        public TradeWindsAccountStatus status;
        public Instant updated;

        public PresenceEntry(TradeWindsAccountStatus s, Instant t)
        {
            status = s;
            updated = t;
        }
    }

    private static class RegistrationRequest
    {
        public String clientUuid;
        public long accountHash;
        public String username;
        public int world;
        public String timestamp;

        public int totalLevel;
        public SkillSnapshot[] skills;

        static class SkillSnapshot
        {
            public String skill;
            public int level;
            public int xp;
        }
    }

    private RegistrationRequest buildRegistrationPayload()
    {
        RegistrationRequest req = new RegistrationRequest();

        req.clientUuid = config.clientUuid();
        req.accountHash = client.getAccountHash();
        req.username = normalizeUsername(client.getLocalPlayer().getName());
        req.world = client.getWorld();
        req.timestamp = Instant.now().toString();

        // total level snapshot
        int total = 0;
        Skill[] skills = Skill.values();
        RegistrationRequest.SkillSnapshot[] snaps = new RegistrationRequest.SkillSnapshot[skills.length];

        for (Skill s : skills)
        {
            int lvl = client.getRealSkillLevel(s);
            int xp  = client.getSkillExperience(s);
            total += lvl;

            RegistrationRequest.SkillSnapshot snap = new RegistrationRequest.SkillSnapshot();
            snap.skill = s.name();
            snap.level = lvl;
            snap.xp    = xp;

            snaps[s.ordinal()] = snap;
        }

        req.totalLevel = total;
        req.skills = snaps;

        return req;
    }


    private static class RegistrationResponse
    {
        public String characterId;
        public String status;
    }

    private static class StatusRequest
    {
        public String clientUuid;
        public long accountHash;
    }

    private static class StatusResponse
    {
        public boolean found;
        public String characterId;
        public String status;
    }

    private static class PresenceHeartbeat
    {
        public String username;
        public int world;
        public String status;
    }

    private static class PresenceQuery
    {
        public int world;
        public List<String> names;
    }

    public void brickAccount(String reason)
    {
        // Store account status and reason per-account
        setAccountConfig("accountStatus", "BRICKED");
        setAccountConfig("brickReason", reason);

        // Optionally send to backend if required
        submitBrick(reason);
    }

    public void submitBrick(String reason)
    {
        // TODO: send brick event to backend or Supabase webhook
        // For now, do nothing.
    }

    private static final int TUTORIAL_ISLAND_REGION = 12336;

    private boolean isOnTutorialIsland()
    {
        if (client.getLocalPlayer() == null)
        {
            return false;
        }

        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        if (wp == null)
        {
            return false;
        }

        return wp.getRegionID() == TUTORIAL_ISLAND_REGION;
    }

    private boolean passesNewAccountChecks()
    {
        int total = 0;
        for (Skill s : Skill.values())
        {
            total += client.getRealSkillLevel(s);
        }

        // example thresholds – tune as you like
        return total <= 33;  // or some small number
    }

}