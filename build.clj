(ns _ (:require ["vendor/make/0.2.0/main" :as b]))

(b/generate
 [(b/module
   {:lang "java"
    :root-ns "nrepl"
    :src-dir "nrepl"
    :target-dir ".github/bin/nrepl"
    :items ["nrepl"]})
  (b/module
   {:lang "java"
    :root-ns "test"
    :src-dir "test"
    :target-dir ".github/bin/test"
    :items ["test"]})
  (b/vendor
   {:lang "java"
    :target-dir ".github/bin"
    :items [{:name "interpreter" :version "0.3.0"}]})])
