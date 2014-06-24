(ns rapipago-map-om.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:search-params {}
                      :search-result {}}))

(defn province-filter [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:provinces [{:name "Buenos Aires" :id "B"}]})
    om/IWillMount
    (will-mount [_]
      )
    om/IRenderState
    (render-state [_, {:keys [provinces]}]
      (dom/div #js {:id "province-filter"}
           (dom/label #js {:for "province"} "Provincia")
           (apply dom/select nil
                   (map #(dom/option #js {:value (:id %)
                                          :selected (= % (:province app))}
                                     (:name %))
                        provinces))))))

(om/root
  province-filter
  app-state
  {:target (. js/document (getElementById "app"))})
