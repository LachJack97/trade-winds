package com.tradewinds;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class BankData
{
    // itemId -> (BankLocation -> quantity)
    private Map<Integer, Map<BankLocation, Integer>> balances = new HashMap<>();
}
