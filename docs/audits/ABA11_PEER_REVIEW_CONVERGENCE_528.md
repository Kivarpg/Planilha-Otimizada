# Aba 11 — convergência dos pareceres técnicos (.528)

Esta rodada transforma o consenso dos seis pareceres em uma constituição explícita de regras, sem tratar hipóteses da especificação como bugs confirmados.

## Confirmado no código e tratado
- Precedência/taxonomia estava distribuída em concatenações locais de prioridade.
- Foi criado `EncounterRulePolicy` com Nature (`LEGALITY`, `STRUCTURAL`, `QUALITY`) e camadas de precedência.
- `resolvePriority` centraliza o ranking de preferências; restrições legais/estruturais filtram o espaço antes do ranking.
- Solar e Sangue de Dragão usam o resolver central na prioridade inicial de Encantos; Solar também o usa na prioridade de PB.
- `EncounterArchetypePolicy` usa o mesmo resolver para prioridades de Habilidades e Encantos.

## Hipóteses dos pareceres verificadas e não tratadas como bug nesta rodada
- Deduplicação do `EncounterBuildPlanner` por `acquiredCharmIds`: no planner atual, o único campo mutado entre nós é o conjunto de Encantos adquiridos (e nomes derivados). Atributos, Habilidades, traits e estilos permanecem constantes durante a busca. Portanto ampliar o fingerprint agora aumentaria custo sem distinguir estados futuros reais.
- Especialização em Habilidade 1 durante XP já possui guarda em `EncounterExperienceService` e validação final em `EncounterValidationService` desde a .526.
- Antigo Reino já possui regra central em `LanguageAcquisitionRules` e testes desde a .527.

## Próximas frentes
1. Formalizar contratos de criação/progressão sobre os mesmos predicados fundamentais.
2. Unificar método ofensivo, defesa derivada e rota de Artes Marciais.
3. Formalizar convergência Lunar Forma/Quimera/Encantos.
4. Substituir Feitiçaria Mental puramente probabilística por decisão contextual mensurável.
5. Implementar BuildCoherence e A/B independente do score do otimizador.
