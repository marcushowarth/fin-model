package eu.howarth.fin.priceindex;

import java.math.BigDecimal;

/** One month's reading of a published price index. */
public record PriceIndexEntry(int year, int month, BigDecimal index) {}
