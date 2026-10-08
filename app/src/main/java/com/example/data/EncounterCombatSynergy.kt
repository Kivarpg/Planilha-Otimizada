package com.example.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Classificador semântico conservador para a Aba 11.
 *
 * O objetivo não é medir "poder" isolado, mas reconhecer relações causais úteis
 * entre Encantos. A análise exige contexto mecânico explícito para Iniciativa,
 * Atordoamento e onslaught, evitando falsos positivos de palavras genéricas.
 * A pontuação é calculada por pares reais de Encantos e usa a melhor relação de
 * cada parceiro, impedindo que uma nuvem agregada de tags invente combos.
 */
internal object EncounterCombatSynergy {
    enum class Tag {
        WITHERING,
        DECISIVE,
        INITIATIVE_GAIN,
        INITIATIVE_DRAIN,
        INITIATIVE_TRANSFER,
        INITIATIVE_COST,
        INITIATIVE_ADVANTAGE,
        CRASH_CAUSE,
        CRASH_EXPLOIT,
        CRASH_RECOVERY,
        JOIN_BATTLE,
        CLASH,
        ONSLAUGHT_APPLY,
        ONSLAUGHT_EXPLOIT,
        EXTRA_ATTACK,
        COUNTERATTACK,
        DEFENSE,
        POSITIONING,
        MINIMUM_DAMAGE,
        ACCURACY,
        DAMAGE,
        CONTROL
    }

    private fun normalized(text: String): String = text.lowercase()
        .replace('–', '-')
        .replace('—', '-')

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)

    // A classificação é chamada repetidamente durante comparações de pares.
    // Compilar as mesmas expressões e reclassificar a mesma descrição em cada
    // par transformava uma etapa O(N²) em milhares de Regex redundantes.
    private val regexCache = ConcurrentHashMap<String, Regex>()
    private val tagCache = ConcurrentHashMap<String, Set<Tag>>()

    private fun matches(text: String, pattern: String): Boolean =
        regexCache.computeIfAbsent(pattern) { Regex(it, RegexOption.IGNORE_CASE) }.containsMatchIn(text)

    fun tags(rawText: String): Set<Tag> = tagCache.computeIfAbsent(rawText) { textoOriginal ->
        val text = normalized(textoOriginal)
        buildSet {
            if (containsAny(text, "fulminante", "withering")) add(Tag.WITHERING)
            if (containsAny(text, "decisiv", "decisive")) add(Tag.DECISIVE)

            // Iniciativa: verbos só contam quando ligados explicitamente ao recurso.
            if (matches(text, "(?:ganha|ganhe|ganhar|recebe|receba|adiciona|aumenta|recupera)[^.!?;]{0,45}iniciativa") ||
                matches(text, "iniciativa[^.!?;]{0,35}(?:adicional|b[oô]nus|ganha|recebe|aumenta)")) add(Tag.INITIATIVE_GAIN)
            if (matches(text, "(?:perde|perca|reduz|reduza|remove|remova|drena|drene|rouba|roube|retira|subtrai)[^.!?;]{0,45}iniciativa") ||
                matches(text, "iniciativa[^.!?;]{0,35}(?:perdida|reduzida|removida|drenada|roubada)")) add(Tag.INITIATIVE_DRAIN)
            if (matches(text, "(?:transfere|transferir|transfira)[^.!?;]{0,55}iniciativa") ||
                matches(text, "iniciativa[^.!?;]{0,35}(?:transferida|para (?:o|um) aliado)")) {
                add(Tag.INITIATIVE_TRANSFER)
                add(Tag.INITIATIVE_GAIN)
            }
            if (matches(text, "(?:^|[,;\\s])\\d+i(?=$|[,;.!?\\s])|custo[^.!?;]{0,20}iniciativa|gasta[^.!?;]{0,30}iniciativa")) add(Tag.INITIATIVE_COST)
            if (matches(text, "(?:maior|mais|superior)[^.!?;]{0,35}iniciativa") ||
                matches(text, "iniciativa[^.!?;]{0,35}(?:maior|mais alta|superior|excede|acima)")) add(Tag.INITIATIVE_ADVANTAGE)

            // Crash: separar produção, exploração e recuperação muda a direção da sinergia.
            if (matches(text, "(?:causa|causar|força|forçar|coloca|colocar|entra|entrar|deixa|deixar)[^.!?;]{0,55}(?:atordoamento de iniciativa|atordoado|crash)") ||
                matches(text, "(?:atordoamento de iniciativa|crash)[^.!?;]{0,35}(?:causado|forçado)")) add(Tag.CRASH_CAUSE)
            if (matches(text, "(?:alvo|oponente|inimigo|personagem)[^.!?;]{0,40}(?:atordoado|em crash)") ||
                containsAny(text, "contra um alvo atordoado", "contra oponentes atordoados", "se o alvo estiver atordoado", "quando um inimigo é atordoado", "quando um oponente é atordoado")) add(Tag.CRASH_EXPLOIT)
            if (matches(text, "(?:recupera|recuperar|sai|sair|retorna|retornar)[^.!?;]{0,50}(?:atordoamento|crash)") ||
                containsAny(text, "iniciativa base")) add(Tag.CRASH_RECOVERY)

            if (containsAny(text, "participar na batalha", "juntar-se à batalha", "juntar-se à batalha", "join battle")) add(Tag.JOIN_BATTLE)
            if (containsAny(text, "colisão", "clash")) add(Tag.CLASH)

            if (matches(text, "(?:impõe|inflige|aumenta|mantém|preserva)[^.!?;]{0,45}(?:onslaught|penalidade cumulativa)") ||
                containsAny(text, "penalidade de onslaught")) add(Tag.ONSLAUGHT_APPLY)
            if (matches(text, "(?:onslaught|penalidade cumulativa)[^.!?;]{0,55}(?:igual|basead|por ponto|tamanho|valor)") ||
                matches(text, "(?:igual|basead)[^.!?;]{0,45}(?:onslaught|penalidade cumulativa)")) add(Tag.ONSLAUGHT_EXPLOIT)

            if (containsAny(text, "segundo ataque", "ataque adicional", "ataque extra", "não conta como seu ataque", "vários ataques", "múltiplos ataques", "atacar novamente", "contra-atacar todos")) add(Tag.EXTRA_ATTACK)
            if (containsAny(text, "contra-ataque", "contra ataque", "counterattack")) add(Tag.COUNTERATTACK)
            if (containsAny(text, "aparar", "evasão", "parry", "evasion") ||
                matches(text, "(?:aumenta|adiciona|ignora|recupera)[^.!?;]{0,35}defesa")) add(Tag.DEFENSE)
            if (containsAny(text, "faixa de alcance", "desengaj", "investida", "movimento", "avança", "aproxima", "alcance curto", "alcance médio", "alcance longo")) add(Tag.POSITIONING)
            if (containsAny(text, "dano mínimo", "minimum damage", "overwhelming")) add(Tag.MINIMUM_DAMAGE)
            if (containsAny(text, "jogada de ataque", "dados de ataque", "sucessos no ataque", "precisão", "attack roll")) add(Tag.ACCURACY)
            if (containsAny(text, "dano bruto", "jogada de dano", "dados de dano", "damage roll")) add(Tag.DAMAGE)
            if (containsAny(text, "derrub", "cego", "imobil", "desarm") ||
                matches(text, "(?:não pode|impede)[^.!?;]{0,45}(?:mover|atacar|defender|agir|ação)")) add(Tag.CONTROL)
        }
    }

    private fun pairScore(a: Set<Tag>, b: Set<Tag>): Int {
        var score = 0
        fun either(left: Tag, right: Tag, points: Int) {
            if ((left in a && right in b) || (right in a && left in b)) score = maxOf(score, points)
        }
        fun directional(producer: Tag, consumer: Tag, points: Int) {
            if (producer in a && consumer in b) score = maxOf(score, points)
            if (producer in b && consumer in a) score = maxOf(score, points)
        }

        // Relações fortes: um Encanto produz explicitamente a condição consumida pelo outro.
        directional(Tag.CRASH_CAUSE, Tag.CRASH_EXPLOIT, 24)
        directional(Tag.ONSLAUGHT_APPLY, Tag.ONSLAUGHT_EXPLOIT, 22)
        directional(Tag.INITIATIVE_DRAIN, Tag.CRASH_EXPLOIT, 18)
        directional(Tag.INITIATIVE_GAIN, Tag.DECISIVE, 17)
        directional(Tag.INITIATIVE_TRANSFER, Tag.DECISIVE, 15)
        directional(Tag.JOIN_BATTLE, Tag.INITIATIVE_ADVANTAGE, 14)

        // Relações de motor: melhoram a chance/volume de transferência Fulminante.
        either(Tag.WITHERING, Tag.ACCURACY, 13)
        either(Tag.WITHERING, Tag.DAMAGE, 13)
        either(Tag.WITHERING, Tag.MINIMUM_DAMAGE, 14)
        either(Tag.WITHERING, Tag.EXTRA_ATTACK, 16)
        either(Tag.WITHERING, Tag.ONSLAUGHT_APPLY, 14)
        either(Tag.EXTRA_ATTACK, Tag.ONSLAUGHT_EXPLOIT, 16)

        // Sustentação e posicionamento do motor.
        either(Tag.DEFENSE, Tag.COUNTERATTACK, 13)
        either(Tag.DEFENSE, Tag.CLASH, 11)
        either(Tag.COUNTERATTACK, Tag.INITIATIVE_GAIN, 12)
        either(Tag.WITHERING, Tag.POSITIONING, 8)
        either(Tag.EXTRA_ATTACK, Tag.POSITIONING, 8)

        return score
    }

    enum class CombatCharmDomain {
        BRIGA,
        ARMAS_BRANCAS,
        ARQUEIRISMO,
        ARREMESSO,
        MARTIAL_ARTS,
        OTHER
    }

    /**
     * Fronteira de combinação para combate físico.
     *
     * Encantos das quatro Habilidades físicas de combate só formam sinergia
     * com a própria Habilidade. Artes Marciais formam um domínio separado e
     * podem combinar entre estilos diferentes, mas nunca com uma Habilidade
     * física comum. OTHER não recebe bloqueio aqui: Social/Mental e outros
     * domínios têm regras próprias e não podem herdar esta restrição física.
     */
    fun domainForCategory(
        category: String,
        martialStyleCategories: Set<String> = emptySet()
    ): CombatCharmDomain {
        if (martialStyleCategories.any { category.equals(it, ignoreCase = true) })
            return CombatCharmDomain.MARTIAL_ARTS
        val normalized = category.trim().lowercase()
        // Encantos marciais persistem a categoria como nome do estilo
        // ("Estilo Tigre", etc.). Reconhecê-los aqui impede que caiam em OTHER
        // quando atravessam caminhos genéricos de progressão/roadmap.
        if (normalized.startsWith("estilo ") || normalized.endsWith(" style"))
            return CombatCharmDomain.MARTIAL_ARTS
        return when (normalized) {
            "briga", "brawl" -> CombatCharmDomain.BRIGA
            "armas brancas", "melee" -> CombatCharmDomain.ARMAS_BRANCAS
            "arqueirismo", "archery" -> CombatCharmDomain.ARQUEIRISMO
            "arremesso", "thrown" -> CombatCharmDomain.ARREMESSO
            "artes marciais", "martial arts" -> CombatCharmDomain.MARTIAL_ARTS
            else -> CombatCharmDomain.OTHER
        }
    }

    fun domainsCanSynergize(
        firstDomain: CombatCharmDomain,
        secondDomain: CombatCharmDomain
    ): Boolean {
        if (firstDomain == CombatCharmDomain.OTHER || secondDomain == CombatCharmDomain.OTHER) return true
        if (firstDomain == CombatCharmDomain.MARTIAL_ARTS ||
            secondDomain == CombatCharmDomain.MARTIAL_ARTS
        ) return firstDomain == CombatCharmDomain.MARTIAL_ARTS &&
            secondDomain == CombatCharmDomain.MARTIAL_ARTS
        return firstDomain == secondDomain
    }

    fun categoriesCanSynergize(
        firstCategory: String,
        secondCategory: String,
        martialStyleCategories: Set<String> = emptySet(),
        martialStyleResolver: ((String) -> EstiloArteMarcialDefinition?)? = null
    ): Boolean {
        val firstDomain = domainForCategory(firstCategory, martialStyleCategories)
        val secondDomain = domainForCategory(secondCategory, martialStyleCategories)
        if (!domainsCanSynergize(firstDomain, secondDomain)) return false
        if (firstDomain == CombatCharmDomain.MARTIAL_ARTS &&
            secondDomain == CombatCharmDomain.MARTIAL_ARTS &&
            martialStyleResolver != null
        ) {
            val firstStyle = martialStyleResolver(firstCategory)
            val secondStyle = martialStyleResolver(secondCategory)
            // Equipamento compartilhado melhora a afinidade marcial, mas não
            // transforma estilos com armas diferentes em combinação ilegal.
            // Requisitos concretos de uso pertencem ao Encanto/ataque.
            if (firstStyle != null && secondStyle != null) return true
        }
        return true
    }

    fun martialEquipmentAffinity(
        firstStyle: EstiloArteMarcialDefinition,
        secondStyle: EstiloArteMarcialDefinition
    ): Int = EncounterMartialAttackCompatibility.weaponAffinity(firstStyle, secondStyle) +
        EncounterMartialArmorAffinity.affinity(firstStyle, secondStyle)

    fun pairAffinity(
        firstText: String,
        firstDomain: CombatCharmDomain,
        secondText: String,
        secondDomain: CombatCharmDomain
    ): Int =
        if (domainsCanSynergize(firstDomain, secondDomain))
            pairScore(tags(firstText), tags(secondText))
        else 0

    internal fun pairAffinityTags(first: Set<Tag>, second: Set<Tag>): Int =
        pairScore(first, second)

    /** Afinidade textual legada; usada quando o domínio ainda não foi fornecido pelo chamador. */
    fun pairAffinity(firstText: String, secondText: String): Int =
        pairScore(tags(firstText), tags(secondText))

    /**
     * Pontua o candidato contra Encantos concretos já selecionados.
     * Só a melhor relação com cada parceiro conta; isso evita double counting
     * de várias tags descrevendo o mesmo efeito e mantém o score interpretável.
     */
    fun score(candidateText: String, selectedTexts: List<String>): Int =
        scoreTags(tags(candidateText), selectedTexts.map(::tags))

    /** Mesma pontuação de [score], aceitando classificação já calculada. */
    fun scoreTags(candidate: Set<Tag>, selected: List<Set<Tag>>): Int {
        if (candidate.isEmpty()) return 0

        var score = 0
        for (selectedTags in selected) {
            if (selectedTags.isNotEmpty()) score += pairScore(candidate, selectedTags)
        }

        // Valor intrínseco pequeno: somente quando o próprio Encanto fecha um
        // microciclo. O grosso da nota continua vindo de sinergia entre peças.
        if (Tag.WITHERING in candidate && (Tag.ACCURACY in candidate || Tag.DAMAGE in candidate || Tag.MINIMUM_DAMAGE in candidate)) score += 5
        if (Tag.INITIATIVE_DRAIN in candidate && Tag.CRASH_CAUSE in candidate) score += 6
        if (Tag.INITIATIVE_GAIN in candidate && Tag.DECISIVE in candidate) score += 5

        // Custo de Iniciativa deve ser coberto pelo motor. Penalidade gradual,
        // não binária: combos bons podem justificar o gasto, peças órfãs não.
        if (Tag.INITIATIVE_COST in candidate) {
            score -= when {
                score >= 24 -> 2
                score >= 12 -> 4
                else -> 8
            }
        }
        return score
    }

    fun text(def: Any): String = when (def) {
        is EncantoSolarDefinition -> listOf(def.nome, def.tipo, def.palavrasChave, def.custo, def.descricao).joinToString(" ")
        is EncantoSangueDeDragaoDefinition -> listOf(def.nome, def.tipo, def.palavrasChave, def.custo, def.descricao).joinToString(" ")
        is EncantoLunarDefinition -> listOf(def.nome, def.tipo, def.palavrasChave, def.custo, def.descricao).joinToString(" ")
        else -> def.toString()
    }

    /**
     * Ponte Etapa 5: traduz o subconjunto mecânico tipado para as tags maduras
     * do ECS. Mantém o ECS como proprietário da pontuação, sem fazê-lo
     * reavaliar legalidade ou identidade.
     */
    internal fun tagsFromCanonical(tags: Set<EncounterMechanicalTag>): Set<Tag> =
        buildSet {
            tags.forEach {
                when (it) {
                    EncounterMechanicalTag.WITHERING -> add(Tag.WITHERING)
                    EncounterMechanicalTag.DECISIVE -> add(Tag.DECISIVE)
                    EncounterMechanicalTag.CLASH -> add(Tag.CLASH)
                    EncounterMechanicalTag.CRASH -> add(Tag.CRASH_CAUSE)
                    EncounterMechanicalTag.INITIATIVE_GAIN -> add(Tag.INITIATIVE_GAIN)
                    EncounterMechanicalTag.INITIATIVE_DRAIN -> add(Tag.INITIATIVE_DRAIN)
                    EncounterMechanicalTag.DEFENSE -> add(Tag.DEFENSE)
                    EncounterMechanicalTag.ACCURACY -> add(Tag.ACCURACY)
                    EncounterMechanicalTag.DAMAGE -> add(Tag.DAMAGE)
                    // O ECS maduro ainda não possui tags 1:1 para estas
                    // categorias. Não inventamos equivalência nesta ponte.
                    EncounterMechanicalTag.REROLL,
                    EncounterMechanicalTag.COST_REDUCTION,
                    EncounterMechanicalTag.SOCIAL,
                    EncounterMechanicalTag.MENTAL -> Unit
                }
            }
        }

}
