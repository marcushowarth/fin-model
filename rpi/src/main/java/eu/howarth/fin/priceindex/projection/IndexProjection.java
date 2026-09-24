package eu.howarth.fin.priceindex.projection;

import eu.howarth.fin.priceindex.PriceIndexEntry;

import java.time.YearMonth;
import java.util.List;

public sealed interface IndexProjection
        permits ConstantIndexProjection {
    List<PriceIndexEntry> project(PriceIndexEntry anchor, YearMonth upTo);
}
