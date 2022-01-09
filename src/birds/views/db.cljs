(ns birds.views.db)

(def default-db
  {:container-name "forest"
   :frame-rate 30
   :num-of-birds 10
   :volume 25
   :spontaneous-sing-prob 0.01 ;; in a given 0.1s
   :motivated-sing-prob 0.9
   :motivated-sing-after 30
   :sing-rest-time 100
   :song-length 20
   :audio-sensitivity 50
   :show-bird-hear?     true
   :show-birds?         true
   :show-observers?     true
   :show-observer-hear? true

   :observer-strategies [:no-movement :random-walk :follow-singing]

   :speed 10
   :tick-length 100})
