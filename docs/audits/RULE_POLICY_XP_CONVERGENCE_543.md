# Exalted.543 — convergência ampliada da progressão por XP

Base estável: Exalted.542.

Este pacote fecha uma duplicação importante entre geração inicial e progressão:
a prioridade de Habilidades/Atributos usada ao comprar Encantos por XP agora
possui uma fachada central em `EncounterRulePolicy`.

Mudanças:
- `abilityPriorityFor(npc)` centraliza a prioridade de uma ficha construída;
- `lunarAttributePriorityFor(npc)` normaliza Casta/Favorecidos Lunares antes do ranking;
- três rotas Solar de progressão usam a fachada;
- rotas Sangue de Dragão de progressão usam a fachada;
- `EncounterXpExpanderFactory` usa a fachada Lunar;
- as duas rotas Lunares da fachada `EncounterGenerator` usam a mesma política;
- testes de contrato cobrem deduplicação e precedência.

A varredura também examinou `EncounterDistributionService`: a composição local
ali inclui embaralhamento seguido de ordenação estável e, portanto, não foi
substituída mecanicamente por `resolvePriority`; isso evita alterar variedade
determinística/semântica sem um teste específico.

Sem mudança deliberada em custo de XP, requisitos, quantidade de Encantos,
Feitiçaria, Forma Espiritual ou Quimera.
