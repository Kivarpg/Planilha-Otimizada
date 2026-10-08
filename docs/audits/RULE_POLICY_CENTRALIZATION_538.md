# Exalted.538 — centralização de prioridade no Sangue de Dragão

Base: Exalted.537, compilada com sucesso no GitHub.

A auditoria pós-.537 encontrou uma concatenação ad-hoc relevante no gerador de
Sangue de Dragão para distribuição de Pontos de Bônus. Ela misturava Foco,
Secundária, Favorecidas, Aspecto, combate, defesa e suporte numa única lista,
fora da política central.

A .538 substitui esse ranking local por `EncounterRulePolicy.resolvePriority` +
`EncounterArchetypePolicy.abilityPriority`, alinhando o Sangue de Dragão ao
contrato já usado pelo Solar:
- intenção explícita primeiro;
- direção do Arquétipo depois;
- Aspecto/Favorecidas participam como preferências legais;
- deduplicação central preserva a primeira precedência.

Foi adicionado teste determinístico garantindo que Foco explícito não seja
deslocado por Aspecto/Favorecida.

Nenhuma regra de legalidade, custo de XP, Charm, Feitiçaria ou quantidade de
pontos foi alterada.
