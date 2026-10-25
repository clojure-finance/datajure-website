{:title "Quick Start"
 :layout :page
 :toc :ul
 :page-index 1
 :navbar? true}

## Installation

Datajure requires Java 21+ and Clojure 1.12+.

With the Clojure CLI, add Datajure to `deps.edn`:

```clojure
{:deps {com.github.clojure-finance/datajure {:mvn/version "2.7.10"}}}
```

With Leiningen, add it to `:dependencies` in `project.clj`:

```clojure
[com.github.clojure-finance/datajure "2.7.10"]
```

The [Clojars page](https://clojars.org/com.github.clojure-finance/datajure) always shows the latest version, and the [changelog](https://github.com/clojure-finance/datajure/blob/main/CHANGELOG.md) lists what changed in each release.

To try Datajure without creating a project, start a REPL with it on the classpath:

```bash
clj -Sdeps '{:deps {com.github.clojure-finance/datajure {:mvn/version "2.7.10"}}}'
```

## A First Query

Datajure works on [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets, which it brings in as a dependency. Create one from a map of columns:

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

Then query it with `dt`. Average price and total volume per symbol:

```clojure
(dt prices
    :by [:sym]
    :agg {:n         nrow
          :avg-price #dt/e (mn :price)
          :total-vol #dt/e (sm :volume)})
```

```text
_unnamed [2 4]:

| :sym | :n | :avg-price | :total-vol |
|------|---:|-----------:|-----------:|
| AAPL |  4 |     101.25 |     4500.0 |
| MSFT |  4 |     201.75 |     4300.0 |
```

`#dt/e` is a reader tag: inside it, bare keywords such as `:price` refer to columns, and `mn` and `sm` are the mean and sum. The missing volume is skipped. The result is an ordinary dataset, so it can be passed to the next `dt` call, to tech.ml.dataset, or to tablecloth.

The first time a query does something worth knowing about, such as aggregating over the whole dataset, Datajure prints a one-time `[datajure] NOTE:` line explaining it.

## Reading Files

`datajure.io` reads and writes files based on their extension:

```clojure
(require '[datajure.io :as dio])

(def ds (dio/read "data.csv"))    ;; also .tsv, .json, .jsonl, .parquet, .arrow, .xlsx, gzipped variants
(dio/write ds "output.csv")
```

Column names are read as keywords. Parquet, Arrow and Excel need the optional tech.ml.dataset libraries for those formats; see [File I/O](https://github.com/clojure-finance/datajure#file-io) in the README.

## REPL and Notebooks

With nREPL, the middleware `datajure.nrepl/wrap-dt` binds `datajure.core/*dt*` to the last dataset a form returned, similar to `*1`. Enable it in `.nrepl.edn`:

```clojure
{:middleware [datajure.nrepl/wrap-dt]}
```

For notebooks, `datajure.clay` and `datajure.clerk` render datasets, `#dt/e` expressions and `describe` output. Call `install!` from the one you use (with Clay or Clerk on the classpath):

```clojure
(require '[datajure.clay :as dc])
(dc/install!)
```

## Next Steps

- The [docs](/pages-output/docs/) explain the `dt` keywords, expressions, and the other namespaces.
- The [examples](/pages-output/examples/) walk through common operations on the dataset above, with output.
- The [README](https://github.com/clojure-finance/datajure#readme) is the complete reference, and [cljdoc](https://cljdoc.org/d/com.github.clojure-finance/datajure/CURRENT) has the API docs.
