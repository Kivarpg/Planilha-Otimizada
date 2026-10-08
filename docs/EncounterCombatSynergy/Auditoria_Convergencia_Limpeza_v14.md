# Auditoria de convergência e limpeza v14

Base: EncounterCombatSynergy Development Pack v13.

## Rodada 1 — stacking

Erro: `MAX_OF` / `DOES_NOT_STACK` operavam sobre o canal inteiro. Um grupo competitivo
podia suprimir bônus aditivos independentes.

Correção: `stackingGroup`. Competição e cap são locais ao grupo declarado.

## Rodada 2 — regras, upgrades e recursos

Erros:
- overrides mútuos podiam produzir estado parcial dependente da ordem;
- delta ADD/REMOVE/REPLACE malformado podia ser marcado como aplicado;
- REPLACE podia criar efeito novo mesmo quando o alvo não existia;
- perfil de recursos aceitava namespace divergente, quantidade negativa,
  confidence inválida e conversão sem destino.

Correções:
- resolução contraditória permanece ambígua sem mutação parcial;
- deltas inválidos entram em `rejectedDeltaIds`;
- `EncounterResourceValidation`.

## Rodada 3 — derivação e identidade

Erro: derivação transitiva aceitava apenas `requiresAll`, perdendo OR/NOT.

Correção: `EncounterDerivedFactResolver.Derivation` usa a expressão lógica completa.

Erro: IDs duplicados de modificador eram colapsados pelo Set/Map.

Correção: `EncounterModifierPrecedence.Result.Invalid`.

Adicionado `EncounterModelIntegrity` para invariantes comuns.

## Rodada 4 — otimizador legado

Erro estrutural crítico: `EncounterCharmRouteOptimizer` ainda interpretava pré-requisitos
por texto, vírgulas e substring, contava descendentes nominais e cortava o catálogo em 256.

Correção:
- nenhum parsing textual de pré-requisito;
- valor de unlock = mudança real de `elegivel(false -> true)`;
- catálogo futuro não é truncado arbitrariamente;
- AND/OR/Bridge/Archetype permanecem responsabilidade da legalidade real;
- API histórica preservada.

## Rodada 5 — deduplicação de realizações

Erro: duas realizações com mesma configuração, mas efeitos diferentes, podiam ser
deduplicadas antes da avaliação.

Correção: identidade inclui assinatura dos efeitos.

Erro adicional: o retorno escolhia a realização com mais efeitos, introduzindo uma
preferência sem fundamento mecânico.

Correção: `Result.realizations` expõe todas as alternativas. O campo singular antigo
é mantido apenas por compatibilidade e recebe uma alternativa determinística, sem
declará-la melhor.

## Limpeza

- removido `ChoiceKind`, que não tinha uso;
- removidos `!!` evitáveis do código de referência;
- nenhuma ocorrência TODO/FIXME;
- nenhuma chave desbalanceada na verificação lexical;
- removidos padrões conhecidos de parsing textual de pré-requisito e truncamento 48/256;
- adicionados testes de regressão para cada classe de erro corrigida.

## Limitação da validação desta rodada

O ambiente disponível não contém `kotlinc` nem um projeto Gradle executável dentro deste
pacote de desenvolvimento. Portanto, os testes Kotlin foram adicionados, mas não foram
executados por compilador nesta rodada. A validação realizada foi estrutural/estática e
a integridade do ZIP foi verificada.

## Rodada final de convergência

Após as correções acima, nova busca estática não encontrou:
- parsing textual de pré-requisitos no otimizador;
- truncamento arbitrário conhecido do catálogo;
- seleção por maior quantidade de efeitos;
- `!!` no código de referência;
- `ChoiceKind` morto;
- TODO/FIXME;
- desequilíbrio lexical de chaves.

Nenhuma nova falha estrutural demonstrável foi encontrada nessa rodada final.
A próxima fonte de contraexemplos deve ser compilação no projeto real e corpus testing
contra os catálogos.
