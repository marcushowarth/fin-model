package eu.howarth.fin.planning;

import eu.howarth.fin.priceindex.scenario.IndexScenarioSet;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RealTermsAdjuster {

    public static RealTermsProjection adjust(ModelProjection nominal, IndexScenarioSet scenarios, YearMonth base) {
        var netWorth = deflateTimeSeries(nominal.netWorth(), scenarios, base);
        var cashPosition = deflateTimeSeries(nominal.cashPosition(), scenarios, base);

        Map<String, Map<String, NavigableMap<YearMonth, BigDecimal>>> itemPositions = new LinkedHashMap<>();
        nominal.itemPositions().forEach((itemName, series) ->
                deflateTimeSeries(series, scenarios, base).forEach((scenarioName, deflated) ->
                        itemPositions.computeIfAbsent(scenarioName, k -> new LinkedHashMap<>())
                                     .put(itemName, deflated)
                )
        );

        return new RealTermsProjection(
                Map.copyOf(netWorth),
                Map.copyOf(cashPosition),
                itemPositions.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> Map.copyOf(e.getValue())
                )),
                base
        );
    }

    private static Map<String, NavigableMap<YearMonth, BigDecimal>> deflateTimeSeries(
            NavigableMap<YearMonth, BigDecimal> series,
            IndexScenarioSet scenarios,
            YearMonth base) {

        Map<String, NavigableMap<YearMonth, BigDecimal>> result = new LinkedHashMap<>();
        series.forEach((month, value) ->
                scenarios.adjust(value, month, base).forEach((scenarioName, realValue) ->
                        result.computeIfAbsent(scenarioName, k -> new TreeMap<>()).put(month, realValue)
                )
        );
        return result;
    }
}
