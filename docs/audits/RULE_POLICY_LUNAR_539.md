# Exalted.539 — centralização da prioridade Lunar

Base estável: Exalted.538, compilada com sucesso no GitHub.

A varredura do Ciclo 1 encontrou a prioridade inicial de Encantos Lunares ainda
montada localmente por concatenação de Foco + prioridade do Arquétipo.

A .539 encaminha essa decisão por `EncounterRulePolicy.resolvePriority`, mantendo
a mecânica existente:
- Foco explícito lidera o ranking quando existe;
- o Arquétipo Lunar continua fornecendo a ordem de Atributos;
- Casta/Favorecidos continuam entrando por `LunarArchetypePolicy`;
- duplicatas são removidas pela política central.

Foi adicionado teste determinístico para garantir precedência do Foco Lunar e
deduplicação quando o Atributo de Foco também aparece entre Casta/Favorecidos.

Não foram alteradas legalidade de Encantos, Forma Espiritual, Quimera, custos,
quantidades de pontos ou regras de Feitiçaria.
