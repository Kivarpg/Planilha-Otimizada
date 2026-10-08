# Exalted.540 — Favorecidas de Sangue de Dragão sob a política central

Base estável: Exalted.539.

A varredura do Ciclo 1 encontrou a seleção das cinco Habilidades Favorecidas do
Sangue de Dragão ainda montando localmente `Foco + Arquétipo + distinct`.

A .540 torna explícitas as duas fases da constituição:
1. filtrar candidatos estruturalmente válidos (fora das Habilidades de Aspecto);
2. ranquear somente candidatos válidos pela `EncounterRulePolicy`.

O comportamento mecânico permanece: exatamente cinco Favorecidas, nenhuma
coincide com Aspecto, Foco válido vem antes das preferências do Arquétipo e o
restante é preenchido pelo RNG existente.

Foi adicionado teste de contrato para impedir que uma Habilidade de Aspecto
reentre no ranking mesmo se aparecer em intenção/prioridade.

Sem alterações em custos, Encantos, XP, Feitiçaria, Forma Espiritual ou Quimera.
