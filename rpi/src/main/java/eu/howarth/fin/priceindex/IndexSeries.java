package eu.howarth.fin.priceindex;

/**
 * A published UK price index this module knows how to load and adjust by.
 * Each constant carries a source identifier and the classpath resource
 * holding its bundled CSV export. {@code RPI}/{@code CPI} use their ONS
 * CDID and refresh weekly from a live ONS feed (see
 * {@link PriceIndexDatasetLoader}, {@code update-ons-data.yml}); the
 * {@code _MILLENNIUM} series are a one-time static bundle from the Bank of
 * England's "A Millennium of Macroeconomic Data for the UK" (v3.1) — same
 * loadable CSV shape, but their source identifier isn't a real ONS CDID and
 * they don't participate in the weekly refresh (kanban #1003).
 */
public enum IndexSeries {

    /** RPI All Items Index: Jan 1987=100. ONS dataset MM23. */
    RPI("CHAW", "/eu/howarth/fin/rpi/ons-chaw.csv", null),

    /** CPI Index 00: All Items 2015=100. ONS dataset MM23. */
    CPI("D7BT", "/eu/howarth/fin/priceindex/ons-d7bt.csv", null),

    /**
     * RPI lineage spliced back to 1209 (BoE "A Millennium of Macroeconomic
     * Data for the UK" v3.1, sheet A47, column G — Clark (2009) / ONS).
     * Annual only, 1209-2016, base 2015=100. Splices onto {@link #RPI} from
     * 2016 onward for a continuous view to the present day — see
     * {@code PriceIndexSplicer}.
     */
    RPI_MILLENNIUM("MILLENNIUM-G", "/eu/howarth/fin/priceindex/ons-millennium-rpi.csv", RPI),

    /**
     * CPI lineage spliced back to 1209 (BoE "A Millennium of Macroeconomic
     * Data for the UK" v3.1, sheet A47, column D — Schumpeter-Gilboy via
     * Mitchell (1988) / Crafts and Mills (1991) / Feinstein (1998, 1991) /
     * ONS). Annual only, 1209-2016, base 2015=100. Splices onto {@link #CPI}
     * from 2016 onward for a continuous view to the present day — see
     * {@code PriceIndexSplicer}.
     */
    CPI_MILLENNIUM("MILLENNIUM-D", "/eu/howarth/fin/priceindex/ons-millennium-cpi.csv", CPI);

    private final String cdid;
    private final String bundledResourcePath;
    private final IndexSeries splicesInto;

    IndexSeries(String cdid, String bundledResourcePath, IndexSeries splicesInto) {
        this.cdid = cdid;
        this.bundledResourcePath = bundledResourcePath;
        this.splicesInto = splicesInto;
    }

    public String cdid() {
        return cdid;
    }

    public String bundledResourcePath() {
        return bundledResourcePath;
    }

    /**
     * The live series this one continues into for a present-day view, or
     * {@code null} if this series already is the live/current one.
     */
    public IndexSeries splicesInto() {
        return splicesInto;
    }
}
