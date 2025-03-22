(ns _ (:import [java.net Socket InetSocketAddress]
               [java.nio ByteBuffer]
               [java.nio.file Files])
    (:require ["../nrepl/nrepl" :as nrepl]
              ["../interpreter/interpreter" :as i]))

(defn- assert_ [expected ^String code]
  (unchecked!
   (let [port (cast int (+ 10000 (* 10000 (Math/random))))
         close_server (nrepl/main (fn [e l] (i/eval e l)) port (atom (i/make_env {})))
         socket (Socket.)]
     (.connect socket (InetSocketAddress. "localhost" port) 1000)
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
           (FIXME "\nExpected: " expected "\n  Actual: " actual)))))))

(defn- execute_code [^int port code]
  (unchecked!
   (let [socket (Socket.)]
     (.connect socket (InetSocketAddress. "localhost" port) 1000)
     (let [out (.getOutputStream socket)
           in (.getInputStream socket)
           data (.getBytes (cast String code))
           len_buf (ByteBuffer/allocate 4)]
       (.putInt len_buf (.-length data))
       (.write out (.array len_buf))
       (.write out data)
       (let [actual (String. (.readAllBytes in))]
         actual)))))

(defn- execute_stage [state_path code]
  (let [env_atom (atom (i/make_env {}))
        port (cast int (+ 10000 (* 10000 (Math/random))))
        close_server (nrepl/main_with_state (.toString state_path)
                                            (fn [e l] (i/eval e l))
                                            env_atom
                                            {:port port})]
    (let [actual (execute_code port code)]
      (close_server)
      actual)))

(defn- test [expected code_before code_after]
  (unchecked!
   (let [state_path (Files/createTempFile "temp_" ".txt")]
     (Files/delete state_path)
     (execute_stage state_path code_before)
     (let [actual (execute_stage state_path code_after)]
       (if (not= expected actual)
         (FIXME "\nExpected: " expected "\n  Actual: " actual))))))

(defn ^void main [^"String[]" _]
  ;; (assert_ "4" "(\n+\n2\n2\n)")
  ;; (assert_ "3" "(\ndo*\n(\ndef*\nx\n3\n)\nx\n)")
  ;; (assert_ "2" "(\ndo*\n(\ndef*\nf\n(\nfn*\n(\nx\n)\nx\n)\n)\n(\nf\n2\n)\n)")
  (test "2" "(\ndef*\nf\n(\nfn*\n(\nx\n)\nx\n)\n)" "(\nf\n2\n)"))
