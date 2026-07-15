# cloud-itonami-4329

Open Business Blueprint for **ISIC Rev.5 4329**: other construction
installation (thermal/acoustic insulation, sound-proofing, elevator/
escalator installation, and other specialty building installation work
not elsewhere classified -- distinct from 4321 electrical installation
and 4322 plumbing, heat and air-conditioning installation).

This repository designs a forkable OSS business for building-installation-
project operations coordination: run by a qualified operator so a
community keeps its own operating records instead of renting a closed
SaaS.

## Scope -- this is a COORDINATION-ONLY actor, not equipment control

This is a safety-relevant domain: fall hazards (attic/roof/elevator-
shaft work), materials hazards (insulation-fiber exposure -- fiberglass
or mineral-wool dust, or legacy asbestos-containing insulation in older
buildings), installation-completion sign-off. **This actor does NOT hold
trade-equipment-control authority, and it does NOT hold installation-
completion-sign-off authority.** Both are the site supervisor / building
official's exclusive authority, always. The Installation Advisor (LLM)
never issues a trade-equipment-control command and never finalizes an
installation-completion sign-off; the independent **Installation
Governor** HARD-blocks any proposal that even tries (un-overridable by
any human approval -- see `installation.governor` ns docstring). This
actor coordinates *potential* trade-crew/equipment dispatch (a proposed
schedule window, a flagged concern, a supply-order proposal) -- it never
directly actuates.

Structurally, EVERY proposal this actor's advisor can produce carries
`:effect :propose`, and the Installation Governor HARD-holds any
proposal that doesn't -- this is a permanent invariant distinguishing
this actor from `cloud-itonami-isic-4211` (the robotics-premise
reference this actor follows structurally), whose sibling actuation ops
DO commit real-world effects. `cloud-itonami-isic-4211`'s README
robotics-premise framing therefore does NOT apply verbatim here: this
actor is deliberately narrower, following the same coordination-only
pattern `cloud-itonami-isic-4330` (building completion and finishing)
established.

## Core Contract

```text
site record + independent verification
        |
        v
Advisor -> Installation Governor -> proceed (log/schedule/flag/order proposal), hold, or human approval
        |
        v
coordination artifacts (schedule proposal, safety-concern flag,
supply-order proposal) + audit ledger -- NEVER trade-equipment dispatch,
NEVER an installation-completion sign-off
```

No automated advice can propose a schedule the governor refuses, suppress
a safety-concern flag, or slip a trade-equipment-control/installation-
completion-sign-off marker past the governor -- and a flagged safety
concern always needs a human sign-off (see `Actuation` below).

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `4329`). Required capabilities:

- `:identity`
- `:forms`
- `:audit-ledger`
- `:notifications`

## Implemented slice (`src/installation`)

`blueprint.edn` names the governor `:installation-governor` and is now
`:implemented`. This repo implements it end-to-end -- **Installation
Advisor ⊣ Installation Governor** -- following the SAME `.cljc` actor
pattern (langgraph-clj StateGraph, mock-by-default advisor, dual
MemStore/Datomic backend, 0→3 phase rollout) every prior
`cloud-itonami-isic-*` actor in this fleet uses, structured after
[`cloud-itonami-isic-4330`](https://github.com/cloud-itonami/cloud-itonami-isic-4330)
(the building-completion/finishing coordination-only reference),
narrowed to coordination-only authority as described above and adapted
to the specific hazard/trade profile of other-construction-installation
work (see `Actuation` below for the one structural similarity shared
with that reference).

This repo is fully portable `.cljc` with **no JVM interop anywhere in
`src/`** -- `installation.notify`'s real-transport seam (`fn-notifier`)
takes caller-injected plain functions instead of embedding a
`java.net.http` client, so the actor runs unmodified on JVM Clojure,
ClojureScript, `nbb`, and `kotoba wasm`/`clojurewasm`.

### Closed op-allowlist (4 ops, all `:effect :propose`)

| Op | Ask | Implementation |
|---|---|---|
| `:log-site-record` | trade-progress / material-usage / hazmat-assessment data logging | Normalizes and commits a patch onto the site's ground-truth fields (`:site-verified?`, `:hazmat-survey-completed?`, `:scaffold-working-height-m`, `:fall-protection-installed?`, concern resolution, etc.) and appends an immutable site-record-log entry. No direct capital/safety risk -- MAY auto-commit at phase 3. |
| `:schedule-installation-operation` | thermal/acoustic-insulation, sound-proofing, elevator/escalator-installation or other specialty-installation scheduling proposal | Drafts a proposed work WINDOW (never a trade-equipment-dispatch command or an installation-completion sign-off). Escalates when the governor is not clean or confidence is low; MAY auto-commit at phase 3 when clean and confident -- see `Actuation`. |
| `:flag-safety-concern` | surface an insulation-fiber-exposure materials hazard (fiberglass/mineral-wool dust, or legacy asbestos-containing insulation) / structural / fall-protection concern | Drafts a safety-concern flag; ALWAYS escalates to a human, unconditionally. Once approved, `installation.notify` sends the notice (mail + phone) to the site's supervisor/safety-officer contact roster. |
| `:order-supplies` | materials/equipment procurement proposal | Drafts a supply-order proposal. Escalates above a cost threshold or below the confidence floor; may auto-commit at phase 3 otherwise. |

**Legal basis is data, not code** -- `src/installation/facts.cljc`'s
`catalog` is the per-jurisdiction EDN source-of-truth the governor checks
every `:schedule-installation-operation` proposal against (JPN/USA/DEU
seeded; DEU stands in for the EU, the same convention
`demolition.facts`/`finishing.facts`/`construction.facts`/`aerospace.
facts` use for EASA). Every citation below was independently verified
against its official source before being written:

| Jurisdiction | Pre-work hazmat-survey legal basis | Fall-protection legal basis |
|---|---|---|
| 🇯🇵 Japan | 石綿障害予防規則（平成17年厚生労働省令第21号）第3条 -- [e-Gov](https://laws.e-gov.go.jp/law/417M60000100021) | 労働安全衛生規則第518条（高さ2m以上で作業床の設置義務）-- [e-Gov](https://laws.e-gov.go.jp/law/347M50002000032) |
| 🇺🇸 USA | 29 CFR 1926.1101 (OSHA Asbestos standard, Construction -- a competent person must conduct an initial exposure assessment immediately before or at the initiation of work that may disturb asbestos-containing material) -- [osha.gov](https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.1101) | 29 CFR 1926.501 (OSHA, 6ft/1.8m fall-protection trigger) -- [osha.gov](https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.501) |
| 🇪🇺 EU (DEU proxy) | Directive 2009/148/EC Art.11 + GefStoffV/TRGS 519 -- [EUR-Lex](https://eur-lex.europa.eu/eli/dir/2009/148/oj) | TRBS 2121 (qualitative -- risk-assessment-based, no fixed EU-wide numeric trigger) -- [BAuA](https://www.baua.de/DE/Angebote/Regelwerk/TRBS/TRBS-2121) |

Japan (2m) and the USA (6ft/1.8m) have real numeric fall-protection
trigger heights; the EU deliberately does NOT --
`installation.facts/fall-protection-noncompliant?` reports `:qualitative`
there rather than fabricating a number. The USA hazmat-survey basis
deliberately cites OSHA's asbestos construction standard rather than a
surface-coating rule -- asbestos-containing legacy insulation
(vermiculite attic fill, pipe/duct lagging) is the domain-appropriate
hazmat concern for installation/retrofit work. See `installation.facts`
ns docstring for the full honesty discipline.

**Governor -- eight HARD checks, ALL un-overridable by human approval:**
unknown op (outside the closed 4-op allowlist), `:effect` not `:propose`,
forbidden action class (trade-equipment-control / direct-actuation /
installation-completion-sign-off-finalization markers), site not
independently verified/registered, legal-basis missing, pre-work hazmat
survey incomplete, fall-protection noncompliant (quantitative
jurisdictions only), unresolved safety concern on file. See
`installation.governor` ns docstring for the full enumeration, rationale
and real-law citations behind each.

## Actuation

This actor performs **no real-world actuation** -- every committed
record carries `:effect :propose` (see `installation.governor` ns
docstring). `:flag-safety-concern` NEVER auto-commits at any phase -- it
always needs a human sign-off, even when the governor is completely
clean (`installation.phase` ns docstring 'Actuation' section,
`installation.governor`'s `high-stakes` set).

**Like `cloud-itonami-isic-4330` and UNLIKE `cloud-itonami-isic-4311`/
`cloud-itonami-isic-4210`:** `:schedule-installation-operation` here is
NOT a permanent `high-stakes` member. `:log-site-record`, `:schedule-
installation-operation` and `:order-supplies` BELOW the cost threshold
(`installation.governor/supply-order-cost-threshold-usd`) MAY all auto-
commit at phase 3 when the governor is clean and confidence is high.
Thermal/acoustic-insulation, sound-proofing, elevator/escalator and
other specialty-installation scheduling does not coordinate potential
HEAVY-equipment dispatch near structural-collapse / public-traffic /
buried-utility risk the way demolition or road/rail earthwork scheduling
does -- it is a materially lower-stakes coordination artifact. The eight
HARD governor checks still apply UNCONDITIONALLY regardless of phase;
only the routing to a human vs. auto-commit changes.

```bash
clojure -M:dev:run    # demo: full coordination episode + every HARD hold
clojure -M:dev:test   # test suite
clojure -M:lint       # clj-kondo, errors fail
```

## License

AGPL-3.0-or-later.
