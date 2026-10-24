{:title "Docs"
 :layout :page
 :toc :ul
 :page-index 2
 :navbar? true}

This page explains how Datajure queries work. The snippets use the `prices` dataset from the [quick start](/pages-output/quick-start/); the [examples](/pages-output/examples/) show their output. Every option is covered in the [README](https://github.com/clojure-finance/datajure#readme).

## The `dt` Function

Every query is a call to `dt` with a dataset and keyword arguments:

```clojure
(require '[datajure.core :refer [dt dt1 nrow asc desc]])

(dt prices
    :where #dt/e (> :price 100)
    :by [:sym]
    :agg {:n nrow :avg-price #dt/e (mn :price)})
```

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

`dt` is a function, not a macro, and always returns a dataset. `dt1` takes the same arguments and returns the single result row as a map, or throws if the result is not exactly one row:

```clojure
(dt1 prices :agg {:n nrow :avg-price #dt/e (mn :price)})
;; => {:n 8, :avg-price 151.5}
```

A query can also be one map, which makes it plain data that can be stored, merged or built by a program:

```clojure
(dt prices {:where [:> :price 100] :by [:sym] :agg {:n [:nrow]}})
```

### Evaluation Order

Whatever order the keywords are written in, `dt` evaluates them in this order:

1. `:where` filters rows.
2. `:set` or `:agg` derives or aggregates (`:by` and `:within-order` apply here).
3. `:select` keeps the listed columns.
4. `:order-by` sorts the result.
5. `:take` limits the number of rows.

`:set` and `:agg` cannot be used in the same call; thread several `dt` calls with `->` instead. An unknown keyword is an error with a suggestion (`:wehre -> :where`), not silently ignored.

### `:by` with `:set` or `:agg`

`:by` combines with `:set` and `:agg` to give four kinds of operation:

| | Without `:by` | With `:by` |
|---|---|---|
| `:set` | Derive columns over the whole dataset | Derive columns within each group (window functions run per group) |
| `:agg` | One summary row | One row per group |

```clojure
;; one row per symbol
(dt prices :by [:sym] :agg {:n nrow :avg-price #dt/e (mn :price)})

;; every row kept, return computed within each symbol in date order
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:ret #dt/e (- (win/ratio :price) 1)})
```

A `:set` query keeps the input row order. `:within-order` only sets the order in which window functions walk each group; use `:order-by` to sort the output.

### Columns Built in Order

`:set` takes a map or a sequence of pairs. In a map, every expression sees only the input columns, and referring to a sibling is an error. In a sequence of pairs, each expression sees the columns derived before it:

```clojure
(dt prices :set [[:value #dt/e (* :price :volume)]
                 [:big?  #dt/e (> :value 200000)]])
```

## Expressions

### `#dt/e` and Plain Functions

`#dt/e` is a reader tag that turns a form into an expression: bare keywords refer to columns, and the expression runs on whole columns at once. Plain Clojure functions also work and are called once per row with a row map:

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

Datajure handles missing values (`nil`) with a few rules rather than one blanket guarantee:

- A comparison with a missing value is false, so `:where` drops that row.
- Arithmetic on a missing value gives a missing value. Arithmetic with a literal `nil` is an error when the expression is read.
- Aggregations skip missing values, and return `nil` if all are missing.
- `coalesce` replaces missing values, `div0` gives `nil` instead of dividing by zero or `nil`, and `win/fills` carries the last value forward.
- Plain functions get no special handling; `pass-nil` wraps one so it returns `nil` when a given column is missing.

```clojure
(dt prices :set {:volume #dt/e (coalesce :volume 0)})
(dt prices :by [:sym] :within-order [(asc :date)]
    :set {:volume #dt/e (win/fills :volume)})
```

### Runtime Values and Data-Forms

`#dt/e` is applied when the code is read, so it cannot see a local variable. For values known only at run time, write the expression as a data-form: a vector headed by an operation keyword, with keywords as columns and anything else as a literal:

```clojure
(let [ticker "MSFT" lo 199]
  (dt prices :where [:and [:= :sym ticker] [:> :price lo]]))
```

Data-forms cover everything `#dt/e` does, including windows (`[:win/lag :price 1]`) and conditionals (`[:cond … :else …]`), and run on the same vectorized path. They make it easy to generate queries:

```clojure
(dt prices :by [:sym]
    :agg (into {} (for [c [:price :volume]]
                    [(keyword (str "max-" (name c))) [:mx c]])))
```

To use a keyword as a value rather than a column, wrap it in `lit`: `#dt/e (= :status (lit :active))`, or `[:lit status]` in a data-form.

### Reusable Expressions

An `#dt/e` expression is a value. Store it in a var and use it in several queries or inside other expressions:

```clojure
(def value #dt/e (* :price :volume))

(dt prices :set {:value value})
(dt prices :by [:sym] :agg {:avg-value #dt/e (mn value)})
```

## Selecting Columns

`:select` accepts several forms:

```clojure
(dt prices :select [:sym :price])               ;; these columns
(dt prices :select [:not :volume])              ;; all but these
(dt prices :select #"p.*")                      ;; names matching a regex
(dt prices :select :type/numerical)             ;; numeric columns (dates count as numeric)
(dt prices :select {:sym :ticker :price :close}) ;; select and rename
```

`rename` renames columns without dropping any: `(rename prices {:sym :ticker})`.

## Grouping

`:by` takes column keywords, and also:

- named computed keys, as single-entry maps: `:by [{:month #dt/e (month :date)}]`
- `xbar` buckets, such as `(xbar :time 5 :minutes)`, for time bars
- `cut` quantile bins, such as `(cut :mktcap 5)`, optionally with breakpoints computed from a subset (`:from`)
- a plain function of the row

```clojure
(require '[datajure.core :refer [xbar]])

(dt prices :by [{:dow #dt/e (dow :date)}] :agg {:avg-price #dt/e (mn :price)})
(dt prices :by [(xbar :price 50)] :agg {:n nrow})
```

For several passes over the same groups, `prepare-grouping` computes the grouping once and can be passed as `:by` in each pass.

## Joins

`datajure.join/join` covers regular joins (`:inner`, `:left`, `:right`, `:outer`), filtering joins (`:semi`, `:anti`), `:cross`, as-of joins (`:asof`) and window joins (`:window`). `:validate` checks key cardinality, and `:report true` prints how many rows matched:

```clojure
(require '[datajure.join :refer [join]])

(join prices sectors :on :sym :how :left :validate :m:1)
```

In an as-of join, the last key in `:on` is matched to the latest right row at or before the left row's value, and the other keys must match exactly. `:direction` (`:backward`, `:forward`, `:nearest`) and `:tolerance` control the match:

```clojure
(join trades quotes :on [:sym :time] :how :asof)
```

See [joins](https://github.com/clojure-finance/datajure#joins), [as-of joins](https://github.com/clojure-finance/datajure#as-of-joins) and [window joins](https://github.com/clojure-finance/datajure#window-joins) in the README for all options.

## Reshaping

`datajure.reshape` has `melt` (wide to long), `cast` (long to wide) and `tsfill`, which fills gaps in panel data so that each group has a row for every period:

```clojure
(require '[datajure.reshape :as reshape])

(reshape/cast prices {:id [:date] :from :sym :value :price})
(reshape/melt prices {:id [:date :sym] :measure [:price :volume]})
```

## Other Namespaces

| Namespace | Contents |
|---|---|
| `datajure.io` | `read`, `write` and `read-seq` for CSV, TSV, JSON, JSON Lines, Nippy, Parquet, Arrow and Excel |
| `datajure.util` | `describe`, `clean-column-names`, `distinct-rows`, `mark-duplicates`, `coerce-columns`, `blank->nil`, `parse-numeric` |
| `datajure.index` | `index-by` and `lookup` for repeated lookups by key |
| `datajure.concise` | Short aliases (`mn`, `sm`, `N`, …) for use outside `#dt/e` |
| `datajure.clay`, `datajure.clerk` | Notebook viewers |
| `datajure.nrepl` | nREPL middleware that binds `*dt*` to the last result |

## Errors

Errors are `ex-info` exceptions whose data has a `:dt/error` key for handling them in code. Many include a suggestion, such as the closest column names for a misspelled column (under `:closest`) or the intended keyword for a misspelled `dt` keyword:

```clojure
(dt prices :set {:value #dt/e (* :prcie :volume)})
;; Unknown column(s) #{:prcie} in :set :value expression
;; ex-data includes :dt/error :unknown-column, :closest {:prcie [:price]}
```

See the [examples](/pages-output/examples/#errors) for more.
