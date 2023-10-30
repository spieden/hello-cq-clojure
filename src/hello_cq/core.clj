(ns hello-cq.core
  (:import (io.cloudquery.messages WriteMessage)
           (io.cloudquery.plugin BackendOptions NewClientOptions Plugin)
           (io.cloudquery.scheduler Scheduler)
           (io.cloudquery.schema ClientMeta Column ColumnResolver Table TableResolver)
           (io.cloudquery.server PluginServe)
           (io.grpc.stub StreamObserver)
           (java.util List)
           (org.apache.arrow.vector.types.pojo ArrowType$Utf8)
           (org.apache.logging.log4j LogManager)))

(def plugin-name "hello-cq")
(def plugin-version "v0.0.1")

(def rows-fixture
  [{:column "Hello CQ!"}
   {:column "Love, Clojure"}])

(def column-resolver
  (reify ColumnResolver
    (resolve [_ _ resource column]
      (let [col-name (.getName column)]
        (.set resource
              col-name
              (get (.getItem resource)
                   (keyword col-name)))))))

(def table-resolver
  (reify TableResolver
    (resolve [_ client parent stream]
      (doseq [row rows-fixture]
        (.write stream row)))))

(def table
  (-> (Table/builder)
      (.name "my_table")
      (.columns [(-> (Column/builder)
                     (.name "column")
                     (.type ArrowType$Utf8/INSTANCE)
                     (.resolver column-resolver)
                     (.build))])
      (.resolver table-resolver)
      (.build)))

(def client
  (reify ClientMeta
    (getId [_] plugin-name)
    (^void write [_ ^WriteMessage message]
      (throw (ex-info "Asked to write a message!?"
                      {:message message})))))

(defn do-sync [include-list
               skip-list
               skip-dependent-tables?
               deterministic-cq-id?
               backend-options
               sync-stream]
  (-> (Scheduler/builder)
      ; there's a pre-initialized one in a protected member of Plugin (could use gen-class to get)
      (.client client)
      ; normally filtering is applied here
      (.tables [table])
      (.syncStream sync-stream)
      (.deterministicCqId deterministic-cq-id?)
      ; also a pre-initialized one of these in a protected member
      (.logger (LogManager/getLogger))
      (.concurrency 1)
      (.build)
      (.sync)))

(def plugin
  (proxy [Plugin] [plugin-name plugin-version]
    (newClient ^ClientMeta [^String spec ^NewClientOptions options]
      client)
    (tables ^List [^List include-list ^List skip-list ^Boolean skip-dependent-tables?]
      [table])
    (sync ^void [^List include-list ^List skip-list ^Boolean skip-dependent-tables? ^Boolean deterministic-cq-id? ^BackendOptions backend-options ^StreamObserver sync-stream]
      (do-sync include-list skip-list skip-dependent-tables? deterministic-cq-id? backend-options sync-stream))
    (read []
      (throw (ex-info "Asked to read!?" {})))
    (write [^WriteMessage message]
      (throw (ex-info "Asked to write!?" {})))
    (close [])))

(defn -main [& args]
  (-> (PluginServe/builder)
      (.plugin plugin)
      (.args (into-array String args))
      (.build)
      (.Serve)
      (System/exit)))
