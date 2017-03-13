(ns theproject.events
  (:require [re-frame.core :as re-frame]
            [theproject.db :as db]
            [cljs.spec :as s]
            [cognitect.transit]
            ;; [clairvoyant.core :refer-macros [trace-forms]]   [re-frame-tracer.core :refer [tracer]]
            ))

;; (trace-forms {:tracer (tracer :color "green")}

(let [w (cognitect.transit/writer :json)
      r (cognitect.transit/reader :json)]
  (defn transit-read [s]
    (cognitect.transit/read r s))
  (defn transit-write [d]
    (cognitect.transit/write w d))
  )

(def check-spec-interceptor
  (re-frame/after #(s/assert :theproject.db/good-state %)))

;; (re-frame/reg-event-db
;;  :initialize-db
;;  [check-spec-interceptor re-frame/debug]
;;  (fn  [_ _]
;;    db/default-db))

(def local-storage-key "rueda-moves-array")

(re-frame/reg-cofx
 :local-store 
 (fn [coeffects local-store-key]
   (assoc coeffects 
          :local-store
          (.getItem js/localStorage local-store-key))))

(re-frame/reg-event-fx
 :initialize-db
 [check-spec-interceptor re-frame/debug (re-frame/inject-cofx :local-store local-storage-key)]
 (fn [cofx _]
   (let [moves-str (:local-store cofx)]
     (if (nil? moves-str)
       {:db db/default-db}
       {:db (assoc db/default-db
                   :moves (transit-read moves-str))}))))

(.addEventListener js/window "storage"
                   #(re-frame/dispatch [:storage-update %]))

(re-frame/reg-event-db
 :storage-update
 [check-spec-interceptor re-frame/debug]
 (fn [db [_ storage-event]]
   (println (.-key storage-event) (.-newValue storage-event))
   (if (= (.-key storage-event) local-storage-key)
    (assoc db :moves (transit-read (.-newValue storage-event))))))

(defn save-to-local-store
  [moves]
  (.setItem js/localStorage local-storage-key (transit-write moves)))
  

(re-frame/reg-event-db
 :set-active-panel
 [check-spec-interceptor re-frame/debug]
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 :set-pos
 [check-spec-interceptor
  ;; re-frame/debug
  ]
 (fn [db [_ pos]]
   (assoc db :pos pos)))

;; )   ; end of trace-forms

