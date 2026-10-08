# Pacote de desenvolvimento — EncounterCombatSynergy

Este pacote nasceu como referência experimental. A partir da Exalted.380, por decisão explícita do projeto, seus componentes foram incorporados ao código da aplicação e passaram a exigir validação pelos testes do projeto.

Objetivo: concentrar em um único artefato o estado de desenvolvimento do afinador de escolhas de Encantos e da proposta de integração futura com o roadmap.

## Conteúdo
- `design/Mudancas_EncounterCombatSynergy_Roadmap.md`: especificação consolidada das mudanças propostas.
- `reference_code/EncounterCombatSynergy.kt`: origem de referência do componente que passou a ser integrado à aplicação a partir da Exalted.380.
- `reference_code/EncounterCharmRouteOptimizer.kt`: implementação de referência associada.
- `tests/`: testes de referência existentes para continuar a maturação.

## Regra de domínio
Solar, Sangue de Dragão e Lunar são domínios nativos mutuamente exclusivos. Artes Marciais e Feitiçaria podem complementar um domínio quando legalmente acessíveis. Encantos nativos dos três tipos de Exaltado nunca devem ser combinados entre si.

## Fronteira de responsabilidade
O sistema afina escolhas de construção e ajuda a selecionar linhas evolutivas de Encantos. Ele não executa, simula ou decide combate.

## Estado
Origem experimental preservada para rastreabilidade. No projeto atual, a integração iniciada na Exalted.380 é tratada como código da aplicação e deve permanecer coberta por regressões.


## Correção v3 — Type

Foi adicionada uma barreira explícita de compatibilidade temporal baseada em `Type`.

Em particular, dois `Simple` não podem mais ser pontuados como uma combinação de mesma rodada. Relações entre dois `Simple` só sobrevivem como afinidade de construção ou como sequência entre rodadas quando o estado produzido realmente persiste.

A avaliação futura deve obedecer:
`afinidade mecânica → Type → janela → duração → realizabilidade`.


## Correção v4 — requisitos compostos e duração por efeito

- Requisitos passam a preservar AND/OR/ONE_OF/NOT.
- Requisito parcial não gera acesso ou sinergia.
- A duração global do Charm não é mais usada como prova da duração de cada efeito.
- `CROSS_ROUND` exige um efeito produtor específico cuja vida útil realmente atravesse a rodada.
- Duração desconhecida não é promovida por inferência otimista.


## Consolidação v5

Adicionados `PowerVariant`, `EffectBranch`, configuração consistente,
`EncounterCombinationEvaluator` e `EncounterSynergyEvidenceReducer`.

A v5 impede que:
- modos alternativos de um mesmo Charm sejam somados;
- variantes inaplicáveis vazem efeitos;
- duas configurações incompatíveis sejam tratadas como simultâneas;
- a mesma causa de sinergia seja pontuada repetidamente.

Uma segunda auditoria identificou a próxima fronteira: tipar o escopo dos compromissos
(FIXED_BUILD / LOADOUT / SCENE / ROUND / ACTION / TRANSIENT), para diferenciar
incompatibilidade simultânea de simples capacidade de alternar configuração.


## Correção v6 — compromissos e transições

A v6 tipa compromissos como FIXED_BUILD, LOADOUT, SCENE, ROUND, ACTION ou TRANSIENT.
Configurações alternativas não são mais conectadas implicitamente: uma troca exige
`TransitionRule` explícita e pode carregar custo estrutural.

Isso elimina a possibilidade de o avaliador colher sinergias de várias configurações
como se a build pudesse alternar gratuitamente entre elas.

A auditoria posterior identificou como próxima fronteira a modelagem quantitativa
de orçamento de recursos, mantendo-a estritamente como análise estática de construção.


## Correção v7 — orçamento estrutural de recursos

Adicionados `EncounterResourceBudget` e `EncounterResourceModifierResolver`.

O modelo diferencia gasto, compromisso, geração, transferência, conversão e limiar mínimo,
sem transformar isso em simulação de combate. Motes normais e motes de feitiçaria permanecem
recursos distintos.

A auditoria da própria correção encontrou e já eliminou uma segunda falha: custos impressos
podem ser substituídos/reduzidos/dispensados por regras explícitas. Esses modificadores agora
possuem representação própria; reembolsos não são abatidos automaticamente porque dependem
de realização causal.


## Correção v8 — ator causal e stacking

A v8 elimina dois novos falsos positivos estruturais:

1. efeitos de aliados, familiares, summons, battle groups, alvos ou entidades criadas
não são mais tratados como produção do próprio NPC;
2. efeitos positivos não são automaticamente somados quando usam MAX_OF, substituição,
não-stacking ou teto compartilhado.

Dependências externas continuam podendo ser coerentes com a build, mas não recebem
crédito de autossustentação. Stacking desconhecido permanece incerto em vez de ser
resolvido otimisticamente.


## Correção v9 — expressões quantitativas e alvos

Caps e magnitudes dependentes da ficha agora podem ser representados por
`EncounterQuantityExpression` e resolvidos contra valores estáticos do BuildState.
Valores dependentes de rolagem permanecem fora do módulo.

Durante a implementação foi encontrada e corrigida uma falha de segunda ordem:
um cap declarado mas não resolvido podia desaparecer e permitir soma indevida.
Agora cap não resolvido força resultado quantitativo incerto.

`EncounterTargetShape` foi adicionado apenas para compatibilidade estrutural;
cardinalidade de alvo não vira valor tático.


## Correção v10 — precedência e resolução de regras

Múltiplos modificadores não usam mais ordem incidental. Precedência ausente ou cíclica
permanece não resolvida.

A auditoria dessa correção revelou outra distinção e ela também foi implementada:
`OVERRIDES/SUPPRESSES` não é o mesmo que `APPLIES_AFTER`. Exceções agora podem apontar
para a regra específica que substituem, evitando que uma permissão estreita elimine
restrições não relacionadas.

Foi iniciada ainda a migração para IDs tipados de traits, mecânicas, efeitos e poderes.


## Correção v11 — fatos revogáveis e verdade tri-state

A v11 elimina fatos fantasmas no `ProjectedBuildState`. Estados derivados agora carregam
fonte, lifetime e exclusividade, podendo ser consumidos, expirados ou revogados.

Também separa FALSE de UNKNOWN na extração semântica; em particular, `NOT(UNKNOWN)`
permanece UNKNOWN.

A auditoria encontrou ainda invalidação transitiva e ciclos de derivação. Eles foram
corrigidos na mesma rodada com recomputação por ponto fixo a partir das fontes-base
restantes: ciclos sem suporte externo não se autossustentam.


## v12 — convergência estrutural

A v12 executa múltiplas rodadas internas de auditoria e corrige:
- hipergrafo lógico de aquisição;
- projeção efetiva de upgrades/recompras;
- disponibilidade/reset sem frequência inventada;
- ledger causal único contra dupla contagem;
- custo marginal de rotas, shared prefixes e horizonte;
- separação entre força, cobertura e foco para suprimir hubs genéricos.

A última rodada de auditoria não encontrou nova falha estrutural que justifique outra
primitiva abstrata antes de testar o modelo contra o corpus real de Charms.


## v13 — consistência entre primitivas

A auditoria de convergência encontrou quatro falhas de integração estrutural:
1. mundo fechado vs mundo aberto não estavam explicitamente separados;
2. mutações de estado não tinham transação atômica;
3. upgrades exclusivos podiam vazar para a mesma projeção;
4. fatos de posse/ativação/estado/equipamento podiam colidir semanticamente.

As quatro foram corrigidas. A rodada posterior não encontrou nova falha abstrata
demonstrável sem testar o corpus real.


## v14 — convergência, erros e limpeza

Auditoria do código real da v13, não apenas das abstrações. Foram corrigidos:
stacking entre grupos independentes, overrides contraditórios, deltas malformados,
validação de recursos, lógica completa de derivação, IDs duplicados, o otimizador
legado que ainda parseava pré-requisitos textualmente, deduplicação destrutiva de
realizações e seleção arbitrária pela quantidade de efeitos.

O código de referência terminou a rodada sem `!!`, TODO/FIXME, parsing textual de
pré-requisito no otimizador ou truncamentos conhecidos de catálogo.


## v15 — convergência ampliada

A v15 auditou estados inválidos, transições, recursos, aritmética, cardinalidade,
rotas, persistência condicional, ledger, coerência, grafo de regras, identidade de
realizações e código morto. As falhas demonstradas foram corrigidas e a última
varredura estática convergiu sem nova falha corrigível no pacote isolado.


## v16 — calibração pelos catálogos Solar, Sangue de Dragão e Lunar

Contraexemplos reais dos três catálogos calibraram `EncounterTargetShape`,
`EncounterActorScope` e `EncounterCombinationEvaluator`. O modelo agora distingue
alvo único, múltiplos alvos distintos, repetição no mesmo alvo, áreas indiscriminadas,
ator que ativa, ator que executa, beneficiário, afetado e identidade concreta de
participantes entre relações.


## v17 — convergência após calibração de corpus

A v17 auditou as interações introduzidas pela v16. Unificou bindings de participantes,
corrigiu wildcards de ator/alvo, levou tri-state ao CombinationEvaluator, preservou
efeitos condicionais inativos, implementou ActionSignature, corrigiu a semântica das
exceções explícitas de ativação e endureceu stacking/precedência.
