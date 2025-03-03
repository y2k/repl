(ns _ (:import [java.net ServerSocket Socket]
               [java.nio ByteBuffer]))

(defn- main_loop [eval env_atom ^ServerSocket server]
  (checked!
   (let [^Socket socket (recover (fn [] (.accept server)) (fn [] nil))]
     (if (some? socket)
       (let [in (.getInputStream socket)
             len_buf (ByteBuffer/allocate 4)
             _ (.read in (.array len_buf))
             length (.getInt len_buf)
             buffer (ByteBuffer/allocate length)
             _ (.read in (.array buffer) 0 length)
             input_bytes (.array buffer)
             lexems (vec (.split (String. input_bytes) "\\n"))
             [result env] (eval (deref env_atom) lexems)
             out (.getOutputStream socket)]
         (reset! env_atom env)
         (.write out (.getBytes (str result)))
         (.flush out)
         (.close out)
         (.close socket)
         (main_loop eval env_atom server))))))

(defn main [eval ^int port env_atom]
  (let [server_socket (atom nil)]
    (.start
     (Thread.
      (fn []
        (reset! server_socket (ServerSocket. port))
        (main_loop eval env_atom (as (deref server_socket) ServerSocket)))))
    (fn []
      (.close (as (deref server_socket) ServerSocket))
      nil)))
