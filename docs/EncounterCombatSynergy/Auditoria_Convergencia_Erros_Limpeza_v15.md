# Auditoria de convergência, erros e limpeza v15

Base real: v14.

Foram executadas rodadas sucessivas; cada correção foi seguida de nova varredura.

## Rodada 1 — transições, recursos e aritmética

Corrigido:
- `minimumScope` existia em TransitionRule mas não era aplicado;
- múltiplas TransitionRules aplicáveis podiam depender da ordem da coleção;
- repetir o mesmo compromisso podia enfraquecer seu escopo silenciosamente;
- modificadores de recurso malformados podiam cair em no-op silencioso;
- soma/multiplicação de quantidades podia sofrer overflow Int.

Agora:
- escopo participa da autorização;
- ambiguidade não é arbitrariamente resolvida;
- SafeResourceModifierResolver rejeita schema inválido;
- overflow quantitativo resulta em desconhecido, não número corrompido.

## Rodada 2 — alvo, aquisição e rota

Corrigido:
- `minTargets` era armazenado mas ignorado;
- shape com máximo menor que mínimo não era reconhecido como inconsistente;
- hipergrafo aceitava IDs/limites/contagens estruturalmente inválidos;
- rota usava `distinctBy(id)`, podendo transformar aquisição duplicada em shared prefix gratuito;
- soma de XP podia overflowar;
- horizontes negativos eram aceitos.

## Rodada 3 — persistência, ledger e coerência

Corrigido:
- `UntilCondition` era tratado como persistente por padrão sem saber se a condição continuava válida;
- ledger aceitava confidence fora de 0..1, causalKey vazio e podia overflowar;
- desempates do ledger dependiam da ordem em casos equivalentes;
- BuildCoherence aceitava contribuições duplicadas do mesmo power e somas sem proteção de overflow.

## Rodada 4 — integridade do grafo de regras

Corrigido:
- RuleId duplicado;
- relações apontando para regras inexistentes;
- relações destrutivas autorreferentes;
- ciclos em APPLIES_AFTER.

Esses casos agora são inválidos/ambíguos em vez de produzirem resolução parcial.

## Rodada 5 — integridade das realizações

`EncounterCombinationEvaluator` agora rejeita:
- Power IDs duplicados/vazios;
- Variant IDs duplicados/vazios dentro do Power;
- Branch IDs duplicados/vazios dentro da Variant;
- ChoiceKey vazio;
- escolhas iniciais vazias.

## Rodada 6 — limpeza

Removido código realmente não referenciado:
- EncounterModelIntegrity;
- helper separado EncounterAcquisitionValidation (validação incorporada ao hipergrafo);
- EncounterRequirementRole / EncounterTypedRequirement não usados;
- IDs tipados experimentais que não estavam conectados a nenhuma API.

A remoção é deliberada: tipos "planejados" mas desconectados aumentavam a impressão
de segurança sem fornecer invariantes reais.

## Verificação final

Varredura final do código de referência:
- TODO/FIXME: 0
- `!!`: 0
- parsing textual de pré-requisitos no otimizador: 0
- cortes conhecidos de catálogo 48/64/128/256: 0
- aleatoriedade: 0
- seleção por maior número de efeitos: 0
- declarações top-level detectadas sem nenhuma referência: 0
- desequilíbrio lexical de chaves: 0

## Limite de validação

O pacote é um development/reference pack, não um projeto Gradle autônomo, e o ambiente
não disponibiliza `kotlinc`. Portanto os testes Kotlin adicionados não foram executados
por compilador. Não há alegação de compilação.

A integridade do ZIP é verificada separadamente.

## Convergência

Após a limpeza, a última varredura não encontrou outra falha estrutural ou erro
demonstrável corrigível apenas com este pacote.

O próximo nível de prova exige:
1. inserir o pack em um projeto Kotlin/Gradle compilável; e
2. corpus testing contra definições reais dos cinco catálogos.

Isso não é um adiamento de falha já conhecida: é o limite de evidência disponível no
pacote isolado.
