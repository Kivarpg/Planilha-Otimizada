# Engineering Baseline — Exalted.494

Baseline estável de origem: Exalted.493 (compilação GitHub confirmada).

## Objetivo
Proteger otimizações futuras da Aba 11 contra regressões de regras, qualidade e custo computacional.

## Sete frentes
1. **Benchmark determinístico** — `EncounterEngineeringBaseline.cases` define seeds canônicas para Solar, Sangue de Dragão e Lunares físico/social/mental, Feitiçaria, Quimera, Artes Marciais e pior caso.
2. **Golden/snapshot mecânico** — `mechanicalFingerprint` canonicaliza o estado observável antes do SHA-256; UUID e ordem incidental não entram no snapshot.
3. **Orçamento de performance** — `WorkBudget` usa contadores determinísticos do `EncounterCharmRouteMetrics`. Tempo de parede permanece telemetria e não bloqueia CI.
4. **Arquitetura preparada** — `PreparedEncounterCatalog` continua sendo a fronteira canônica de dados imutáveis/IDs/grafo; geração e regras continuam consumidores, sem alteração de semântica nesta versão.
5. **Invariantes** — a suíte existente de regras/legalidade permanece obrigatória e o novo gate executa contratos de baseline/índice/métricas.
6. **Estado compacto** — `CompactSelectionIndex` fornece ordinal estável derivado de `StableContentId` e representação `BitSet`, com round-trip testado. Nesta versão ele é shadow infrastructure: não substitui o estado semântico do RulesEngine ainda.
7. **Performance gate** — GitHub Actions ganhou etapa determinística dedicada. Regressões de contadores/contratos podem falhar o CI; diferenças de relógio não.

## Regra de migração
Nenhuma troca de `Set<String>`/`Map<String,Int>` no hot path deve ocorrer sem equivalência contra o baseline Exalted.493. Migrar por fronteira, medir, comparar fingerprint/invariantes e só então remover o adaptador legado.

## Próximo passo técnico
Depois da confirmação de compilação da 494, coletar o baseline real das seeds no runner e preencher budgets por cenário. Em seguida migrar primeiro o cache de presença/seleção para IDs compactos, mantendo RulesEngine como autoridade de legalidade.
