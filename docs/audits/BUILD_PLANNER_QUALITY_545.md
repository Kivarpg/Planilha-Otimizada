# Exalted.545 — Planner: valor atual e crescimento futuro

Base estável: Exalted.544.

Início do ciclo de qualidade do planejador.

- `EncounterBuildPlanner.Candidate` passa a aceitar `futureScore` opcional.
- `PlannedAcquisition` registra separadamente valor atual e futuro.
- `EncounterBuildQuality` limita crescimento futuro a +3, impedindo que uma
  opção ruim hoje vença apenas por abrir muitas possibilidades.
- `EncounterCanonicalPlanner` mede crescimento pela quantidade de candidatos
  que passam de indisponíveis para disponíveis após a aquisição.
- O bônus é limitado a três desbloqueios.
- `unlockedByPreviousSteps` agora significa que o item não estava disponível
  no estado inicial e tornou-se alcançável durante o plano, em vez de apenas
  indicar que ele não foi o primeiro passo.
- Três testes determinísticos cobrem limite, valor negativo e composição.

Legalidade continua exclusiva do RulesEngine; o Planner apenas ordena opções
legais. Nenhuma regra de custo, XP ou criação inicial foi alterada.
