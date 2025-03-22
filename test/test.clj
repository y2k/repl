(ns _ (:import [java.net Socket InetSocketAddress]
               [java.nio ByteBuffer]
               [java.nio.file Files])
    (:require ["../nrepl/nrepl" :as nrepl]
              ["../interpreter/interpreter" :as i]))

(declare test)

(defn ^void main [^"String[]" _]
  (test "4" "0" "(+ 2 2)")
  (test "3" "0" "(do (def x 3) x)")
  (test "3" "(def x 3)" "x")
  (test "2" "0" "(do (defn f [x] x) (f 2))")
  (test "2" "(defn f [x] x)" "(f 2)"))

;; Infrastructure for running tests

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

(defn- compile_to_string [^String code]
  (unchecked!
   (let [p (.start (ProcessBuilder. "clj2js" "compile" "-src" "@stdin" "-target" "bytecode" "-no_lint" "true"))]
     (.write (.getOutputStream p) (.getBytes code))
     (.close (.getOutputStream p))
     (String. (.readAllBytes (.getInputStream p))))))

(defn- test [expected ^String code_before ^String code_after]
  (unchecked!
   (let [state_path (Files/createTempFile "temp_" ".txt")]
     (Files/delete state_path)
     (execute_stage state_path (compile_to_string code_before))
     (let [actual (execute_stage state_path (compile_to_string code_after))]
       (if (not= expected actual)
         (FIXME "\nExpected: " expected "\n  Actual: " actual))))))
