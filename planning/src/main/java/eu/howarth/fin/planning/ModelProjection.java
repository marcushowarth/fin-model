package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public record ModelProjection(
        NavigableMap<YearMonth, BigDecimal> netWorth,
        NavigableMap<YearMonth, BigDecimal> cashPosition,
        Map<String, NavigableMap<YearMonth, BigDecimal>> itemPositions,
        List<SolvencyWarning> warnings
) {}
