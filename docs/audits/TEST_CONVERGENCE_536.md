# Exalted.536 — convergência dos testes

O log da .535 revelou quatro falhas reais.

1. `EncounterArchetypePolicyTest`: `priorizarOcultismo=true` esperava Ocultismo
   no topo, mas a política de .528 classificava essa preferência como `efficiency`,
   abaixo do Arquétipo. A chamada agora a envia como `explicitIntent`, coerente
   com a precedência documentada para intenção explícita/Feitiçaria.

2. Dois testes de XP esperavam criar Especialização sobre Habilidade 1. Isso
   contradiz a regra estrutural P-SPECIALTY-2 já implementada e solicitada:
   Especialização exige Habilidade 2+. Os fixtures foram corrigidos para nível 2;
   a produção não foi enfraquecida.

3. O teste de Essência de Sangue de Dragão partia de 72 XP e dependia do gasto
   de 3 XP da Especialização para alcançar o limiar seguinte. Como o fixture
   também estava em Habilidade 1, nenhum gasto legal ocorria e a Essência
   permanecia 2. O fixture agora usa Habilidade 2, preservando o objetivo real
   do teste: subir e depois desfazer a Essência/motes.

Nenhuma regra de legalidade foi relaxada e nenhum teste foi simplesmente removido.
