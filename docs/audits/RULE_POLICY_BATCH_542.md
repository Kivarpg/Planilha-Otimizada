# Exalted.542 — pacote ampliado de convergência da política central

Base estável: Exalted.541.

A pedido do usuário, esta versão agrupa um pacote maior de mudanças seguras,
mantendo o princípio de não sacrificar estabilidade.

Centralizações realizadas:
1. Solar: composição das 5 Favorecidas.
2. Solar: Habilidades estruturais relevantes da distribuição.
3. Sangue de Dragão: Habilidades estruturais relevantes.
4. Sangue de Dragão: união Aspecto + Favorecidas para Pontos de Bônus.
5. Sangue de Dragão: mesma união no ranking do Arquétipo.
6. Sangue de Dragão: prioridade para Especialidade adicional.
7. Sangue de Dragão: Aspecto + Favorecidas na seleção inicial de Encantos.
8. Lunar: inserção do Foco entre Atributos Favorecidos.
9. Lunar: união Casta + Favorecidos.
10. Seletor de Encantos: prioridade + catálogo completo de Habilidades.
11. Excelências: árvores já usadas + árvores com Excelência.

Todas passam por `EncounterRulePolicy.resolvePriority`, preservando a primeira
ocorrência e removendo duplicatas. Não foram centralizadas coleções cujo
`distinct()` representa validação, auditoria, orçamento de recursos ou histórico,
pois não são rankings.

Novos testes cobrem estrutura/fallback e sinergia/fallback.

Sem mudanças deliberadas em custos, XP, legalidade de Encantos, quantidade de
pontos, Forma Espiritual, Quimera ou Feitiçaria.
