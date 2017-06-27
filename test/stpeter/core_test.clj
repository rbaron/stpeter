(ns stpeter.core-test
  (:require [clojure.core.async :as async]
            [clojure.test :refer :all]
            [stpeter.core :refer :all]))

(deftest handle-msg-test
  (testing "It handles valid temp messages"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set ac1 temp 18" :channel "c"} out-chan my-user-id)
      (let [to-esp-msg (async/<!! to-esp)
            to-slack-msg (async/<!! out-chan)]
        (is (= to-esp-msg "set ac1 temp 18"))
        (is (= to-slack-msg (make-msg "Ok!" "c"))))))

  (testing "It handles turn ac off message"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set ac1 off" :channel "c"} out-chan my-user-id)
      (let [to-esp-msg (async/<!! to-esp)
            to-slack-msg (async/<!! out-chan)]
        (is (= to-esp-msg "set ac1 off"))
        (is (= to-slack-msg (make-msg "Ok!" "c"))))))

  (testing "It handles invalid temp messages"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set ac1 temp 0" :channel "c"} out-chan my-user-id)
      (let [to-slack-msg (async/<!! out-chan)]
        (is (= to-slack-msg (make-msg "Invalid temp. Keep it >= 18 and <= 26" "c"))))))

  (testing "It handles invalid commands"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> help" :channel "c"} out-chan my-user-id)
      (let [to-slack-msg (async/<!! out-chan)]
        (is (= to-slack-msg (make-msg (str "Invalid command. " help) "c"))))))
)

(deftest parse-temp-test
  (testing "It parses temp"
    (let [tmp (parse-temp "123")]
      (is (= 123 tmp)))
  )

  (testing "It doesnt break on huge numbers"
    (let [tmp (parse-temp "1231203812312730712397120931720938127")]
      (is (= nil tmp)))
  )
)
