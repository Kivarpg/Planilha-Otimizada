# Exalted.546 — correção de compatibilidade do Candidate

Base funcional: Exalted.545.

O código principal compilou, mas os testes canônicos falharam na compilação porque
`Candidate` ganhou `futureScore` depois de `score`. Em Kotlin, as chamadas antigas
`Candidate(id) { ... }` associam a trailing lambda ao último parâmetro; assim, `score`
ficou sem argumento.

Correção estrutural: `futureScore` foi movido antes de `score`, mantendo `score` como
último parâmetro. Isso preserva todas as chamadas históricas com trailing lambda e
também as chamadas novas com argumentos nomeados. Nenhuma regra mecânica ou score
foi alterado.
