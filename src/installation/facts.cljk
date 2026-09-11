(ns installation.facts
  "Per-jurisdiction other-construction-installation regulatory catalog --
  the spec-basis table the Installation Governor checks every
  `:schedule-installation-operation` proposal against ('did the advisor
  cite an OFFICIAL public source for this jurisdiction's pre-work
  hazmat-survey / at-height fall-protection requirements, or did it
  invent one?'). Same honest-coverage discipline `demolition.facts`
  (`cloud-itonami-isic-4311`) / `finishing.facts` (`cloud-itonami-isic-
  4330`) established for this fleet: a jurisdiction not in this table has
  NO spec-basis, full stop -- the advisor must not fabricate one, and the
  governor holds if it tries.

  Coverage is reported HONESTLY (see `coverage`); this is a STARTING
  catalog (JPN/USA/DEU), not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to `catalog`,
  cite a real source, done -- never invent a jurisdiction's requirements
  to make coverage look bigger.

  Two independent legal bases, both citable and both real:

    1. `:hazmat-survey-basis` -- the PRE-WORK asbestos/hazmat-material
       survey duty that applies before thermal/acoustic insulation,
       sound-proofing, elevator/escalator installation or other specialty
       building-installation work disturbs an existing built-up surface,
       cavity, attic, pipe chase or shaft in an older building (older
       vermiculite/pipe/duct insulation and other legacy building
       materials may themselves contain asbestos, and demolition of an
       existing installation to make way for a new one is squarely inside
       the SAME kind of duty `demolition.facts`/`finishing.facts` cite for
       renovation work generally -- this is not a separate carve-out).
    2. `:fall-protection-basis` -- the at-height (attic, roof, scaffold,
       elevator shaft, ladder/lift) fall-protection duty that applies once
       an installation crew (insulation install above ground level,
       elevator/escalator installation inside a shaft or well, exterior
       specialty installation, etc.) works above a jurisdiction-specific
       trigger height. This is this domain's OWN real, independently-
       recheckable numeric trigger, the SAME kind `finishing.facts`
       establishes for building-completion/finishing trade work -- the
       underlying occupational-safety-and-health law is the SAME generic
       at-height duty that applies across building trades, not something
       this actor invents for its own domain.

  `:threshold-model` mirrors the SAME honest quantitative/qualitative
  split `demolition.facts`/`finishing.facts` established:
    :quantitative -- the jurisdiction's OSH law states a fixed numeric
                     height that triggers a fall-protection duty (Japan's
                     2m under the Industrial Safety and Health Regulations;
                     the USA's 6ft/1.8m under OSHA 1926.501).
                     `fall-protection-noncompliant?` can independently
                     recompute a HARD hold from this.
    :qualitative  -- the jurisdiction imposes a documented risk-assessment
                     (Gefährdungsbeurteilung) duty with NO single fixed
                     EU-wide/federal numeric trigger height (Germany/EU,
                     TRBS 2121 under the Betriebssicherheitsverordnung --
                     the trigger height is scenario-dependent, not one
                     fixed number). This actor does NOT invent a height to
                     make this jurisdiction look automatable --
                     `fall-protection-noncompliant?` returns `:qualitative`
                     and `:schedule-installation-operation` for that
                     jurisdiction is left to the ordinary confidence-floor
                     gate (see `installation.governor` ns docstring)
                     rather than a fabricated HARD numeric rule.

  DEU is used as the EU-jurisdiction proxy, the SAME convention
  `demolition.facts`/`finishing.facts`/`construction.facts`/`aerospace.
  facts` established -- there is no ISO-3166 alpha-3 code for the EU
  itself, and the asbestos directive is transposed into national law per
  member state (here: Germany's Gefahrstoffverordnung / TRGS 519), so the
  citation lists BOTH the EU directive and its German transposition
  rather than inventing an EU country code. All citations below were
  independently verified against their official source before being
  written (osha.gov / laws.e-gov.go.jp / eur-lex.europa.eu / baua.de).
  The USA `:hazmat-survey-basis` deliberately cites OSHA's asbestos
  construction standard (29 CFR 1926.1101, which this actor independently
  confirmed requires a competent person to conduct an initial exposure
  assessment immediately before or at the initiation of any operation
  that may disturb asbestos-containing material) rather than
  `finishing.facts`'s EPA lead-paint Renovation, Repair and Painting Rule
  -- asbestos-containing legacy insulation (vermiculite attic fill,
  pipe/duct lagging) is the domain-appropriate hazmat concern for
  installation/retrofit work, distinct from the surface-coating concern
  painting/finishing work raises."
  )

(def catalog
  "iso3 -> requirement map. `:hazmat-survey-basis` / `:fall-protection-
  basis` / their `-provenance` pairs, plus `:owner-authority`, are the
  G2-style citation the governor requires before a `:schedule-
  installation-operation` proposal can ever commit."
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省（労働基準監督署）"
          :hazmat-survey-basis "石綿障害予防規則（平成17年厚生労働省令第21号）第3条（建築物・工作物の解体・改修等の作業を行う前に、石綿使用の有無について事前調査を行う義務 -- 断熱・吸音・昇降機等の設置工事に伴う既存構造物の改修も対象）"
          :hazmat-survey-provenance "https://laws.e-gov.go.jp/law/417M60000100021"
          :fall-protection-basis "労働安全衛生規則第518条（作業床の設置等）-- 高さが2メートル以上の箇所で作業を行う場合、墜落により労働者に危険を及ぼすおそれのあるときは作業床を設置し、それが困難なときは防網の設置・要求性能墜落制止用器具の使用等の措置を講じる義務"
          :fall-protection-provenance "https://laws.e-gov.go.jp/law/347M50002000032"
          :threshold-model :quantitative
          :fall-protection-trigger-height-m 2.0
          :threshold-note "高さ2メートル以上の箇所での作業について、作業床の設置または防網・墜落制止用器具等の代替措置が義務（労働安全衛生規則第518条）"}
   "USA" {:name "United States"
          :owner-authority "Occupational Safety and Health Administration (OSHA), U.S. Department of Labor"
          :hazmat-survey-basis "29 CFR 1926.1101 (OSHA Asbestos standard, Construction -- a 'competent person' must conduct an initial exposure assessment immediately before or at the initiation of any operation covered by the standard, e.g. work that may disturb legacy asbestos-containing insulation such as vermiculite attic fill or pipe/duct lagging)"
          :hazmat-survey-provenance "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.1101"
          :fall-protection-basis "29 CFR 1926.501 (OSHA -- Duty to have fall protection, Subpart M: employees on a walking/working surface with an unprotected side or edge at 6 feet (1.8 m) or more above a lower level must be protected by guardrail systems, safety net systems, or personal fall arrest systems)"
          :fall-protection-provenance "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.501"
          :threshold-model :quantitative
          :fall-protection-trigger-height-m 1.8
          :threshold-note "6 feet (1.8 m) or more above a lower level triggers the OSHA 1926.501 fall-protection duty for construction/installation work."}
   "DEU" {:name "Germany (EU jurisdiction proxy, see ns docstring)"
          :owner-authority "Bundesministerium für Arbeit und Soziales (BMAS) / Bundesanstalt für Arbeitsschutz und Arbeitsmedizin (BAuA); EU level: European Agency for Safety and Health at Work (EU-OSHA)"
          :hazmat-survey-basis "Directive 2009/148/EC (protection of workers from the risks related to exposure to asbestos at work, codified version) Art.11 -- a documented plan of work must be drawn up before demolition or asbestos-removal/renovation work is started; nationally transposed via the Gefahrstoffverordnung (GefStoffV) and TRGS 519 (Technical Rule for Hazardous Substances -- Asbestos: demolition, reconstruction or maintenance work)"
          :hazmat-survey-provenance "https://eur-lex.europa.eu/eli/dir/2009/148/oj"
          :framework-provenance "https://www.baua.de/EN/Service/Technical-rules/TRGS/TRGS-519"
          :fall-protection-basis "TRBS 2121 (Technische Regel für Betriebssicherheit -- Gefährdung von Beschäftigten durch Absturz), issued by BAuA under the Betriebssicherheitsverordnung (BetrSichV) -- requires a documented risk assessment (Gefährdungsbeurteilung) for fall hazards; the required height at which technical fall-protection measures apply is SCENARIO-DEPENDENT (assessment-based), not one single fixed federal/EU-wide numeric trigger the way JPN/USA have."
          :fall-protection-provenance "https://www.baua.de/DE/Angebote/Regelwerk/TRBS/TRBS-2121"
          :threshold-model :qualitative
          :fall-protection-trigger-height-m nil
          :threshold-note "EU/ドイツの墜落防止規則（TRBS 2121、BetrSichVに基づく）は危険性評価（Gefährdungsbeurteilung）に基づく措置義務を課すのみで、日本の2m・米国の6ft(1.8m)のような固定の数値トリガーはEU全域では法定されていない -- ここで数値を創作しない。"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any `:schedule-installation-operation`
  proposal that tries to cite one."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4329 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `installation.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn fall-protection-noncompliant?
  "Independently recompute whether `site`'s own recorded ground-truth
  fields -- `:scaffold-working-height-m` (how high the installation crew
  is actually working -- attic, roof, elevator shaft, exterior wall,
  etc.) and `:fall-protection-installed?` (whether a guardrail/net/
  personal-fall-arrest measure is actually in place) -- leave the site
  out of compliance with `iso3`'s fall-protection trigger.

  Three-valued, deliberately (the same shape
  `demolition.facts/notification-lead-insufficient?` /
  `finishing.facts/fall-protection-noncompliant?` established):
    true         -- a :quantitative jurisdiction (Japan, USA) whose own
                    numeric trigger height is independently confirmed MET
                    OR EXCEEDED by the site's own recorded actual working
                    height, AND no fall-protection measure is recorded as
                    installed -- a bright-line legal violation. The
                    Installation Governor turns this into a HARD,
                    un-overridable hold on `:schedule-installation-
                    operation`.
    false        -- either below the trigger height, or at/above it with
                    a fall-protection measure already recorded installed.
    :qualitative -- a jurisdiction with NO fixed numeric trigger (DEU/EU).
                    This actor cannot independently confirm
                    'compliant'/'noncompliant' by arithmetic alone -- the
                    law itself requires a documented risk-assessment
                    judgment call. Never fabricate a trigger height here.
    nil          -- no spec-basis at all for `iso3` (a jurisdiction not in
                    `catalog`)."
  [iso3 {:keys [scaffold-working-height-m fall-protection-installed?]}]
  (when-let [{:keys [threshold-model fall-protection-trigger-height-m]} (spec-basis iso3)]
    (case threshold-model
      :quantitative
      (boolean (and (number? scaffold-working-height-m)
                    (>= scaffold-working-height-m fall-protection-trigger-height-m)
                    (not (true? fall-protection-installed?))))
      :qualitative
      :qualitative
      nil)))
