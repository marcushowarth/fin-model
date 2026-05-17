package eu.howarth.fin.rpi.projection;

import eu.howarth.fin.rpi.RpiEntry;

import java.time.YearMonth;
import java.util.List;

public sealed interface InflationProjection
        permits ConstantInflationProjection {
    List<RpiEntry> project(RpiEntry anchor, YearMonth upTo);
}
