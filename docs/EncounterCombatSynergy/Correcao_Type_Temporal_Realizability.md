# Correção estrutural — Type, janela temporal e realizabilidade

## Problema

A versão anterior podia atribuir sinergia mecânica a dois Encantos sem provar que seus efeitos podiam ser combinados legalmente.

Isso é incorreto para `Simple`: um Encanto Simples constitui uma ação, não entra em flurry e, pela regra geral, somente um Encanto Simples pode ser usado por rodada.

## Correção

A avaliação passa a separar:

1. `BUILD_SYNERGY` — os Encantos pertencem a uma construção coerente.
2. `SAME_ACTION_SYNERGY` — os efeitos podem coexistir na mesma ação.
3. `SAME_ROUND_SYNERGY` — os efeitos são realizáveis na mesma rodada em janelas distintas.
4. `CROSS_ROUND_SYNERGY` — o primeiro cria estado persistente utilizável posteriormente.
5. `PASSIVE_SUPPORT` — um Permanente modifica/sustenta outra aquisição sem disputar ação.

## Regra de dois Simple

`Simple + Simple` nunca recebe `SAME_ACTION` ou `SAME_ROUND` pela regra geral.

A relação só pode sobreviver como:
- afinidade de build; ou
- relação sequencial entre rodadas, quando o estado necessário realmente persiste.

Uma exceção somente pode existir se o texto da regra/Encanto explicitamente a autorizar.

## Supplemental

Dois Supplemental diferentes podem ser combinados somente quando ambos possuem uma ação válida comum para aprimorar.

O mesmo Supplemental não é contado múltiplas vezes sobre a mesma ação.

## Reflexive

`Reflexive` não significa "compatível com tudo". A realizabilidade depende do gatilho, da ação/valor que ele modifica e das restrições específicas. Enquanto esses fatos não estiverem normalizados, a combinação fica como potencial, não como sinergia realizada.

## Permanent

Permanent é suporte passivo. Não consome uma janela normal de ativação, mas ainda pode possuir condições e exceções próprias.

## Duração

Uma relação sequencial exige persistência real do estado. `Instant` não é suficiente para justificar automaticamente `CROSS_ROUND`.

A ordem consolidada passa a ser:

Mechanical affinity
→ Type compatibility
→ Activation window compatibility
→ Duration/effect lifetime
→ State/resource compatibility
→ Realizable synergy

Somente `Realizable synergy` pode receber o peso integral de combinação.
