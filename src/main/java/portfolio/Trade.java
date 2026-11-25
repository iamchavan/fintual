package portfolio;

import java.math.BigDecimal;

/**
 * A suggested trade.
 *
 * @param symbol   the stock to trade
 * @param action   BUY or SELL
 * @param quantity number of shares, always positive
 * @param value    quantity * price at the time of calculation
 */
public record Trade(String symbol, Action action, BigDecimal quantity, BigDecimal value) {

    public enum Action { BUY, SELL }
}
