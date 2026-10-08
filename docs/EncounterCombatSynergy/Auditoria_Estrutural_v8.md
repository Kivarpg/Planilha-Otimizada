# Auditoria estrutural v8

## Falha corrigida 1 — vazamento de ator

Uma relação causal precisa registrar QUEM produz, QUEM recebe e QUEM é afetado.

Sem isso, o grafo podia confundir:
- aliado produz X -> NPC produz X;
- familiar/summon produz X -> NPC produz X;
- arma viva possui Iniciativa -> NPC possui essa Iniciativa;
- alvo sofre estado X -> NPC entra em X;
- battle group fornece condição -> condição intrínseca da build.

A v8 introduz `EncounterActorScope` e impede crédito de autossustentação quando a origem é externa.

## Falha corrigida 2 — stacking/caps

Dois efeitos positivos não são necessariamente aditivos.

A v8 representa:
- ADDITIVE
- MAX_OF
- REPLACES
- DOES_NOT_STACK
- SHARED_CAP
- UNKNOWN

Assim, efeitos que competem pelo mesmo teto ou substituem uns aos outros não geram
sinergia fictícia por soma.

## Auditoria posterior

### Falha residual — relações podem mudar de ator
Alguns poderes transferem controle/benefício ou criam entidades autônomas. O ator deve
ser propriedade do efeito, não apenas do Charm. A v8 já modela isso em `ScopedMechanic`.

### Falha residual — "external common" exige fonte
Não classificar uma dependência como comum apenas por intuição. A frequência/normalidade
de uma condição externa deve vir de regra explícita ou metadado curado. Na ausência disso,
usar EXTERNAL_CONDITIONAL.

### Falha residual — caps podem ser contextuais
O teto pode depender de Atributo, Habilidade, Essência ou outra expressão. A v8 aceita
somente cap numérico conhecido; cap dinâmico deve permanecer UNKNOWN até existir um
resolvedor de expressões quantitativas.

### Falha residual — alvo plural
SELF/ALLY/TARGET etc. ainda não codificam cardinalidade (um alvo, todos, grupo, área).
Isso pode importar para coerência de build, mas não deve ser convertido em valor de
combate sem necessidade. Futuro modelo pode adicionar TargetShape sem simulação.

## Regra conservadora
Origem externa, stacking desconhecido ou cap não resolvido nunca recebe bônus integral
de sinergia realizável.
