(ns poloblaster.core
  (:require [balonius.stream :as bs]
            [clojure.core.async :refer [<!! go]]
            [postal.core :as p]
            [config.core :refer [env]])
  (:gen-class))

(def stop (atom false))

(def pairs [[:USDT :ETH]
            [:USDT :XMR]
            [:USDT :XRP]
            [:BTC :XRP]
            [:BTC :BURST]])

(.addShutdownHook
 (Runtime/getRuntime)
 (Thread.
  (fn []
    (reset! stop true))))

(defn get-ticker [conn pair]
  (try
    (let [c (bs/follow! conn pair)]
      [pair (<!! c)])
    (catch Exception e
      (println (.getMessage e)))))

(defn sendmail [[pair ticker]]
  (try
    (p/send-message
     {:host (:host env)
      :user (:user env)
      :pass (:pass env)
      :ssl (:ssl env)}
     {:from (:from env)
      :to (:to env)
      :subject (str pair)
      :body (str (:rate ticker))})
    (catch Exception ex
      (println (.getMessage ex)))))

(defn send-rates-loop []
  (loop []
    (when (not @stop)
      (let [conn @(bs/connect!)
            tickers (into {} (remove nil? (map (partial get-ticker conn) pairs)))]
        (doall (map sendmail tickers))
        (java.lang.Thread/sleep (:interval env))
        (recur)))))

(defn -main [& args]
  (send-rates-loop))
