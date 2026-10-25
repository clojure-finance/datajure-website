# Datajure Website

The documentation website for [Datajure](https://github.com/clojure-finance/datajure), built with [Cryogen](https://cryogenweb.org/).

Live at https://clojure-finance.github.io/datajure-website/. Every push to `main` is built and deployed to the `gh-pages` branch by GitHub Actions.

## Requirements

- Java
- [Clojure CLI](https://clojure.org/guides/install_clojure)

## Development

```bash
clojure -X:serve       # serve with live reload at http://localhost:3000/datajure-website/
clojure -X:serve-fast  # same, but recompile only changed files
clojure -M:build       # build the site into public/
clojure -M:nrepl       # start an nREPL server (port written to .nrepl-port)
```

## Layout

- `content/md/pages/`: one Markdown file per page. `:page-index` sets the order in the top navigation, `:nav-title` an optional shorter name for it, and `:subtitle` the line under the page title.
  - `index.md`: the home page (layout `:home`)
  - `quick-start.md`, `docs.md`, `examples.md`
  - `time-series.md`: returns, rolling windows, gaps, bars, as-of and window joins
  - `coming-from.md`: dplyr, data.table, pandas and Polars side by side with Datajure
- `content/config.edn`: site settings.
- `themes/lotus/`: the theme.
  - `html/base.html`: page frame (top bar, footer, scripts)
  - `html/home.html`: home page hero and feature cards
  - `html/page.html`: other pages, with the table of contents beside the text
  - `css/site.css`: all styles, plain CSS with light and dark colors as variables at the top; there is no Sass step
  - `js/site.js`: highlighting, copy buttons, table of contents tracking, light/dark toggle

### Writing Pages

- A ` ```clojure ` block followed directly by a ` ```text ` block shows the code with its output; the theme labels the second one "Output".
- Code in other languages uses ` ```r `, ` ```python ` or ` ```bash `.
- A blockquote starting with `**Note:**` or `**Tip:**` becomes a callout; one starting with `**Warning:**` gets the warning style.
- Raw HTML is allowed. The diagrams (`div.grid4`, `ol.flow`) and the link cards (`div.cards`) are styled in `site.css`. Keep an HTML block free of blank lines, or Markdown will split it.
- Link to other pages as `/pages-output/<name>/`. Cryogen adds the `/datajure-website` prefix.

## Checking the Examples

The code on the pages runs against a small sample dataset, and the outputs shown are real. To check that every snippet still runs and prints what the page says:

```bash
clojure -M:check content/md/pages/*.md
```

It uses the Datajure version in the `:check` alias of `deps.edn`. For a new Datajure release:

1. Bump that version and the one in `content/md/pages/quick-start.md` (and the version mentioned in `examples.md`).
2. Run the check, and update any output that changed.
3. The hero example in `themes/lotus/html/home.html` is not checked. It is the `:where`/`:by`/`:agg` query on `prices` with `(> :volume 1000)`, so rerun it by hand and compare its result table.
