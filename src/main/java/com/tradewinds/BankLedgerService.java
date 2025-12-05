package com.tradewinds;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class BankLedgerService
{
    private final Map<Integer, Map<BankLocation, Integer>> balances = new HashMap<>();
    private BankData bankData = new BankData();
    private TradeWindsStorage storage;

    public void setStorage(TradeWindsStorage storage)
    {
        this.storage = storage;
    }

    public void saveBalances()
    {
        if (storage == null)
        {
            return;
        }

        bankData.setBalances(new HashMap<>(balances));
        storage.saveBankData(bankData);

        log.info("Saved TradeWinds balances: {} items", balances.size());
    }

    public void loadBalances()
    {
        if (storage == null)
        {
            log.warn("TradeWindsStorage not initialised yet when calling loadBalances()");
            return;
        }

        storage.loadBankData().ifPresent(loaded ->
        {
            bankData = loaded;

            balances.clear();
            if (bankData.getBalances() != null)
            {
                balances.putAll(bankData.getBalances());
            }

            log.info("Loaded TradeWinds balances: {} items", balances.size());
        });
    }

    public void resetBalances()
    {
        log.warn("RESETTING ALL TRADEWINDS BALANCES");
        balances.clear();
        bankData = new BankData();

        if (storage != null)
        {
            storage.saveBankData(bankData);
        }
    }

    public void reconcileWithGlobalTotals(BankLocation currentBankLocation, Map<Integer, Integer> newGlobalTotals)
    {
        if (currentBankLocation == null)
        {
            return;
        }

        Set<Integer> allItemIds = new HashSet<>();
        allItemIds.addAll(newGlobalTotals.keySet());
        allItemIds.addAll(balances.keySet());

        for (int itemId : allItemIds)
        {
            int newGlobal = newGlobalTotals.getOrDefault(itemId, 0);

            Map<BankLocation, Integer> perBank = balances.get(itemId);
            int oldGlobal = 0;
            if (perBank != null)
            {
                for (int qty : perBank.values())
                {
                    oldGlobal += qty;
                }
            }

            int delta = newGlobal - oldGlobal;
            if (delta == 0)
            {
                continue;
            }

            if (perBank == null && newGlobal > 0)
            {
                perBank = new HashMap<>();
                balances.put(itemId, perBank);
            }

            int oldLocal = perBank != null ? perBank.getOrDefault(currentBankLocation, 0) : 0;
            int newLocal = Math.max(0, oldLocal + delta);

            if (perBank != null)
            {
                if (newLocal == 0)
                {
                    perBank.remove(currentBankLocation);
                }
                else
                {
                    perBank.put(currentBankLocation, newLocal);
                }

                if (perBank.isEmpty() || newGlobal == 0)
                {
                    balances.remove(itemId);
                }
            }

            log.debug("Reconcile @ {}: item {} oldGlobal={} newGlobal={} delta={} oldLocal={} newLocal={}",
                    currentBankLocation, itemId, oldGlobal, newGlobal, delta, oldLocal, newLocal);
        }
    }

    public int getLocalQuantity(int itemId, BankLocation currentBankLocation)
    {
        if (currentBankLocation == null)
        {
            return 0;
        }

        Map<BankLocation, Integer> perBank = balances.get(itemId);
        if (perBank == null)
        {
            return 0;
        }

        return perBank.getOrDefault(currentBankLocation, 0);
    }

    public int getGlobalQuantity(int itemId)
    {
        Map<BankLocation, Integer> perBank = balances.get(itemId);
        if (perBank == null)
        {
            return 0;
        }

        int total = 0;
        for (int qty : perBank.values())
        {
            total += qty;
        }
        return total;
    }

    public Map<BankLocation, Integer> getPerBank(int itemId)
    {
        return balances.get(itemId);
    }

    public void clear()
    {
        balances.clear();
    }
}