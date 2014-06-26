(ns rapipago-map-om.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events])
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

(defn province-filter [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (json-xhr {:method "GET"
                 :url "http://localhost:3001/provinces"
                 :on-complete #(om/transact! app :provinces (fn [_] %))}))
    om/IRender
    (render [_]
      (dom/div #js {:id "province-filter"}
               (dom/label #js {:htmlFor "province"} "Provincia")
               (apply dom/select #js {:id "province"}
                      (map #(dom/option #js {:value (:id %)}
                                        (:name %))
                           (:provinces app)))))))

(om/root
  province-filter
  app-state
  {:target (. js/document (getElementById "app"))})
