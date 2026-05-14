package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SolvencyWarning(YearMonth month, BigDecimal cashPosition) {}
