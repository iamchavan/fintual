package portfolio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A portfolio with current holdings and a target allocation.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Money and quantities use {@link BigDecimal} to avoid floating-point surprises.</li>
 *   <li>{@code holdings}: what we own now (symbol -> shares).
 *       {@code allocation}: what we want (symbol -> weight, weights sum to 1).</li>
 *   <li>{@link #rebalance(boolean)} is pure. It returns trades and changes nothing,
 *       so the caller decides whether to execute them.</li>
 * </ul>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>total value = sum(quantity * price) over all holdings.</li>
 *   <li>For each symbol held or allocated: target value = total * weight
 *       (weight is 0 if not allocated); delta = target - current value.</li>
 *   <li>delta &gt; 0 means BUY, delta &lt; 0 means SELL, converted to shares by price.</li>
 * </ol>
 * The rebalance is self-financing: sells and buys match in value. With fractional shares
 * they match up to division precision. With whole shares they match approximately.
 *
 * <h2>Assumptions</h2>
 * No cash balance, fees, taxes or minimum trade sizes. Stocks held but not in the
 * allocation are sold completely. With whole shares, quantities round to the nearest
 * share (half-even) and trades smaller than one share are dropped.
 */
public final class Portfolio {
    private static final MathContext MC = MathContext.DECIMAL128;

    private final Map<String, BigDecimal> allocation = new HashMap<>();
    private final Map<String, Stock> stocks = new HashMap<>();
    private final Map<String, BigDecimal> holdings = new HashMap<>();

    /** @param targetAllocation symbol -> weight, e.g. META 0.4 and AAPL 0.6. Must sum to 1. */
    public Portfolio(Map<String, BigDecimal> targetAllocation) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : targetAllocation.entrySet()) {
            if (e.getValue().signum() < 0) {
                throw new IllegalArgumentException("Allocation weights cannot be negative");
            }
            sum = sum.add(e.getValue());
        }
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Allocation must sum to 1, got " + sum);
        }
        allocation.putAll(targetAllocation);
    }

    /** Adds shares of a stock. Creates the position if it is new. */
    public void addStock(Stock stock, BigDecimal quantity) {
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        stocks.put(stock.symbol(), stock);
        holdings.merge(stock.symbol(), quantity, BigDecimal::add);
    }

    /** Makes a stock known so it can be priced even if we do not own it yet. */
    public void registerStock(Stock stock) {
        stocks.putIfAbsent(stock.symbol(), stock);
    }

    public BigDecimal totalValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
            total = total.add(e.getValue().multiply(stocks.get(e.getKey()).currentPrice()));
        }
        return total;
    }

    /**
     * Returns the trades needed to reach the target allocation, ordered by symbol.
     *
     * @param wholeShares if true, round quantities to whole shares
     */
    public List<Trade> rebalance(boolean wholeShares) {
        TreeSet<String> symbols = new TreeSet<>(holdings.keySet());
        symbols.addAll(allocation.keySet());
        for (String s : symbols) {
            if (!stocks.containsKey(s)) {
                throw new IllegalArgumentException("Unknown stock (add or register it): " + s);
            }
        }

        BigDecimal total = totalValue();
        List<Trade> trades = new ArrayList<>();
        for (String symbol : symbols) {
            BigDecimal price = stocks.get(symbol).currentPrice();
            BigDecimal currentValue = holdings.getOrDefault(symbol, BigDecimal.ZERO).multiply(price);
            BigDecimal targetValue = total.multiply(allocation.getOrDefault(symbol, BigDecimal.ZERO));
            BigDecimal shares = targetValue.subtract(currentValue).divide(price, MC);
            if (wholeShares) {
                shares = shares.setScale(0, RoundingMode.HALF_EVEN);
            }
            if (shares.signum() == 0) {
                continue;
            }
            Trade.Action action = shares.signum() > 0 ? Trade.Action.BUY : Trade.Action.SELL;
            BigDecimal qty = shares.abs();
            trades.add(new Trade(symbol, action, qty, qty.multiply(price)));
        }
        return trades;
    }
}
