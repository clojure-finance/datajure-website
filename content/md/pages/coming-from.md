{:title "Coming from R or Python"
 :nav-title "From R & Python"
 :subtitle "Map what you know from dplyr, data.table, pandas and Polars onto Datajure."
 :layout :page
 :toc :ul
 :page-index 5
 :navbar? true}

If you already know dplyr, data.table, pandas or Polars, you know most of Datajure. This page shows the few Clojure ideas you need, the same query in each library, and a translation table for everyday tasks. The examples use the `prices` dataset from the [quick start](/pages-output/quick-start/): two symbols, four days, and one missing volume.

## Clojure in Five Minutes

You need only a handful of ideas to read and write Datajure queries.

**Calls put the function first.** `f(x, y)` in R or Python is `(f x y)` in Clojure. Operators are functions too, so `1 + 2` is `(+ 1 2)`:

```clojure
(+ 1 2)
```

```text
3
```

**Keywords name columns.** A keyword starts with a colon: `:price`, `:sym`. Column names in Clojure usually use hyphens (`:avg-price`) rather than underscores.

**Maps and vectors hold data.** A map `{:sym "AAPL" :price 100.0}` is like a named list or a dict, and a vector `[:sym :date]` is like a list. Commas are optional whitespace. A keyword also works as a function that looks itself up in a map:

```clojure
(def row {:sym "AAPL" :price 100.0})

(:price row)
```

```text
100.0
```

**`->` is the pipe.** It passes each result as the first argument of the next call, like `|>` in R or method chaining in pandas and Polars:

```clojure
(require '[datajure.core :refer [dt dt1 nrow asc desc rename]])

(-> prices
    (dt :where #dt/e (> :price 100))
    (dt :by [:sym] :agg {:n nrow}))
```

```text
_unnamed [2 2]:

| :sym | :n |
|------|---:|
| AAPL |  2 |
| MSFT |  4 |
```

**Nothing changes in place.** Every `dt` call returns a new dataset and leaves its input alone, so there is no `inplace=True` and no `:=`. Adding a column to `prices` gives a new dataset, and `prices` still has its original four columns:

```clojure
(def with-value (dt prices :set {:value #dt/e (* :price :volume)}))

[(ds/column-names prices) (ds/column-names with-value)]
```

```text
[(:date :sym :price :volume) (:date :sym :price :volume :value)]
```

**`nil` is the missing value.** It plays the role of `NA` in R, and `None`, `NaN` or `null` in pandas and Polars.

**`#dt/e` marks an expression.** Inside `#dt/e (...)`, a bare keyword such as `:price` means "the price column", much like a bare column name inside `dplyr::filter()` or `pl.col("price")` in Polars. The [docs](/pages-output/docs/#expressions) cover expressions in full.

## The Same Query in Five Libraries

For each symbol, on days with a volume above 1000: count the days, average the price, and total the volume, then sort by total volume, largest first.

**dplyr**

```r
prices |>
  filter(volume > 1000) |>
  summarise(n = n(), avg_price = mean(price), total_vol = sum(volume), .by = sym) |>
  arrange(desc(total_vol))
```

**data.table**

```r
prices[volume > 1000,
       .(n = .N, avg_price = mean(price), total_vol = sum(volume)),
       by = sym][order(-total_vol)]
```

**pandas**

```python
(prices[prices["volume"] > 1000]
 .groupby("sym", as_index=False)
 .agg(n=("price", "size"), avg_price=("price", "mean"), total_vol=("volume", "sum"))
 .sort_values("total_vol", ascending=False))
```

**Polars**

```python
(prices
 .filter(pl.col("volume") > 1000)
 .group_by("sym")
 .agg(n=pl.len(), avg_price=pl.col("price").mean(), total_vol=pl.col("volume").sum())
 .sort("total_vol", descending=True))
```

**Datajure**

```clojure
(dt prices
    :where    #dt/e (> :volume 1000)
    :by       [:sym]
    :agg      {:n         nrow
               :avg-price #dt/e (mn :price)
               :total-vol #dt/e (sm :volume)}
    :order-by [(desc :total-vol)])
```

```text
_unnamed [2 4]:

| :sym | :n | :avg-price | :total-vol |
|------|---:|-----------:|-----------:|
| AAPL |  3 |      102.0 |     4500.0 |
| MSFT |  2 |      202.5 |     2400.0 |
```

All five give the same numbers. The Datajure version reads most like data.table: one call with named parts, where `:where` is data.table's `i`, `:agg` is its `j`, and `:by` is its `by`. The parts can be written in any order, because `dt` always runs them in the same [fixed order](/pages-output/docs/#evaluation-order).

## Translation Table

Each row does the same thing on `prices` (and on `sectors`, `trades` and `quotes` for the joins, as in the [examples](/pages-output/examples/#joins)). The Datajure column assumes the `require` above; joins and reshaping need their own namespaces:

```clojure
(require '[datajure.join :refer [join]]
         '[datajure.reshape :as reshape])
```

### dplyr and pandas

| Task | dplyr | pandas | Datajure |
|---|---|---|---|
| Filter rows | `filter(prices, price > 100)` | `prices[prices["price"] > 100]` | `(dt prices :where #dt/e (> :price 100))` |
| Add a column | `mutate(prices, value = price * volume)` | `prices.assign(value=prices["price"] * prices["volume"])` | `(dt prices :set {:value #dt/e (* :price :volume)})` |
| Summarize by group | `summarise(prices, avg = mean(price), .by = sym)` | `prices.groupby("sym", as_index=False).agg(avg=("price", "mean"))` | `(dt prices :by [:sym] :agg {:avg #dt/e (mn :price)})` |
| Count rows per group | `count(prices, sym)` | `prices.groupby("sym", as_index=False).size()` | `(dt prices :by [:sym] :agg {:n nrow})` |
| Previous value within group | `mutate(prices, prev = lag(price, order_by = date), .by = sym)` | `prices.assign(prev=prices.groupby("sym")["price"].shift())` | `(dt prices :by [:sym] :within-order [(asc :date)] :set {:prev #dt/e (win/lag :price 1)})` |
| Sort, descending | `arrange(prices, desc(price))` | `prices.sort_values("price", ascending=False)` | `(dt prices :order-by [(desc :price)])` |
| Top 3 rows | `slice_max(prices, price, n = 3)` | `prices.nlargest(3, "price")` | `(dt prices :order-by [(desc :price)] :take 3)` |
| Keep columns | `select(prices, sym, price)` | `prices[["sym", "price"]]` | `(dt prices :select [:sym :price])` |
| Drop a column | `select(prices, -volume)` | `prices.drop(columns="volume")` | `(dt prices :select [:not :volume])` |
| Rename a column | `rename(prices, ticker = sym)` | `prices.rename(columns={"sym": "ticker"})` | `(rename prices {:sym :ticker})` |
| Left join | `left_join(prices, sectors, by = "sym")` | `prices.merge(sectors, on="sym", how="left")` | `(join prices sectors :on :sym :how :left)` |
| Anti join | `anti_join(prices, sectors, by = "sym")` | `prices[~prices["sym"].isin(sectors["sym"])]` | `(join prices sectors :on :sym :how :anti)` |
| As-of join | `left_join(trades, quotes, join_by(sym, closest(time >= time)))` | `pd.merge_asof(trades, quotes, on="time", by="sym")` | `(join trades quotes :on [:sym :time] :how :asof)` |
| Long to wide | `pivot_wider(prices, id_cols = date, names_from = sym, values_from = price)` | `prices.pivot(index="date", columns="sym", values="price")` | `(reshape/cast prices {:id [:date] :from :sym :value :price})` |
| Wide to long | `pivot_longer(prices, c(price, volume))` | `prices.melt(id_vars=["date", "sym"], value_vars=["price", "volume"])` | `(reshape/melt prices {:id [:date :sym] :measure [:price :volume]})` |
| Replace missing values | `mutate(prices, volume = coalesce(volume, 0L))` | `prices.fillna({"volume": 0})` | `(dt prices :set {:volume #dt/e (coalesce :volume 0)})` |
| Drop rows with a missing value | `filter(prices, !is.na(volume))` | `prices.dropna(subset=["volume"])` | `(dt prices :where #(some? (:volume %)))` |
| Fill forward within group | `tidyr::fill(prices, volume, .by = sym)` | `prices.assign(volume=prices.groupby("sym")["volume"].ffill())` | `(dt prices :by [:sym] :within-order [(asc :date)] :set {:volume #dt/e (win/fills :volume)})` |

The pandas lag and fill rows, and dplyr's `fill`, assume the rows are already sorted by date; `pd.merge_asof` needs both inputs sorted by `time`. In Datajure, `:within-order` sets the order a window function walks in, so the input order does not matter, and the as-of join sorts for you.

### data.table and Polars

| Task | data.table | Polars | Datajure |
|---|---|---|---|
| Filter rows | `prices[price > 100]` | `prices.filter(pl.col("price") > 100)` | `(dt prices :where #dt/e (> :price 100))` |
| Add a column | `prices[, value := price * volume]` | `prices.with_columns(value=pl.col("price") * pl.col("volume"))` | `(dt prices :set {:value #dt/e (* :price :volume)})` |
| Summarize by group | `prices[, .(avg = mean(price)), by = sym]` | `prices.group_by("sym").agg(avg=pl.col("price").mean())` | `(dt prices :by [:sym] :agg {:avg #dt/e (mn :price)})` |
| Count rows per group | `prices[, .N, by = sym]` | `prices.group_by("sym").len()` | `(dt prices :by [:sym] :agg {:n nrow})` |
| Previous value within group | `prices[order(date), prev := shift(price), by = sym]` | `prices.with_columns(prev=pl.col("price").shift().over("sym", order_by="date"))` | `(dt prices :by [:sym] :within-order [(asc :date)] :set {:prev #dt/e (win/lag :price 1)})` |
| Top 3 rows | `prices[order(-price)][1:3]` | `prices.top_k(3, by="price")` | `(dt prices :order-by [(desc :price)] :take 3)` |
| As-of join | `quotes[trades, on = .(sym, time), roll = TRUE]` | `trades.join_asof(quotes, on="time", by="sym")` | `(join trades quotes :on [:sym :time] :how :asof)` |
| Long to wide | `dcast(prices, date ~ sym, value.var = "price")` | `prices.pivot(on="sym", index="date", values="price")` | `(reshape/cast prices {:id [:date] :from :sym :value :price})` |
| Replace missing values | `prices[is.na(volume), volume := 0L]` | `prices.with_columns(pl.col("volume").fill_null(0))` | `(dt prices :set {:volume #dt/e (coalesce :volume 0)})` |
| Fill forward within group | `prices[, volume := nafill(volume, "locf"), by = sym]` | `prices.with_columns(pl.col("volume").forward_fill().over("sym", order_by="date"))` | `(dt prices :by [:sym] :within-order [(asc :date)] :set {:volume #dt/e (win/fills :volume)})` |

The data.table fill row, like the pandas one, assumes the rows are already sorted by date; the Polars rows pass `order_by`, so their input order does not matter. The data.table `:=` rows change `prices` itself; the Datajure versions return a new dataset instead.

## Differences Worth Knowing

### Missing Values

Each library has its own rules for missing values. Datajure's are:

- **Aggregations skip missing values.** R's `sum(volume)` is `NA` unless you pass `na.rm = TRUE`; Datajure's `sm`, like pandas and Polars, skips the missing value. `ct` counts the values that are present, and `nrow` counts rows:

```clojure
(dt1 prices :agg {:total #dt/e (sm :volume)
                  :n     #dt/e (ct :volume)
                  :rows  nrow})
```

```text
{:total 8800.0, :n 7, :rows 8}
```

- **A comparison with a missing value is `false`, not missing.** So `:where #dt/e (> :volume 1200)` drops the missing row, as in every library. But `not` turns that `false` into `true`, so the negated filter *keeps* the missing row. pandas behaves the same way; dplyr, data.table and Polars drop the row, because there the comparison is `NA` or `null`:

```clojure
(dt prices :where #dt/e (not (> :volume 1200)))
```

```text
_unnamed [5 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |
| 2024-01-04 | AAPL |   99.0 |         |
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-03 | MSFT |  198.0 |    1100 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
```

- **Arithmetic on a missing value gives a missing value**, as in R and Polars.
- **Plain Clojure functions get no special handling.** `#(some? (:volume %))` above is a plain function, called once per row with the row as a map. `pass-nil` wraps one so it returns `nil` when a column is missing.

See [Missing Values](/pages-output/docs/#missing-values) in the docs and [Nil handling](https://github.com/clojure-finance/datajure#nil-handling) in the README for the full rules.

### Filtering After Aggregating

Because `dt` runs `:where` before `:agg`, a single call cannot filter on an aggregated value (SQL's `HAVING`, or a `filter()` after `summarise()`). Thread a second call instead:

```clojure
(-> prices
    (dt :by [:sym] :agg {:avg-price #dt/e (mn :price)})
    (dt :where #dt/e (> :avg-price 150)))
```

```text
_unnamed [1 2]:

| :sym | :avg-price |
|------|-----------:|
| MSFT |     201.75 |
```

The same holds for `:set` and `:agg`, which cannot appear in the same call.

### Columns That Use Each Other

In dplyr's `mutate()` and Polars' `with_columns()` the rules differ: dplyr lets a later column use an earlier one in the same call, Polars does not. Datajure lets you choose with the shape of `:set`. In a map, every column sees only the input; in a vector of pairs, each column sees the ones before it:

```clojure
(dt prices :set [[:value #dt/e (* :price :volume)]
                 [:big?  #dt/e (> :value 200000)]])
```

A map whose columns refer to each other is an error that says to use pairs, rather than a silent surprise.

### Grouped Columns Keep Every Row

`:by` with `:set` is dplyr's grouped `mutate()`, data.table's `:=` with `by`, and Polars' `.over()`: each row gets a value computed within its group. An aggregation inside it, such as `mn`, uses the group's rows:

```clojure
(dt prices :by [:sym] :set {:dev #dt/e (- :price (mn :price))})
```

```text
_unnamed [8 5]:

|      :date | :sym | :price | :volume |  :dev |
|------------|------|-------:|--------:|------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | -1.25 |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.75 |
| 2024-01-04 | AAPL |   99.0 |         | -2.25 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  2.75 |
| 2024-01-02 | MSFT |  200.0 |     900 | -1.75 |
| 2024-01-03 | MSFT |  198.0 |    1100 | -3.75 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.25 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  5.25 |
```

The output keeps the input row order. Unlike pandas, there is no index to reset afterwards.

### Expressions Are Values

Like a Polars expression, a `#dt/e` expression is a value you can store and reuse. Unlike R's quoted expressions, it needs no `!!` or `{{ }}` to splice in:

```clojure
(def value #dt/e (* :price :volume))

(dt prices :by [:sym] :agg {:avg-value #dt/e (mn value)})
```

```text
_unnamed [2 2]:

| :sym | :avg-value |
|------|-----------:|
| AAPL |   153400.0 |
| MSFT |   217225.0 |
```

### Values Known Only at Run Time

`#dt/e` is expanded when the code is read, so it cannot see a local variable such as a function argument. For those, write the expression as plain data, a vector headed by an operation keyword. This plays the role of `.data[[col]]` and `{{ }}` in dplyr, or building `pl.col(name)` in Polars:

```clojure
(defn above [ds col threshold]
  (dt ds :where [:> col threshold]))

(above prices :price 200)
```

```text
_unnamed [2 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

See [Runtime Values and Data-Forms](/pages-output/docs/#runtime-values-and-data-forms) in the docs.

## Next Steps

- The [quick start](/pages-output/quick-start/) installs Datajure and runs a first query.
- The [docs](/pages-output/docs/) explain each `dt` keyword and the expression language.
- The [examples](/pages-output/examples/) and [time series](/pages-output/time-series/) pages show more queries with their output.
- The [README](https://github.com/clojure-finance/datajure#readme) is the complete reference.
