(ns _ (:import [java.net Socket InetSocketAddress]
               [java.nio ByteBuffer])
    (:require ["../nrepl/nrepl" :as nrepl]
              ["../interpreter/interpreter" :as i]))

(defn ^void main [^"String[]" args]
  (checked!
   (let [close_server (nrepl/main (fn [e l] (i/eval e l)) 8090 (atom (i/make_env {})))
         socket (Socket.)]
     (.connect socket (InetSocketAddress. "localhost" 8090) 1000)
     (let [out (.getOutputStream socket)
           in (.getInputStream socket)
           data (.getBytes "(\n+\n2\n2\n)")
           len_buf (ByteBuffer/allocate 4)]
       (.putInt len_buf (.-length data))
       (.write out (.array len_buf))
       (.write out data)
       (let [actual (String. (.readAllBytes in))]
         (close_server)
         (if (not= "41" actual)
           (FIXME actual)))))))
