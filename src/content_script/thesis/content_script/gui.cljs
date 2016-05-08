(ns thesis.content-script.gui
  (:require-macros [reagent.ratom :refer [reaction]]
                   [hiccups.core :as hiccups :refer [html]])
  (:require [reagent.core :as r]
            [thesis.content-script.animation :as anim]
            [chromex.logging :refer-macros [log]]
            [domina.core :refer [by-id value set-value! destroy! append! by-class]]))

(def img-data (r/atom nil))
(def data (r/atom nil))
(def dim (r/atom [0 0]))
(def mouse (r/atom [0 0]))

(defn satellite [el]
  (let [text (get @el :text)
        x (r/cursor el [:x])
        y (r/cursor el [:y])
        fs (r/cursor el [:font-size])
        dots (get @el :dots)
        xsp (anim/spring x)
        ysp (anim/spring y)
        fssp (anim/interpolate-to fs)]
    (r/create-class
      {:component-will-unmount
       #(log "gone")
       :display-name "satellite"
       :reagent-render
        (fn [el]
          [:div
           [:h1 {:class "ext-h1" 
                 :style {:position "absolute" :left @xsp :top @ysp
                         :font-size (str @fssp "px")
                         :background-color "transparent"
                         :transform (str "translate( @xsp px, @ysp px)")}} (str text)
           (for [i (range dots)] ^{:key i} [:span i])]])})))

(defn satellites []
  [:div {:style {:position "absolute" :left "0" :top "0"}}
   (for [i (range (count @data))]
     ^{:key i} [satellite (r/cursor data [i])])])

(defn screenshot []
  (let [tilt (r/atom 0)
        rotation (anim/spring tilt)
        flip (r/atom 1)
        scale (anim/interpolate-to flip)
        w (.-innerWidth js/window)]
    (fn a-screenshot []
      [:div 
       [anim/timeout #(reset! flip 0.2) 100]
       [anim/timeout #(reset! tilt 45) 100]
       [:img {:src @img-data
              :id "ext-screenshot"
              :style (zipmap [:-ms-transform
                              :-moz-transform
                              :-webkit-transform
                              :transform]
                             (repeat (str "scale(" @scale "," @scale ") rotateY(" 0 "deg)")))}]])))

(defn root []
  [:div
   [satellites]
   [screenshot]])

(defn init! [img tabdict]
  (let [node (.. js/document (createElement "div"))
        el (.. js/document -body (appendChild node))
        div (set! (.-id el) "ext-canvas-container")]
    (reset! img-data img)
    (->> (vec (map #(hash-map
                         :x (int (rand 800))
                         :y (int (rand 600))
                         :font-size (int (+ 10 (rand 5)))
                         :text %
                         :dots (rand 20)
                         ) (vec tabdict)))
    (reset! data))
    (reset! dim [(.-innerWidth js/window) (.-innerHeight js/window)])

    (js/setTimeout (fn [] (swap! data conj {:x 200 :font-size 10 :y 200 :text "huhu" :dots 25})) 3000)
    (js/setTimeout (fn [] (swap! data (fn [e] (vec (map-indexed #(assoc %2 :x 400 :y (* 20 %1) :font-size (rand 20)) e))))) 4000)
    (js/setTimeout (fn [] (swap! data (fn [e] (vec (remove #(> (get % :x) 500) e))))) 5000)

    (r/render [root] (by-id "ext-canvas-container"))))

