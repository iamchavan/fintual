# Portfolio rebalancer (Java 17)

A `Portfolio` holds stocks and a target allocation (e.g. 40% META, 60% AAPL).
`rebalance()` returns the BUY and SELL trades needed to reach that allocation.
It does not execute anything.

```java
Portfolio p = new Portfolio(Map.of("META", new BigDecimal("0.4"), "AAPL", new BigDecimal("0.6")));
p.addStock(new Stock("META", () -> new BigDecimal("100")), new BigDecimal("10"));
p.addStock(new Stock("AAPL", () -> new BigDecimal("100")), new BigDecimal("10"));
p.rebalance(false);   // SELL 2 META, BUY 2 AAPL
```

Run tests: `mvn test`

## Decisions
- `BigDecimal` for money, not double.
- `rebalance()` is pure, so it is easy to test and to review before trading.
- Held stocks outside the allocation are sold fully.
- `wholeShares=true` rounds to whole shares. Buys and sells may then differ slightly.

## Not covered (on purpose)
Cash balance, fees, taxes, minimum trade size, drift thresholds.

## LLM usage
Built with an LLM assistant. The conversation is linked in the application.
