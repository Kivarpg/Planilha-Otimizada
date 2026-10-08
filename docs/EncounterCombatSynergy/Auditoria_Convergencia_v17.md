# Auditoria de convergência v17

Base: v16.

## Rodada 1 — verdade lógica e TargetShape

- Unificada a expressão de requisitos com avaliação tri-state.
- `OneOf` agora preserva UNKNOWN corretamente.
- O adaptador legado `isSatisfiedBy(Set)` continua explicitamente CLOSED_WORLD.
- `SINGLE_OR_MULTIPLE` não implica mais `REPEATED_SAME`.
- fogo amigo UNKNOWN não é promovido a seguro.

## Rodada 2 — identidade de participantes

Havia dois sistemas de binding paralelos:
- Strings em ActorScope;
- Strings em CombinationEvaluator.

Foram substituídos por:
- `EncounterParticipantId`;
- `EncounterBindingKey`;
- `EncounterParticipantBindings.Constraint`;
- relações SAME e DIFFERENT.

Isso elimina divergência semântica entre os dois avaliadores.

## Rodada 3 — efeitos condicionais

CombinationEvaluator descartava silenciosamente efeitos condicionais não ativos.
Isso fazia duas realizações distintas parecerem iguais.

Agora `inactiveConditionalEffectIds` participa da realização e da assinatura de
deduplicação.

O helper duplicado `EncounterCombinationBindings` foi removido.

## Rodada 4 — wildcards

- `ANY_CHARACTER` produzido não prova `ALLY` específico.
- `ALLY` satisfaz requisito genérico `CHARACTER`.
- `CHARACTER` genérico não prova `ENEMY`.

Os casos incertos permanecem UNKNOWN.

## Rodada 5 — open world no CombinationEvaluator

O avaliador usava requisitos booleanos de mundo fechado. Assim `NOT(X)` podia virar
TRUE apenas porque X não havia sido extraído.

`Configuration` agora possui `knownFalseFacts`.
Variant, branch e effect requirement só são realizados quando a expressão tri-state
é comprovadamente TRUE.

## Rodada 6 — ativação e ActionSignature

Foi detectado um erro sintático real: `EncounterCharmActivationCompatibility.kt`
estava com fechamento de objeto inconsistente após a edição anterior. Corrigido.

Falha estrutural: `explicitAllowedWith` promovia qualquer exceção diretamente a
SAME_ACTION. Agora a exceção precisa declarar sua janela em `explicitAllowedWindow`;
sem janela, permanece BUILD_ONLY.

Foi finalmente implementada `EncounterActionSignature`:
- ActionKind;
- governing trait;
- weapon class;
- range;
- properties;
- charmEnhanceable.

UNKNOWN não é tratado como compatível.

## Rodada 7 — stacking e precedência

- IDs duplicados de contribuição são rejeitados;
- replace dangling/autorreferente é rejeitado;
- canal/grupo vazio é rejeitado;
- cap negativo é rejeitado;
- soma de stacking usa overflow seguro;
- desempate MAX_OF é determinístico;
- precedência para modificador inexistente é inválida;
- precedência autorreferente é inválida.

## Limpeza final

Varredura final:
- TODO/FIXME: 0
- `!!`: 0
- bindings legados: 0
- cortes 48/64/128/256: 0
- parsing textual conhecido de pré-requisitos: 0
- aleatoriedade: 0
- desequilíbrio lexical de chaves: 0
- nomes top-level duplicados: 0

## Limitação

Este pacote continua sendo código de referência, sem Gradle/kotlinc disponível no
ambiente. Os testes foram atualizados/adicionados, mas não há alegação de compilação.

A última rodada estática não produziu nova correção demonstrável. O próximo nível de
evidência é compilação no projeto real e corpus testing automatizado.
