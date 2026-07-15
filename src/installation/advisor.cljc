(ns installation.advisor
  "Installation Advisor client -- the *contained intelligence node* for
  the other-construction-installation-project OPERATIONS COORDINATION
  actor (ISIC 4329: thermal/acoustic insulation, sound-proofing,
  elevator/escalator installation, other specialty building installation
  work not elsewhere classified -- distinct from 4321 electrical / 4322
  plumbing, heat and air-conditioning installation).

  It normalizes site-record logging (trade-progress/material-usage/
  hazmat-assessment data), drafts an insulation/sound-proofing/specialty-
  installation SCHEDULE PROPOSAL (never a trade-equipment-dispatch
  command or an installation-completion sign-off), drafts a safety-
  concern FLAG (insulation-fiber-exposure materials hazard, structural,
  or fall-protection concern), and drafts a materials/equipment supply-
  order PROPOSAL. CRITICAL: it is a smart-but-untrusted advisor, and it
  is SCOPED -- it holds NO trade-equipment-control authority and NO
  installation-completion-sign-off authority (that is the site
  supervisor / building official's exclusively). It returns a *proposal*
  (with a rationale + the fields it cited), NEVER a committed record, a
  real mail/phone send, a real procurement order, or a real crew/
  equipment dispatch. Every output carries `:effect :propose` and is
  censored downstream by `installation.governor` before anything touches
  the SSoT -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so the
  actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the legal-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     :propose       ; ALWAYS :propose -- see `installation.governor`
     :value      map            ; the coordination-artifact payload
     :stake      kw|nil         ; the op itself (drives high-stakes gating) or nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [installation.facts :as facts]
            [installation.store :as store]
            [langchain.model :as model]))

(defn- log-site-record
  "Site-record-log normalization -- the LLM only normalizes/validates the
  patch (trade-progress / material-usage / hazmat-assessment data); it
  does not invent the site, its jurisdiction or its ground-truth fields.
  High confidence, low stakes -- `:stake nil`, the lowest-risk op in this
  actor's closed allowlist."
  [_db {:keys [patch]}]
  {:summary    (str "現場記録更新: " (pr-str (keys patch)))
   :rationale  "入力patchの正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :propose
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- schedule-installation-operation
  "Draft a thermal/acoustic-insulation, sound-proofing, elevator/
  escalator-installation or other specialty-installation SCHEDULE
  PROPOSAL -- a proposed work window (never a trade-equipment-dispatch
  command or an installation-completion sign-off; see `installation.
  governor` ns docstring). `:stake :schedule-installation-operation` --
  escalates when the governor is not clean OR confidence is below the
  floor; MAY auto-commit at phase 3 when clean and confident (deliberately
  LOWER stakes than the sibling demolition/road-rail schedule ops -- see
  `installation.governor`/`installation.phase` ns docstrings for the
  rationale)."
  [db {:keys [subject window trade notes]}]
  (let [a (store/site db subject)
        iso3 (:jurisdiction a)
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式legal-basisが見つかりません -- スケジュール提案不可")
       :rationale  "installation.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :propose
       :value      {:site-id subject :jurisdiction iso3 :window window :trade trade :notes notes :spec-basis nil}
       :stake      :schedule-installation-operation
       :confidence 0.2}
      {:summary    (str subject " 向け" (some-> trade name) "スケジュール提案 (" (:owner-authority sb) ")"
                        (when a (str " (site=" (:name a) ")")))
       :rationale  (str "石綿等事前調査basis: " (:hazmat-survey-basis sb)
                       " / 墜落防止basis: " (:fall-protection-basis sb)
                       " / site-verified?=" (:site-verified? a)
                       " / hazmat-survey-completed?=" (:hazmat-survey-completed? a)
                       " / scaffold-working-height-m=" (:scaffold-working-height-m a)
                       " / fall-protection-installed?=" (:fall-protection-installed? a))
       :cites      [(:hazmat-survey-basis sb) (:hazmat-survey-provenance sb)
                    (:fall-protection-basis sb) (:fall-protection-provenance sb)]
       :effect     :propose
       :value      {:site-id subject :jurisdiction iso3 :window window :trade trade :notes notes
                    :spec-basis (:fall-protection-provenance sb)}
       :stake      :schedule-installation-operation
       :confidence (if (and a (:site-verified? a) (:hazmat-survey-completed? a)) 0.9 0.3)})))

(defn- flag-safety-concern
  "Draft a SAFETY-CONCERN FLAG -- surfacing an insulation-fiber-exposure
  materials hazard (fiberglass/mineral-wool dust, or legacy asbestos-
  containing insulation disturbed during retrofit), structural, or fall-
  protection concern for human review. `:stake :flag-safety-concern` --
  ALWAYS escalates to a human, unconditionally, regardless of confidence
  (see README `Actuation` + `installation.governor` ns docstring
  `high-stakes`)."
  [db {:keys [subject concern-type concern-description]}]
  (let [a (store/site db subject)
        sb (facts/spec-basis (:jurisdiction a))]
    {:summary    (str subject ": 安全性懸念を検出（" (name (or concern-type :fiber-exposure)) "）"
                      (when a (str " (site=" (:name a) ")")))
     :rationale  (if sb
                   (str "関連basis: " (:hazmat-survey-basis sb))
                   "現場記録または法域spec-basisが見つかりません")
     :cites      (if sb [(:hazmat-survey-basis sb)] [])
     :effect     :propose
     :value      {:site-id subject
                  :concern-type (or concern-type :fiber-exposure)
                  :concern-description concern-description
                  :subject-line (str "[至急] " (:name a) " 安全性懸念のお知らせ")
                  :body (str (:name a) "の現場で安全性懸念（" (name (or concern-type :fiber-exposure))
                            "）が検出されました。詳細を確認し対応を検討してください。")
                  :message (str (:name a) "、安全性懸念が検出されました。至急ご確認ください。")}
     :stake      :flag-safety-concern
     :confidence 0.9}))

(defn- order-supplies
  "Draft a materials/equipment SUPPLY-ORDER PROPOSAL (insulation batts/
  rolls, acoustic panels, elevator/escalator components, specialty
  installation hardware and rental equipment). `:stake :order-supplies`
  -- escalates when `:cost-usd` exceeds `installation.governor/supply-
  order-cost-threshold-usd`, or when confidence is low (see
  `installation.governor`)."
  [db {:keys [subject items cost-usd vendor]}]
  (let [a (store/site db subject)]
    {:summary    (str subject " 向け資材/機材発注提案 (" (pr-str items) ", "
                      cost-usd " USD, vendor=" vendor ")"
                      (when a (str " (site=" (:name a) ")")))
     :rationale  (if a (str "site-verified?=" (:site-verified? a)) "現場記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :propose
     :value      {:site-id subject :items items :cost-usd cost-usd :vendor vendor}
     :stake      :order-supplies
     :confidence (if (and a (:site-verified? a)) 0.85 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :log-site-record                (log-site-record db request)
    :schedule-installation-operation (schedule-installation-operation db request)
    :flag-safety-concern             (flag-safety-concern db request)
    :order-supplies                  (order-supplies db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :propose :value {} :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは建築物のその他設置工事（断熱・吸音・昇降機/エスカレーター・"
       "その他専門設置工事）プロジェクトの運行調整（オペレーションズ・"
       "コーディネーション）エージェントの助言者です。与えられた事実のみに"
       "基づき、提案を1つだけEDNマップで返します。説明や前置きは一切書かず、"
       "EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) :effect(常に:propose) "
       ":value(提案内容のmap) "
       ":stake(:log-site-record|:schedule-installation-operation|:flag-safety-concern|"
       ":order-supplies のいずれか) :confidence(0..1)。\n"
       "重要: あなたは工事用機材を直接操作するコマンドや、設置完了サインオフを"
       "確定させる提案を絶対に作成してはいけません（現場監督/建築主事の専権事項）。"
       "登録されていない法域の要件を絶対に創作してはいけません。"
       "legal-basisが無い場合は:citesを空にしconfidenceを上げないこと。"))

(defn- facts-for [st {:keys [subject]}]
  {:site (store/site st subject)})

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Installation Governor
  escalates/holds -- an LLM hiccup can never auto-schedule an operation,
  auto-flag (or suppress) a safety concern, or auto-order supplies."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :propose))
          (update :value #(or % {})))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :propose :value {} :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :advisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
