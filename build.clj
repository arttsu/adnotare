(ns build
  (:require
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/adnotare.jar")
(def dist-dir "target/dist")
(def archive-dir (str dist-dir "/archives"))
(def package-input-dir "target/package-input")
(def app-name "Adnotare")

(defn- kw-args [args]
  (into {}
        (map (fn [[k v]]
               [(if (keyword? k) k (keyword k)) v]))
        args))

(defn- normalize-os [os]
  (let [s (cond
            (keyword? os) (name os)
            (string? os) os
            (nil? os) "mac"
            :else (str os))
        s (string/lower-case s)]
    (case s
      ("mac" "macos" "osx" "darwin") "mac"
      ("win" "windows") "win"
      ("linux") "linux"
      (throw (ex-info "Unsupported os" {:os os :normalized s})))))

(defn- jfx-alias [os]
  (case os
    "mac" :jfx-mac
    "win" :jfx-win
    "linux" :jfx-linux
    (throw (ex-info "Unsupported os" {:os os}))))

(defn- basis [os]
  (b/create-basis {:project "deps.edn"
                   :aliases [:dev (jfx-alias os)]}))

(defn- normalize-os-arch [raw]
  (case raw
    "amd64" "x64"
    "x86_64" "x64"
    "x64" "x64"
    "aarch64" "arm64"
    "arm64" "arm64"
    raw))

(defn- default-arch [os]
  (if (= os "win")
    "x64"
    (normalize-os-arch
     (string/lower-case (or (System/getProperty "os.arch") "x64")))))

(defn- app-image-name [os]
  (if (= os "mac")
    (str app-name ".app")
    app-name))

(defn- os-label [os]
  (case os
    "mac" "macos"
    "win" "windows"
    "linux" "linux"))

(defn- archive-ext [os]
  (if (= os "linux") "tar.gz" "zip"))

(defn- archive-name [os arch version]
  (str app-name "-" (os-label os) "-" arch "-" version "." (archive-ext os)))

(defn- mkdirs! [path]
  (.mkdirs (java.io.File. path)))

(defn- prepare-package-input! []
  (b/delete {:path package-input-dir})
  (mkdirs! package-input-dir)
  (b/copy-file {:src uber-file
                :target (str package-input-dir "/adnotare.jar")}))

(defn- assert-process-ok! [{:keys [exit out err]} step]
  (when-not (zero? exit)
    (throw (ex-info (str step " failed")
                    {:exit exit
                     :out out
                     :err err}))))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [args]
  (let [{:keys [os] :or {os "mac"}} (kw-args args)
        os (normalize-os os)
        basis' (basis os)]
    (clean nil)
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis'
             :main 'adnotare.main})))

(defn package [args]
  (let [{:keys [os out-dir]
         :or {os "mac"}} (kw-args args)
        os (normalize-os os)
        package-out-dir (or out-dir (str dist-dir "/" os))]
    (uber {:os os})
    (prepare-package-input!)
    (b/delete {:path package-out-dir})
    (mkdirs! package-out-dir)
    (assert-process-ok!
     (b/process {:command-args ["jpackage"
                                "--type" "app-image"
                                "--name" app-name
                                "--input" package-input-dir
                                "--main-jar" "adnotare.jar"
                                "--main-class" "clojure.main"
                                "--arguments" "-m"
                                "--arguments" "adnotare.main"
                                "--dest" package-out-dir
                                "--java-options" "-Dadnotare.malli-dev=false"]})
     "package")
    {:os os
     :out-dir package-out-dir
     :app-path (str package-out-dir "/" (app-image-name os))}))

(defn dist [args]
  (let [{:keys [os version arch] :or {os "mac" version "dev"}} (kw-args args)
        os (normalize-os os)
        arch' (normalize-os-arch (or arch (default-arch os)))
        {:keys [app-path]} (package {:os os})
        archive-name' (archive-name os arch' version)
        archive-path (str archive-dir "/" archive-name')]
    (mkdirs! archive-dir)
    (b/delete {:path archive-path})
    (case os
      "mac"
      (assert-process-ok!
       (b/process {:command-args ["ditto" "-c" "-k" "--sequesterRsrc" "--keepParent"
                                  app-path archive-path]})
       "dist-mac")

      "win"
      (assert-process-ok!
       (b/process {:command-args ["powershell" "-NoProfile" "-Command"
                                  (str "Compress-Archive -Path '" app-path "' -DestinationPath '" archive-path "' -Force")]})
       "dist-win")

      "linux"
      (assert-process-ok!
       (b/process {:command-args ["tar" "-C" (str dist-dir "/" os)
                                  "-czf" archive-path
                                  (app-image-name os)]})
       "dist-linux"))
    {:archive archive-path}))

(defn zip-mac [_]
  (dist {:os :macos :version "dev-local"}))

(defn package-mac [_]
  (uber {:os :macos})
  (prepare-package-input!)
  (b/delete {:path dist-dir})
  (mkdirs! dist-dir)
  (assert-process-ok!
   (b/process {:command-args ["jpackage"
                              "--type" "app-image"
                              "--name" app-name
                              "--input" package-input-dir
                              "--main-jar" "adnotare.jar"
                              "--main-class" "clojure.main"
                              "--arguments" "-m"
                              "--arguments" "adnotare.main"
                              "--dest" dist-dir
                              "--java-options" "-Dadnotare.malli-dev=false"]})
   "package-mac")
  (assert-process-ok!
   (b/process {:command-args ["ditto" "-c" "-k" "--sequesterRsrc" "--keepParent"
                              "target/dist/Adnotare.app"
                              "target/dist/Adnotare-macos.zip"]})
   "zip-mac"))
