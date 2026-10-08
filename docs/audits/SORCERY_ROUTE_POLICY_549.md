# Exalted.549 — política de exploração de Feitiçaria

Base estável: Exalted.548.

Início da revisão profunda por Tipo de Exaltado, começando pela regra compartilhada
Solar/Sangue de Dragão para exploração da rota de Feitiçaria.

- Nova EncounterSorceryRoutePolicy centraliza a decisão de explorar a rota.
- Focus explícito Feitiçaria exige exploração, independentemente do Arquétipo.
- Sem Focus explícito, somente Mental participa do sorteio automático.
- A probabilidade existente de 9/10 foi preservada exatamente.
- Solar e Sangue de Dragão deixam de duplicar a expressão de decisão.
- A política retorna também a razão da decisão, preparando observabilidade futura.
- Testes cobrem Focus explícito nos três Arquétipos, ausência de exploração automática
  em Físico/Social e equivalência determinística com a regra 9/10 existente.

Importante: explorar uma rota não significa que ela venceu uma comparação de qualidade.
Este pacote centraliza a intenção de exploração sem mudar requisitos, acesso a círculos,
seleção de Feitiços ou autoridade do RulesEngine/Auditor.
