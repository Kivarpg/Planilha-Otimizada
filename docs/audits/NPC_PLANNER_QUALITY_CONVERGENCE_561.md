# Exalted.561 — convergência de qualidade do planejamento

Base: Exalted.560, confirmado compilado.

Falha estrutural confirmada:
`EncounterCanonicalPlanner.futureScore` simulava a aquisição futura adicionando
somente `acquiredCharmIds`. O `EncounterBuildPlanner`, porém, materializa a mesma
aquisição com ID + nome. Como parte da compatibilidade de pré-requisitos ainda
resolve nomes, o lookahead podia subestimar uma árvore real A -> B.

Correção:
- a simulação de futuro agora replica ID + nome, exatamente como o planner;
- não foram criados pesos novos;
- `EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS` continua limitando crescimento;
- legalidade continua exclusivamente no RulesEngine;
- ECS continua responsável pela utilidade/sinergia de combate.

Regressões:
1. uma raiz que desbloqueia dependente por pré-requisito nominal recebe
   `futureValue = 1`;
2. crescimento futuro extremo continua incapaz de superar sozinho uma opção
   suficientemente melhor no presente por causa do teto canônico.

Esta etapa melhora coerência presente + continuidade de árvore sem alterar
Archetype, Focus, Sorcery, RNG ou regras de aquisição.
