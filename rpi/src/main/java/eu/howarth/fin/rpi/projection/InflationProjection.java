package eu.howarth.fin.rpi.projection;

import eu.howarth.fin.rpi.RpiEntry;

import java.time.YearMonth;
import java.util.List;

public interface InflationProjection {
    List<RpiEntry> project(RpiEntry anchor, YearMonth upTo);
}
