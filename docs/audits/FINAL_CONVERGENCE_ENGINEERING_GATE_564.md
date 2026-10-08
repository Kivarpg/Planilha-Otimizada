# Exalted.564 — convergência final do ciclo lógico

Base: Exalted.563, confirmado compilado.

A inspeção final encontrou uma lacuna de governança: os testes novos de convergência
de Feitiçaria e da matriz 3×3 rodavam na suíte completa, mas ainda não faziam parte
do contrato explícito verificado pelo Engineering Gate. Uma futura alteração no
workflow poderia removê-los sem que `EngineeringGateConfigurationTest` denunciasse.

Correção:
- `EncounterArchetypeMatrixTest` entra no gate explícito;
- `EncounterSorceryRoutePolicyTest` entra no gate explícito;
- `EncounterSpecializationBoundaryTest` entra no gate explícito;
- Golden mecânico e Auditor permanecem protegidos;
- `EngineeringGateConfigurationTest` passa a exigir esses contratos no workflow.

Assim, o fechamento do ciclo liga diretamente as etapas 557–563 ao CI: política de
Feitiçaria, nove cenários Tipo × Arquétipo, limite de Especializações, Golden,
RulesEngine/Auditor e regressões de engenharia.

Não há mudança de regra de geração, score, RNG, beam ou UI nesta versão.
