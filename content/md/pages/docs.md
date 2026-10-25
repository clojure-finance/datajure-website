{:title "Docs"
 :nav-title "Docs"
 :subtitle "How Datajure queries work, from the core idea to the helper namespaces."
 :layout :page
 :toc :ul
 :page-index 2
 :navbar? true}

This page explains how Datajure works, from the core idea to the helper namespaces. It builds on the `prices` dataset from the [quick start](/pages-output/quick-start/): two stocks, four trading days, one missing volume. The [examples](/pages-output/examples/) show more queries with their output. Every option is covered in the [README](https://github.com/clojure-finance/datajure#readme), and the API docs are on [cljdoc](https://cljdoc.org/d/com.github.clojure-finance/datajure/CURRENT).

## The Mental Model

A Datajure query takes a dataset (a table of named columns) and returns a new dataset. The input is never changed. Every query is one call to `dt`, which runs up to five steps in a fixed order: keep some rows, compute new columns or summaries, pick columns, sort, and take the first few rows. The computations are written as expressions that describe whole columns, such as "price times volume", rather than loops over rows.

Because the result is always a dataset, queries chain: the output of one `dt` call is the input of the next.

## How a Query Works

### The `dt` Function

`dt` takes a dataset and keyword arguments. Each keyword is optional:

```clojure
(require '[datajure.core :refer [dt dt1 nrow asc desc]])

(dt prices
    :where #dt/e (> :price 100)
    :by [:sym]
    :agg {:n nrow :avg-price #dt/e (mn :price)})
```

```text
_unnamed [2 3]:

| :sym | :n | :avg-price |
|------|---:|-----------:|
| AAPL |  2 |     103.00 |
| MSFT |  4 |     201.75 |
```

Read it as: keep the rows with a price above 100, group them by symbol, and for each group count the rows and average the price.

| Keyword | Purpose |
|---|---|
| `:where` | Keep the rows where an expression is true |
| `:set` | Add or replace columns, keeping every row |
| `:agg` | Collapse rows into one row per group (or one row in total) |
| `:by` | Group for `:set` or `:agg` |
| `:within-order` | Sort rows within each group before `:set` or `:agg` runs |
| `:select` | Keep, drop or rename columns |
| `:order-by` | Sort the result |
| `:take` | Keep the first n rows, or the last n if negative |

`dt` is an ordinary function, not a macro, and always returns a dataset.

### Evaluation Order

Whatever order the keywords are written in, `dt` evaluates them in this order:

<ol class="flow" aria-label="Evaluation order">
<li><code>:where</code><span>keeps the matching rows</span></li>
<li><code>:set</code> / <code>:agg</code><span>computes, per <code>:by</code> group, in <code>:within-order</code></span></li>
<li><code>:select</code><span>keeps the listed columns</span></li>
<li><code>:order-by</code><span>sorts the result</span></li>
<li><code>:take</code><span>keeps the first (or last) n rows</span></li>
</ol>

So this query, written back to front, still filters first and sorts last:

```clojure
(dt prices
    :order-by [(desc :avg-price)]
    :agg {:n nrow :avg-price #dt/e (mn :price)}
    :by [:sym]
    :where #dt/e (> :price 100))
```

```text
_unnamed [2 3]:

| :sym | :n | :avg-price |
|------|---:|-----------:|
| MSFT |  4 |     201.75 |
| AAPL |  2 |     103.00 |
```

`:set` and `:agg` cannot be used in the same call; thread several `dt` calls with `->` instead. An unknown keyword is an error with a suggestion (`:wehre -> :where`), not silently ignored.

### Rows In, Rows Out: `:by` with `:set` or `:agg`

The two ways of computing differ in what happens to the rows:

- `:set` keeps every row and adds columns, like a formula filled down a spreadsheet column.
- `:agg` performs an aggregation: it collapses many rows into one, such as a sum or a mean.

`:by` splits the rows into groups first (also called partitions). Combined, they give four kinds of operation:

<div class="grid4" role="img" aria-label="Without :by, :set adds a column to every row and :agg collapses all rows into one. With :by, :set computes the new column within each group and :agg returns one row per group.">
<div class="g-corner"></div>
<div class="g-head">without <code>:by</code></div>
<div class="g-head">with <code>:by</code></div>
<div class="g-row"><code>:set</code></div>
<div class="g-cell"><span class="g-tag">:set</span><strong>Add a column</strong><span class="g-shape"><span class="mt"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt" style="--cols:3"><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i><i></i><i></i><i class="n"></i></span></span><span class="g-desc">Same rows, plus new columns.</span></div>
<div class="g-cell"><span class="g-tag">:set + :by</span><strong>Compute per group</strong><span class="g-shape"><span class="mt"><i class="a"></i><i></i><i class="a"></i><i></i><i class="b"></i><i></i><i class="b"></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt" style="--cols:3"><i class="a"></i><i></i><i class="na"></i><i class="a"></i><i></i><i class="na"></i><i class="b"></i><i></i><i class="nb"></i><i class="b"></i><i></i><i class="nb"></i></span></span><span class="g-desc">Same rows; each group computed on its own.</span></div>
<div class="g-row"><code>:agg</code></div>
<div class="g-cell"><span class="g-tag">:agg</span><strong>Summarize everything</strong><span class="g-shape"><span class="mt"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt"><i class="n"></i><i class="n"></i></span></span><span class="g-desc">One row in total.</span></div>
<div class="g-cell"><span class="g-tag">:agg + :by</span><strong>Summarize per group</strong><span class="g-shape"><span class="mt"><i class="a"></i><i></i><i class="a"></i><i></i><i class="b"></i><i></i><i class="b"></i><i></i></span><span class="g-arrow">&rarr;</span><span class="mt"><i class="a"></i><i class="n"></i><i class="b"></i><i class="n"></i></span></span><span class="g-desc">One row per group.</span></div>
</div>

| | Without `:by` | With `:by` |
|---|---|---|
| `:set` | Derive columns over the whole dataset | Derive columns within each group |
| `:agg` | One summary row | One row per group |

With `:by` and `:agg`, each symbol becomes one row. `:within-order` sorts each group by date first, so `first-val` and `last-val` give the opening and closing price:

```clojure
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :agg {:open  #dt/e (first-val :price)
          :close #dt/e (last-val :price)
          :hi    #dt/e (mx :price)
          :lo    #dt/e (mi :price)})
```

```text
_unnamed [2 5]:

| :sym | :open | :close |   :hi |   :lo |
|------|------:|-------:|------:|------:|
| AAPL | 100.0 |  104.0 | 104.0 |  99.0 |
| MSFT | 200.0 |  207.0 | 207.0 | 198.0 |
```

With `:by` and `:set`, every row is kept and each value is computed within its own group. This is where window functions come in: functions such as `win/lag` (the previous value) or `win/delta` (the change from the previous value) that look at neighboring rows. Here they never reach across from AAPL into MSFT:

```clojure
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:prev #dt/e (win/lag :price 1)
          :chg  #dt/e (win/delta :price)})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :prev | :chg |
|------------|------|-------:|--------:|------:|-----:|
| 2024-01-02 | AAPL |  100.0 |    1200 |       |      |
| 2024-01-03 | AAPL |  102.0 |    1500 | 100.0 |  2.0 |
| 2024-01-04 | AAPL |   99.0 |         | 102.0 | -3.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  99.0 |  5.0 |
| 2024-01-02 | MSFT |  200.0 |     900 |       |      |
| 2024-01-03 | MSFT |  198.0 |    1100 | 200.0 | -2.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 198.0 |  4.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 202.0 |  5.0 |
```

An aggregation inside `:set` gives the same value for every row of the group, which makes shares and deviations easy. Each price as a share of its symbol's total:

```clojure
(dt prices :by [:sym]
    :set {:share #dt/e (/ :price (sm :price))}
    :select [:sym :price :share]
    :take 3)
```

```text
_unnamed [3 3]:

| :sym | :price |     :share |
|------|-------:|-----------:|
| AAPL |  100.0 | 0.24691358 |
| AAPL |  102.0 | 0.25185185 |
| AAPL |   99.0 | 0.24444444 |
```

A `:set` query keeps the input row order. `:within-order` only sets the order in which window functions walk each group; use `:order-by` to sort the output. The [time series](/pages-output/time-series/) page covers window functions in depth.

### Columns Built in Order

`:set` takes a map or a sequence of pairs. In a map, every expression sees only the input columns, and referring to a sibling is an error. In a sequence of pairs, each expression sees the columns derived before it:

```clojure
(dt prices :set [[:value #dt/e (* :price :volume)]
                 [:big?  #dt/e (> :value 200000)]])
```

### `dt1` and Queries as Maps

`dt1` takes the same arguments as `dt` and returns the single result row as a map. It throws if the result is not exactly one row, so it suits whole-dataset summaries:

```clojure
(dt1 prices :agg {:n nrow :avg-price #dt/e (mn :price)})
```

```text
{:n 8, :avg-price 151.5}
```

A query can also be one map, which makes it plain data that can be stored, merged or built by a program:

```clojure
(dt prices {:where [:> :price 100] :by [:sym] :agg {:n [:nrow]}})
```

## Expressions

### `#dt/e` and Plain Functions

`#dt/e` is a reader tag: a marker placed before a form that changes how Clojure reads it. Inside it, bare keywords such as `:price` refer to columns, and the expression is vectorized: it runs on whole columns at once, which is much faster than calling a function for each row. Plain Clojure functions also work and are called once per row with a row map:

```clojure
(dt prices :where #dt/e (> :price 150))     ;; expression, vectorized
(dt prices :where #(> (:price %) 150))      ;; plain function, per row
```

| | `#dt/e` | Plain function |
|---|---|---|
| Column access | `:price` | `(:price %)` |
| Speed | Vectorized | One call per row in `:where` and `:set` |
| Missing values | Handled by the rules below | Up to the function (see `pass-nil`) |
| Checking | Unknown columns and operations are reported before running, with suggestions | Errors at run time |

Prefer `#dt/e`, and use plain functions for logic that has no vectorized form, such as Java interop. In `:agg`, a plain function receives the whole group dataset rather than a row.

### Operations

Inside `#dt/e`:

| Kind | Operations |
|---|---|
| Arithmetic and math | `+ - * /`, `sq`, `sqrt`, `pow`, `log`, `exp`, `log1p`, `expm1`, `abs`, `signum`, `floor`, `ceil`, `round` |
| Comparison and logic | `= < <= > >=`, `and`, `or`, `not`, `in` (set membership), `between?` |
| Conditionals and bindings | `if`, `cond`, `let`, `when-finite` |
| Missing values | `coalesce`, `coalesce-finite`, `div0` |
| Aggregations | `mn` (mean), `sm` (sum), `md` (median), `qnt` (quantile), `sd`, `mx`, `mi`, `ct` (non-missing count), `nuniq`, `first-val`, `last-val`, `wavg`, `wsum`, `cov`, `corr`, `tstat`, `nrow` |
| Calendar parts | `year`, `month`, `day`, `dow` (1 = Monday), `quarter` |
| Window functions | `win/rank`, `win/lag`, `win/lead`, `win/cumsum`, `win/ratio`, `win/delta`, `win/mavg`, `win/mdev`, `win/ema`, `win/fills`, `win/scan`, and [more](https://github.com/clojure-finance/datajure#window-functions) |
| Across columns in a row | `row/sum`, `row/mean`, `row/min`, `row/max`, `row/count-nil`, `row/any-nil?` |
| Column transforms | `stat/standardize`, `stat/demean`, `stat/winsorize`, `stat/trim`, `stat/rescale` |
| Binning | `xbar` (fixed width), `cut` (equal-count quantile bins) |

```clojure
(dt prices :set {:size #dt/e (cond (> :price 200) "large"
                                   (> :price 100) "medium"
                                   :else          "small")})

(dt prices :where #dt/e (and (in :sym #{"AAPL" "MSFT"})
                             (between? :price 100 205)))
```

Aggregations also have full names (`mean`, `sum`, `median`, …), and `datajure.concise` exports short aliases such as `N` for `nrow`.

### Missing Values

Real data has gaps. Datajure represents a missing value as `nil` and handles it with a few rules rather than one blanket guarantee:

- A comparison with a missing value is false, so `:where` drops that row.
- Arithmetic on a missing value gives a missing value. Arithmetic with a literal `nil` is an error when the expression is read.
- Aggregations skip missing values, and return `nil` if all are missing.
- `coalesce` replaces missing values, `div0` gives `nil` instead of dividing by zero or `nil`, and `win/fills` carries the last value forward.
- Plain functions get no special handling (see [Plain Functions and Missing Values](#plain-functions-and-missing-values)).

The AAPL volume on 2024-01-04 is missing, so `ct` counts seven volumes and `sm` adds up those seven:

```clojure
(dt1 prices :agg {:rows        nrow
                  :with-volume #dt/e (ct :volume)
                  :total-vol   #dt/e (sm :volume)})
```

```text
{:rows 8, :with-volume 7, :total-vol 8800.0}
```

Filling the gap, either with a constant or with the previous value of the same symbol:

```clojure
(dt prices :set {:volume #dt/e (coalesce :volume 0)})
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:volume #dt/e (win/fills :volume)})
```

### Plain Functions and Missing Values

A plain function sees `nil` as is, and often fails on it. `pass-nil` wraps the function so that it returns `nil` whenever one of the named columns is missing:

```clojure
(require '[datajure.core :refer [pass-nil]])

;; without pass-nil this throws: Cannot invoke "java.lang.Number.doubleValue()"
(dt prices
    :set {:label (pass-nil #(str (:sym %) "/" (long (:volume %))) :volume)}
    :select [:sym :volume :label]
    :take 4)
```

```text
_unnamed [4 3]:

| :sym | :volume |    :label |
|------|--------:|-----------|
| AAPL |    1200 | AAPL/1200 |
| AAPL |    1500 | AAPL/1500 |
| AAPL |         |           |
| AAPL |    1800 | AAPL/1800 |
```

### Keywords as Values: `lit`

Inside `#dt/e` every keyword is a column. To compare against a keyword value instead, wrap it in `lit`:

```clojure
(def orders (ds/->dataset {:id     [1 2 3]
                           :status [:active :closed :active]}))

(dt orders :where #dt/e (= :status (lit :active)))
```

```text
_unnamed [2 2]:

| :id | :status |
|----:|---------|
|   1 | :active |
|   3 | :active |
```

In a data-form (next section) the same is `[:lit :active]`.

### Runtime Values and Data-Forms

`#dt/e` is applied when the code is read, before the program runs, so it cannot see a local variable. For values known only at run time, write the expression as a data-form: a vector headed by an operation keyword, with keywords as columns and anything else as a literal value:

```clojure
(let [ticker "MSFT" lo 199]
  (dt prices :where [:and [:= :sym ticker] [:> :price lo]]))
```

```text
_unnamed [3 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

Data-forms cover everything `#dt/e` does, including windows (`[:win/lag :price 1]`) and conditionals (`[:cond … :else …]`), and run on the same vectorized path. Because they are plain vectors, a program can generate them. The maximum of each listed column:

```clojure
(dt prices :by [:sym]
    :agg (into {} (for [c [:price :volume]]
                    [(keyword (str "max-" (name c))) [:mx c]])))
```

### Reusable Expressions

An `#dt/e` expression is a value. Store it in a var and use it in several queries or inside other expressions:

```clojure
(def value #dt/e (* :price :volume))

(dt prices :set {:value value})
(dt prices :by [:sym] :agg {:avg-value #dt/e (mn value)})
```

This works for window functions too, which builds a shared vocabulary of returns and other measures:

```clojure
(def ret     #dt/e (- (win/ratio :price) 1))
(def log-ret #dt/e (log (+ 1 ret)))

(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:ret ret :log-ret log-ret})
```

## Selecting Columns

`:select` accepts several forms:

```clojure
(dt prices :select [:sym :price])                ;; these columns
(dt prices :select [:not :volume])               ;; all but these
(dt prices :select #"p.*")                       ;; names matching a regex
(dt prices :select :type/numerical)              ;; numeric columns (dates count as numeric)
(dt prices :select {:sym :ticker :price :close}) ;; select and rename
```

`rename` renames columns without dropping any: `(rename prices {:sym :ticker})`.

## Grouping

`:by` takes column keywords, and also:

- named computed keys, as single-entry maps, such as `{:month #dt/e (month :date)}`
- `xbar` buckets and `cut` quantile bins (see [Binning](#binning-xbar-and-cut))
- a plain function of the row

A computed key is derived on the fly and appears in the result under the name you give it. Average price by day of the week, where 2 is Tuesday:

```clojure
(dt prices
    :by [{:dow #dt/e (dow :date)}]
    :agg {:avg-price #dt/e (mn :price)})
```

```text
_unnamed [4 2]:

| :dow | :avg-price |
|-----:|-----------:|
|    2 |      150.0 |
|    3 |      150.0 |
|    4 |      150.5 |
|    5 |      155.5 |
```

Use one map per key (`:by [{:y …} {:m …}]`), since the order of keys in a map is not reliable. See [computed `:by`](https://github.com/clojure-finance/datajure#computed-by--named-expression-keys--custom-grouping-functions) in the README for the rules.

## Dates

`year`, `month`, `day`, `dow` (day of week, 1 = Monday to 7 = Sunday) and `quarter` extract calendar fields from date and date-time columns. A missing date gives a missing value:

```clojure
(dt prices
    :set {:yr  #dt/e (year :date)
          :m   #dt/e (month :date)
          :q   #dt/e (quarter :date)
          :dow #dt/e (dow :date)}
    :select [:date :yr :m :q :dow]
    :take 4)
```

```text
_unnamed [4 5]:

|      :date |  :yr | :m | :q | :dow |
|------------|-----:|---:|---:|-----:|
| 2024-01-02 | 2024 |  1 |  1 |    2 |
| 2024-01-03 | 2024 |  1 |  1 |    3 |
| 2024-01-04 | 2024 |  1 |  1 |    4 |
| 2024-01-05 | 2024 |  1 |  1 |    5 |
```

They also work directly in `:where`, as in `#dt/e (= (quarter :date) 2)`, and in `:by` as named keys (above). For lags by date rather than by row, and for filling gaps in a panel, see the [time series](/pages-output/time-series/) page.

## Binning: `xbar` and `cut`

Binning turns a continuous value into a small number of groups. Datajure has two kinds:

- `xbar` cuts into fixed-width buckets, rounding down: `(xbar :price 50)` maps 99 to 50 and 104 to 100. With a unit, such as `(xbar :time 5 :minutes)`, it builds time bars.
- `cut` makes equal-count (quantile) bins numbered from 1: `(cut :price 2)` splits at the median.

Both work in `:by`, to group, and inside `#dt/e`, to add a column. Counting prices in buckets of 50:

```clojure
(require '[datajure.core :refer [xbar cut]])

(dt prices
    :by [(xbar :price 50)]
    :agg {:n nrow}
    :order-by [(asc :price)])
```

```text
_unnamed [4 2]:

| :price | :n |
|-------:|---:|
|     50 |  1 |
|    100 |  3 |
|    150 |  1 |
|    200 |  3 |
```

`cut` in `:by` names its result column after the column and the number of bins, here `:price-q2`:

```clojure
(dt prices
    :by [(cut :price 2)]
    :agg {:n nrow :lo #dt/e (mi :price) :hi #dt/e (mx :price)})
```

```text
_unnamed [2 4]:

| :price-q2 | :n |   :lo |   :hi |
|----------:|---:|------:|------:|
|         1 |  4 |  99.0 | 104.0 |
|         2 |  4 | 198.0 | 207.0 |
```

With `:from`, the breakpoints are computed from a subset of rows and then applied to all rows. In finance this is how size portfolios use NYSE breakpoints for every stock. Here the median comes from AAPL prices only, so every MSFT price lands in the upper bin:

```clojure
(dt prices
    :set {:half #dt/e (cut :price 2 :from (= :sym "AAPL"))}
    :select [:sym :price :half])
```

```text
_unnamed [8 3]:

| :sym | :price | :half |
|------|-------:|------:|
| AAPL |  100.0 |     1 |
| AAPL |  102.0 |     2 |
| AAPL |   99.0 |     1 |
| AAPL |  104.0 |     2 |
| MSFT |  200.0 |     2 |
| MSFT |  198.0 |     2 |
| MSFT |  202.0 |     2 |
| MSFT |  207.0 |     2 |
```

When `:by` also has ordinary keys, such as `:by [:date (cut :mktcap 5)]`, the breakpoints are computed separately within each date. See [`xbar`](https://github.com/clojure-finance/datajure#bucketing-with-xbar) and [`cut`](https://github.com/clojure-finance/datajure#quantile-binning-with-cut) in the README.

## Window Functions

A window function computes each row's value from a window of nearby rows in the same group: the previous row, the last 20 rows, or all rows so far. They run in `:set`, usually with `:by` to keep groups apart and `:within-order` to walk each group in date order, as in [Rows In, Rows Out](#rows-in-rows-out-by-with-set-or-agg). Without `:by`, the whole dataset is one group.

The main families are lags and leads (`win/lag`, `win/lead`, `win/tlag`), changes (`win/delta`, `win/ratio`), running totals (`win/cumsum`, `win/scan`), moving windows (`win/mavg`, `win/mdev`, `win/mmax`, …), `win/ema`, fills (`win/fills`, `win/bfill`) and ranks (`win/rank`, `win/row-number`). The [time series](/pages-output/time-series/) page covers them with examples, and the README lists [all of them](https://github.com/clojure-finance/datajure#window-functions).

## Row-Wise Functions

Window functions work down a column; `row/*` functions work across columns within one row. `row/sum` counts a missing value as 0, `row/mean`, `row/min` and `row/max` skip it, and all four give a missing value when every input is missing:

```clojure
(def quarterly
  (ds/->dataset {:firm ["A" "B" "C"]
                 :q1   [10 5 nil]
                 :q2   [12 nil nil]
                 :q3   [11 7 3]}))

(dt quarterly
    :set {:total  #dt/e (row/sum :q1 :q2 :q3)
          :avg    #dt/e (row/mean :q1 :q2 :q3)
          :n-miss #dt/e (row/count-nil :q1 :q2 :q3)})
```

```text
_unnamed [3 7]:

| :firm | :q1 | :q2 | :q3 | :total | :avg | :n-miss |
|-------|----:|----:|----:|-------:|-----:|--------:|
|     A |  10 |  12 |  11 |   33.0 | 11.0 |       0 |
|     B |   5 |     |   7 |   12.0 |  6.0 |       1 |
|     C |     |     |   3 |    3.0 |  3.0 |       2 |
```

`row/any-nil?` gives `true` for rows with any missing input, which is handy in `:where`.

## Statistical Transforms

`stat/*` functions transform a whole column using statistics of that column:

| Function | Result |
|---|---|
| `stat/standardize` | z-score: (x − mean) / standard deviation |
| `stat/demean` | x − mean |
| `stat/rescale` | Scaled to [0, 1], or to `[lo hi]` given as extra arguments |
| `stat/winsorize` | Values beyond the p and 1 − p quantiles clipped to them |
| `stat/trim` | Values beyond those quantiles made missing |

With `:by`, the statistics come from each group, so each symbol is standardized against its own prices:

```clojure
(dt prices :by [:sym]
    :set {:z   #dt/e (stat/standardize :price)
          :dm  #dt/e (stat/demean :price)
          :r01 #dt/e (stat/rescale :price)}
    :select [:sym :price :z :dm :r01])
```

```text
_unnamed [8 5]:

| :sym | :price |          :z |   :dm |       :r01 |
|------|-------:|------------:|------:|-----------:|
| AAPL |  100.0 | -0.56373452 | -1.25 | 0.20000000 |
| AAPL |  102.0 |  0.33824071 |  0.75 | 0.60000000 |
| AAPL |   99.0 | -1.01472214 | -2.25 | 0.00000000 |
| AAPL |  104.0 |  1.24021595 |  2.75 | 1.00000000 |
| MSFT |  200.0 | -0.45310844 | -1.75 | 0.22222222 |
| MSFT |  198.0 | -0.97094667 | -3.75 | 0.00000000 |
| MSFT |  202.0 |  0.06472978 |  0.25 | 0.44444444 |
| MSFT |  207.0 |  1.35932533 |  5.25 | 1.00000000 |
```

Winsorizing and trimming tame outliers. With p = 0.1, values below the 10th percentile (1.9) and above the 90th (18.1) are clipped, or removed:

```clojure
(def scores (ds/->dataset {:x [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 100.0]}))

(dt scores :set {:wins #dt/e (stat/winsorize :x 0.1)
                 :trim #dt/e (stat/trim :x 0.1)})
```

```text
_unnamed [10 3]:

|    :x | :wins | :trim |
|------:|------:|------:|
|   1.0 |   1.9 |       |
|   2.0 |   2.0 |   2.0 |
|   3.0 |   3.0 |   3.0 |
|   4.0 |   4.0 |   4.0 |
|   5.0 |   5.0 |   5.0 |
|   6.0 |   6.0 |   6.0 |
|   7.0 |   7.0 |   7.0 |
|   8.0 |   8.0 |   8.0 |
|   9.0 |   9.0 |   9.0 |
| 100.0 |  18.1 |       |
```

Missing values are left out of the statistics and stay missing. `stat/winsorize` also clips one tail only with `{:tail :upper}` or `{:tail :lower}`; see [statistical transforms](https://github.com/clojure-finance/datajure#statistical-transforms) in the README.

## Joins

`datajure.join/join` combines two datasets on key columns. It covers regular joins (`:inner`, `:left`, `:right`, `:outer`), filtering joins (`:semi` keeps left rows with a match, `:anti` those without), `:cross`, as-of joins (`:asof`) and window joins (`:window`). `:validate` checks key cardinality, for example that each key appears only once on the right (`:m:1`), and `:report true` prints how many rows matched:

```clojure
(require '[datajure.join :refer [join]])

(join prices sectors :on :sym :how :left :validate :m:1)
```

An as-of join matches each left row to the latest right row at or before it, such as each trade to the quote in force at that time. The last key in `:on` is the time key, and the other keys must match exactly:

```clojure
(join trades quotes :on [:sym :time] :how :asof)
```

The [time series](/pages-output/time-series/) page covers as-of and window joins with their options, and the README covers [joins](https://github.com/clojure-finance/datajure#joins) in full.

## Reshaping

`datajure.reshape` has `melt` (wide to long), `cast` (long to wide) and `tsfill`, which fills gaps in panel data so that each group has a row for every period:

```clojure
(require '[datajure.reshape :as reshape])

(reshape/cast prices {:id [:date] :from :sym :value :price})
(reshape/melt prices {:id [:date :sym] :measure [:price :volume]})
```

See [reshaping](https://github.com/clojure-finance/datajure#reshaping) in the README for the options.

## Cleaning and Inspecting Data

`datajure.util` has helpers for the first look at a dataset and for messy input. `describe` gives summary statistics, one row per column:

```clojure
(require '[datajure.util :as du])

(-> (du/describe prices [:price :volume])
    (dt :select [:column :n :n-missing :mean :min :max]))
```

```text
_unnamed [2 6]:

| :column | :n | :n-missing |         :mean |  :min |   :max |
|---------|---:|-----------:|--------------:|------:|-------:|
|  :price |  8 |          0 |  151.50000000 |  99.0 |  207.0 |
| :volume |  8 |          1 | 1257.14285714 | 900.0 | 1800.0 |
```

`describe` also reports the data type, standard deviation and quartiles. For a raw file, `clean-column-names` turns headers into keywords, `blank->nil` turns empty strings into missing values, `parse-numeric` reads numbers out of text such as `"$1,204.50"` (leaving values such as `"n/a"` missing), and `distinct-rows` drops duplicate rows:

```clojure
(def raw
  (ds/->dataset {"Ticker Symbol"   ["AAPL" "MSFT" "MSFT" " "]
                 "Close Price ($)" ["$1,204.50" "n/a" "n/a" "310"]}))

(-> raw
    du/clean-column-names
    du/blank->nil
    (du/parse-numeric [:close-price])
    du/distinct-rows)
```

```text
_unnamed [3 2]:

| :ticker-symbol | :close-price |
|----------------|-------------:|
|           AAPL |       1204.5 |
|           MSFT |              |
|                |        310.0 |
```

Other helpers include `mark-duplicates`, `drop-constant-columns` and `coerce-columns`; see [utilities](https://github.com/clojure-finance/datajure#utilities) in the README.

## Fast Lookups by Key

Looking up one key in a large dataset with `:where` scans every row each time. When the same dataset is queried by key again and again, build an index once with `datajure.index`; each lookup then returns the matching rows directly, in their original order:

```clojure
(require '[datajure.index :as idx])

(def by-sym (idx/index-by prices :sym))

(-> (idx/lookup by-sym "AAPL")
    (dt :order-by [(desc :date)] :take 2))
```

```text
_unnamed [2 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-05 | AAPL |  104.0 |    1800 |
| 2024-01-04 | AAPL |   99.0 |         |
```

A key that is not present gives an empty dataset. An index can also use several columns, looked up with a vector such as `["AAPL" date]`. See [keyed lookups](https://github.com/clojure-finance/datajure#keyed-lookups).

## Reusing a Grouping

Each `dt` call with `:by` groups the rows, and with `:within-order` it also sorts them. When several passes use the same groups, `prepare-grouping` does that work once, and the result is passed as `:by`:

```clojure
(require '[datajure.core :refer [prepare-grouping]])

(let [g (prepare-grouping prices [:sym] [(asc :date)])]
  (-> prices
      (dt :by g :set {:ret #dt/e (- (win/ratio :price) 1)})
      (dt :by g :agg {:mean-ret #dt/e (mn :ret)})))
```

```text
_unnamed [2 2]:

| :sym |  :mean-ret |
|------|-----------:|
| AAPL | 0.01369776 |
| MSFT | 0.01165150 |
```

It stays valid as long as the rows and their order are unchanged; adding columns is fine.

## Other Namespaces

| Namespace | Contents |
|---|---|
| `datajure.io` | `read`, `write` and `read-seq` for CSV, TSV, JSON, JSON Lines, Nippy, Parquet, Arrow and Excel |
| `datajure.util` | `describe`, `clean-column-names`, `distinct-rows`, `mark-duplicates`, `coerce-columns`, `blank->nil`, `parse-numeric` |
| `datajure.index` | `index-by` and `lookup` for repeated lookups by key |
| `datajure.join` | `join`, including as-of and window joins |
| `datajure.reshape` | `melt`, `cast`, `tsfill` |
| `datajure.concise` | Short aliases (`mn`, `sm`, `N`, …) for use outside `#dt/e` |
| `datajure.clay`, `datajure.clerk` | Notebook viewers |
| `datajure.nrepl` | nREPL middleware that binds `*dt*` to the last result |

## Errors

Errors are `ex-info` exceptions whose data has a `:dt/error` key for handling them in code. Many suggest a fix, such as the closest column name for a misspelled column or the intended keyword for a misspelled `dt` keyword:

```clojure
(dt prices :set {:value #dt/e (* :prcie :volume)})
;; Unknown column(s) #{:prcie} in :set :value expression
;; Did you mean: :price (edit distance 1)
;; Available: :date :price :sym :volume
```

See the [examples](/pages-output/examples/#errors) for more.
