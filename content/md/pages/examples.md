{:title "Examples"
 :layout :page
 :toc :ul
 :page-index 1
 :navbar? true}

The following are several examples showing how to use Dature to conveniently complete various data processing operations.

---  

## Backend Specification

The following code sets `tablecloth` as the backend using the function `set-backend`:

```clojure
(set-backend "tablecloth")
```

## Datasets Construction

The following code construct a dataset object `data` of the specified backend from an associative map `data-map` using the function `dataset`:

```clojure
(def data-map {:age [31 25 18 18 25]
               :name ["a" "b" "c" "c" "d"]
               :salary [200 500 200 370 3500]})
(def data (dataset data-map))
```

## Datasets Printing

The following code prints the content of the dataset `data` using the function `print-dataset`:

```clojure
(print-dataset data)
```

Sample output:

<pre>
| :age | :name | :salary |
|-----:|-------|--------:|
|   31 |     a |     200 |
|   25 |     b |     500 |
|   18 |     c |     200 |
|   18 |     c |     370 |
|   25 |     d |    3500 |
</pre>

## Operations

### Example 1

```clojure
(print-dataset (dt-get data [[:salary #(< 300 %)] [:age #(> 20 %)]] []))
```

Sample output:

<pre>
| :age | :name | :salary |
|-----:|-------|--------:|
|   18 |     c |     370 |
</pre>

### Example 2

```clojure
(print-dataset (dt-get data [[:sum :salary #(< 1000 %)]] [:age :sum :salary] [:group-by :age]))
```

Sample output:

<pre>
| :age | :salary-sum |
|-----:|------------:|
|   25 |      4000.0 |
</pre>

### Example 3

```clojure
(print-dataset (dt-get data [] [:age :sum :salary :sd :salary] [:group-by :age :sort-by :sd :salary >]))
```

Sample output:

<pre>
| :age | :salary-sum |    :salary-sd |
|-----:|------------:|--------------:|
|   25 |      4000.0 | 2121.32034356 |
|   18 |       570.0 |  120.20815280 |
|   31 |       200.0 |               |
</pre>

### Example 4

```clojure
(print-dataset (dt-get data [] [:age :name :sum :salary] [:group-by :age :name]))
```

Sample output:

<pre>
| :age | :name | :salary-sum |
|-----:|-------|------------:|
|   31 |     a |       200.0 |
|   25 |     b |       500.0 |
|   18 |     c |       570.0 |
|   25 |     d |      3500.0 |
</pre>

### Example 5

```clojure
(print-dataset (dt-get data [[:salary #(< 0 %)] [:age #(< 24 %)]] []))
```

Sample output:

<pre>
| :age | :name | :salary |
|-----:|-------|--------:|
|   31 |     a |     200 |
|   25 |     b |     500 |
|   25 |     d |    3500 |
</pre>

### Example 6

```clojure
(print-dataset (dt-get data [[:sum :salary #(< 0 %)] [:age #(< 0 %)]] [:name :age :salary :sum :salary :sd :salary] [:group-by :name :age :sort-by :salary])))
```

Sample output:

<pre>
| :name | :age | :salary | :salary-sum |  :salary-sd |
|-------|-----:|--------:|------------:|------------:|
|     a |   31 |     200 |       200.0 |             |
|     c |   18 |     200 |       570.0 | 120.2081528 |
|     b |   25 |     500 |       500.0 |             |
|     d |   25 |    3500 |      3500.0 |             |
</pre>