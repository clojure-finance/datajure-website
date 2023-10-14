{:title "Examples"
 :layout :page
 :toc :ul
 :page-index 1
 :navbar? true}

The following are several examples showing how to use Datajure to conveniently complete various data processing operations.

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

<div>
    <label for="backends">Choose a backend: </label>
    <select name="backends" id="backends">
        <option value="tech.ml.dataset">tech.ml.dataset</option>
        <option value="tablecloth" selected="selected">Tablecloth</option>
        <option value="clojask">Clojask</option>
        <option value="geni">Geni</option>
    </select>
</div>

<script type="text/javascript">
    document.getElementById('backends').onchange = function() {
        document.getElementById('tech.ml.dataset-examples').style.display = 'none';
        document.getElementById('tablecloth-examples').style.display = 'none';
        document.getElementById('clojask-examples').style.display = 'none';
        document.getElementById('geni-examples').style.display = 'none';
        document.getElementById(this.value + '-examples').style.display = 'block';
    };
</script>

<div id="tech.ml.dataset-examples" style="display: none;">tech.ml.dataset examples...</div>

<div id="tablecloth-examples" style="display: block;">

### Example 1

- Select rows with `salary` > 300, `age` < 20

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

- Group rows by `age` with sum of `salary` > 1000
- Show `age` and sum of `salary`

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

- Group rows by `age`
- Show `age`, sum of `salary` and standard deviation of `salary`
- Sort by standard deviation of `salary` in descending order

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

- Group rows by `age` and `name`
- Show `age`, `name` and sum of `salary`

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

- Select rows with `salary` > 0, `age` < 24

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

- Select rows with sum of `salary` > 0 (after grouping), `age` > 0
- Group rows by `name` and `age`, show `name`, `age`, `salary` (of the first record in the group), sum of `salary` and standard deviation of `salary`
- Sort by `salary`

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

</div>

<div id="clojask-examples" style="display: none;">Clojask examples...</div>

<div id="geni-examples" style="display: none;">Geni examples...</div>