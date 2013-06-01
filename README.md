This is a fork of [clj-statsd](http://github.com/pyr/clj-statsd) that adds support for [Datadog's](http://datadoghq.com) tag extension to the statsd protocol.

clj-statsd is a client for the [statsd](https://github.com/etsy/statsd)
protocol for the [clojure](http://clojure.org) programming language.

An Example
----------

Here is a snippet showing the use of clj-statsd:

    (ns testing
        (:require [clj-statsd :as s]))

    (s/setup "127.0.0.1" 8125)

    ; simple increment
    (s/increment :some_counter)

    ; simple decrement
    (s/decrement "some_other_counter")

    ; double increment
    (s/increment :some_counter 2)

    ; sampled double increment
    (s/increment :some_counter 2 {:rate 0.1})

    ; record 300ms for "timing_value"
    (s/timing :timing_value 300)

    ; record an arbitrary value
    (s/gauge :current_value 42)

    ; send tags
    (s/gauge :current_value 42 {:tags [:tag:one "tag:two" :tagthree]})

Buckets can be strings or keywords. For more information please refer to
[statsd](https://github.com/etsy/statsd)

Installing
----------

I haven't made this fork available on any public repository because I don't want to confuse users of pyr/clj-statsd. If you'd like to use it yourself, then I recommend uploading it to your own repo or to clojars.
