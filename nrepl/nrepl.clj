(ns _ (:import [java.net ServerSocket Socket]
               [java.nio ByteBuffer]))

(defn- update_env [eval env_atom code]
  (let [lexems (vec (.split (cast String code) "\\n"))
        [result env] (eval (deref env_atom) lexems)]
    (reset! env_atom env)
    result))

(defn- main_loop [eval env_atom ^ServerSocket server]
  (unchecked!
   (let [^Socket socket (recover (fn [] (.accept server)) (fn [] nil))]
     (if (some? socket)
       (let [in (.getInputStream socket)
             len_buf (ByteBuffer/allocate 4)
             _ (.read in (.array len_buf))
             length (.getInt len_buf)
             buffer (ByteBuffer/allocate length)
             _ (.read in (.array buffer) 0 length)
             input_bytes (.array buffer)
             result (recover
                     (fn [] (update_env eval env_atom (String. input_bytes)))
                     (fn [e] (str e)))
             out (.getOutputStream socket)]
         (.write out (.getBytes (str result)))
         (.flush out)
         (.close out)
         (.close socket)
         (main_loop eval env_atom server))))))

(defn main [eval env_atom config]
  (let [server_socket (atom nil)]
    (.start
     (Thread.
      (fn []
        (reset! server_socket (ServerSocket. (cast int (:port config))))
        (main_loop eval env_atom (as (deref server_socket) ServerSocket)))))
    (fn []
      (.close (as (deref server_socket) ServerSocket))
      nil)))
