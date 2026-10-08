# Exalted.557 — seleção de candidato de Feitiçaria

Checkpoint de base: Exalted.556, confirmado compilado.

Esta etapa formaliza a fronteira entre exploração aleatória e seleção por qualidade.
`EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate` recebe dois scores já
produzidos pelo modelo canônico `EncounterBuildQuality`.

Regras:
- rota não explorada ou não construível nunca é selecionada;
- Focus Feitiçaria explícito seleciona a rota quando ela é legal;
- exploração automática não força Feitiçaria;
- no automático, Feitiçaria precisa ter qualidade total estritamente maior;
- empate preserva o candidato puro;
- nenhum RNG adicional é consumido.

Foram adicionados cinco testes determinísticos cobrindo essas invariantes.

Esta versão cria a política pura e testável. A materialização Solar/DB ainda usa a
fronteira existente; a próxima etapa deve produzir e comparar os dois candidatos
reais antes de conectar esta política à materialização. Isso evita introduzir
pesos arbitrários ou alterar a sequência do Random.
