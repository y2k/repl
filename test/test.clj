(ns _ (:import [java.net Socket InetSocketAddress]
               [java.nio ByteBuffer])
    (:require ["../nrepl/nrepl" :as nrepl]
              ["../interpreter/interpreter" :as i]))

(defn- assert_ [expected ^String code]
  (checked!
   (let [close_server (nrepl/main (fn [e l] (i/eval e l)) 8090 (atom (i/make_env {})))
         socket (Socket.)]
     (.connect socket (InetSocketAddress. "localhost" 8090) 1000)
     (let [out (.getOutputStream socket)
           in (.getInputStream socket)
           data (.getBytes code)
           len_buf (ByteBuffer/allocate 4)]
       (.putInt len_buf (.-length data))
       (.write out (.array len_buf))
       (.write out data)
       (let [actual (String. (.readAllBytes in))]
         (close_server)
         (if (not= expected actual)
           (FIXME actual)))))))

(defn ^void main [^"String[]" _]
  (assert_ "4" "(\n+\n2\n2\n)")
  (assert_ "3" "(\ndo*\n(\ndef*\nx\n3\n)\nx\n)")
  (assert_ "2" "(\ndo*\n(\ndef*\nf\n(\nfn*\n(\nx\n)\nx\n)\n)\n(\nf\n2\n)\n)"))
