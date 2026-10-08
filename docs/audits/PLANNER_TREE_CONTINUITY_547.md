# Exalted.547 — continuidade de árvore no Planner

Base estável: Exalted.546.

Pacote amplia o ciclo Planner/qualidade sem alterar legalidade.

- PreparedEncounterCatalog ganha índice reverso de pré-requisitos diretos.
- O crescimento futuro deixa de varrer todos os candidatos para cada aquisição:
  examina somente descendentes diretos reais da árvore.
- O RulesEngine ainda confirma se o descendente efetivamente passa de bloqueado
  para disponível; o índice não concede legalidade.
- O limite de crescimento futuro da Exalted.545/546 permanece em +3.
- A semântica corrigida de unlockedByPreviousSteps recebe regressão explícita.
- Teste garante que irmãos sem dependência não são tratados como descendentes.

Resultado esperado: melhor continuidade de árvore, menos avaliações redundantes e
uma base mais limpa para distinguir qualidade presente de progressão futura.
