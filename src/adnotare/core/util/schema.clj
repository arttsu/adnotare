(ns adnotare.core.util.schema)

(def Label [:string {:min 1}])

(def Color [:int {:min 0 :max 9}])

(def Identifier
  [:and
   :string
   [:re #"^[A-Za-z0-9_-]+$"]])

(def Millis [:int {:min 0}])

(def Status [:enum :ok :error])

(def ReadEDNFileError [:enum :not-found :eof])

(def ReadVersionedEDNFileError [:enum :not-found :eof :unsupported-version :invalid-schema])

(def SelectorOption
  [:map
   [:id :uuid]
   [:text Label]])

(def SelectorOptions
  [:map
   [:options [:sequential SelectorOption]]
   [:selected [:maybe SelectorOption]]])

(defn IDd [T]
  [:tuple :uuid T])

(defn IDSeq [T]
  [:sequential (IDd T)])
