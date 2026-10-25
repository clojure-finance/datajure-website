{:title "Examples"
 :subtitle "Common tasks on one small dataset of daily prices, each with its output."
 :layout :page
 :toc :ul
 :page-index 3
 :navbar? true}

These examples walk through common operations on a small dataset of daily prices. Every output below comes from running the code with Datajure 2.7.12. The [docs](/pages-output/docs/) explain the concepts.

## Sample Data

```clojure
(require '[datajure.core :refer [dt dt1 nrow asc desc]]
         '[tech.v3.dataset :as ds])

(def prices
  (ds/->dataset
   {:date   (map #(java.time.LocalDate/parse %)
                 ["2024-01-02" "2024-01-03" "2024-01-04" "2024-01-05"
                  "2024-01-02" "2024-01-03" "2024-01-04" "2024-01-05"])
    :sym    ["AAPL" "AAPL" "AAPL" "AAPL" "MSFT" "MSFT" "MSFT" "MSFT"]
    :price  [100.0 102.0 99.0 104.0 200.0 198.0 202.0 207.0]
    :volume [1200 1500 nil 1800 900 1100 1000 1300]}))
```

```text
_unnamed [8 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |
| 2024-01-03 | AAPL |  102.0 |    1500 |
| 2024-01-04 | AAPL |   99.0 |         |
| 2024-01-05 | AAPL |  104.0 |    1800 |
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-03 | MSFT |  198.0 |    1100 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

The AAPL volume on 2024-01-04 is missing.

## Filtering Rows

Rows with a price above 150:

```clojure
(dt prices :where #dt/e (> :price 150))
```

```text
_unnamed [4 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-03 | MSFT |  198.0 |    1100 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

Conditions combine with `and`, `or` and `not`:

```clojure
(dt prices :where #dt/e (and (= :sym "AAPL") (> :price 100)))
```

```text
_unnamed [2 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-03 | AAPL |  102.0 |    1500 |
| 2024-01-05 | AAPL |  104.0 |    1800 |
```

A comparison with a missing value is false, so the row with no volume is dropped rather than causing an error:

```clojure
(dt prices :where #dt/e (> :volume 1200))
```

```text
_unnamed [3 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-03 | AAPL |  102.0 |    1500 |
| 2024-01-05 | AAPL |  104.0 |    1800 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

## Adding Columns

`:set` adds columns and keeps every row. Arithmetic on a missing value gives a missing value:

```clojure
(dt prices :set {:value #dt/e (* :price :volume)})
```

```text
_unnamed [8 5]:

|      :date | :sym | :price | :volume |   :value |
|------------|------|-------:|--------:|---------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 120000.0 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 153000.0 |
| 2024-01-04 | AAPL |   99.0 |         |          |
| 2024-01-05 | AAPL |  104.0 |    1800 | 187200.0 |
| 2024-01-02 | MSFT |  200.0 |     900 | 180000.0 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 217800.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 202000.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 269100.0 |
```

With a sequence of pairs, later columns can use earlier ones:

```clojure
(dt prices :set [[:value #dt/e (* :price :volume)]
                 [:big?  #dt/e (> :value 200000)]])
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume |   :value | :big? |
|------------|------|-------:|--------:|---------:|-------|
| 2024-01-02 | AAPL |  100.0 |    1200 | 120000.0 | false |
| 2024-01-03 | AAPL |  102.0 |    1500 | 153000.0 | false |
| 2024-01-04 | AAPL |   99.0 |         |          | false |
| 2024-01-05 | AAPL |  104.0 |    1800 | 187200.0 | false |
| 2024-01-02 | MSFT |  200.0 |     900 | 180000.0 | false |
| 2024-01-03 | MSFT |  198.0 |    1100 | 217800.0 |  true |
| 2024-01-04 | MSFT |  202.0 |    1000 | 202000.0 |  true |
| 2024-01-05 | MSFT |  207.0 |    1300 | 269100.0 |  true |
```

Replace missing values with `coalesce`:

```clojure
(dt prices :set {:volume #dt/e (coalesce :volume 0)})
```

```text
_unnamed [8 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |
| 2024-01-03 | AAPL |  102.0 |    1500 |
| 2024-01-04 | AAPL |   99.0 |       0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-03 | MSFT |  198.0 |    1100 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

Use `cond` for several cases:

```clojure
(dt prices :set {:size #dt/e (cond (> :price 200) "large"
                                   (> :price 100) "medium"
                                   :else          "small")})
```

```text
_unnamed [8 5]:

|      :date | :sym | :price | :volume |  :size |
|------------|------|-------:|--------:|--------|
| 2024-01-02 | AAPL |  100.0 |    1200 |  small |
| 2024-01-03 | AAPL |  102.0 |    1500 | medium |
| 2024-01-04 | AAPL |   99.0 |         |  small |
| 2024-01-05 | AAPL |  104.0 |    1800 | medium |
| 2024-01-02 | MSFT |  200.0 |     900 | medium |
| 2024-01-03 | MSFT |  198.0 |    1100 | medium |
| 2024-01-04 | MSFT |  202.0 |    1000 |  large |
| 2024-01-05 | MSFT |  207.0 |    1300 |  large |
```

## Grouping and Aggregating

One row per symbol. The sum skips the missing volume:

```clojure
(dt prices :by [:sym]
    :agg {:n         nrow
          :avg-price #dt/e (mn :price)
          :total-vol #dt/e (sm :volume)})
```

```text
_unnamed [2 4]:

| :sym | :n | :avg-price | :total-vol |
|------|---:|-----------:|-----------:|
| AAPL |  4 |     101.25 |       4500 |
| MSFT |  4 |     201.75 |       4300 |
```

Without `:by`, `:agg` summarizes the whole dataset. `dt1` returns that single row as a map:

```clojure
(dt1 prices :agg {:n nrow :avg-price #dt/e (mn :price)})
```

```text
{:n 8, :avg-price 151.5}
```

Aggregations can be generated with data-forms, here the maximum of each listed column:

```clojure
(dt prices :by [:sym]
    :agg (into {} (for [c [:price :volume]]
                    [(keyword (str "max-" (name c))) [:mx c]])))
```

```text
_unnamed [2 3]:

| :sym | :max-price | :max-volume |
|------|-----------:|------------:|
| AAPL |      104.0 |        1800 |
| MSFT |      207.0 |        1300 |
```

## Sorting, Limiting and Selecting

The three highest prices:

```clojure
(dt prices :order-by [(desc :price)] :take 3)
```

```text
_unnamed [3 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-05 | MSFT |  207.0 |    1300 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-02 | MSFT |  200.0 |     900 |
```

Select and rename columns with a map:

```clojure
(dt prices :select {:sym :ticker :price :close} :take 3)
```

```text
_unnamed [3 2]:

| :ticker | :close |
|---------|-------:|
|    AAPL |  100.0 |
|    AAPL |  102.0 |
|    AAPL |   99.0 |
```

## Window Functions

With `:by` and `:within-order`, window functions run within each symbol in date order. Here the daily return, the previous price and the cumulative volume:

```clojure
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:ret     #dt/e (- (win/ratio :price) 1)
          :prev    #dt/e (win/lag :price 1)
          :cum-vol #dt/e (win/cumsum :volume)})
```

```text
_unnamed [8 7]:

|      :date | :sym | :price | :volume |        :ret | :prev | :cum-vol |
|------------|------|-------:|--------:|------------:|------:|---------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |             |       |   1200.0 |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.02000000 | 100.0 |   2700.0 |
| 2024-01-04 | AAPL |   99.0 |         | -0.02941176 | 102.0 |   2700.0 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  0.05050505 |  99.0 |   4500.0 |
| 2024-01-02 | MSFT |  200.0 |     900 |             |       |    900.0 |
| 2024-01-03 | MSFT |  198.0 |    1100 | -0.01000000 | 200.0 |   2000.0 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.02020202 | 198.0 |   3000.0 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  0.02475248 | 202.0 |   4300.0 |
```

A two-day moving average and the rank of each price within its symbol. Moving windows are shorter at the start of each group:

```clojure
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:ma-2 #dt/e (win/mavg :price 2)
          :rank #dt/e (win/rank :price)})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume | :ma-2 | :rank |
|------------|------|-------:|--------:|------:|------:|
| 2024-01-02 | AAPL |  100.0 |    1200 | 100.0 |     1 |
| 2024-01-03 | AAPL |  102.0 |    1500 | 101.0 |     2 |
| 2024-01-04 | AAPL |   99.0 |         | 100.5 |     3 |
| 2024-01-05 | AAPL |  104.0 |    1800 | 101.5 |     4 |
| 2024-01-02 | MSFT |  200.0 |     900 | 200.0 |     1 |
| 2024-01-03 | MSFT |  198.0 |    1100 | 199.0 |     2 |
| 2024-01-04 | MSFT |  202.0 |    1000 | 200.0 |     3 |
| 2024-01-05 | MSFT |  207.0 |    1300 | 204.5 |     4 |
```

Carry the last known volume forward:

```clojure
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:volume #dt/e (win/fills :volume)})
```

```text
_unnamed [8 4]:

|      :date | :sym | :price | :volume |
|------------|------|-------:|--------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |
| 2024-01-03 | AAPL |  102.0 |    1500 |
| 2024-01-04 | AAPL |   99.0 |    1500 |
| 2024-01-05 | AAPL |  104.0 |    1800 |
| 2024-01-02 | MSFT |  200.0 |     900 |
| 2024-01-03 | MSFT |  198.0 |    1100 |
| 2024-01-04 | MSFT |  202.0 |    1000 |
| 2024-01-05 | MSFT |  207.0 |    1300 |
```

## Multi-Step Pipelines

Thread `dt` calls with `->`: compute daily returns, summarize them per symbol, then sort:

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

## Reusable Expressions

Store expressions in vars and build on them. Here `log-ret` uses `ret`, and both are computed within each symbol:

```clojure
(def ret     #dt/e (- (win/ratio :price) 1))
(def log-ret #dt/e (log (+ 1 ret)))

(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:ret ret :log-ret log-ret})
```

```text
_unnamed [8 6]:

|      :date | :sym | :price | :volume |        :ret |    :log-ret |
|------------|------|-------:|--------:|------------:|------------:|
| 2024-01-02 | AAPL |  100.0 |    1200 |             |             |
| 2024-01-03 | AAPL |  102.0 |    1500 |  0.02000000 |  0.01980263 |
| 2024-01-04 | AAPL |   99.0 |         | -0.02941176 | -0.02985296 |
| 2024-01-05 | AAPL |  104.0 |    1800 |  0.05050505 |  0.04927105 |
| 2024-01-02 | MSFT |  200.0 |     900 |             |             |
| 2024-01-03 | MSFT |  198.0 |    1100 | -0.01000000 | -0.01005034 |
| 2024-01-04 | MSFT |  202.0 |    1000 |  0.02020202 |  0.02000067 |
| 2024-01-05 | MSFT |  207.0 |    1300 |  0.02475248 |  0.02445110 |
```

## Runtime Values

`#dt/e` cannot see local variables. Data-forms can:

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

A whole query can be a single map:

```clojure
(dt prices {:where [:> :price 100] :by [:sym] :agg {:n [:nrow]}})
```

```text
_unnamed [2 2]:

| :sym | :n |
|------|---:|
| AAPL |  2 |
| MSFT |  4 |
```

## Joins

Add each symbol's sector, then average prices by sector. `:validate :m:1` checks that each symbol appears only once in `sectors`:

```clojure
(require '[datajure.join :refer [join]])

(def sectors
  (ds/->dataset {:sym    ["AAPL" "MSFT"]
                 :sector ["Hardware" "Software"]}))

(-> (join prices sectors :on :sym :how :left :validate :m:1)
    (dt :by [:sector] :agg {:avg-price #dt/e (mn :price)}))
```

```text
_unnamed [2 2]:

|  :sector | :avg-price |
|----------|-----------:|
| Hardware |     101.25 |
| Software |     201.75 |
```

An as-of join gives each trade the latest quote for the same symbol at or before the trade's time:

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

## Reshaping

`cast` turns long data into wide, one column per symbol:

```clojure
(require '[datajure.reshape :as reshape])

(reshape/cast prices {:id [:date] :from :sym :value :price})
```

```text
_unnamed [4 3]:

|      :date | :AAPL | :MSFT |
|------------|------:|------:|
| 2024-01-02 | 100.0 | 200.0 |
| 2024-01-03 | 102.0 | 198.0 |
| 2024-01-04 |  99.0 | 202.0 |
| 2024-01-05 | 104.0 | 207.0 |
```

## Errors

Mistakes are reported as `ex-info` exceptions with a `:dt/error` key and, where possible, a suggestion. A misspelled column:

```clojure
(dt prices :set {:value #dt/e (* :prcie :volume)})
```

```text
Unknown column(s) #{:prcie} in :set :value expression
Did you mean: :price (edit distance 1)
Available: :date :price :sym :volume
```

A misspelled keyword:

```clojure
(dt prices :wehre #dt/e (> :price 100))
```

```text
Unknown dt query key(s) [:wehre]. Did you mean: :wehre -> :where? Supported: [:agg :by :off-heap :order-by :select :set :take :where :within-order].
```

A map-form `:set` whose columns refer to each other:

```clojure
(dt prices :set {:value #dt/e (* :price :volume)
                 :big?  #dt/e (> :value 200000)})
```

```text
In map-form :set, column :big? references #{:value}, which are being derived in the same map. Map semantics are simultaneous — use vector-of-pairs [[:col1 expr1] [:col2 expr2]] for sequential derivation.
```
