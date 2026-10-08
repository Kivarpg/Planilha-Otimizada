# Auditoria estrutural v10

## Falha corrigida — precedência incidental de modificadores

A v9 já representava modificadores de custo, mas múltiplos modificadores podiam ser
aplicados conforme a ordem incidental da coleção.

Isso é estruturalmente incorreto porque, em geral:
- reduzir e depois limitar pode produzir valor diferente de limitar e depois reduzir;
- substituir e depois reduzir difere de reduzir e depois substituir;
- uma exceção pode substituir a regra geral;
- uma recompra pode modificar apenas determinada camada.

A v10 introduz `EncounterModifierPrecedence` e `EncounterSafeResourceModifierResolver`.

Sem precedência explícita:
`AMBIGUOUS`, não um número inventado.

Com ciclo:
`CYCLE`, não desempate arbitrário.

## IDs tipados

A auditoria anterior já apontava risco de strings livres para traits/mecânicas.
A v10 inicia a migração com:
- EncounterTraitId
- EncounterMechanicId
- EncounterEffectId
- EncounterPowerId

A migração completa fica incremental para não quebrar o protótipo inteiro de uma vez.

## Nova auditoria

### Falha descoberta — regra específica versus regra geral não é apenas "ordem"

Há casos em que uma regra específica não deve ser aplicada depois da geral:
ela a SUBSTITUI. Portanto, precedência e override são conceitos diferentes.

O modelo já possui `REPLACE_COST`, mas a arquitetura madura precisa de
`RuleResolution` com:
- APPLIES_AFTER
- OVERRIDES
- SUPPRESSES
- COEXISTS

Isso deve valer não apenas para custos, mas para restrições, Type, stacking e duração.

### Falha descoberta — exceção pode ter escopo estreito

"Pode ser usado com Charm X" não significa que todas as demais restrições entre os
dois foram abolidas. Uma exceção precisa declarar QUAL regra ela substitui.

A representação futura deve apontar para um `RuleId`, evitando exceção global.

## Correção conservadora já aplicada

A v10 não promove ambiguidades de precedência. Assim, as duas falhas acima não
produzem atualmente um falso positivo integral: permanecem não resolvidas.

A próxima camada estrutural madura deve ser um `RuleResolutionEngine` tipado,
antes de calibrar pesos.
