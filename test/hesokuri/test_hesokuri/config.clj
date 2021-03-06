; Copyright (C) 2013 Google Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;    http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns hesokuri.test-hesokuri.config
  (:use clojure.test
        hesokuri.config
        hesokuri.test-hesokuri.data))

(deftest test-source-defs
  (are [config sources]
       (is (= sources (source-defs config)))

       {:sources "foo"} "foo"
       {:sources "bar", :comment "baz"} "bar"
       ["foo" "bar"] ["foo" "bar"]))

(deftest test-join-error-strings
  (are [all-strings result]
       (is (= result (#'hesokuri.config/join-error-strings all-strings)))

       [] nil
       [nil nil] nil
       [nil nil "foo" nil] "foo"
       ["foo"] "foo"
       ["foo" nil "bar"] "foo\nbar"))

(deftest test-source-defs-validation-error
  (are [source-defs has-error]
       (is (= has-error (boolean (#'hesokuri.config/source-defs-validation-error
                                  source-defs))))
       [] false
       *sources-eg* false
       [:foo] true
       [{}] true
       (conj *sources-eg* {:missing-host-to-path 42}) true
       (conj *sources-eg* {"" "host-name-is-empty-string"}) true))

(defn- error-is-correct [error-string okay substrings]
  (is (= okay (= error-string nil)))
  (doseq [substring substrings]
    (is (not= -1 (.indexOf error-string substring))))
  true)

(deftest test-round-trip-validation-error
  (are [data okay substrings]
       (error-is-correct
        (#'hesokuri.config/round-trip-validation-error data) okay substrings)

       ["ok"] true []
       [nil nil] true []
       [true false] true []
       {true :keyword, false 1.5} true []
       [#{} #{} #{}] true []
       [(list 'foo 'bar)] false ["(foo bar)"]
       ['(foo) "ok" '(bar)] false ["(foo)" "(bar)"]
       #{[] [1] ["two"]} true []))

(deftest test-validation-error
  (are [config okay substrings]
       (error-is-correct
        (#'hesokuri.config/validation-error config) okay substrings)

       *sources-eg* true []
       *config-eg* true []
       {:comment "missing sources"} false []
       {:comment "sources is wrong type" :sources #{}} false []
       {:comment ["not round-trippable" 'foo] :sources []} false ["foo"]
       {:comment ["no sources is okay"] :sources []} true []
       #{"must be a map or vector"} false ["PersistentHashSet"]))
