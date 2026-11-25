package portfolio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A tradable stock identified by its symbol.
 *
 * <p>The price comes from a {@link Supplier}. In a real app it would call a market-data
 * service. Injecting it keeps this class easy to test.
 */
public final class Stock {
    private final String symbol;
    private final Supplier<BigDecimal> priceSource;

    public Stock(String symbol, Supplier<BigDecimal> priceSource) {
        this.symbol = Objects.requireNonNull(symbol);
        this.priceSource = Objects.requireNonNull(priceSource);
    }

    public String symbol() {
        return symbol;
    }

    /** Last available price. Must be positive. */
    public BigDecimal currentPrice() {
        BigDecimal price = priceSource.get();
        if (price == null || price.signum() <= 0) {
            throw new IllegalStateException("Invalid price for " + symbol + ": " + price);
        }
        return price;
    }

    @Override
    public String toString() {
        return "Stock(" + symbol + ")";
    }
}
