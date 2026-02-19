(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/adnotare.jar")
(def dist-dir "target/dist")

(defn- basis []
  (b/create-basis {:project "deps.edn"
                   :aliases [:dev :jfx-mac]}))

(defn- assert-process-ok! [{:keys [exit out err]} step]
  (when-not (zero? exit)
    (throw (ex-info (str step " failed")
                    {:exit exit
                     :out out
                     :err err}))))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (let [basis' (basis)]
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis'
             :main 'adnotare.main})))

(defn zip-mac [_]
  (assert-process-ok!
   (b/process {:command-args ["ditto" "-c" "-k" "--sequesterRsrc" "--keepParent"
                              "target/dist/Adnotare.app"
                              "target/dist/Adnotare-macos.zip"]})
   "zip-mac"))

(defn package-mac [_]
  (uber nil)
  (b/delete {:path dist-dir})
  (assert-process-ok!
   (b/process {:command-args ["jpackage"
                              "--type" "app-image"
                              "--name" "Adnotare"
                              "--input" "target"
                              "--main-jar" "adnotare.jar"
                              "--main-class" "clojure.main"
                              "--arguments" "-m"
                              "--arguments" "adnotare.main"
                              "--dest" dist-dir
                              "--java-options" "-Dadnotare.malli-dev=false"]})
   "package-mac")
  (zip-mac nil))
