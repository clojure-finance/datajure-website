# Datajure Website

The documentation website for [Datajure](https://github.com/clojure-finance/datajure), built with [Cryogen](https://cryogenweb.org/).

Live at https://clojure-finance.github.io/datajure-website/. Every push to `main` is built and deployed to the `gh-pages` branch by GitHub Actions.

## Requirements

- Java
- [Clojure CLI](https://clojure.org/guides/install_clojure)

## Development

Content lives in `content/md/` (`pages/` and `posts/`); site settings in `content/config.edn`.

```bash
clojure -X:serve       # serve with live reload at http://localhost:3000/datajure-website/
clojure -X:serve-fast  # same, but recompile only changed files
clojure -M:build       # build the site into public/
clojure -M:nrepl       # start an nREPL server (port written to .nrepl-port)
```
