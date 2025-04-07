(ns _ (:import [java.net Socket InetSocketAddress]
               [java.nio ByteBuffer])
    (:require ["../nrepl/nrepl" :as nrepl]
              ["../interpreter/interpreter" :as i]))

(declare test)

(defn ^void main [^"String[]" _]
  (test "5" ["(defn a [x] x)" "(defn b [y] y)"] "(+ (a 2) (b 3))")
  (test "4" ["0"] "(+ 2 2)")
  (test "3" ["0"] "(do (def x 3) x)")
  (test "10" ["(def x (+ (+ 1 2) (+ 3 4)))"] "(do x x)")
  (test "2" ["0"] "(do (defn f [x] x) (f 2))")
  (test "2" ["(defn f [x] x)"] "(f 2)"))

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

(defn- execute_stage [external code]
  (let [env_atom (atom (i/make_env {}))
        port (cast int (+ 10000 (* 10000 (Math/random))))
        close_server (nrepl/main {}
                                 (fn [e l] (i/eval external e l))
                                 env_atom
                                 {:port port})]
    (let [actual (execute_code port code)]
      (close_server)
      actual)))

(defn- compile_to_string [code]
  (unchecked!
   (let [p (.start (ProcessBuilder. "clj2js" "compile" "-src" "@stdin" "-target" "bytecode" "-no_lint" "true"))]
     (.write (.getOutputStream p) (.getBytes (cast String code)))
     (.close (.getOutputStream p))
     (String. (.readAllBytes (.getInputStream p))))))

(defn- test [expected codes_before ^String code_after]
  (unchecked!
   (let [store_atom (atom {})
         already_resolved (atom [])
         external {:interpreter:save (fn [name code]
                                      ;;  (eprintln (str "[LOG][save] " name "\n" code))
                                       (swap! store_atom (fn [x] (assoc x name (str code)))))
                   :interpreter:resolve (fn [name]
                                          (if (contains? (deref already_resolved) name)
                                            (FIXME "Already resolved: " name)
                                            (swap! already_resolved (fn [x] (conj x name))))
                                          (let [result (get (deref store_atom) name)]
                                            ;; (eprintln "[LOG][resolve]" name "\n" result)
                                            result))}]
     (map
      (fn [code]
        (execute_stage external (compile_to_string code)))
      codes_before)
     (let [actual (execute_stage external (compile_to_string code_after))]
       (if (not= expected actual)
         (FIXME "\nExpected: " expected "\n  Actual: " actual))))))
