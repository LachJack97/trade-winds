package com.tradewinds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class TradeWindsStorage
{
    private static final String DIR_NAME = "tradewinds";
    private static final String FILE_PREFIX = "bankdata-";
    private static final String FILE_SUFFIX = ".dat";

    private static final byte OBFUSCATION_KEY = 0x5A;

    private final File baseDir;
    private final Client client;
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public TradeWindsStorage(File runeLiteDir, Client client)
    {
        this.client = client;
        this.baseDir = new File(runeLiteDir, DIR_NAME);
        if (!baseDir.exists() && !baseDir.mkdirs())
        {
            log.warn("Could not create Tradewinds data directory: {}", baseDir);
        }
        else
        {
            log.info("Tradewinds data directory: {}", baseDir.getAbsolutePath());
        }
    }

    private File getBankFile()
    {
        String name = "unknown";

        if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
        {
            name = client.getLocalPlayer().getName();
        }

        String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
        File file = new File(baseDir, FILE_PREFIX + safe + FILE_SUFFIX);
        log.debug("Using bank data file: {}", file.getAbsolutePath());
        return file;
    }

    private byte[] xorBytes(byte[] input)
    {
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++)
        {
            out[i] = (byte) (input[i] ^ OBFUSCATION_KEY);
        }
        return out;
    }

    public Optional<BankData> loadBankData()
    {
        File file = getBankFile();

        if (!file.exists())
        {
            log.info("No existing bank data file at {}", file.getAbsolutePath());
            return Optional.empty();
        }

        try
        {
            byte[] raw = Files.readAllBytes(file.toPath());
            log.info("Read {} bytes from {}", raw.length, file.getName());

            byte[] decoded = Base64.getDecoder().decode(raw);
            byte[] jsonBytes = xorBytes(decoded);

            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            log.debug("Decoded bank data JSON: {}", json);

            BankData data = gson.fromJson(json, BankData.class);
            if (data == null)
            {
                log.warn("BankData was null after JSON parse");
                return Optional.empty();
            }

            log.info("Loaded BankData with {} items",
                    data.getBalances() != null ? data.getBalances().size() : 0);

            return Optional.of(data);
        }
        catch (Exception e)
        {
            log.warn("Failed to load bank data", e);
            return Optional.empty();
        }
    }

    public void saveBankData(BankData data)
    {
        File file = getBankFile();

        try
        {
            String json = gson.toJson(data);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            byte[] obfuscated = xorBytes(jsonBytes);
            byte[] encoded = Base64.getEncoder().encode(obfuscated);

            Files.write(file.toPath(), encoded);

            log.info("Saved bank data ({} bytes) to {}", encoded.length, file.getAbsolutePath());
        }
        catch (Exception e)
        {
            log.warn("Failed to save bank data", e);
        }
    }


}
