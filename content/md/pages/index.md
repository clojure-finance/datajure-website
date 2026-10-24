{:title "Welcome to Datajure!"
 :layout :page
 :page-index 0
 :navbar? true
 :home? true}

[![Clojars Project](https://img.shields.io/clojars/v/com.github.clojure-finance/datajure.svg)](https://clojars.org/com.github.clojure-finance/datajure)
[![CI](https://github.com/clojure-finance/datajure/actions/workflows/tests.yml/badge.svg)](https://github.com/clojure-finance/datajure/actions/workflows/tests.yml)

**One function. Seven keywords. Two expression modes.**

Datajure is a Clojure data manipulation library built on [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset), developed at HKU Business School. It provides one composable query function, `dt`, for filtering, transforming, grouping, and aggregating tabular data.

```clojure
(require '[datajure.core :refer [dt nrow asc desc]])

;; Filter, group, aggregate: one call
(dt prices
    :where #dt/e (> :price 100)
    :by [:sym]
    :agg {:n nrow :avg-price #dt/e (mn :price)})

;; Window functions: same keywords
(dt prices
    :by [:sym]
    :within-order [(asc :date)]
    :set {:ret #dt/e (- (win/ratio :price) 1)})

;; Thread for multi-step pipelines
(-> prices
    (dt :set {:value #dt/e (* :price :volume)})
    (dt :by [:sym] :agg {:total #dt/e (sm :value)})
    (dt :order-by [(desc :total)]))
```

Datajure is a syntax layer, not an engine. It compiles `#dt/e` expressions to vectorized operations and leaves all computation to tech.ml.dataset, so every result is a standard dataset that works with tablecloth, Clerk, Clay, and the rest of the Scicloj ecosystem.

To get started, go through the [quick start](/pages-output/quick-start/), read the [docs](/pages-output/docs/) and try the [examples](/pages-output/examples/). The complete reference is the [README](https://github.com/clojure-finance/datajure#readme).

## Why Datajure

Datajure borrows from whichever library got a given idea right:

- **R's data.table**: `DT[i, j, by]` maps onto `(dt ds :where i :set-or-agg j :by by)`, without in-place mutation.
- **Python's Polars**: expressions are first-class values, and comparisons with missing values are safe.
- **q/kdb+**: time-series primitives such as `win/ratio`, `win/mavg`, `win/ema`, `win/fills` and `xbar`, plus as-of and window joins.
- **Julia's DataFramesMeta.jl**: `#dt/e` plays the role of `@transform` and `@subset`, using Clojure's reader tags.

## Datajure v1

Datajure v2 (March 2026) is a rewrite. Datajure v1 (1.x) was a query syntax that routed to several backends: tech.ml.dataset, tablecloth, Clojask and Geni. Its code is on the [`v1` branch](https://github.com/clojure-finance/datajure/tree/v1), its documentation is in [this site's history](https://github.com/clojure-finance/datajure-website/tree/559361a/content/md/pages), and the [posts](/archives/) describe its design. Thanks to [YANG Ming-Tian](https://github.com/skylee03) for the original v1 implementation.

## Report Bugs

If you find a bug, please [open an issue](https://github.com/clojure-finance/datajure/issues).

Datajure is distributed under the [Apache License, Version 2.0](https://github.com/clojure-finance/datajure/blob/main/LICENSE).
