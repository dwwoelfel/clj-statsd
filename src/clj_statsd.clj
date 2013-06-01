(ns clj-statsd
  "Send metrics to statsd."
  (:import [java.util Random])
  (:import [java.net DatagramPacket DatagramSocket InetAddress]))

(def
  ^{:doc "Atom holding the socket configuration"}
  cfg
  (atom nil))

(def
  ^{:doc "Agent holding the datagram socket"}
  sockagt
  (agent nil))

(defn setup
  "Initialize configuration"
  [host port & opts]
  (send sockagt #(or % (DatagramSocket.)))
  (swap! cfg #(or % (merge {:random (Random.)
                            :host   (InetAddress/getByName host)
                            :port   (if (integer? port) port (Integer/parseInt port))}
                           (apply hash-map opts)))))

(defn- send-packet
  ""
  [^DatagramSocket socket ^DatagramPacket packet]
  (try
    (doto socket (.send packet))
    (catch Exception e
      socket)))

(defn send-stat 
  "Send a raw metric over the network."
  [^String content]
  (when-let [packet (try
                      (DatagramPacket.
                       ^"[B" (.getBytes content)
                       ^Integer (count content)
                       ^InetAddress (:host @cfg)
                       ^Integer (:port @cfg))
                      (catch Exception e
                        nil))]
    (send sockagt send-packet packet)))

(defn publish
  "Send a metric over the network, based on the provided sampling rate.
  This should be a fully formatted statsd metric line."
  [^String content {:keys [rate tags]
                    :or {rate 1.0}}]
  (let [prefix (:prefix @cfg)
        content (if prefix (str prefix content) content)
        tag-content (if-not tags
                      ""
                      (format "|#%s" (clojure.string/join "," (map name tags))))]
    (cond
      (nil? @cfg) nil

      (<= (.nextDouble ^Random (:random @cfg)) rate)
      (send-stat (format "%s|@%f%s" content (float rate) tag-content)))))

(defn increment
  "Increment a counter at specified rate, defaults to a one increment
  with a 1.0 rate"
  ([k]        (increment k 1))
  ([k v]      (increment k v {:rate 1.0}))
  ([k v args] (publish (format "%s:%d|c" (name k) v) args)))

(defn timing
  "Time an event at specified rate, defaults to 1.0 rate"
  ([k v]      (timing k v {:rate 1.0}))
  ([k v args] (publish (format "%s:%d|ms" (name k) v) args)))

(defn decrement
  "Decrement a counter at specified rate, defaults to a one decrement
  with a 1.0 rate"
  ([k]        (increment k -1))
  ([k v]      (increment k (* -1 v) {:rate 1.0}))
  ([k v args] (increment k (* -1 v) args)))

(defn gauge
  "Send an arbitrary value."
  ([k v]      (gauge k v {:rate 1.0}))
  ([k v args] (publish (format "%s:%d|g" (name k) v) args)))

(defn unique
  "Send an event, unique occurences of which per flush interval
   will be counted by the statsd server."
  ([k v] (publish (format "%s:%d|s" (name k) v) {})))

(defmacro with-sampled-timing
  "Time the execution of the provided code, with sampling."
  [k rate & body]
  `(let [start# (System/currentTimeMillis)
         result# (do ~@body)]
    (timing ~k (- (System/currentTimeMillis) start#) {:rate ~rate})
    result#))

(defmacro with-timing
  "Time the execution of the provided code."
  [k & body]
  `(with-sampled-timing ~k 1.0 ~@body))
