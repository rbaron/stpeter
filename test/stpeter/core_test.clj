(ns stpeter.core-test
  (:require [clojure.core.async :as async]
            [clojure.test :refer :all]
            [stpeter.core :refer :all]))

(deftest handle-msg-test
  (testing "It handles valid temp messages"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set temp 17"} out-chan my-user-id)
      (let [to-esp-msg (async/<!! to-esp)
            to-slack-msg (async/<!! out-chan)]
        (is (= to-esp-msg "set temp 17"))
        (is (= to-slack-msg "Ok!")))))

  (testing "It handles turn ac off message"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set ac off"} out-chan my-user-id)
      (let [to-esp-msg (async/<!! to-esp)
            to-slack-msg (async/<!! out-chan)]
        (is (= to-esp-msg "set ac off"))
        (is (= to-slack-msg "Ok!")))))

  (testing "It handles invalid temp messages"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> set temp 0"} out-chan my-user-id)
      (let [to-slack-msg (async/<!! out-chan)]
        (is (= to-slack-msg "Invalid temp. Keep it >= 17 and <= 26")))))

  (testing "It handles invalid commands"
    (let [out-chan (async/chan)
          my-user-id "my-user-id"]
      (handle-msg {:text "<@my-user-id> help"} out-chan my-user-id)
      (let [to-slack-msg (async/<!! out-chan)]
        (is (= to-slack-msg "Invalid command. Available commands:\nset temp [17-26]\nset ac off")))))
)
