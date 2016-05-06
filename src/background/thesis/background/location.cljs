(ns thesis.background.location
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.object]
            [cljs.core.async :refer [<! chan timeout]]
            [chromex.protocols :refer [get set]]
            [chromex.ext.storage :as storage]
            [chromex.logging :refer-macros [log info warn error group group-end]]))

(defn watch-location!
  []
  (let [local-storage (storage/get-local)
        geo-chan (chan)]
    (go-loop []
       (-> js/navigator (.-geolocation) (.getCurrentPosition #(go (>! geo-chan %)) #() (clj->js {:maximumAge 1000})))
       (<! (timeout 30000))
       (recur))

    (go-loop [i 0]
      (let [loc (<! geo-chan)]
        (set local-storage (clj->js {:lat (.. loc -coords -latitude)
                                                     :lon (.. loc -coords -longitude)
                                                     :acc (.. loc -coords -accuracy)
                                                     :it i
                                                     :ts (.. loc -timestamp)})))
        (recur (+ i 1)))))
