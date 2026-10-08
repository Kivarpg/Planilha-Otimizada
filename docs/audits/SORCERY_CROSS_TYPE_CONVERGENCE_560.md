# Exalted.560 — convergência global da política de Feitiçaria

Base: Exalted.559, confirmado compilado.

Esta etapa não adiciona uma quarta interpretação da regra. Ela elimina a
duplicação que havia surgido durante a integração Solar/DB/Lunar.

`EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates` passa a ser o
contrato único para decidir quando existe competição de qualidade:
- somente Arquétipo Mental;
- exploração automática ativa;
- sem Focus explícito em Feitiçaria;
- rota efetivamente construível.

Solar, Sangue de Dragão e Lunar agora consultam o mesmo contrato.

Consequências:
- Focus explícito continua intenção forte, fora da competição automática;
- Físico/Social não entram acidentalmente na comparação de 90%;
- catálogo/círculo indisponível não gera candidato mágico vencedor;
- Lunar exige Inteligência mínima + definição do Círculo antes da comparação;
- nenhuma alteração no número de slots, pré-requisitos, Quimera ou RNG.

Testes de convergência cobrem todas as fronteiras do contrato e confirmam que
uma rota não construível nunca vence mesmo com score artificialmente maior.
