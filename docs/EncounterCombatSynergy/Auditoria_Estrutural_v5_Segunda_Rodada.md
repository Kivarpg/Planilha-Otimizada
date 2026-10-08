# Auditoria estrutural v5 — segunda rodada

Após introduzir ramos, variantes, configuração consistente e deduplicação causal, a própria solução foi revisada novamente.

## Falha descoberta durante a segunda rodada: "configuração única" pode ser rígida demais

Arma, forma, Aura e outros estados não possuem todos a mesma natureza.

Há pelo menos três classes conceituais:

1. **FIXED_BUILD** — escolha estrutural que não pode ser presumida como trocável gratuitamente.
2. **SWITCHABLE_LOADOUT** — a build pode possuir alternativas, mas elas não são simultâneas.
3. **TRANSIENT_STATE** — estado temporário que pode mudar conforme regras próprias.

Portanto, `Configuration.choices` na v5 deve ser entendido conservadoramente como
"hipóteses que precisam permanecer consistentes dentro da realização avaliada", não como afirmação
de que toda escolha é permanentemente fixa na ficha.

O futuro `BuildState` deverá tipar compromissos:

```text
CommitmentScope:
FIXED_BUILD
LOADOUT
SCENE
ROUND
ACTION
TRANSIENT
```

Isso evita dois erros opostos:
- somar configurações incompatíveis como simultâneas;
- declarar duas capacidades incompatíveis na ficha inteira quando são apenas alternativas de loadout/estado.

## Outra falha residual: busca por uma única realização

`realize()` prova existência de uma configuração consistente, mas não calcula ainda:
- custo para trocar de configuração;
- quantas configurações diferentes a build exige para realizar todas as suas sinergias;
- perda de coerência causada por fragmentação entre loadouts.

Por segurança, v5 NÃO concede automaticamente simultaneidade entre configurações diferentes.
A futura camada `BuildState + CommitmentScope` deverá medir essa fragmentação.

## Outra falha residual: fatos derivados

Alguns requisitos dependem de fatos derivados (por exemplo, uma combinação de equipamento,
estado e característica), não apenas fatos literais. `RequirementExpression` já suporta a lógica,
mas a produção de `facts` deverá vir de um normalizador determinístico, não de strings inferidas
ad hoc pelo scorer.

## Regra de segurança

Na dúvida:
- preservar afinidade de build;
- reduzir confiança;
- não promover a relação a `RealizableSynergy`.

Nenhuma incerteza semântica deve virar bônus integral.
