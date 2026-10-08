# Exalted.566 — CPU safety hardening

Base: Exalted.565, confirmado compilado.

Foram adicionados tetos determinísticos de trabalho aos dois caminhos de busca
combinatória centrais, sem usar tempo de relógio e sem reduzir os limites normais.

- EncounterBuildPlanner: máximo de 1.000.000 avaliações de candidatos por plano,
  além dos limites existentes de profundidade e beam.
- EncounterCharmRouteOptimizer: orçamento de 100.000 unidades, consumido por
  callbacks de elegibilidade e avaliações de candidatos.
- Ao atingir o orçamento, a expansão termina controladamente e usa a melhor
  informação já calculada, em vez de continuar consumindo CPU.
- EncounterCpuSafetyBudgetTest força orçamento muito pequeno para verificar
  término controlado e integra o Engineering Gate.

O roadmap já conserva seu limite de segurança de 128 passos. Não foram
alterados pesos, legalidade, RNG, Focus, Archetype, Sorcery, XP ou UI.
