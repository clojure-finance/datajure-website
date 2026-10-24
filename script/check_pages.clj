;; Checks the Clojure snippets on the site against the Datajure version in the
;; :check alias of deps.edn:
;;
;;   clojure -M:check content/md/pages/*.md
;;
;; Each page's ```clojure blocks run in order, after a prelude that defines the
;; sample datasets the pages use. A block followed directly by a ```text block
;; must print exactly that output: a dataset as tech.ml.dataset prints it, an
;; exception as its message (optionally followed by its ex-data), anything else
;; with pr-str. A block without output must not throw, unless a comment in it
;; quotes the error message. Blocks that need files or optional dependencies
;; are skipped.
(require '[clojure.string :as str]
         '[tech.v3.dataset :as ds])

(def prelude
  "(require '[datajure.core :refer [dt dt1 nrow asc desc rename]]
            '[datajure.join :refer [join]]
            '[datajure.reshape :as reshape]
            '[tech.v3.dataset :as ds])
   (def prices
     (ds/->dataset
      {:date   (map #(java.time.LocalDate/parse %)
                    [\"2024-01-02\" \"2024-01-03\" \"2024-01-04\" \"2024-01-05\"
                     \"2024-01-02\" \"2024-01-03\" \"2024-01-04\" \"2024-01-05\"])
       :sym    [\"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"MSFT\" \"MSFT\" \"MSFT\" \"MSFT\"]
       :price  [100.0 102.0 99.0 104.0 200.0 198.0 202.0 207.0]
       :volume [1200 1500 nil 1800 900 1100 1000 1300]}))
   (def sectors (ds/->dataset {:sym [\"AAPL\" \"MSFT\"] :sector [\"Hardware\" \"Software\"]}))
   (def trades (ds/->dataset {:sym [\"AAPL\" \"AAPL\" \"MSFT\"] :time [10 25 30] :qty [100 50 75]}))
   (def quotes (ds/->dataset {:sym [\"AAPL\" \"AAPL\" \"AAPL\" \"MSFT\"] :time [5 20 30 28]
                              :bid [99.5 100.5 101.0 201.0]}))")

(defn blocks [md]
  (vec (for [[_ lang body] (re-seq #"(?ms)^```(\w*)\n(.*?)^```$" md)]
         {:lang lang :body (str/trimr body)})))

(defn skip? [body]
  (or (str/starts-with? body "{:deps")         ; deps.edn snippet
      (str/starts-with? body "[com.github")    ; Leiningen coordinate
      (str/starts-with? body "{:middleware")   ; .nrepl.edn
      (str/includes? body "dio/")              ; needs data files
      (str/includes? body "datajure.clay")))   ; needs Clay

(defn norm [s]
  (->> (str/split-lines (str/trim s)) (map str/trimr) (str/join "\n")))

(defn eval-and-render
  "Evaluates code (discarding anything it prints) and renders its value."
  [code]
  (let [v (binding [*out* (java.io.StringWriter.)] (load-string code))
        v (if (var? v) @v v)]
    (if (ds/dataset? v) (str v) (pr-str v))))

(defn run-block
  "Evaluates code, returning {:out rendered-value} or {:error msg :data ex-data}."
  [code]
  (try
    {:out (eval-and-render code)}
    (catch Throwable t
      (let [c (or (ex-cause t) t)]
        {:error (ex-message c) :data (ex-data c)}))))

(defn check-page [file]
  (let [bs (blocks (slurp file))
        failures (atom 0)
        fail! (fn [& msg] (swap! failures inc) (apply println "FAIL" file "\n" msg))]
    (binding [*ns* (create-ns (gensym "page"))]
      (refer-clojure)
      (load-string prelude)
      (doseq [[i {:keys [lang body]}] (map-indexed vector bs)
              :when (and (= lang "clojure") (not (skip? body)))]
        (let [next-block (get bs (inc i))
              expected (when (= "text" (:lang next-block)) (norm (:body next-block)))
              {:keys [out error data]} (run-block body)
              actual (if error (str error "\n" (pr-str data)) out)]
          (cond
            (and error (nil? expected))
            (when-not (every? #(str/includes? body %) (str/split-lines error))
              (fail! body "\nthrew:" error))

            (nil? expected) nil

            (or (= expected (norm actual))
                (and error (= expected (norm error))))
            nil

            :else
            (fail! body "\n--- expected\n" expected "\n--- actual\n" actual)))))
    (println file ":" @failures "failure(s)")
    @failures))

(let [n (reduce + (map check-page *command-line-args*))]
  (shutdown-agents)
  (System/exit (if (zero? n) 0 1)))
