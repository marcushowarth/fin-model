package eu.howarth.fin.priceindex;

/**
 * A published UK price index this module knows how to load and adjust by.
 * Each constant carries its ONS CDID (series code) and the classpath
 * resource holding its bundled CSV export — both series use the same ONS
 * "Download full time series as CSV" export shape (see
 * {@link PriceIndexDatasetLoader}), so adding a new series here is the only
 * step needed to make it loadable.
 */
public enum IndexSeries {

    /** RPI All Items Index: Jan 1987=100. ONS dataset MM23. */
    RPI("CHAW", "/eu/howarth/fin/rpi/ons-chaw.csv"),

    /** CPI Index 00: All Items 2015=100. ONS dataset MM23. */
    CPI("D7BT", "/eu/howarth/fin/priceindex/ons-d7bt.csv");

    private final String cdid;
    private final String bundledResourcePath;

    IndexSeries(String cdid, String bundledResourcePath) {
        this.cdid = cdid;
        this.bundledResourcePath = bundledResourcePath;
    }

    public String cdid() {
        return cdid;
    }

    public String bundledResourcePath() {
        return bundledResourcePath;
    }
}
