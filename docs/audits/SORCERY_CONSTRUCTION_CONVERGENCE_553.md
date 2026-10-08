# Exalted.553 — convergência da construção de Feitiçaria

Base estável: Exalted.552.

A revisão comparativa confirmou duas inconsistências remanescentes em Solar/Sangue de Dragão:

- Mental priorizava Ocultismo mesmo nos 10% em que a rota automática não era explorada.
- Sangue de Dragão podia exigir quatro Encantos de Ocultismo apenas porque a exploração foi sorteada, mesmo quando o Círculo não estava disponível/materializável.

Correções:
- decisão de exploração é calculada antes da prioridade de Encantos;
- `shouldPrioritizeOccultism` centraliza a regra: explorar + Ocultismo mínimo, preservando a exceção histórica do Físico;
- Focus Feitiçaria Social/Mental também recebe prioridade quando a rota é materializável;
- Sangue de Dragão só impõe o mínimo adicional de quatro Encantos quando o projeto de Feitiçaria será realmente construído;
- Solar e Sangue de Dragão usam a mesma semântica;
- Lunar permanece com requisitos próprios por Atributos Mentais.

A probabilidade 9/10, requisitos, círculos, orçamento total, RulesEngine e Auditor não foram alterados.
