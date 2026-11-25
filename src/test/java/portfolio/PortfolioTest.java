package portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import portfolio.Trade.Action;

class PortfolioTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static Stock stock(String symbol, String price) {
        return new Stock(symbol, () -> bd(price));
    }

    private static Map<String, Trade> bySymbol(List<Trade> trades) {
        return trades.stream().collect(Collectors.toMap(Trade::symbol, t -> t));
    }

    @Test
    void exampleFromBrief() {
        // 10 META @100 = 1000, 10 AAPL @100 = 1000. Target 40/60 of 2000.
        Portfolio p = new Portfolio(Map.of("META", bd("0.4"), "AAPL", bd("0.6")));
        p.addStock(stock("META", "100"), bd("10"));
        p.addStock(stock("AAPL", "100"), bd("10"));
        Map<String, Trade> t = bySymbol(p.rebalance(false));
        assertEquals(Action.SELL, t.get("META").action());
        assertEquals(0, t.get("META").quantity().compareTo(bd("2")));
        assertEquals(Action.BUY, t.get("AAPL").action());
        assertEquals(0, t.get("AAPL").quantity().compareTo(bd("2")));
    }

    @Test
    void balancedPortfolioHasNoTrades() {
        Portfolio p = new Portfolio(Map.of("META", bd("0.5"), "AAPL", bd("0.5")));
        p.addStock(stock("META", "100"), bd("5"));
        p.addStock(stock("AAPL", "50"), bd("10"));
        assertTrue(p.rebalance(false).isEmpty());
    }

    @Test
    void unallocatedHoldingIsSoldAndMissingOneIsBought() {
        Portfolio p = new Portfolio(Map.of("META", bd("1")));
        p.addStock(stock("TSLA", "200"), bd("5"));
        p.registerStock(stock("META", "100"));
        Map<String, Trade> t = bySymbol(p.rebalance(false));
        assertEquals(Action.SELL, t.get("TSLA").action());
        assertEquals(0, t.get("TSLA").quantity().compareTo(bd("5")));
        assertEquals(Action.BUY, t.get("META").action());
        assertEquals(0, t.get("META").quantity().compareTo(bd("10")));
    }

    @Test
    void buysEqualSells() {
        Portfolio p = new Portfolio(Map.of("A", bd("0.3"), "B", bd("0.3"), "C", bd("0.4")));
        p.addStock(stock("A", "17"), bd("9"));
        p.addStock(stock("B", "33"), bd("4"));
        p.addStock(stock("C", "7"), bd("20"));
        List<Trade> trades = p.rebalance(false);
        BigDecimal buys = sum(trades, Action.BUY);
        BigDecimal sells = sum(trades, Action.SELL);
        assertTrue(buys.subtract(sells).abs().compareTo(bd("0.0000001")) < 0);
    }

    @Test
    void wholeShares() {
        Portfolio p = new Portfolio(Map.of("A", bd("0.5"), "B", bd("0.5")));
        p.addStock(stock("A", "30"), bd("7"));  // 210
        p.addStock(stock("B", "10"), bd("1"));  // 10 -> total 220, target 110 each
        Map<String, Trade> t = bySymbol(p.rebalance(true));
        assertEquals(0, t.get("A").quantity().compareTo(bd("3")));
        assertEquals(0, t.get("B").quantity().compareTo(bd("10")));
    }

    @Test
    void invalidAllocation() {
        assertThrows(IllegalArgumentException.class,
            () -> new Portfolio(Map.of("A", bd("0.5"), "B", bd("0.4"))));
    }

    @Test
    void unknownStock() {
        Portfolio p = new Portfolio(Map.of("A", bd("1")));
        assertThrows(IllegalArgumentException.class, () -> p.rebalance(false));
    }

    private static BigDecimal sum(List<Trade> trades, Action action) {
        return trades.stream().filter(t -> t.action() == action)
            .map(Trade::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
