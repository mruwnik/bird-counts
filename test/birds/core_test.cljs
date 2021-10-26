(ns birds.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [birds.core :as sut]))

(let [bird (sut/->Bird (atom {:pos {:x 100 :y 100}
                              :volume 50
                              :audio-sensitivity 100}) nil)]
  ;; out of range
  (not (sut/hears? bird {:pos {:x 1000 :y 1000} :volume 10}))
  (not (sut/hears? bird {:pos {:x 1000 :y -1000} :volume 10}))
  (not (sut/hears? bird {:pos {:x 100 :y 210} :volume 10}))
  (not (sut/hears? bird {:pos {:x 100 :y 240} :volume 40}))
  (not (sut/hears? bird {:pos {:x 200 :y 200} :volume 41}))

  ;; edge of range
  (sut/hears? bird {:pos {:x 200 :y 200} :volume 42})
  (sut/hears? bird {:pos {:x 100 :y 219} :volume 20}))
