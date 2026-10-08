# Mudanças propostas --- EncounterCombatSynergy + Roadmap evolutivo

Base auditada: **Exalted.375.zip**

## Objetivo

A função do `EncounterCombatSynergy` é **afinar a escolha de Encantos e
ajudar a escolher a melhor linha evolutiva**, sem executar, simular ou
decidir ações de combate.

A progressão continua obedecendo às regras próprias de cada Exaltado:

-   Solar usa exclusivamente Encantos Solares como catálogo nativo.
-   Sangue de Dragão usa exclusivamente Encantos de Sangue de Dragão
    como catálogo nativo.
-   Lunar usa exclusivamente Encantos Lunares como catálogo nativo.
-   Artes Marciais e Feitiçaria podem complementar cada um quando forem
    legalmente acessíveis.
-   Encantos nativos de tipos diferentes de Exaltado nunca entram no
    mesmo espaço de escolha.

## Descoberta na auditoria da 375

O arquivo persistido `Exalted.375.zip` contém
`EncounterCharmRouteOptimizer.kt` e suas chamadas nos
seletores/progressores, porém **não contém
`EncounterCombatSynergy.kt`**.

Consequência: o `EncounterCharmRouteOptimizer` atualmente decide
principalmente por:

-   economia de XP;
-   número de dependentes diretos;
-   Essência mínima;
-   concentração na mesma categoria;
-   beam search de profundidade 3.

Logo, a sinergia mecânica discutida anteriormente não está efetivamente
participando da escolha nessa base auditada.

Esta alteração deve corrigir primeiro essa lacuna.

------------------------------------------------------------------------

# 1. Separar legalidade, afinidade e valor evolutivo

Não colocar todas as responsabilidades dentro de
`EncounterCombatSynergy`.

Criar três conceitos:

``` kotlin
enum class EncounterCharmDomain {
    SOLAR,
    DRAGON_BLOODED,
    LUNAR,
    MARTIAL_ARTS,
    SORCERY
}

data class EncounterSynergyEvaluation(
    val immediateAffinity: Int,
    val complementarity: Int,
    val redundancyPenalty: Int,
    val conflictPenalty: Int,
    val total: Int
)

data class EncounterRouteEvaluation(
    val immediateScore: Int,
    val lineageScore: Int,
    val accessCostPenalty: Int,
    val saturationPenalty: Int,
    val total: Int
)
```

`EncounterCombatSynergy` não decide se uma compra é legal. A
elegibilidade continua pertencendo aos serviços Solar, Sangue de Dragão
e Lunar.

Ele recebe apenas candidatos que já passaram pela legalidade e mede a
coerência da escolha.

------------------------------------------------------------------------

# 2. Bloqueio estrutural entre tipos de Exaltado

Toda avaliação deve receber o domínio do NPC.

``` kotlin
data class EncounterSynergyContext(
    val exaltDomain: EncounterCharmDomain,
    val selectedNativeCharmNames: Set<String>,
    val selectedMartialArtsCharmNames: Set<String> = emptySet(),
    val selectedSpellNames: Set<String> = emptySet()
)
```

Regra:

``` kotlin
fun domainsCanCoexist(
    npcDomain: EncounterCharmDomain,
    candidateDomain: EncounterCharmDomain
): Boolean =
    candidateDomain == npcDomain ||
    candidateDomain == EncounterCharmDomain.MARTIAL_ARTS ||
    candidateDomain == EncounterCharmDomain.SORCERY
```

Nunca pontuar:

-   Solar ↔ Lunar
-   Solar ↔ Sangue de Dragão
-   Lunar ↔ Sangue de Dragão

Artes Marciais e Feitiçaria entram como complementos, nunca como
autorização para misturar catálogos nativos.

------------------------------------------------------------------------

# 3. Substituir "quantidade de desbloqueios" por "qualidade dos descendentes"

Problema atual:

``` kotlin
val desbloqueios = desbloqueiosPorNome[novoNome.lowercase()] ?: 0
...
desbloqueios * 14
```

Isso favorece um Encanto simplesmente porque ele possui muitos
descendentes.

Substituir por avaliação prospectiva.

``` kotlin
data class EncounterLineageNode<D>(
    val definition: D,
    val depth: Int,
    val prerequisiteDistance: Int
)
```

Para cada candidato legal atual:

1.  percorre seus descendentes;
2.  mantém apenas descendentes do domínio permitido;
3.  calcula afinidade de cada descendente com a construção;
4.  desconta profundidade e custo necessário para chegar nele;
5.  conserva os melhores caminhos, e não a soma de todos os
    descendentes.

Exemplo de cálculo:

``` kotlin
lineageScore =
    bestDescendantScores
        .sortedDescending()
        .take(MAX_RELEVANT_DESCENDANTS)
        .mapIndexed { index, value ->
            value * lineageDiscount(index)
        }
        .sum()
```

Assim, uma árvore enorme porém irrelevante deixa de vencer uma árvore
pequena e altamente coerente.

------------------------------------------------------------------------

# 4. Diferenciar Encanto-ponte de Encanto-destino

Criar:

``` kotlin
enum class EncounterRouteRole {
    ROOT,
    BRIDGE,
    DESTINATION,
    SUPPORT
}
```

Um `BRIDGE` pode ter baixa afinidade imediata e ainda ser escolhido se
abrir um `DESTINATION` de grande afinidade.

Pontuação:

``` text
VALOR DA ESCOLHA
= afinidade imediata
+ complementaridade
+ valor da melhor linhagem futura
- redundância
- conflitos
- custo de acesso
- distância evolutiva
```

Não exigir que todo Encanto comprado seja forte isoladamente.

------------------------------------------------------------------------

# 5. Busca evolutiva mais profunda sem aumentar o roadmap persistido

Hoje:

``` kotlin
DEFAULT_DEPTH = 3
PASSOS_PREFETCH = 3
```

Esses dois valores não devem representar a mesma coisa.

Manter:

``` kotlin
PASSOS_PREFETCH = 3
```

O roadmap continua curto, barato e revalidável.

Aumentar apenas o horizonte virtual de análise:

``` kotlin
private const val DEFAULT_ROUTE_LOOKAHEAD = 6
```

Esse lookahead **não compra seis Encantos** e **não grava seis passos**.

Ele apenas pergunta:

> "Se eu comprar este Encanto agora, que qualidade de linha evolutiva
> ele abre?"

Depois de escolhida a melhor primeira aquisição, somente a aquisição
real entra no próximo passo do roadmap.

------------------------------------------------------------------------

# 6. Evitar especialização autoalimentada

O atual:

``` kotlin
val focoCategoria =
    (contagens[novaCategoria] ?: 0).coerceAtMost(3) * 2
```

recompensa continuamente a categoria já escolhida.

Substituir por curva de aprofundamento com saturação.

Exemplo:

``` kotlin
fun depthAffinity(count: Int): Int = when (count) {
    0 -> 0
    1 -> 5
    2 -> 9
    3 -> 11
    4 -> 10
    5 -> 7
    else -> 3
}
```

A ideia não é usar obrigatoriamente esses números finais, mas preservar
a propriedade:

-   primeiros Encantos coerentes reforçam identidade;
-   aprofundamento continua valioso;
-   acumular indefinidamente a mesma função deixa de produzir bônus
    crescente.

------------------------------------------------------------------------

# 7. Distinguir redundância de aprofundamento

Dois Encantos na mesma Habilidade/Atributo não são automaticamente
redundantes.

Criar papéis mecânicos abstratos de construção, sem prescrever ações:

``` kotlin
enum class EncounterCharmFunction {
    ACCURACY,
    WITHERING_DAMAGE,
    INITIATIVE_GAIN,
    INITIATIVE_DRAIN,
    CRASH_ENABLE,
    CRASH_EXPLOIT,
    DEFENSE,
    COUNTER,
    ONSLAUGHT,
    EXTRA_ATTACK,
    POSITIONING,
    DECISIVE_SUPPORT,
    RESOURCE_EFFICIENCY,
    CONTROL,
    RECOVERY,
    OTHER
}
```

A redundância ocorre quando novas aquisições repetem essencialmente a
mesma função sem abrir nova consequência relevante.

Complementaridade ocorre quando uma aquisição acrescenta uma função que
fortalece as funções já escolhidas.

O sistema avalia **construção**, não sequência de uso em combate.

------------------------------------------------------------------------

# 8. Anti-sinergia explícita

Adicionar penalidades para escolhas que disputam ou contradizem
requisitos estruturais.

``` kotlin
enum class EncounterBuildConflict {
    INCOMPATIBLE_DOMAIN,
    INCOMPATIBLE_WEAPON_REQUIREMENT,
    INCOMPATIBLE_STYLE_REQUIREMENT,
    RESOURCE_OVERCOMMITMENT,
    EXCLUSIVE_STATE_REQUIREMENT,
    DUPLICATE_FUNCTION_WITHOUT_NEW_ROUTE
}
```

Apenas conflitos que possam ser inferidos com segurança dos dados devem
ser aplicados.

Não inventar comportamento tático.

------------------------------------------------------------------------

# 9. Artes Marciais devem ser avaliadas como investimento de entrada

Não misturar todos os Encantos de Artes Marciais diretamente ao pool
nativo.

Primeiro avaliar o estilo:

``` kotlin
data class EncounterMartialArtRouteValue(
    val styleName: String,
    val entryCost: Int,
    val compatibleDescendants: Int,
    val lineageAffinity: Int,
    val total: Int
)
```

Somente estilos cuja linha global seja coerente com a construção entram
como ramificação elegível.

Isso evita comprar fragmentos de estilos diferentes apenas porque cada
Encanto individual recebeu boa pontuação.

------------------------------------------------------------------------

# 10. Feitiçaria deve ser avaliada como ramificação com custo de acesso

Feitiço não deve ser tratado como Encanto nativo.

Antes de pontuar um Feitiço, confirmar:

1.  círculo permitido para o tipo de Exaltado;
2.  círculo efetivamente desbloqueado;
3.  custo de abrir/manter a ramificação;
4.  afinidade da ramificação com a construção.

O afinador pode recomendar abrir Feitiçaria, mas apenas se o valor
futuro justificar o investimento necessário.

------------------------------------------------------------------------

# 11. Solar, Sangue de Dragão e Lunar compartilham o motor, não a política

O núcleo de comparação pode ser comum.

A política deve permanecer específica:

``` kotlin
interface EncounterLineagePolicy<D> {
    fun category(definition: D): String
    fun minimumTrait(definition: D): Int
    fun isEligible(...)
    fun acquisitionCost(...)
    fun domain(): EncounterCharmDomain
}
```

Implementações:

``` text
SolarLineagePolicy
DragonBloodedLineagePolicy
LunarLineagePolicy
```

Solar e Sangue de Dragão preservam suas relações com Habilidades.

Lunar preserva sua progressão orientada por Atributos.

Não converter Lunar artificialmente para o modelo Solar/DB.

------------------------------------------------------------------------

# 12. Integrar a escolha ao EncounterCharmRouteOptimizer

Alterar a assinatura para receber avaliação de afinidade e política de
linhagem:

``` kotlin
fun <D> escolher(
    candidatos: List<D>,
    catalogoCompleto: List<D>,
    nomesSelecionados: Set<String>,
    contagensCategorias: Map<String, Int>,
    elegivel: (D, Set<String>, Map<String, Int>) -> Boolean,
    nome: (D) -> String,
    categoria: (D) -> String,
    custoXp: (D) -> Int,
    synergyScore: (
        candidate: D,
        selectedNames: Set<String>
    ) -> Int,
    lineageScore: (
        candidate: D,
        selectedNames: Set<String>,
        counts: Map<String, Int>
    ) -> Int,
    ...
): D?
```

O `scoreImediato` deixa de usar `desbloqueios * 14`.

Proposta:

``` kotlin
val immediate =
    economyScore(candidato) +
    categoryDepthScore(candidato, contagens) +
    synergyScore(candidato, selecionados) -
    essenceDistancePenalty(candidato)

val future =
    lineageScore(candidato, selecionados, contagens)

return immediate + future
```

------------------------------------------------------------------------

# 13. O roadmap deve consumir a melhor escolha, não escolher por conta própria

`EncounterProgressionRoadmapService` não deve duplicar heurísticas de
Encantos.

Fluxo correto:

``` text
Roadmap
  ↓
Expander específico do Exaltado
  ↓
EncounterCharmRouteOptimizer
  ↓
EncounterCombatSynergy + LineageEvaluator
  ↓
melhor próxima aquisição legal
  ↓
EncounterProgressionStep
```

O roadmap continua sendo **cache/plano curto de próximas compras**.

A inteligência de escolha permanece no afinador/otimizador.

------------------------------------------------------------------------

# 14. Registrar justificativa evolutiva no roadmap sem afetar execução

Adicionar metadados opcionais:

``` kotlin
data class EncounterProgressionStep(
    ...
    val routeId: String? = null,
    val routeScore: Int? = null,
    val routeReason: String? = null
)
```

Exemplos de `routeReason`:

``` text
"Aprofunda linha principal e abre 2 descendentes de alta afinidade."
"Encanto-ponte necessário para a melhor continuação da árvore."
"Complementa função já presente sem repetir efeito dominante."
```

Esses campos servem para diagnóstico, testes e futura UI.

Eles não determinam ações em combate.

Como o modelo persistido muda, incrementar:

``` kotlin
SCHEMA_VERSION = 2
ALGORITHM_VERSION = 4
```

------------------------------------------------------------------------

# 15. Melhor escolha deve ser recalculada a cada passo

O roadmap não deve ficar preso à previsão original se o NPC mudar.

A infraestrutura existente de `preconditionFingerprint` deve ser
mantida.

Ao mudar:

-   Encantos;
-   Habilidades/Atributos relevantes;
-   Casta/Aspecto/Favorecidos;
-   Supernal;
-   catálogo;
-   versão do algoritmo;

o passo antigo é descartado e a melhor rota é recalculada.

Isso permite planejamento evolutivo sem transformar o roadmap em
compromisso permanente.

------------------------------------------------------------------------

# 16. Testes necessários

Adicionar testes determinísticos:

### Domínio

-   Solar nunca recebe valor de Encanto Lunar.
-   Lunar nunca recebe valor de Encanto Solar.
-   Sangue de Dragão nunca recebe valor de Encanto Solar/Lunar.
-   Arte Marcial pode complementar qualquer um quando legal.
-   Feitiçaria só participa quando o acesso é legal.

### Linhagem

-   árvore com 10 descendentes irrelevantes perde para árvore com 3
    descendentes altamente coerentes;
-   Encanto-ponte fraco pode vencer candidato mediano quando abre
    destino de alta afinidade;
-   descendente muito distante sofre desconto;
-   linhagem ilegal não produz valor futuro.

### Saturação

-   segundo/terceiro Encanto coerente aprofundam a linha;
-   sexto Encanto funcionalmente redundante não recebe bônus crescente;
-   complemento novo não é confundido com dispersão.

### Roadmap

-   `PASSOS_PREFETCH` continua 3;
-   lookahead pode ser maior que 3 sem persistir compras fictícias;
-   alteração de estado invalida o passo;
-   roadmap registra a escolha realmente feita pelo otimizador;
-   metadados de rota não alteram a compra nem a reversão de XP.

------------------------------------------------------------------------

# Resultado arquitetural esperado

A pergunta central deixa de ser:

> "Qual Encanto elegível tem mais descendentes?"

e passa a ser:

> "Entre as compras legais agora, qual delas melhora a construção atual
> e abre a melhor linha evolutiva coerente para este tipo específico de
> Exaltado?"

O sistema não joga, não simula combate e não prescreve sequência de
ações.

Ele apenas melhora a qualidade das escolhas de construção e progressão.
