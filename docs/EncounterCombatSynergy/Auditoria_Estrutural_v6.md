# Auditoria estrutural v6

## Falha corrigida: teleportação de configuração

A v5 provava consistência de uma realização isolada, mas não representava a passagem entre
configurações diferentes. Isso permitia que um avaliador futuro pudesse somar:
- sinergia com arma A;
- sinergia com arma B;
- sinergia com forma C;
sem demonstrar que as trocas eram permitidas ou reconhecer seu custo estrutural.

A v6 introduz:
- `CommitmentScope`
- `Commitment`
- `TransitionRule`
- `ConfigurationPathEvaluator`

Nenhuma troca é presumida. Ela precisa ser explicitamente representada.

## Segunda auditoria após a correção

### Falha residual 1 — ordem parcial, não sequência de combate
O avaliador de configuração recebe passos hipotéticos apenas para provar alcançabilidade.
Ele NÃO deve evoluir para um planejador de ações. A ordem real de combate continua fora
da responsabilidade de EncounterCombatSynergy.

### Falha residual 2 — custo de transição não pode virar frequência inventada
Um custo estrutural informa atrito entre configurações. Não autoriza inferir quantas vezes
o NPC trocará arma/forma/Aura durante combate.

### Falha residual 3 — escopo não substitui regras específicas
`TRANSIENT` não significa "livre para trocar". Mesmo estados transitórios exigem uma
`TransitionRule` suportada pelas regras/fontes.

### Falha residual 4 — simultaneidade e alcançabilidade são métricas distintas
Duas configurações podem ser alcançáveis pela mesma build sem serem simultâneas.
O score deve manter:
- simultaneousRealizability
- reachableAlternativeConfiguration
separados.

### Falha residual 5 — recursos quantitativos
Ainda é inadequado representar apenas "possui recurso". Custos como motes, Vontade,
Iniciativa, anima e recursos específicos possuem quantidades e, às vezes, compromisso.
A futura camada de orçamento deve avaliar tensão estática da build sem simular combate.

## Invariantes adicionais
- ausência de TransitionRule = não presumir troca;
- FIXED_BUILD não muda implicitamente;
- configuração alternativa não recebe score de simultaneidade;
- custo de troca entra como custo estrutural, não como decisão tática;
- o sistema nunca escolhe quando realizar a troca.
