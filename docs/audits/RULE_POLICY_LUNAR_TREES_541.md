# Exalted.541 — ordem de árvores Lunares sob política central

Base estável: Exalted.540.

Centraliza em `EncounterRulePolicy.resolvePriority` quatro composições locais de
ordem em `LunarEncounterCharmSelection`: árvores do mesmo Arquétipo, fallback
fora do Arquétipo, árvore ativa inicial e preenchimento final.

A ordem mecânica anterior é preservada: intenção calculada primeiro, fallback
depois, primeira ocorrência vence e duplicatas são removidas. Não muda
aprofundamento de árvore, vagas, requisitos, Feitiçaria, Forma Espiritual ou
Quimera.

Inclui teste determinístico para Foco + fallback sem duplicação.
