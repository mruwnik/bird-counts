(ns birds.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [birds.actors :as actors]
            [birds.bird :as suta]))

(let [bird (sut/map->Bird {:pos {:x 100 :y 100}
                           :volume 50
                           :audio-sensitivity 100})]
  ;; out of range
  (not (actors/hears? bird {:pos {:x 1000 :y 1000} :volume 10}))
  (not (actors/hears? bird {:pos {:x 1000 :y -1000} :volume 10}))
  (not (actors/hears? bird {:pos {:x 100 :y 210} :volume 10}))
  (not (actors/hears? bird {:pos {:x 100 :y 240} :volume 40}))
  (not (actors/hears? bird {:pos {:x 200 :y 200} :volume 41}))

  ;; edge of range
  (actors/hears? bird {:pos {:x 200 :y 200} :volume 42})
  (actors/hears? bird {:pos {:x 100 :y 219} :volume 20}))
