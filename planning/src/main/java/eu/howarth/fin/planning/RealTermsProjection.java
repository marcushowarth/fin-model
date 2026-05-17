package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.NavigableMap;

public record RealTermsProjection(
        Map<String, NavigableMap<YearMonth, BigDecimal>> netWorth,
        Map<String, NavigableMap<YearMonth, BigDecimal>> cashPosition,
        Map<String, Map<String, NavigableMap<YearMonth, BigDecimal>>> itemPositions,
        YearMonth base
) {}
