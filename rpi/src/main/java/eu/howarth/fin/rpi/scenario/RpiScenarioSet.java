package eu.howarth.fin.rpi.scenario;

import eu.howarth.fin.rpi.RpiAdjuster;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RpiScenarioSet(List<RpiScenario> scenarios) {

    public RpiScenarioSet {
        scenarios = List.copyOf(scenarios);
    }

    public Map<String, BigDecimal> adjust(BigDecimal value, YearMonth from, YearMonth to) {
        return scenarios.stream().collect(Collectors.toMap(
                RpiScenario::name,
                s -> RpiAdjuster.adjust(value, from, to, s.dataset())
        ));
    }
}
