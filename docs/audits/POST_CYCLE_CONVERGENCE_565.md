# Exalted.565 — convergência pós-ciclo

Base: Exalted.564, confirmado compilado.

A rodada pós-ciclo consolida duas invariantes transversais:
- empate automático puro × Feitiçaria continua escolhendo a construção pura;
- crescimento futuro permanece limitado por `MAX_FUTURE_GROWTH_BONUS`.

`EncounterPostCycleConvergenceTest` protege as duas em conjunto e passa a ser
exigido explicitamente pelo Engineering Gate, incluindo o teste que fiscaliza
a própria configuração do workflow.

Nenhuma regra de geração, score, RNG, beam, Focus, Archetype, catálogo ou UI
foi alterada.
