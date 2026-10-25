{:title "Time Series and Panels"
 :nav-title "Time Series"
 :subtitle "Returns, rolling windows, gaps, bars and as-of joins: the everyday work of panel data."
 :layout :page
 :toc :ul
 :page-index 4
 :navbar? true}

This page covers the work that fills most of a finance or economics project: returns, rolling statistics, gaps in the data, intraday bars, and matching records by time. It uses the `prices` dataset from the [quick start](/pages-output/quick-start/) and a few small datasets defined along the way. Every output comes from running the code.

## The Basic Pattern

A *time series* is a sequence of observations in time order, such as one stock's daily prices. A *panel* stacks many time series in one table, with one row per entity and period: each stock on each day, or each firm in each year. `prices` is a panel of two stocks over four days.

Most panel calculations follow the same recipe: split the rows by entity, put each entity's rows in time order, then compute something that looks at neighbouring rows. In Datajure that recipe is three keywords:

```clojure
(dt prices
    :by [:sym]                  ;; one group per stock
    :within-order [(asc :date)] ;; oldest first within each group
    :set {:prev #dt/e (win/lag :price 1)
          :next #dt/e (win/lead :price 1)})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :prev | :next |
|------------|------|-------:|--------:|------:|------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |       | 102.0 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 100.0 |  99.0 |
| 2024-01-04 | AAPL |   99.0 |         | 102.0 | 104.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  99.0 |       |
| 2024-01-02 | MSFT |  200.0 |     900 |       | 198.0 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 200.0 | 202.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 198.0 | 207.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 202.0 |       |
```

The functions that start with `win/` are *window functions*: each row's result depends on other rows of its group. They only work inside `:set`. The first row of each stock has no previous price, so `win/lag` gives a missing value there instead of borrowing the other stock's price.

`:within-order` decides the order in which the window functions walk each group. It does not reorder the result: rows come back in the order they went in. Here the same query runs on a copy of `prices` sorted by date, so the two stocks are interleaved, and each row still gets its own stock's previous price:

```clojure
(def by-date (dt prices :order-by [(asc :date)]))

(dt by-date :by [:sym] :within-order [(asc :date)]
    :set {:prev #dt/e (win/lag :price 1)})
```

```text
_unnamed [8 5]:

|      :date | :sym | :price | :volume | :prev |
|------------|------|-------:|--------:|------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |       |
| 2024-01-02 | MSFT |  200.0 |     900 |       |
| 2024-01-03 | AAPL |  102.0 |    1500 | 100.0 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 200.0 |
| 2024-01-04 | AAPL |   99.0 |         | 102.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 198.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  99.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 202.0 |
```

Add `:order-by` when the output should be sorted. Without `:by`, the whole dataset is one group, which suits a single time series.

> **Tip:** `win/lag` and `win/lead` take an options map to fill the rows that have no neighbour: `#dt/e (win/lag :price 1 {:fill 0})`. Only those boundary rows are filled; a value that is missing in the data stays missing.

## Returns and Changes

`win/ratio` divides each value by the previous one, and `win/delta` subtracts the previous one. A simple return is the ratio minus one, and a log return is the log of the ratio:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:ret     #dt/e (- (win/ratio :price) 1)
          :log-ret #dt/e (log (win/ratio :price))
          :chg     #dt/e (win/delta :price)})
```

```text
_unnamed [8 7]:

|      :date | :sym | :price | :volume |        :ret |    :log-ret | :chg |
|------------|------|-------:|--------:|------------:|------------:|-----:|
| 2024-01-02 | AAPL |  100.0 |    1200 |             |             |      |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.02000000 |  0.01980263 |  2.0 |
| 2024-01-04 | AAPL |   99.0 |         | -0.02941176 | -0.02985296 | -3.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  0.05050505 |  0.04927105 |  5.0 |
| 2024-01-02 | MSFT |  200.0 |     900 |             |             |      |
| 2024-01-03 | MSFT |  198.0 |    1100 | -0.01000000 | -0.01005034 | -2.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.02020202 |  0.02000067 |  4.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  0.02475248 |  0.02445110 |  5.0 |
```

If the previous price is zero or missing, `win/ratio` gives a missing value rather than infinity, so one bad price does not spread through later calculations.

Returns are needed in many queries, so it helps to define them once. An `#dt/e` expression is a value that can be stored in a var and used anywhere a column expression fits (see [reusable expressions](/pages-output/docs/#reusable-expressions)):

```clojure
(def ret #dt/e (- (win/ratio :price) 1))
```

## Rolling Windows

A *rolling* (or moving) window looks at the current row and a fixed number of rows before it. `win/mavg` is the moving average, `win/mmax` and `win/mmin` the moving maximum and minimum, `win/msum` the moving sum and `win/mdev` the moving standard deviation. Here is a three-day window:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:ma-3      #dt/e (win/mavg :price 3)
          :ma-3-full #dt/e (win/mavg :price 3 {:min-periods 3})
          :hi-3      #dt/e (win/mmax :price 3)})
```

```text
_unnamed [8 7]:

|      :date | :sym | :price | :volume |        :ma-3 |   :ma-3-full | :hi-3 |
|------------|------|-------:|--------:|-------------:|-------------:|------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 100.00000000 |              | 100.0 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 101.00000000 |              | 102.0 |
| 2024-01-04 | AAPL |   99.0 |         | 100.33333333 | 100.33333333 | 102.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 | 101.66666667 | 101.66666667 | 104.0 |
| 2024-01-02 | MSFT |  200.0 |     900 | 200.00000000 |              | 200.0 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 199.00000000 |              | 200.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 200.00000000 | 200.00000000 | 202.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 202.33333333 | 202.33333333 | 207.0 |
```

At the start of each group there are fewer than three rows to look at. By default the window is *expanding* there: the first row averages one price, the second averages two. With `{:min-periods 3}`, the result stays missing until three rows are available, as R's `zoo::rollapplyr` does.

Moving functions skip missing values. The AAPL volume on 2024-01-04 is missing, so the two-day moving sum on that day is just the previous day's volume:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:vol-2 #dt/e (win/msum :volume 2)
          :sd-3  #dt/e (win/mdev :price 3)})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :vol-2 |      :sd-3 |
|------------|------|-------:|--------:|-------:|-----------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 1200.0 |            |
| 2024-01-03 | AAPL |  102.0 |    1500 | 2700.0 | 1.41421356 |
| 2024-01-04 | AAPL |   99.0 |         | 1500.0 | 1.52752523 |
| 2024-01-05 | AAPL |  104.0 |    1800 | 1800.0 | 2.51661148 |
| 2024-01-02 | MSFT |  200.0 |     900 |  900.0 |            |
| 2024-01-03 | MSFT |  198.0 |    1100 | 2000.0 | 1.41421356 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 2100.0 | 2.00000000 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 2300.0 | 4.50924975 |
```

A standard deviation needs at least two values, so `:sd-3` is missing on each stock's first day.

### Exponential Moving Average

An exponential moving average (EMA) weights recent rows more heavily than older ones, without a fixed window. `win/ema` takes either a period or a smoothing factor *alpha*; a period of n means alpha = 2 / (n + 1), so a period of 3 and an alpha of 0.5 give the same result:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:ema-3   #dt/e (win/ema :price 3)
          :ema-0.5 #dt/e (win/ema :price {:alpha 0.5})})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :ema-3 | :ema-0.5 |
|------------|------|-------:|--------:|-------:|---------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 100.00 |   100.00 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 101.00 |   101.00 |
| 2024-01-04 | AAPL |   99.0 |         | 100.00 |   100.00 |
| 2024-01-05 | AAPL |  104.0 |    1800 | 102.00 |   102.00 |
| 2024-01-02 | MSFT |  200.0 |     900 | 200.00 |   200.00 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 199.00 |   199.00 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 200.50 |   200.50 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 203.75 |   203.75 |
```

## Running Totals

`win/cumsum` adds up a column from the first row of each group, and `win/scan` does the same with another operation: `*` for compounding, `max` or `min` for a running high or low.

The *wealth index* is what one dollar invested on the first day is worth on each later day. It is the running product of one plus the return. The first return is missing, so `coalesce` treats it as zero:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set [[:ret    #dt/e (- (win/ratio :price) 1)]
          [:wealth #dt/e (win/scan * (+ 1 (coalesce :ret 0)))]])
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume |        :ret | :wealth |
|------------|------|-------:|--------:|------------:|--------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |             |   1.000 |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.02000000 |   1.020 |
| 2024-01-04 | AAPL |   99.0 |         | -0.02941176 |   0.990 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  0.05050505 |   1.040 |
| 2024-01-02 | MSFT |  200.0 |     900 |             |   1.000 |
| 2024-01-03 | MSFT |  198.0 |    1100 | -0.01000000 |   0.990 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.02020202 |   1.010 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  0.02475248 |   1.035 |
```

`:set` is given a sequence of pairs here rather than a map, so `:wealth` can use the `:ret` column defined just before it.

The *drawdown* is how far the price is below its highest level so far. The running maximum comes from `win/scan max`:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set [[:peak     #dt/e (win/scan max :price)]
          [:drawdown #dt/e (- (/ :price :peak) 1)]])
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :peak |   :drawdown |
|------------|------|-------:|--------:|------:|------------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 100.0 |  0.00000000 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 102.0 |  0.00000000 |
| 2024-01-04 | AAPL |   99.0 |         | 102.0 | -0.02941176 |
| 2024-01-05 | AAPL |  104.0 |    1800 | 104.0 |  0.00000000 |
| 2024-01-02 | MSFT |  200.0 |     900 | 200.0 |  0.00000000 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 200.0 | -0.01000000 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 202.0 |  0.00000000 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 207.0 |  0.00000000 |
```

## Filling Missing Values

Datajure never fills missing values on its own: a gap stays visible until you decide what it should mean. Three window functions fill it on request:

- `win/fills` carries the last known value forward (pandas `ffill`, "last observation carried forward").
- `win/fills` with `{:limit n}` carries it at most n rows into a gap, after which the value is treated as stale.
- `win/bfill` carries the next known value backward.

A single series of readings with a three-day gap shows the difference. There is no `:by`, so the whole dataset is one series:

```clojure
(def readings
  (ds/->dataset {:day   [1 2 3 4 5 6]
                 :value [1.0 nil nil nil 5.0 nil]}))

(dt readings :within-order [(asc :day)]
    :set {:carried   #dt/e (win/fills :value)
          :at-most-2 #dt/e (win/fills :value {:limit 2})
          :next      #dt/e (win/bfill :value)})
```

```text
_unnamed [6 5]:

| :day | :value | :carried | :at-most-2 | :next |
|-----:|-------:|---------:|-----------:|------:|
|    1 |    1.0 |      1.0 |        1.0 |   1.0 |
|    2 |        |      1.0 |        1.0 |   5.0 |
|    3 |        |      1.0 |        1.0 |   5.0 |
|    4 |        |      1.0 |            |   5.0 |
|    5 |    5.0 |      5.0 |        5.0 |   5.0 |
|    6 |        |      5.0 |        5.0 |       |
```

To fill both ways, nest them: `#dt/e (win/fills (win/bfill :value))`. To replace missing values with a constant instead, use `coalesce`.

## Panels with Missing Periods

### Lag by Date, Not by Row

`win/lag` counts rows. When a period is missing from the data, the previous row is not the previous period. In this yearly panel, firm B has no row for 2016:

```clojure
(def sales
  (ds/->dataset {:firm  ["A" "A" "A" "A" "B" "B" "B"]
                 :year  [2015 2016 2017 2018 2015 2017 2018]
                 :sales [10.0 12.0 15.0 16.0 50.0 60.0 66.0]}))

(dt sales :by [:firm] :within-order [(asc :year)]
    :set {:lag-row  #dt/e (win/lag :sales 1)
          :lag-year #dt/e (win/tlag :sales :year)})
```

```text
_unnamed [7 5]:

| :firm | :year | :sales | :lag-row | :lag-year |
|-------|------:|-------:|---------:|----------:|
|     A |  2015 |   10.0 |          |           |
|     A |  2016 |   12.0 |     10.0 |      10.0 |
|     A |  2017 |   15.0 |     12.0 |      12.0 |
|     A |  2018 |   16.0 |     15.0 |      15.0 |
|     B |  2015 |   50.0 |          |           |
|     B |  2017 |   60.0 |     50.0 |           |
|     B |  2018 |   66.0 |     60.0 |      60.0 |
```

For firm B in 2017, `win/lag` returns the 2015 value, so a "one-year growth" computed from it would really be a two-year growth. `win/tlag` looks up the row whose `:year` is one less, finds none, and returns a missing value. The date column can be a number, as here, or a `java.time` date with a unit such as `{:unit :month}`; see [date-aware lag](https://github.com/clojure-finance/datajure#date-aware-lag-wintlag) in the README.

### Making Gaps Explicit

`tsfill` (named after Stata's command) inserts the missing periods as rows, with missing values in every column except the keys. After that, every row-based function sees one row per period:

```clojure
(reshape/tsfill sales {:by [:firm] :date :year})
```

```text
_unnamed [8 3]:

| :firm | :year | :sales |
|-------|------:|-------:|
|     A |  2015 |   10.0 |
|     A |  2016 |   12.0 |
|     A |  2017 |   15.0 |
|     A |  2018 |   16.0 |
|     B |  2015 |   50.0 |
|     B |  2016 |        |
|     B |  2017 |   60.0 |
|     B |  2018 |   66.0 |
```

Each firm's rows run from its own first year to its own last year, and rows that were already there are never dropped. For dates rather than year numbers, `:every :month` (or `:day`, `:week`, `:quarter`, `:year`) sets the step, and `:grid` takes an explicit list such as trading days.

## Bars from Ticks

Trade data arrives one row per trade (a *tick*). A *bar* summarizes the ticks in a fixed interval: the first price (open), the highest and lowest, the last price (close) and the total size traded. `xbar` assigns each timestamp to its interval, and `:within-order` makes `first-val` and `last-val` see each interval's ticks in time order:

```clojure
(require '[datajure.core :refer [xbar]])

(def ticks
  (ds/->dataset
   {:time  (map #(java.time.LocalDateTime/parse %)
                ["2024-01-02T09:30:05" "2024-01-02T09:31:40" "2024-01-02T09:33:10"
                 "2024-01-02T09:34:55" "2024-01-02T09:35:20" "2024-01-02T09:36:45"
                 "2024-01-02T09:38:30" "2024-01-02T09:41:00"])
    :price [100.0 100.4 99.8 100.2 100.6 101.0 100.7 100.9]
    :size  [200 100 300 150 250 100 200 400]}))

(dt ticks
    :by [(xbar :time 5 :minutes)]
    :within-order [(asc :time)]
    :agg {:open   #dt/e (first-val :price)
          :high   #dt/e (mx :price)
          :low    #dt/e (mi :price)
          :close  #dt/e (last-val :price)
          :volume #dt/e (sm :size)})
```

```text
_unnamed [3 6]:

|    :time | :open | :high |  :low | :close | :volume |
|---------:|------:|------:|------:|-------:|--------:|
| 28403130 | 100.0 | 100.4 |  99.8 |  100.2 |   750.0 |
| 28403135 | 100.6 | 101.0 | 100.6 |  100.7 |   550.0 |
| 28403140 | 100.9 | 100.9 | 100.9 |  100.9 |   400.0 |
```

For a date-time column, the bar key is the start of the interval counted in units since 1970-01-01, here minutes. A plain function turns it back into a time:

```clojure
(defn minutes->time [m]
  (java.time.LocalDateTime/ofEpochSecond (* 60 m) 0 java.time.ZoneOffset/UTC))

(-> ticks
    (dt :by [(xbar :time 5 :minutes)]
        :within-order [(asc :time)]
        :agg {:open   #dt/e (first-val :price)
              :high   #dt/e (mx :price)
              :low    #dt/e (mi :price)
              :close  #dt/e (last-val :price)
              :volume #dt/e (sm :size)})
    (dt :set {:time #(minutes->time (:time %))}))
```

```text
_unnamed [3 6]:

|            :time | :open | :high |  :low | :close | :volume |
|------------------|------:|------:|------:|-------:|--------:|
| 2024-01-02T09:30 | 100.0 | 100.4 |  99.8 |  100.2 |   750.0 |
| 2024-01-02T09:35 | 100.6 | 101.0 | 100.6 |  100.7 |   550.0 |
| 2024-01-02T09:40 | 100.9 | 100.9 | 100.9 |  100.9 |   400.0 |
```

With several instruments, add the symbol to `:by`: `:by [(xbar :time 5 :minutes) :sym]`. On a number column, `xbar` makes equal-width buckets: `(xbar :price 10)` groups prices into 100–110, 110–120 and so on.

## Matching Records by Time

### As-of Joins

Two time series rarely share timestamps. Trades happen at one set of times and quotes at another. An *as-of join* gives each left row the most recent right row at or before its time, which is the information that was available at that moment. These datasets give times as plain numbers:

```clojure
(def trades (ds/->dataset {:sym  ["AAPL" "AAPL" "MSFT"]
                           :time [10 25 30]
                           :qty  [100 50 75]}))

(def quotes (ds/->dataset {:sym  ["AAPL" "AAPL" "AAPL" "MSFT"]
                           :time [5 20 30 28]
                           :bid  [99.5 100.5 101.0 201.0]}))

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

The last key in `:on` (`:time`) is matched as-of; the keys before it (`:sym`) must match exactly. Every left row is kept.

`:tolerance` limits how old a match may be. A quote more than 3 time units older than the trade counts as no match:

```clojure
(join trades quotes :on [:sym :time] :how :asof :tolerance 3)
```

```text
_unnamed [3 4]:

| :sym | :time | :qty |  :bid |
|------|------:|-----:|------:|
| AAPL |    10 |  100 |       |
| AAPL |    25 |   50 |       |
| MSFT |    30 |   75 | 201.0 |
```

`:direction :forward` matches the first right row at or after the left row's time instead, and `:direction :nearest` the closest one either way:

```clojure
(join trades quotes :on [:sym :time] :how :asof :direction :forward)
```

```text
_unnamed [3 4]:

| :sym | :time | :qty |  :bid |
|------|------:|-----:|------:|
| AAPL |    10 |  100 | 100.5 |
| AAPL |    25 |   50 | 101.0 |
| MSFT |    30 |   75 |       |
```

> **Note:** For research data, the default backward direction is usually the right one: it never attaches information from the future. With dates as keys, `:tolerance` takes a duration such as `[90 :days]`.

### Window Joins

A *window join* gives each left row a summary of all right rows in a time window around it, rather than a single match. Here each trade gets the average bid and the number of quotes in the 20 time units up to and including the trade:

```clojure
(join trades quotes :on [:sym :time] :how :window
      :window [-20 0]
      :agg {:avg-bid  #dt/e (mn :bid)
            :n-quotes nrow})
```

```text
_unnamed [3 5]:

| :sym | :time | :qty | :avg-bid | :n-quotes |
|------|------:|-----:|---------:|----------:|
| AAPL |    10 |  100 |     99.5 |         1 |
| AAPL |    25 |   50 |    100.0 |         2 |
| MSFT |    30 |   75 |    201.0 |         1 |
```

The window can be given in time units, such as `[-5 0 :minutes]`, when the key column holds epoch milliseconds. See [window joins](https://github.com/clojure-finance/datajure#window-joins) in the README for the details.

## Cross-Sections

A *cross-section* is all entities at one point in time. Many finance methods compare stocks with each other within each month: standardizing a signal, or sorting stocks into size groups. In Datajure this is the same pattern with the date in `:by` instead of the entity. This small dataset has four stocks over two months:

```clojure
(require '[datajure.core :refer [cut]])

(def stocks
  (ds/->dataset
   {:month  ["2024-01" "2024-01" "2024-01" "2024-01" "2024-02" "2024-02" "2024-02" "2024-02"]
    :sym    ["A" "B" "C" "D" "A" "B" "C" "D"]
    :mktcap [5.0 40.0 12.0 90.0 6.0 38.0 15.0 95.0]
    :ret    [0.04 0.01 0.03 -0.01 -0.02 0.02 0.05 0.00]}))

(dt stocks :by [:month]
    :set {:size #dt/e (cut :mktcap 2)
          :z    #dt/e (stat/standardize :ret)})
```

```text
_unnamed [8 6]:

|  :month | :sym | :mktcap |  :ret | :size |          :z |
|---------|------|--------:|------:|------:|------------:|
| 2024-01 |    A |     5.0 |  0.04 |     1 |  1.01472214 |
| 2024-01 |    B |    40.0 |  0.01 |     2 | -0.33824071 |
| 2024-01 |    C |    12.0 |  0.03 |     1 |  0.56373452 |
| 2024-01 |    D |    90.0 | -0.01 |     2 | -1.24021595 |
| 2024-02 |    A |     6.0 | -0.02 |     1 | -1.08838387 |
| 2024-02 |    B |    38.0 |  0.02 |     2 |  0.25116551 |
| 2024-02 |    C |    15.0 |  0.05 |     1 |  1.25582754 |
| 2024-02 |    D |    95.0 |  0.00 |     2 | -0.41860918 |
```

`cut` splits each month's stocks into equal-count groups by market cap (1 = small, 2 = big), and `stat/standardize` turns each month's returns into z-scores. Other transforms such as `stat/winsorize` and `stat/demean` work the same way.

Put `cut` in `:by` to group by those bins directly. Here is the average return of small and big stocks in each month, the first step of a size-sorted portfolio:

```clojure
(dt stocks :by [:month (cut :mktcap 2)]
    :agg {:n nrow :avg-ret #dt/e (mn :ret)})
```

```text
_unnamed [4 4]:

|  :month | :mktcap-q2 | :n | :avg-ret |
|---------|-----------:|---:|---------:|
| 2024-01 |          1 |  2 |    0.035 |
| 2024-01 |          2 |  2 |    0.000 |
| 2024-02 |          1 |  2 |    0.015 |
| 2024-02 |          2 |  2 |    0.010 |
```

Because `:month` is also in `:by`, each month gets its own breakpoints. With `:from`, the breakpoints come from a subset of stocks and apply to all of them, as with NYSE breakpoints: `(cut :mktcap 5 :from #dt/e (= :exchcd 1))`. See [quantile binning](https://github.com/clojure-finance/datajure#quantile-binning-with-cut) in the README.

## Putting It Together

A per-stock summary in three steps: derive daily returns and drawdowns, collapse each stock to one row, and sort. `:within-order` also works with `:agg`, so `first-val` and `last-val` see each stock's first and last day:

```clojure
(-> prices
    (dt :by [:sym] :within-order [(asc :date)]
        :set [[:ret  #dt/e (- (win/ratio :price) 1)]
              [:peak #dt/e (win/scan max :price)]
              [:dd   #dt/e (- (/ :price :peak) 1)]])
    (dt :by [:sym] :within-order [(asc :date)]
        :agg {:total-ret #dt/e (- (/ (last-val :price) (first-val :price)) 1)
              :vol       #dt/e (sd :ret)
              :max-dd    #dt/e (mi :dd)})
    (dt :order-by [(desc :total-ret)]))
```

```text
_unnamed [2 4]:

| :sym | :total-ret |       :vol |     :max-dd |
|------|-----------:|-----------:|------------:|
| AAPL |      0.040 | 0.04032943 | -0.02941176 |
| MSFT |      0.035 | 0.01888828 | -0.01000000 |
```

The full list of window functions, including ranks, `win/cummax`, `win/each-prior` and `win/grr`, is in the [README](https://github.com/clojure-finance/datajure#window-functions).
