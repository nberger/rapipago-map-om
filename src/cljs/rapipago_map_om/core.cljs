(ns rapipago-map-om.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.core.async :refer [<! chan put! sliding-buffer]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

(defn json-xhr [{:keys [method url on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (js->clj (.getResponseJson xhr) :keywordize-keys true))))
    (. xhr (send url method))))

(def app-state (atom {:provinces []
                      :cities []}))

(defn handle-province-selected [e app province-chan]
  (let [province-id (.. e -target -value)]
    (println "handling province...")
    (->> (:provinces @app)
         (filter #(= province-id (:id %)))
         first
         (put! province-chan))
    (println "province handled...")))

(defn province-filter [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (json-xhr {:method "GET"
                 :url "http://localhost:3001/provinces"
                 :on-complete #(om/transact! app :provinces (fn [_] %))}))
    om/IRenderState
    (render-state [_ {:keys [province-chan]}]
      (dom/div #js {:id "province-filter"}
               (dom/label #js {:htmlFor "province"} "Provincia")
               (apply dom/select #js {:id "province"
                                      :onChange #(handle-province-selected % app province-chan)}
                      (map #(dom/option #js {:value (:id %)}
                                        (:name %))
                           (:provinces app)))))))

(defn city-filter [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [province-chan (om/get-state owner :province-chan)]
        (go (while true
              (let [province (<! province-chan)]
                (json-xhr {:method "GET"
                           :url (str "http://localhost:3001/provinces/" (:id province) "/cities")
                           :on-complete #(om/transact! app :cities (fn [_] %))}))))))
    om/IRender
    (render [_]
      (dom/div #js {:id "cities-filter"}
               (dom/label #js {:htmlFor "city"} "Ciudad")
               (apply dom/select #js {:id "city"}
                      (map #(dom/option #js {:value (:id %)}
                                        (:name %))
                           (:cities app)))))))

(defn filters [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:province-chan (chan (sliding-buffer 1))})
    om/IRenderState
    (render-state [_ {:keys [province-chan]}]
      (dom/div nil
        (om/build province-filter app
                  {:init-state {:province-chan province-chan}})
        (om/build city-filter app
                  {:init-state {:province-chan province-chan}})))))

(om/root
  filters
  app-state
  {:target (. js/document (getElementById "app"))})
