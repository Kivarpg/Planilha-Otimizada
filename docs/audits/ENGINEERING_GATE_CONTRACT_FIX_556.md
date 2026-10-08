# Exalted.556 — contrato do Engineering Gate compatível com CI consolidado

Base: Exalted.555 (falhou em um único teste de configuração).
Checkpoint estável: Exalted.553.

O log da 555 mostrou 798 testes executados e apenas uma falha:
`EngineeringGateConfigurationTest.workflow points engineering gate at an existing non excluded test class`.

Causa:
- a 555 removeu corretamente a segunda execução Gradle do Engineering Gate;
- o teste de configuração ainda exigia literalmente a antiga flag
  `--tests "com.example.data.EncounterEngineeringBaselineTest"`;
- portanto o teste validava a implementação antiga do pipeline, não a garantia
  arquitetural que precisamos preservar.

Correção:
- o contrato agora exige que o workflow execute `testDebugUnitTest`;
- exige também que `EncounterEngineeringBaselineTest` permaneça explicitamente
  listado pelo verificador de cobertura do Engineering Gate;
- as proteções contra classes antigas/excluídas e task obsoleta continuam intactas;
- o workflow otimizado da 555 permanece com uma única invocação Gradle.

Nenhuma regra de NPC, Feitiçaria, UI, RulesEngine ou Auditor foi alterada.
