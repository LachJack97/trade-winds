package com.tradewinds;

public class TradeWindsBankLedger
{
    private final TradeWindsStorage storage;
    private final ItemManager itemManager;
    private final Map<Integer, Map<BankLocation, Integer>> balances = new HashMap<>();
    private BankLocation currentBankLocation;

    private BankData bankData = new BankData();
    private boolean balancesLoaded = false;

    @Inject
    public TradeWindsBankLedger(TradewindsStorage storage, ItemManager itemManager)
    {
        this.storage = storage;
        this.itemManager = itemManager;
    }

    // lifecycle-ish
    public void onStartUp()
    {
        loadBalances();
    }

    public void onShutDown()
    {
        saveBalances();
        balances.clear();
        currentBankLocation = null;
        balancesLoaded = false;
    }

    // game tick hook (called from plugin)
    public void ensureLoaded()
    {
        if (!balancesLoaded)
        {
            loadBalances();
            balancesLoaded = true;
        }
    }

    // location
    public void setCurrentBankLocation(WorldPoint wp)
    {
        this.currentBankLocation = BankLocation.fromWorldPoint(wp);
    }

    public BankLocation getCurrentBankLocation()
    {
        return currentBankLocation;
    }

    // reconcile + accessors
    public void reconcileWithGlobalTotals(Map<Integer, Integer> newGlobalTotals)
    {
        // move your reconcile logic in here, unchanged
    }

    public int getLocalQuantity(int itemId) { /*…*/ }
    public int getGlobalQuantity(int itemId) { /*…*/ }

    public Map<Integer, Map<BankLocation, Integer>> getBalancesSnapshot()
    {
        // copy logic from plugin
    }

    public String getItemNameSafe(int itemId)
    {
        // use itemManager here instead of plugin
    }

    private void saveBalances() { /*…*/ }
    private void loadBalances() { /*…*/ }
}
