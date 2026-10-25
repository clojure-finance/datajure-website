{:title "Datajure"
 :layout :home
 :page-index 0
 :navbar? true
 :home? true}

## See It in Action

The examples use `prices`, a small table of daily prices for two stocks, defined in the [quick start](/pages-output/quick-start/). Every output on this site comes from running the code.

### Compute within Groups

Daily returns per stock, in date order. Every row is kept:

```clojure
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:ret #dt/e (- (win/ratio :price) 1)})
```

```text
_unnamed [8 5]:

|      :date | :sym | :price | :volume |        :ret |
|------------|------|-------:|--------:|------------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |             |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.02000000 |
| 2024-01-04 | AAPL |   99.0 |         | -0.02941176 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  0.05050505 |
| 2024-01-02 | MSFT |  200.0 |     900 |             |
| 2024-01-03 | MSFT |  198.0 |    1100 | -0.01000000 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.02020202 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  0.02475248 |
```

### Chain Steps

Each `dt` call returns a dataset, so steps chain with `->`. Returns, then their mean and volatility per stock, then a sort:

```clojure
(-> prices
    (dt :by [:sym] :within-order [(asc :date)]
        :set {:ret #dt/e (- (win/ratio :price) 1)})
    (dt :by [:sym] :agg {:mean-ret #dt/e (mn :ret) :vol #dt/e (sd :ret)})
    (dt :order-by [(desc :mean-ret)]))
```

```text
_unnamed [2 3]:

| :sym |  :mean-ret |       :vol |
|------|-----------:|-----------:|
| AAPL | 0.01369776 | 0.04032943 |
| MSFT | 0.01165150 | 0.01888828 |
```

### Compare across Stocks

Group by date instead of by stock to work across the cross-section. Each return minus that day's average return:

```clojure
(-> prices
    (dt :by [:sym] :within-order [(asc :date)]
        :set {:ret #dt/e (- (win/ratio :price) 1)})
    (dt :by [:date]
        :set {:excess #dt/e (stat/demean :ret)}
        :select [:date :sym :ret :excess]
        :order-by [(asc :date)]))
```

```text
_unnamed [8 4]:

|      :date | :sym |        :ret |     :excess |
|------------|------|------------:|------------:|
| 2024-01-02 | AAPL |             |             |
| 2024-01-02 | MSFT |             |             |
| 2024-01-03 | AAPL |  0.02000000 |  0.01500000 |
| 2024-01-03 | MSFT | -0.01000000 | -0.01500000 |
| 2024-01-04 | AAPL | -0.02941176 | -0.02480689 |
| 2024-01-04 | MSFT |  0.02020202 |  0.02480689 |
| 2024-01-05 | AAPL |  0.05050505 |  0.01287629 |
| 2024-01-05 | MSFT |  0.02475248 | -0.01287629 |
```

### Match by Time

An as-of join gives each trade the latest quote for its symbol at or before the trade's time:

```clojure
(join trades quotes :on [:sym :time] :how :asof)
```

```text
_unnamed [3 4]:

| :sym | :time | :qty |  :bid |
|------|------:|-----:|------:|
| AAPL |    10 |  100 |  99.5 |
| AAPL |    25 |   50 | 100.5 |
| MSFT |    30 |   75 | 201.0 |
```

## The Core Idea

Two keywords decide what a query does to the rows. `:set` adds columns and keeps every row; `:agg` collapses rows into summaries. `:by` splits the rows into groups first. Together they give four operations:

<div class="grid4" role="img" aria-label="Without :by, :set adds a column to every row and :agg collapses all rows into one. With :by, :set computes the new column within each group and :agg returns one row per group.">
<div class="g-corner"></div>
<div class="g-head">without <code>:by</code></div>
<div class="g-head">with <code>:by</code></div>
<div class="g-row"><code>:set</code></div>
<div class="g-cell"><span class="g-tag">:set</span><strong>Add a column</strong><span class="g-shape"><span class="mt"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt" style="--cols:3"><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i></span></span><span class="g-desc">Same rows, plus new columns.</span></div>
<div class="g-cell"><span class="g-tag">:set + :by</span><strong>Compute per group</strong><span class="g-shape"><span class="mt"><i class="a"></i><i></i><i class="a"></i><i></i><i class="b"></i><i></i><i class="b"></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt" style="--cols:3"><i class="a"></i><i></i><i class="na"></i><i class="a"></i><i></i><i class="na"></i><i class="b"></i><i></i><i class="nb"></i><i class="b"></i><i></i><i class="nb"></i></span></span><span class="g-desc">Same rows; each group computed on its own, e.g. returns per stock.</span></div>
<div class="g-row"><code>:agg</code></div>
<div class="g-cell"><span class="g-tag">:agg</span><strong>Summarize everything</strong><span class="g-shape"><span class="mt"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt"><i class="n"></i><i class="n"></i></span></span><span class="g-desc">One row in total.</span></div>
<div class="g-cell"><span class="g-tag">:agg + :by</span><strong>Summarize per group</strong><span class="g-shape"><span class="mt"><i class="a"></i><i></i><i class="a"></i><i></i><i class="b"></i><i></i><i class="b"></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt"><i class="a"></i><i class="n"></i><i class="b"></i><i class="n"></i></span></span><span class="g-desc">One row per group.</span></div>
</div>

Around that core, `:where` filters rows first, and `:select`, `:order-by` and `:take` shape the result. The [docs](/pages-output/docs/) explain each step.

## Why Datajure

Datajure is a syntax layer, not an engine. It compiles `#dt/e` expressions to vectorized operations on [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset), so results are standard datasets that work with tablecloth, Clerk, Clay and the rest of the Scicloj ecosystem.

Its design borrows from whichever library got a given idea right:

- **R's data.table**: `DT[i, j, by]` becomes `(dt ds :where i :set-or-agg j :by by)`, without in-place changes.
- **Python's Polars**: expressions are values you can store and combine, and comparisons with missing values are safe.
- **q/kdb+**: time-series tools such as `win/ratio`, `win/mavg`, `win/ema`, `win/fills` and `xbar`, plus as-of and window joins.
- **Julia's DataFramesMeta.jl**: `#dt/e` plays the role of `@transform` and `@subset`, using a Clojure reader tag.

## Where to Go Next

<div class="cards">
<a class="card" href="/pages-output/quick-start/"><strong>Quick Start</strong><span>Install Datajure and run your first query in a few minutes.</span></a>
<a class="card" href="/pages-output/docs/"><strong>Docs</strong><span>How queries work: keywords, expressions, missing values, grouping and joins.</span></a>
<a class="card" href="/pages-output/examples/"><strong>Examples</strong><span>Common tasks on one small dataset, each with its output.</span></a>
<a class="card" href="/pages-output/time-series/"><strong>Time Series</strong><span>Returns, rolling windows, gaps, bars and as-of joins for panel data.</span></a>
<a class="card" href="/pages-output/coming-from/"><strong>From R &amp; Python</strong><span>The same tasks in dplyr, data.table, pandas, Polars and Datajure.</span></a>
<a class="card" href="https://github.com/clojure-finance/datajure#readme"><strong>Reference</strong><span>The README covers every option; API docs are on cljdoc.</span></a>
</div>

## Datajure v1

Datajure v2 (March 2026) is a rewrite. Datajure v1 (1.x) was a query syntax that routed to several backends: tech.ml.dataset, tablecloth, Clojask and Geni. Its code is on the [`v1` branch](https://github.com/clojure-finance/datajure/tree/v1), its documentation is in [this site's history](https://github.com/clojure-finance/datajure-website/tree/559361a/content/md/pages), and [posts](https://github.com/clojure-finance/datajure-website/tree/559361a/content/md/posts) from that time describe its design. Thanks to [YANG Ming-Tian](https://github.com/skylee03) for the original v1 implementation.

Found a bug? Please [open an issue](https://github.com/clojure-finance/datajure/issues). Datajure is distributed under the [Apache License, Version 2.0](https://github.com/clojure-finance/datajure/blob/main/LICENSE).
