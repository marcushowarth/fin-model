package eu.howarth.fin.priceindex.scenario;

import eu.howarth.fin.priceindex.PriceIndexAdjuster;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record IndexScenarioSet(List<IndexScenario> scenarios) {

    public IndexScenarioSet {
        scenarios = List.copyOf(scenarios);
    }

    public Map<String, BigDecimal> adjust(BigDecimal value, YearMonth from, YearMonth to) {
        return scenarios.stream().collect(Collectors.toMap(
                IndexScenario::name,
                s -> PriceIndexAdjuster.adjust(value, from, to, s.dataset())
        ));
    }
}
