# Auditoria estrutural v11

## Falha corrigida 1 — fatos monotônicos / fatos fantasmas

As versões anteriores usavam conjuntos de fatos em vários pontos. Isso é adequado para
pré-requisitos permanentes, mas não para estados derivados que podem deixar de existir.

Exemplos estruturais:
- Aura consumida;
- Forma abandonada;
- equipamento trocado;
- efeito de rodada expirado;
- variante exclusiva substituída;
- fonte de um benefício removida.

A v11 introduz `EncounterProjectedFactState` com:
- proveniência (`SourceId`);
- lifetime;
- grupos exclusivos;
- Produce;
- RevokeSource;
- Consume;
- Expire.

Assim, a rota não pode continuar recebendo crédito por um fato depois que sua fonte
foi removida.

## Falha corrigida 2 — lógica booleana binária diante de informação incompleta

`RequirementExpression.isSatisfiedBy(Set<String>)` trata ausência como não satisfeita.
Isso é correto para legalidade quando o conjunto de fatos é completo, mas perigoso na
extração semântica parcial.

Especialmente:
`NOT(UNKNOWN)` não pode virar `TRUE`.

A v11 adiciona `EncounterTruth = TRUE/FALSE/UNKNOWN` e `EncounterTriRequirement`.
Isso separa:
- fato conhecido verdadeiro;
- fato conhecido falso;
- fato ainda não conhecido/extraído.

A camada antiga continua válida onde o universo de fatos é completo e fechado.

## Auditoria posterior

### Falha residual — provenance pode ter múltiplas fontes
Um mesmo fato pode ser sustentado por duas fontes. `revokeSource` já preserva a outra
instância porque os fatos carregam SourceId separadamente.

### Falha residual — efeitos derivados em cadeia
Se A produz B e B produz C, revogar A precisa invalidar C. Isso exige grafo de
dependência de fatos, não apenas SourceId direto.

### Falha residual — ciclos de derivação
Um grafo derivado pode conter A -> B -> A. A invalidação precisa trabalhar por
ponto fixo e não por recursão ingênua.

Essas duas falhas são reais e foram corrigidas na mesma versão pelo resolvedor
de dependências derivadas.
