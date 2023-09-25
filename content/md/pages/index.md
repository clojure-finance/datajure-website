{:title "Welcome to Datajure!"
 :layout :page
 :page-index 0
 :navbar? true
 :home? true}

Welcome to Datajure! This is an open-source domain-specific language for data processing developed at HKU Business School.

To get started, please read our [docs](pages-output/docs) and try our [examples](pages-output/examples).

To know more about our design and implementation ideas, please read our [posts](archives).

## About

Domain Specific Language (DSL) is a computer language, declared syntax or grammar that is specialised to a specific application. In contrast to General-Purpose Language, implemenation of DSL is designed with specific goals in that application domain. The use of macros in Lisp dialects enables developers to rewrite source code at compile time, making implementation of DSL more convenient. As one of the Lisp dialects, Clojure also inheriates such advantage. In addition to macros, the heavy use of core data literals in Clojure also gives an extensive developing opportunity in implementing DSLs.

In this project, a DSL extension to existing data processing libraries, including [`tech.ml.dataset`](https://github.com/techascent/tech.ml.dataset), [`tablecloth`](https://github.com/scicloj/tablecloth), [`clojask`](https://github.com/clojure-finance/clojask) and [`geni`](https://github.com/zero-one-group/geni), will be developed. A generic query using core data literal serves as the foundation of the DSL. This enables huge flexibility in defining the syntax, subject to Clojure’s limitation.

## Report Bugs 

Datajure is currently under active development.  
If you find any bugs or errors, we would appreciate if you could help [report](https://github.com/clojure-finance/datajure/issues) these issues so that we could repair them accordingly.
