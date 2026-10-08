# Exalted.552 — exploração vs. construtibilidade da rota de Feitiçaria

Base estável: Exalted.551.

A revisão comparativa encontrou uma assimetria entre Solar e Sangue de Dragão:
Solar só reservava as cinco vagas do projeto quando Ocultismo já satisfazia o mínimo,
mas Sangue de Dragão podia reservar as vagas apenas porque a exploração 9/10 havia sido sorteada.

Correção:
- a pré-condição Ocultismo mínimo foi centralizada em EncounterSorceryRoutePolicy;
- Solar e Sangue de Dragão usam a mesma função de construtibilidade;
- Sangue de Dragão não reserva cinco vagas para uma rota impossível por Ocultismo insuficiente;
- a busca do catálogo pela definição do Círculo só ocorre depois dessa pré-condição;
- exploração e construtibilidade permanecem conceitos separados;
- Lunar não usa esse helper, pois sua rota tem requisitos próprios de Atributos/Encantos Mentais.

Nenhuma probabilidade, requisito do RulesEngine, custo, círculo ou orçamento total foi alterado.
