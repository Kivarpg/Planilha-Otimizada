# Auditoria estrutural v7

## Falha corrigida — recursos tratados como rótulos

Uma build pode ser legal e ter sinergias realizáveis, mas concentrar custos incompatíveis ou
depender de um recurso sem suporte estrutural. A v7 introduz `EncounterResourceBudget`.

A análise é estática. Ela NÃO:
- prevê quantas vezes um Charm será usado;
- escolhe quando gastar recurso;
- simula recuperação;
- estima duração de combate.

Ela distingue:
- SPEND
- COMMIT
- GENERATE
- TRANSFER
- CONVERT
- REQUIRE_MINIMUM

e mantém namespaces diferentes, especialmente MOTES e SORCEROUS_MOTES.

## Erros impedidos

1. Tratar transferência como criação de recurso.
2. Tratar limiar mínimo como custo.
3. Somar motes normais e motes de feitiçaria.
4. Ignorar compromisso simultâneo acima da capacidade conhecida.
5. Transformar custo variável/desconhecido em número inventado.
6. Transformar concentração de custos em proibição absoluta.

## Nova auditoria após a correção

### Falha residual encontrada — recursos são contextuais

O mesmo custo pode ser substituído, reembolsado, dispensado ou alterado por outro Charm.
Portanto, o orçamento não pode ser calculado diretamente do custo impresso de cada poder.

A representação final deverá permitir `ResourceModifier`:
- REPLACE_COST
- REDUCE_COST
- REFUND
- WAIVE
- CAP_COST
- CHANGE_RESOURCE

Esses modificadores devem ser aplicados apenas quando a combinação que os habilita for
realizável. Exemplo estrutural: um Permanent pode autorizar dois Charms juntos e substituir
o custo combinado; somar os custos impressos seria incorreto.

### Falha residual encontrada — efeitos negativos não são recursos

Dano, penalidade, exposição, perda de Defesa e outras consequências não devem ser enfiados
em `Resource.OTHER`. Eles precisam permanecer em efeitos/restrições próprios.

### Falha residual encontrada — geração não cancela custo automaticamente

Ter um gerador de Iniciativa ou motes não autoriza subtrair sua produção dos custos da build
sem uma relação causal e janela realizável. Caso contrário o sistema voltaria a simular combate
implicitamente.

## Regra de segurança

O orçamento pode:
- declarar compromisso simultâneo impossível quando capacidade conhecida é excedida;
- marcar concentração/tensão;
- marcar recurso sem suporte;
- reduzir confiança quando quantidade é desconhecida.

Ele não pode afirmar sustentabilidade temporal sem uma regra explícita.
