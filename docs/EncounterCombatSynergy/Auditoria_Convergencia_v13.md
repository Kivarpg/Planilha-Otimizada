# Auditoria de convergência v13

A v13 partiu do pacote v12 persistido e auditou a consistência ENTRE as primitivas.

## Rodada 1 — semântica de conhecimento

Falha: havia duas semânticas legítimas, mas não explicitamente separadas:
- legalidade com universo completo: ausência = FALSE;
- extração/análise parcial: ausência = UNKNOWN.

Misturar ambas pode tanto liberar quanto bloquear rotas incorretamente.

Correção: `EncounterRequirementContext` declara `CLOSED_WORLD` ou `OPEN_WORLD`.
UNKNOWN nunca satisfaz uma exigência de legalidade.

## Rodada 2 — atomicidade

Falha: uma transição composta pode ter várias mutações. Se a pré-condição falha,
nenhuma mutação pode sobreviver parcialmente.

Correção: `EncounterStateTransaction` aplica mudanças atomicamente. FALSE e UNKNOWN
não persistem mutações.

## Rodada 3 — upgrades e escolhas exclusivas

Falha: `EncounterPowerProjection` sabe ADD/REPLACE/REMOVE, mas isoladamente não sabe
se dois deltas pertencem a opções mutuamente exclusivas.

Correção: `EncounterUpgradeChoiceResolver` filtra deltas pela configuração antes da
projeção. Assim, upgrades de variantes alternativas não vazam uns para os outros.

## Rodada 4 — namespace de fatos

Falha: strings iguais podiam representar conceitos distintos:
- possuir um Charm;
- Charm estar ativo;
- estado existir;
- trait de build;
- equipamento;
- regra.

Correção: `EncounterFactNamespace`.

Isso impede, por exemplo, que `OWNED_POWER:FORM_X` satisfaça acidentalmente um requisito
que exige `ACTIVE_POWER:FORM_X`.

## Rodada final

Reauditados:
domínio, aquisição, Type, timing, duração por efeito, variantes, configuração,
transições, recursos, modificadores, ator, stacking, caps, quantidades, alvo,
precedência, overrides, fatos revogáveis, tri-state, derivação, hipergrafo,
upgrades, resets, ledger causal, horizonte, shared prefixes e hubs genéricos.

Nenhuma nova falha estrutural demonstrável apareceu após a quarta correção.

O próximo passo correto não é inventar mais abstrações: é corpus testing contra os
Charms reais. Contraexemplos reais podem então revelar novas estruturas necessárias.
