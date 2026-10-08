# Exalted.550 — Lunar integrado à política de exploração de Feitiçaria

Base estável: Exalted.549.

A varredura comparativa dos três geradores encontrou uma divergência:
Solar e Sangue de Dragão já submetiam o Arquétipo Mental ao sorteio 9/10,
enquanto Lunar só recebia `exigirFeiticaria` quando o Focus era explicitamente mágico.

Correção:
- Lunar agora consulta EncounterSorceryRoutePolicy uma única vez por geração;
- a mesma decisão é reutilizada nas duas passagens de seleção de Encantos;
- Focus Feitiçaria continua exigindo exploração;
- Mental Lunar passa a usar a mesma política 9/10 de exploração;
- não há segundo sorteio durante a segunda passagem;
- a regra histórica específica de Lunar Físico + Inteligência 3+ permanece intacta;
- requisitos de quatro Encantos de Atributo Mental e acesso ao Círculo Terrestre permanecem no seletor/RulesEngine.

A mudança harmoniza a semântica de exploração sem transformar o sorteio em garantia
de qualidade, nem alterar círculos, custos, quantidade total de Encantos ou Auditor.
