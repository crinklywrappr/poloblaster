(ns poloblaster.core
  (:require [balonius.stream :as bs]
            [clojure.core.async :refer [<!! go]]
            [postal.core :as p])
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
  (let [c (bs/follow! conn pair)]
    [pair (<!! c)]))

(defn sendmail [[pair ticker]]
  (p/send-message
   {:host "smtp.example.com"
    :user "user"
    :pass "pass"
    :ssl true}
   {:from "sender@addr.com"
    :to "number@addr.net"
    :subject (str pair)
    :body (str (:rate ticker))}))

(defn send-rates-loop []
  (loop []
    (when (not @stop)
      (let [conn @(bs/connect!)
            tickers (into {} (map (partial get-ticker conn) pairs))]
        (doall (map sendmail tickers))
        (java.lang.Thread/sleep 900000)
        (recur)))))

(defn -main [& args]
  (send-rates-loop))
