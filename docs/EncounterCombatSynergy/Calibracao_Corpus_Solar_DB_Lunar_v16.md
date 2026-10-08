# Calibração por corpus — Solares, Sangue de Dragão e Lunares — v16

Base: v15.

Esta rodada foi orientada por contraexemplos reais dos três catálogos, não por abstrações
inventadas.

## EncounterTargetShape

### Falha 1 — uma única categoria de alvo era insuficiente
Há Encantos Solares que permitem:
- um único alvo;
- vários alvos;
- o mesmo alvo várias vezes.

Essas três coisas não são equivalentes.

Correção:
- `kinds: Set<Kind>`;
- `Selection.SINGLE`;
- `MULTIPLE_DISTINCT`;
- `REPEATED_SAME`;
- `SINGLE_OR_MULTIPLE`.

### Falha 2 — área não implica seleção segura
Há Encanto de Sangue de Dragão cuja linha atinge todos os personagens pegos,
explicitamente incluindo aliados.

Correção:
`Inclusion` distingue:
- ONLY_ALLOWED_ACTORS;
- MAY_INCLUDE_ALLIES;
- MAY_INCLUDE_SELF;
- INDISCRIMINATE;
- UNKNOWN.

Assim, um requisito que proíbe fogo amigo não aceita automaticamente um efeito de área
só porque ambos "atingem inimigos".

### Falha 3 — grupos de batalha são sujeitos próprios
Os catálogos Lunar e Solar possuem efeitos especificamente sobre grupos de batalha,
inclusive aliados, e efeitos que podem ser divididos entre vários grupos.

Correção: `BATTLE_GROUP` permanece um Kind próprio, separado de CHARACTER.

## EncounterActorScope

### Falha 4 — produtor/beneficiário/afetado não bastavam
Exemplos reais mostram:
- o Exaltado ativa;
- outro personagem executa a ação;
- esse personagem recebe o bônus;
- terceiros são afetados.

Correção:
- activator;
- actionActor;
- beneficiary;
- affected.

### Falha 5 — categoria de ator não prova identidade
"um aliado" em duas cláusulas não significa necessariamente o mesmo aliado.

Correção:
`ActorRef.bindingId` e `requiredBindings`.
Quando a identidade é necessária e não foi provada, o resultado é UNKNOWN, não suporte.

### Falha 6 — UNKNOWN era absorvido por unsupported
ActorScope agora diferencia `uncertain` e o classificador possui
`EncounterDependencySource.UNKNOWN`.

## EncounterCombinationEvaluator

### Falha 7 — configuração não carregava identidade de participantes
Duas relações podiam ser estruturalmente somadas usando aliados/alvos diferentes quando
a sinergia exigia o mesmo participante.

Correção:
`Configuration.bindings`, `EffectBranch.bindings` e `sameParticipant`.

A realização é rejeitada quando:
- duas atribuições para a mesma variável discordam;
- a identidade exigida não foi provada;
- dois papéis obrigados a coincidir apontam para participantes diferentes.

A assinatura de deduplicação também inclui bindings, impedindo colapso de realizações
que têm os mesmos efeitos, mas participantes estruturalmente diferentes.

## Casos do corpus que motivaram a calibração

Solares:
- ataques que podem ser distribuídos entre um ou vários alvos e repetidos;
- defesa/assistência a outro personagem;
- Performance com alternativa entre um personagem, múltiplos aliados e grupos de batalha.

Sangue de Dragão:
- efeitos que beneficiam aliados;
- gatilhos dependentes de ação de aliado;
- área/linha que pode atingir aliados e inimigos;
- Aura como estado que condiciona/encerra efeitos (já coberto pelas camadas de estado).

Lunares:
- aprimorar teste feito por outro personagem;
- redirecionar ação de um personagem originalmente dirigida a outro;
- efeitos e bônus especificamente concedidos a grupo de batalha aliado;
- Metamorfose condicionada à forma e relação do alvo (continua requisito/variante, não TargetShape).

## Limite deliberado

TargetShape descreve geometria/cardinalidade, não escolhe alvos.
ActorScope descreve papéis causais, não decide ações.
CombinationEvaluator prova coexistência/identidade, não cria sequência de combate.

Nenhuma dessas três classes recebeu lógica de combate.
