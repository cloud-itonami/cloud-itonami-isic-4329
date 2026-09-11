(ns installation.governor-contract-test
  "The governor contract as executable tests -- the building-installation-
  coordination analog of `demolition.governor-contract-test`/`finishing.
  governor-contract-test`. The single invariant under test:

    Installation Advisor never schedules an installation operation, files
    a safety-concern flag or a supply order the Installation Governor
    would reject; `:flag-safety-concern` NEVER auto-commits at any phase;
    `:log-site-record` (no direct capital/safety risk), `:schedule-
    installation-operation` (governor-clean, high-confidence --
    deliberately LOWER stakes than the sibling demolition/road-rail
    schedule ops, see `installation.governor` ns docstring) and
    `:order-supplies` (below the cost threshold) MAY auto-commit when
    clean; and every decision (commit OR hold) leaves exactly one ledger
    fact. Every committed record's `:effect` is `:propose` -- this actor
    never performs a real-world actuation."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [installation.store :as store]
            [installation.operation :as op]
            [installation.governor :as governor]
            [installation.phase :as phase]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :site-supervisor :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

;; ----------------------------- :log-site-record -----------------------------

(deftest clean-log-site-record-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :log-site-record :subject "site-1" :patch {:id "site-1" :hazmat-detected? true}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (true? (:hazmat-detected? (store/site db "site-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))
    (is (= "JPN-SRL-000000" (get (first (store/site-record-log-history db)) "record_id")))))

(deftest log-site-record-can-resolve-a-safety-concern
  (let [[db actor] (fresh)]
    (exec-op actor "t1b" {:op :log-site-record :subject "site-6" :patch {:id "site-6" :safety-concern-unresolved? false}} operator)
    (is (false? (:safety-concern-unresolved? (store/site db "site-6"))))))

;; ----------------------------- :schedule-installation-operation -----------------------------

(deftest clean-schedule-installation-operation-auto-commits
  (testing "site-1 is fully clean (verified, hazmat-surveyed, interior work, high confidence) -- AUTO-COMMITS at phase 3, UNLIKE the sibling demolition/road-rail schedule ops"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :schedule-installation-operation :subject "site-1" :trade :insulation :window {}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "JPN-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id")))
      (is (= 1 (count (store/schedule-proposal-history db)))))))

(deftest fabricated-jurisdiction-is-held
  (testing "site-2 (ATL, no spec-basis in installation.facts) -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3" {:op :schedule-installation-operation :subject "site-2" :trade :elevator-installation :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-legal-basis} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db)) "no schedule proposal recorded"))))

(deftest not-independently-verified-site-is-held
  (testing "site-3 has site-verified? false -> HARD hold, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :schedule-installation-operation :subject "site-3" :trade :sound-proofing :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:site-not-verified} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db))))))

(deftest hazmat-survey-incomplete-is-held
  (testing "site-4 has hazmat-survey-completed? false -> HARD hold"
    (let [[db actor] (fresh)
          res (exec-op actor "t5" {:op :schedule-installation-operation :subject "site-4" :trade :insulation :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:hazmat-survey-incomplete} (-> (store/ledger db) first :basis))))))

(deftest fall-protection-noncompliant-is-held
  (testing "site-5's scaffold-working-height-m (3.5) meets/exceeds JPN's 2m trigger with no fall protection installed -> HARD hold, independent of proposal confidence"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :schedule-installation-operation :subject "site-5" :trade :escalator-installation :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:fall-protection-noncompliant} (-> (store/ledger db) first :basis))))))

(deftest unresolved-safety-concern-is-held
  (testing "site-6 has safety-concern-unresolved? true on file -> HARD hold, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t7" {:op :schedule-installation-operation :subject "site-6" :trade :elevator-installation :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:unresolved-safety-concern} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db))))))

(deftest qualitative-jurisdiction-never-fabricates-a-numeric-hold-and-still-auto-commits-when-clean
  (testing "site-8 (DEU/EU, qualitative) -- fall-protection-noncompliant? never fires there; clean + high confidence -> AUTO-COMMITS"
    (let [[db actor] (fresh)
          res (exec-op actor "t8" {:op :schedule-installation-operation :subject "site-8" :trade :sound-proofing :window {}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "DEU-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id"))))))

(deftest usa-cross-jurisdiction-happy-path-above-trigger-but-compliant-auto-commits
  (testing "site-7 (USA, quantitative 1.8m/6ft trigger), height=2.4m but fall-protection-installed? true -- compliant, clean, high confidence -> AUTO-COMMITS"
    (let [[db actor] (fresh)
          res (exec-op actor "t9" {:op :schedule-installation-operation :subject "site-7" :trade :insulation :window {}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "USA-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id"))))))

(deftest low-confidence-schedule-still-escalates-even-when-governor-clean
  (testing "an uncovered-jurisdiction low-confidence branch already HARD-holds (see fabricated-jurisdiction-is-held); this test proves the SOFT confidence-floor path independently via governor/check directly, since the mock advisor's own clean branch is always high-confidence"
    (let [db (store/seed-db)
          request {:op :schedule-installation-operation :subject "site-1"}
          proposal {:summary "s" :rationale "r" :cites ["x"] :effect :propose
                    :value {:site-id "site-1" :spec-basis "https://example.com"} :stake :schedule-installation-operation :confidence 0.4}
          verdict (governor/check request {} proposal db)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict) "confidence below the floor still escalates even when every HARD check is clean"))))

;; ----------------------------- :flag-safety-concern -----------------------------

(deftest flag-safety-concern-always-escalates-even-when-clean
  (testing "site-1 is fully clean -- :flag-safety-concern STILL always interrupts, unconditionally"
    (let [[db actor] (fresh)
          r1 (exec-op actor "t10" {:op :flag-safety-concern :subject "site-1"
                                   :concern-type :fiber-exposure
                                   :concern-description "glass-wool fiber dust observed near attic access hatch"} operator)]
      (is (= :interrupted (:status r1)))
      (let [r2 (approve! actor "t10")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (true? (:safety-concern-unresolved? (store/site db "site-1"))))
        (is (some? (get (first (store/safety-concern-flag-history db)) "document")) "rendered notice document present")))))

(deftest flag-safety-concern-triggers-notification-only-after-approval
  (let [[_db actor] (fresh)
        r1 (exec-op actor "t11" {:op :flag-safety-concern :subject "site-1"
                                 :concern-type :fiber-exposure :concern-description "suspected legacy asbestos-containing pipe insulation"} operator)]
    (is (nil? (:notify-result (:state r1))) "no notify before human approval")
    (let [r2 (approve! actor "t11")
          notify-result (:notify-result (:state r2))]
      (is (= 2 (count notify-result)) "one result entry per site-1 safety-contact")
      (is (every? #(= :sent (get-in % [:mail :status])) notify-result))
      (is (every? #(= :sent (get-in % [:phone :status])) notify-result)))))

;; ----------------------------- :order-supplies -----------------------------

(deftest order-supplies-below-threshold-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t12" {:op :order-supplies :subject "site-1"
                                  :items ["glass-wool-batt"] :cost-usd 800 :vendor "Local Building Supply Co."} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= 1 (count (store/supply-order-proposal-history db))))))

(deftest order-supplies-above-threshold-escalates
  (let [[db actor] (fresh)
        r1 (exec-op actor "t13" {:op :order-supplies :subject "site-1"
                                 :items ["elevator-hoist-motor"] :cost-usd 9000 :vendor "Access Equipment Rentals"} operator)]
    (is (= :interrupted (:status r1)) "above cost threshold -- always a human's call")
    (let [r2 (approve! actor "t13")]
      (is (= :commit (get-in r2 [:state :disposition])))
      (is (= 1 (count (store/supply-order-proposal-history db)))))))

;; ----------------------------- closed op-allowlist -----------------------------

(deftest op-outside-the-closed-allowlist-is-held
  (testing "an op outside {:log-site-record :schedule-installation-operation :flag-safety-concern :order-supplies} -> HARD hold, never reaches a human, regardless of what the advisor's default branch returns"
    (let [[db actor] (fresh)
          res (exec-op actor "t14" {:op :direct-equipment-command :subject "site-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:unknown-op} (-> (store/ledger db) first :basis))))))

;; ----------------------------- effect / forbidden-action-class (direct governor/check) -----------------------------
;; The mock advisor never produces these -- they exercise the governor's
;; defense-in-depth against a hypothetically compromised/malfunctioning
;; advisor, so they are tested directly against `governor/check` rather
;; than through the full actor (which can only ever see what the mock
;; advisor actually proposes).

(deftest effect-not-propose-is-a-permanent-hard-violation
  (let [db (store/seed-db)
        request {:op :log-site-record :subject "site-1"}
        proposal {:summary "s" :rationale "r" :cites ["x"] :effect :direct-write
                  :value {:id "site-1"} :stake nil :confidence 0.99}
        verdict (governor/check request {} proposal db)]
    (is (:hard? verdict))
    (is (some #{:effect-not-propose} (map :rule (:violations verdict))))
    (is (not (:ok? verdict)))))

(deftest forbidden-action-class-markers-are-permanent-hard-violations
  (doseq [marker [:trade-equipment-control? :direct-actuation? :finalizes-installation-completion-sign-off?]]
    (testing marker
      (let [db (store/seed-db)
            request {:op :schedule-installation-operation :subject "site-1"}
            proposal {:summary "s" :rationale "r" :cites ["x"] :effect :propose
                      :value {marker true} :stake :schedule-installation-operation :confidence 0.99}
            verdict (governor/check request {} proposal db)]
        (is (:hard? verdict))
        (is (some #{:forbidden-action-class} (map :rule (:violations verdict))))))))

;; ----------------------------- ledger discipline -----------------------------

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :log-site-record :subject "site-1" :patch {:id "site-1" :hazmat-detected? true}} operator)
      (exec-op actor "b" {:op :schedule-installation-operation :subject "site-2" :trade :elevator-installation :window {}} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))

(deftest approver-rejection-is-held-not-committed
  (let [[db actor] (fresh)
        r1 (exec-op actor "t15" {:op :flag-safety-concern :subject "site-1"
                                 :concern-type :fiber-exposure :concern-description "test"} operator)]
    (is (= :interrupted (:status r1)))
    (let [r2 (g/run* actor {:approval {:status :rejected :by "op-1"}} {:thread-id "t15" :resume? true})]
      (is (= :hold (get-in r2 [:state :disposition])))
      (is (empty? (store/safety-concern-flag-history db))))))

;; ----------------------------- phase structural invariants (belt-and-suspenders) -----------------------------

(deftest flag-safety-concern-never-auto-at-any-phase
  (testing "structural invariant: never auto-eligible, even when clean, at any phase"
    (is (= :escalate (:disposition (phase/gate 3 {:op :flag-safety-concern} :commit)))
        ":flag-safety-concern must escalate to a human even when the governor is clean at phase 3")))

(deftest schedule-installation-operation-is-auto-eligible-at-phase-3-unlike-siblings
  (testing "structural invariant proving the deliberate difference from demolition.phase/roadrail.phase"
    (is (= :commit (:disposition (phase/gate 3 {:op :schedule-installation-operation} :commit)))
        ":schedule-installation-operation MAY auto-commit at phase 3 when the governor is clean -- see installation.governor/installation.phase ns docstrings")))
