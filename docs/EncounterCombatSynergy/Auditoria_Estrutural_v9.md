# Auditoria estrutural v9

## Falha corrigida — quantidades dependentes da ficha

Caps, mínimos e magnitudes podem depender de valores estáticos da construção.
Mantê-los como UNKNOWN para sempre perde informação; convertê-los em constantes
inventadas cria falsos resultados.

A v9 introduz `EncounterQuantityExpression`, resolvida exclusivamente contra
`EncounterBuildQuantities`.

Suporta inicialmente:
- Constant
- Trait
- Add
- Min
- Max
- Multiply
- HalfRoundedUp
- Unknown

Isso é aritmética da ficha, não simulação de combate.

## Falha descoberta durante a própria implementação

A primeira versão do resolvedor dinâmico deixava um cap não resolvido virar `null`,
e o resolvedor antigo podia então somar as contribuições como se não houvesse cap.

Isso foi corrigido na mesma rodada:
**cap declarado mas não resolvido => resultado quantitativo UNKNOWN**, nunca "sem cap".

## Cardinalidade de alvo

Foi introduzido `EncounterTargetShape` somente para compatibilidade estrutural.
Ele NÃO concede score por atingir mais alvos.

Permite distinguir, quando necessário:
- self
- single target/ally/enemy
- multiple targets
- all allies/enemies
- battle group
- area
- created entity

Quantidade desconhecida permanece desconhecida.

## Auditoria posterior

### Falha residual — expressões quantitativas podem depender de resultado de rolagem
Essas expressões NÃO devem entrar em `BuildQuantities`. Resultado de ataque,
sucessos excedentes, dano rolado e outros valores de combate permanecem dinâmicos
e fora da responsabilidade deste módulo.

### Falha residual — ordem de modificadores
Se vários efeitos substituem/reduzem/capam o mesmo valor, a ordem pode ser definida
pela regra específica. O resolvedor não deve inventar precedência universal.
Casos de ordem não demonstrada devem permanecer ambíguos.

### Falha residual — identidade de trait
Strings como "DEXTERITY" são protótipo. Na integração madura devem virar IDs tipados
para impedir colisões e erros de grafia.

Nenhuma dessas falhas residuais exige nova heurística agora; a regra conservadora
já impede promoção de informação desconhecida a sinergia integral.
