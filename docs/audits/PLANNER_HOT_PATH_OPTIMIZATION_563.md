# Exalted.563 — otimização medida do hot path do planner

Base: Exalted.562, confirmado compilado.

A inspeção do caminho quente encontrou duas alocações/recomputações estruturais
que não carregavam estado variável:

1. `EncounterCanonicalPlanner.score` reconstruía tags canônicas dos candidatos
   adquiridos em cada avaliação de cada estado do beam, consultando novamente o
   grafo.
2. `EncounterBuildPlanner` materializava um Map completo de elegibilidade para
   cada estado, embora ele fosse apenas iterado uma vez.

Correções:
- tags dos candidatos e nomes canônicos são preparados uma vez antes da busca;
- o score consulta o cache imutável;
- a enumeração de elegíveis passa diretamente por Sequence -> forEach, sem Map
  temporário intermediário.

Não foram alterados beamWidth, maxSteps, score, futureScore, legalidade,
desempate, Focus, Archetype, Sorcery ou RNG. Portanto a otimização reduz trabalho
estrutural sem reduzir espaço de busca ou qualidade.

Engineering regressions verificam que o cache permanece fora do hot path e que
o Map temporário não seja reintroduzido.
