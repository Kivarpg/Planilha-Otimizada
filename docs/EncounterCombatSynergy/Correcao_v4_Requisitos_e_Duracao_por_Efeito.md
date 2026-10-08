# Correção estrutural v4 — requisitos lógicos e duração por efeito

## 1. Requisitos não são arestas independentes

A representação passa a preservar `AND`, `OR`, `ONE_OF` e `NOT`.

Exemplo:

`A AND (B OR C)`

não pode ser reduzido a três arestas independentes. A expressão somente é satisfeita quando A existe junto de pelo menos uma alternativa B/C.

A estrutura é reutilizável para:
- aquisição;
- ativação;
- variantes;
- efeitos;
- reset.

## 2. Duração pertence ao efeito

A duração declarada do Charm continua sendo metadado importante, mas não prova que cada consequência produzida pelo Charm dura o mesmo período.

Cada `EncounterMechanicalEffect` recebe seu próprio `EffectLifetime`.

Um Charm de duração `One Scene` pode conter:
- efeito instantâneo;
- efeito até o fim do turno;
- estado de cena;
- efeito até determinado gatilho.

Somente o efeito concreto necessário à sinergia pode justificar persistência.

## 3. Simple + Simple

A correção v3 foi endurecida.

Dois `Simple` continuam proibidos como combinação de mesma rodada pela regra geral.

Para existir `CROSS_ROUND`, agora não basta o Charm A ter duração longa. Deve existir um `EncounterMechanicalEffect` de A que:
1. produza exatamente a mecânica exigida;
2. esteja disponível com todos os seus requisitos satisfeitos;
3. tenha `EffectLifetime` que sobreviva até rodada posterior.

## 4. Regra de segurança

`Unknown` e `Explicit(description)` não são promovidos automaticamente a persistentes.

Quando o parser não consegue provar a vida útil do efeito, o avaliador não concede sinergia cross-round integral.

## 5. Próxima integração conceitual

A autoridade final continua destinada ao futuro `EncounterCombinationEvaluator`, que avaliará ação + conjunto de Charms + variantes + estados + restrições. Esta v4 corrige a representação necessária antes dessa etapa.
