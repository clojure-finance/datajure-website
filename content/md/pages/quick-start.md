{:title "Quick Start"
 :layout :page
 :toc :ul
 :page-index 1
 :navbar? true}

Currently, users can use Datajure in two ways: [as a library](#using-datajure-as-a-library), or [via the REPL](#using-datajure-via-the-repl).

## Using Datajure as a Library

To use Datajure as a library in your Clojure project, you should include Datajure and all other required dependencies based on the build tool you're using.

For instance, if you're using Leiningen, add the following code snippet to the `project.clj` file:

```
  :dependencies [[com.github.clojure-finance/datajure "1.1.0"]
                 [org.apache.logging.log4j/log4j-core "2.21.0"]
                 [org.clojure/clojure "1.11.1"]]
```

If you are using Java 17 or newer versions, you should also specify the following JVM options in the `project.clj` file:

```clojure
  :jvm-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED"
             "--add-opens=java.base/java.net=ALL-UNNAMED"
             "--add-opens=java.base/java.lang=ALL-UNNAMED"
             "--add-opens=java.base/java.util=ALL-UNNAMED"
             "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
             "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]
```

Then, use the following code snippet in your source code to access the Datajure APIs:

```clojure
(require '[datajure.dsl :as dtj])
```

For detailed information, refer to our [documentation](pages-output/docs) and try our [examples](pages-output/examples).

## Using Datajure via the REPL

To use Datajure as a standalone REPL tool, you need to download the launcher based on the operating system you are using. The following table describes the launchers based on the operating system:

| OS | Launcher |
|:-:|:-:|
| <i class="fa-brands fa-linux"></i> Linux / <i class="fa-brands fa-apple"></i> macOS | [`datajure`](https://raw.githubusercontent.com/clojure-finance/datajure/main/scripts/datajure) |
| <i class="fa-brands fa-windows"></i> Windows | [`datajure.ps1`](https://raw.githubusercontent.com/clojure-finance/datajure/main/scripts/datajure.ps1) |

The launcher downloads the latest release of Datajure REPL if it is not found and runs the Datajure REPL.

For Windows users, the launcher automatically checks and downloads the [Windows Utilities for Hadoop](https://github.com/cdarlint/winutils) to ensure that it runs smoothly.

Optionally, Linux users can install the Datajure launcher to `/usr/local/bin` by running the following commands:

```bash
wget https://raw.githubusercontent.com/clojure-finance/datajure/main/scripts/datajure
chmod a+x datajure
sudo mv datajure /usr/local/bin/
```

Usage:

```
datajure [<path>] [--force-download] [--proxy <https-proxy>] [--help]
```

`<path>` is an optional input file that should be a Datajure source file (`.dtj` suffix).

For detailed information, refer to our [documentation](pages-output/docs) and try our [examples](pages-output/examples).