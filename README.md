# fin-model

A data-oriented Java financial planning engine. Pure library — no framework, no
web layer, no persistence. You hand it a set of financial items and assumptions,
it returns a year-by-year projection.

It is the calculation core behind **[FIN OPTICS](https://github.com/marcushowarth/fin-optics-ui)**
(a [REST API](https://github.com/marcushowarth/fin-optics-api) + Vue front end),
but it has no dependency on either and can be consumed by anything on the JVM.

## Modules

| Module | What it does |
|---|---|
| `fin-model-rpi` | Inflation adjustment over ONS CHAW RPI data, plus forward inflation projection and named scenarios. |
| `fin-model-planning` | The planning domain — financial items that evolve over time, aggregated into a projection. |

### `rpi`

The pilot module — a small domain used to establish the conventions before
tackling planning.

- `RpiDataset` / `RpiEntry` — immutable records over the ONS CHAW series
- `RpiAdjuster` — pure `adjust(value, from, to)` inflation maths
- `RpiProjector` + `InflationProjection` — splice historical data with a forward
  assumption (`ConstantInflationProjection`) into one continuous series
- `RpiScenario` / `RpiScenarioSet` — adjust a value across several inflation
  scenarios at once (e.g. low / base / high)

### `planning`

Financial items whose values evolve through configurable parameters, modelled as
a sealed `FinancialItem` with seven record subtypes:

- **Asset** — growth/depreciation, optional sale date
- **Investment** — compounding pot with optional drawdown
- **BankAccount** — seeds the starting cash pool
- **Income** — growing monthly inflow, optional end (no end = runs to the horizon)
- **Expenditure** — growing monthly outflow, optional end (no end = runs to the horizon)
- **Liability** — amortising balance with interest
- **FinancialEvent** — a one-off, dated cash movement; signed amount (positive in,
  negative out) for lump sums like a bonus, inheritance, wedding, or car

`FinancialModel` aggregates the items and produces a `ModelProjection`: net worth,
cumulative cash position, per-item positions, and solvency warnings. The model
**runs to completion even when cash goes negative** — it surfaces the problem so
you can adjust inputs, rather than refusing to compute. `RealTermsAdjuster` then
deflates the nominal projection into today's money per inflation scenario.

## Design

- **Data-oriented programming** — sealed interfaces, records, and exhaustive
  pattern-matching `switch` for calculation dispatch; the compiler enforces that
  every item type is handled everywhere
- **Pure functions** — projections are deterministic; no hidden state
- **TDD throughout** — every calculation rule tested in isolation before assembly
  (95 tests)

## Build

Requires **JDK 25** and Maven.

```bash
mvn verify        # compile + run all tests
mvn install       # install to local ~/.m2 for downstream use
```

## Consuming

Published to GitHub Packages on push to `main`. Add the repository and depend on
whichever module you need:

```xml
<dependency>
    <groupId>eu.howarth.fin</groupId>
    <artifactId>fin-model-planning</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

GitHub Packages requires authentication even for reads — configure a token with
`read:packages` in your `~/.m2/settings.xml`. See the
[GitHub Packages Maven docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

## License

[MIT](LICENSE)
