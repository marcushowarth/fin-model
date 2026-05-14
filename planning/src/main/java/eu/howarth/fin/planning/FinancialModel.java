package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

public class FinancialModel {

    private final List<FinancialItem> items;

    public FinancialModel(List<FinancialItem> items) {
        this.items = List.copyOf(items);
    }

    public ModelProjection project(YearMonth from, YearMonth to) {
        BigDecimal liquidity = items.stream()
                .map(FinancialItem::startingLiquidity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aggregate flows per month across all items
        NavigableMap<YearMonth, BigDecimal> flowsByMonth = new TreeMap<>();
        for (FinancialItem item : items) {
            item.flows(from, to).forEach((month, flow) ->
                    flowsByMonth.merge(month, flow, BigDecimal::add));
        }

        // Aggregate positions per month and track per-item
        NavigableMap<YearMonth, BigDecimal> totalPositions = new TreeMap<>();
        Map<String, NavigableMap<YearMonth, BigDecimal>> itemPositions = new LinkedHashMap<>();
        for (FinancialItem item : items) {
            var pos = item.positions(from, to);
            pos.forEach((month, value) -> totalPositions.merge(month, value, BigDecimal::add));
            if (!pos.isEmpty()) {
                itemPositions.merge(item.name(), new TreeMap<>(pos), (a, b) -> {
                    b.forEach((m, v) -> a.merge(m, v, BigDecimal::add));
                    return a;
                });
            }
        }

        NavigableMap<YearMonth, BigDecimal> netWorth     = new TreeMap<>();
        NavigableMap<YearMonth, BigDecimal> cashPosition = new TreeMap<>();
        List<SolvencyWarning> warnings = new ArrayList<>();

        BigDecimal cash = liquidity;
        YearMonth current = from;
        while (!current.isAfter(to)) {
            cash = cash.add(flowsByMonth.getOrDefault(current, BigDecimal.ZERO));
            cashPosition.put(current, cash);

            BigDecimal positions = totalPositions.getOrDefault(current, BigDecimal.ZERO);
            netWorth.put(current, positions.add(cash));

            if (cash.compareTo(BigDecimal.ZERO) < 0) {
                warnings.add(new SolvencyWarning(current, cash));
            }

            current = current.plusMonths(1);
        }

        return new ModelProjection(netWorth, cashPosition, Map.copyOf(itemPositions), List.copyOf(warnings));
    }
}
