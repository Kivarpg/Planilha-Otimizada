package com.example.data

import kotlin.random.Random
import com.example.model.ArquetipoEncontro

/**
 * Seleção de Feitiços pra NPCs de encontro — pedido explícito do usuário.
 * Antes desta implementação, `feiticos` era sempre uma lista vazia na
 * geração de NPCs, independente do arquétipo.
 */
object EncounterSpellSelectionService {

    // Lista fechada de Feitiços Terrestres considerados na seleção —
    // exatamente os 7 nomes que o usuário especificou.
    private val NOMES_TERRESTRES_CONSIDERADOS = setOf(
        "Demônio do Primeiro Círculo",
        "Cão de Caça dos Cinco Ventos",
        "Mensageiro Infalível",
        "Invocar Elemental",
        "Pele Invulnerável de Bronze",
        "Virtuoso Guardião da Chama",
        "Voo da Separação"
    )

    private const val EXPRESSAO_CONTROLE = "feitiço de controle"

    private data class IndiceCatalogo(
        val terrestresConsiderados: List<FeiticoDefinition>,
        val controlesPorCirculo: Map<String, List<FeiticoDefinition>>
    )

    private fun ehFeiticoDeControle(feitico: FeiticoDefinition): Boolean =
        feitico.descricao.contains(EXPRESSAO_CONTROLE, ignoreCase = true)

    private fun indexarCatalogo(
        catalogo: List<FeiticoDefinition>,
        circulosControle: Set<String> = emptySet(),
        incluirTerrestresConsiderados: Boolean = false
    ): IndiceCatalogo {
        if (!incluirTerrestresConsiderados && circulosControle.isEmpty()) {
            return IndiceCatalogo(emptyList(), emptyMap())
        }
        // Preserva a ordem de cada circulo com uma unica passagem pelo
        // catalogo, inclusive quando ambas as categorias forem solicitadas.
        val terrestres = ArrayList<FeiticoDefinition>()
        val controles = LinkedHashMap<String, MutableList<FeiticoDefinition>>()
        for (feitico in catalogo) {
            if (
                incluirTerrestresConsiderados &&
                feitico.circulo == "Terrestre" &&
                feitico.nome in NOMES_TERRESTRES_CONSIDERADOS
            ) {
                terrestres.add(feitico)
            }
            if (feitico.circulo in circulosControle && ehFeiticoDeControle(feitico)) {
                controles.getOrPut(feitico.circulo) { ArrayList() }.add(feitico)
            }
        }
        return IndiceCatalogo(terrestres, controles)
    }

    /**
     * Seleciona Feitiços Terrestres dentre os 7 considerados.
     * - Seleção aleatória (1.1).
     * - Prefere incluir ao menos 1, mas não é exigência absoluta (1.2).
     * - Nunca seleciona exatamente 1 no total, exceto quando esse único
     *   Feitiço for um "feitiço de controle" (1.3).
     *
     * [quantidadeAlvo] é quantos Feitiços Terrestres tentar incluir —
     * a função ajusta o resultado pra nunca violar a regra 1.3.
     */
    fun selecionarTerrestres(
        catalogo: List<FeiticoDefinition>,
        quantidadeAlvo: Int,
        random: Random
    ): List<FeiticoDefinition> {
        if (quantidadeAlvo <= 0) return emptyList()
        val disponiveis = indexarCatalogo(catalogo, incluirTerrestresConsiderados = true).terrestresConsiderados
        return selecionarTerrestresDisponiveis(disponiveis, quantidadeAlvo, random)
    }

    private fun selecionarTerrestresDisponiveis(
        disponiveis: List<FeiticoDefinition>,
        quantidadeAlvo: Int,
        random: Random
    ): List<FeiticoDefinition> {
        if (quantidadeAlvo <= 0 || disponiveis.isEmpty()) return emptyList()

        val embaralhados = disponiveis.shuffled(random)
        var selecionados = embaralhados.take(quantidadeAlvo.coerceAtMost(disponiveis.size))

        // 1.3: nunca exatamente 1 no total, a menos que seja um feitiço
        // de controle.
        if (selecionados.size == 1 && !ehFeiticoDeControle(selecionados[0])) {
            selecionados = if (disponiveis.size >= 2) {
                embaralhados.take(2)
            } else {
                emptyList()
            }
        }
        return selecionados
    }

    /**
     * Seleciona Feitiços Celestiais ou Solares. A única condição de
     * elegibilidade é o texto conter "feitiço de controle" (2.2) — sem
     * outros critérios de preferência ou restrição.
     */
    fun selecionarCelestiaisOuSolares(
        catalogo: List<FeiticoDefinition>,
        circulo: String,
        quantidadeAlvo: Int,
        random: Random
    ): List<FeiticoDefinition> {
        if (quantidadeAlvo <= 0) return emptyList()
        val elegiveis = indexarCatalogo(catalogo, circulosControle = setOf(circulo))
            .controlesPorCirculo[circulo]
            .orEmpty()
        if (elegiveis.isEmpty()) return emptyList()
        return elegiveis.shuffled(random).take(quantidadeAlvo.coerceAtMost(elegiveis.size))
    }

    /**
     * Feitiçaria do Círculo Terrestre concede 1 Feitiço gratuito adicional.
     * O Feitiço fica fora do orçamento normal de Encantos e, portanto,
     * eleva o total de poderes iniciais de 15 para 16.
     *
     * A concessão é independente da quantidade de Feitiços que o arquétipo
     * já tenha recebido pela seleção normal. O sorteio evita duplicar um
     * Feitiço já selecionado quando houver outro Terrestre disponível.
     */
    fun adicionarFeiticoGratuitoSeNecessario(
        possuiFeiticariaTerrestre: Boolean,
        selecionados: List<FeiticoDefinition>,
        catalogo: List<FeiticoDefinition>,
        random: Random
    ): List<FeiticoDefinition> {
        if (!possuiFeiticariaTerrestre) return selecionados

        val nomesSelecionados = selecionados.map { it.nome }.toSet()
        // Uma passagem pelo catálogo alimenta as duas listas, preservando
        // a ordem e os sorteios da implementação anterior.
        val terrestres = ArrayList<FeiticoDefinition>()
        val aindaNaoSelecionados = ArrayList<FeiticoDefinition>()
        for (feitico in catalogo) {
            if (feitico.circulo != "Terrestre") continue
            terrestres.add(feitico)
            if (feitico.nome !in nomesSelecionados) aindaNaoSelecionados.add(feitico)
        }
        // Se a seleção normal já consumiu todos os Terrestres disponíveis,
        // reutilizar um selecionado materializa a vaga sem duplicar feitiços.
        val gratuito = aindaNaoSelecionados.shuffled(random).firstOrNull()
            ?: selecionados.firstOrNull { it.circulo == "Terrestre" }
            ?: terrestres.shuffled(random).firstOrNull()
            ?: return selecionados

        return if (gratuito.nome in nomesSelecionados) selecionados else selecionados + gratuito
    }

    /**
     * Orquestra a seleção completa pra um NPC, respeitando a diretriz de
     * criação de inimigos: Físico tem consideravelmente menos círculos de
     * magia que Social; Social um pouco menos que Mental; Mental sempre
     * possui Feitiços, com pelo menos 4 (sem contar um eventual feitiço
     * gratuito de arquétipo. O feitiço inicial gratuito é concedido
     * separadamente quando o NPC desbloqueia Feitiçaria Terrestre).
     */
    fun selecionarParaArquetipo(
        arquetipo: ArquetipoEncontro,
        catalogoTerrestre: List<FeiticoDefinition>,
        catalogoCelestial: List<FeiticoDefinition>,
        catalogoSolar: List<FeiticoDefinition>,
        possuiFeiticariaTerrestre: Boolean,
        random: Random,
        possuiFeiticariaCelestial: Boolean = true,
        possuiFeiticariaSolar: Boolean = true
    ): List<FeiticoDefinition> {
        // Nenhum NPC pode possuir Feitiços sem ter adquirido primeiro o
        // Encanto que desbloqueia o Círculo Terrestre. Esta guarda fica no
        // serviço central para impedir que qualquer gerador contorne o
        // pré-requisito por acidente.
        if (!possuiFeiticariaTerrestre) return emptyList()

        // APPROVED PERFORMANCE REFACTOR
        // Indexa somente os catálogos/círculos que o arquétipo realmente
        // consulta. Físico nunca usa Celestial/Solar; Social nunca usa Solar.
        // Isso elimina scans completos de catálogos potencialmente grandes
        // sem alterar nenhuma decisão aleatória ou regra de elegibilidade.
        val indiceTerrestre = indexarCatalogo(
            catalogoTerrestre,
            incluirTerrestresConsiderados = true
        )
        val indiceCelestial = if (
            possuiFeiticariaCelestial &&
            (arquetipo == ArquetipoEncontro.SOCIAL || arquetipo == ArquetipoEncontro.MENTAL)
        ) {
            indexarCatalogo(catalogoCelestial, circulosControle = setOf("Celestial"))
        } else {
            IndiceCatalogo(emptyList(), emptyMap())
        }
        val indiceSolar = if (possuiFeiticariaSolar && arquetipo == ArquetipoEncontro.MENTAL) {
            indexarCatalogo(catalogoSolar, circulosControle = setOf("Solar"))
        } else {
            IndiceCatalogo(emptyList(), emptyMap())
        }

        fun selecionarControle(indice: IndiceCatalogo, circulo: String, quantidade: Int): List<FeiticoDefinition> {
            if (quantidade <= 0) return emptyList()
            val elegiveis = indice.controlesPorCirculo[circulo].orEmpty()
            if (elegiveis.isEmpty()) return emptyList()
            return elegiveis.shuffled(random).take(quantidade.coerceAtMost(elegiveis.size))
        }

        return when (arquetipo) {
            ArquetipoEncontro.FISICO -> {
                // Bem menos círculos de magia que Social — a maioria dos
                // NPCs físicos não deveria ter feitiçaria alguma.
                if (random.nextInt(0, 100) < 25) {
                    selecionarTerrestresDisponiveis(indiceTerrestre.terrestresConsiderados, quantidadeAlvo = 1, random)
                } else emptyList()
            }
            ArquetipoEncontro.SOCIAL -> {
                // Um pouco menos que Mental, mas presença bem mais comum
                // que Físico.
                val terrestres = selecionarTerrestresDisponiveis(indiceTerrestre.terrestresConsiderados, quantidadeAlvo = random.nextInt(1, 3), random)
                val celestiais = selecionarControle(indiceCelestial, "Celestial", random.nextInt(0, 2))
                terrestres + celestiais
            }
            ArquetipoEncontro.MENTAL -> {
                // Sempre possui Feitiços — pelo menos 4 no total.
                // Materializa o catálogo terrestre considerado uma única vez.
                // O resultado inicial e o fallback compartilham exatamente a
                // mesma população elegível, evitando uma segunda filtragem.
                val terrestresDisponiveis = indiceTerrestre.terrestresConsiderados
                val terrestres = selecionarTerrestresDisponiveis(
                    terrestresDisponiveis,
                    quantidadeAlvo = random.nextInt(1, 3),
                    random
                )
                val celestiais = selecionarControle(indiceCelestial, "Celestial", random.nextInt(1, 3))
                var solares = selecionarControle(indiceSolar, "Solar", 1)
                var total = terrestres + celestiais + solares
                // Garante o mínimo de 4, complementando com mais Terrestres
                // se a rolagem inicial não tiver alcançado o piso.
                if (total.size < 4) {
                    val jaEscolhidos = total.map { it.nome }.toSet()
                    val extrasTerrestres = terrestresDisponiveis
                        .filter { it.nome !in jaEscolhidos }
                        .shuffled(random)
                        .take(4 - total.size)
                    total = total + extrasTerrestres
                }
                total
            }
        }
    }
}
