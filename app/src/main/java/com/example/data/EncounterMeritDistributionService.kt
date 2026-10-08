package com.example.data

import com.example.model.Merito
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import kotlin.random.Random


/**
 * Distribuição automática de Méritos da Aba 11.
 *
 * Regras centrais:
 * - Solar/Lunar: 10 pontos totais; 6 são consumidos pelos dois Artefatos
 *   obrigatórios e os 4 restantes são distribuídos normalmente.
 * - Sangue de Dragão: 13 pontos normais; 6 são consumidos pelos dois
 *   Artefatos e os 7 restantes são distribuídos normalmente. Depois recebe
 *   5 pontos ADICIONAIS exclusivamente nos Méritos da lista restrita.
 * - Artefatos de arma/armadura custam pontos do pool normal, 3 cada.
 * - Sangue de Dragão nunca recebe Méritos sobrenaturais.
 */
object EncounterMeritDistributionService {
    private const val PONTOS_SOLAR_LUNAR = 10
    private const val PONTOS_NORMAIS_SANGUE = 13
    private const val PONTOS_RESTRITOS = 5
    private const val CUSTO_ARTEFATO = 3

    private val IDIOMAS_ENCONTRO = listOf(
        "Alto Reino",
        "Baixo Reino",
        "Língua dos Dragões",
        "Dialeto dos Rios",
        "Língua do Céu",
        "Língua de Fogo",
        "Língua da Floresta",
        "Língua do Mar",
        "Dialeto da Guilda",
        "Antigo Reino"
    )

    private fun comprarPontosExatos(
        total: Int,
        candidatos: List<MeritoDefinition>,
        attributes: Map<String, Int>,
        abilities: Map<String, Int>,
        meritosJaAdquiridos: List<Merito>,
        random: Random,
        origem: String,
        limitePorMerito: Int? = null,
        excluirDetalhesIdioma: Set<String> = emptySet(),
        priorizarMeritosExistentes: Boolean = false
    ): List<Merito> = EncounterMeritExactPurchase.comprarPontosExatos(
        total,
        candidatos,
        attributes,
        abilities,
        meritosJaAdquiridos,
        random,
        origem,
        limitePorMerito,
        excluirDetalhesIdioma,
        priorizarMeritosExistentes
    )



    val MERITOS_RESTRITOS = setOf(
        "Apoio", "Comando", "Contatos", "Seguidores",
        "Influência", "Idioma", "Recursos", "Vassalos"
    )

    fun distribuir(
        npc: NpcEncontro,
        catalogo: List<MeritoDefinition>,
        random: Random,
        origemNomeSangueDeDragao: OrigemNomeSangueDeDragao = OrigemNomeSangueDeDragao.SEM_CASTA,
        preferirArtistaMarcial: Boolean = false,
        exigirArtistaMarcial: Boolean = false
    ): List<Merito> {
        // Catálogo vazio: nada pra distribuir, retorna sem Méritos em vez
        // de exigir candidatos elegíveis mais adiante — pedido implícito
        // do usuário: chamadores que intencionalmente não usam Méritos
        // (ex.: testes focados só em outro aspecto da geração) não devem
        // crashar por causa disso.
        if (catalogo.isEmpty()) return emptyList()
        val sangue = npc.tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO
        require(npc.arma?.tipo == "Artefato") {
            "Gerador de Encontros: a Arma Artefato é obrigatória para ${npc.tipoExaltado}."
        }
        require(npc.armadura?.tipo == "Artefato") {
            "Gerador de Encontros: a Armadura Artefato é obrigatória para ${npc.tipoExaltado}."
        }
        val custoArtefatos = CUSTO_ARTEFATO + (npc.armadura?.custoMeritoArtefato ?: CUSTO_ARTEFATO)

        // O orçamento normal já inclui o custo obrigatório dos dois Artefatos.
        // Solar/Lunar: 10 totais - 6 de Artefatos = 4 pontos livres.
        // Sangue de Dragão: 13 normais - 6 de Artefatos = 7 pontos livres.
        val pontosNormaisTotais = if (sangue) PONTOS_NORMAIS_SANGUE else PONTOS_SOLAR_LUNAR
        val pontosNormaisDisponiveis = pontosNormaisTotais - custoArtefatos
        require(pontosNormaisDisponiveis >= 0) {
            "Orçamento de Méritos insuficiente para os Artefatos obrigatórios."
        }

        // Sangue de Dragão do Império e de Lookshy não pode adquirir o
        // Mérito Culto. A restrição vale tanto para o pool normal quanto
        // para qualquer pool adicional usado pela geração automática.
        val cultoPermitido = !(sangue && origemNomeSangueDeDragao in setOf(
            OrigemNomeSangueDeDragao.IMPERIO,
            OrigemNomeSangueDeDragao.LOOKSHY
        ))

        val candidatosNormaisBase = catalogo.filter { def ->
            // Sangue de Dragão nunca recebe Méritos sobrenaturais.
            (def.categoria == "normal" || (!sangue && def.categoria == "sobrenatural")) &&
                (def.restritoAoTemplate == null || (def.restritoAoTemplate == "SangueDeDragao" && sangue)) &&
                def.nome != "Artefato" &&
                (cultoPermitido || !def.nome.equals("Culto", ignoreCase = true))
        }
        val candidatosRestritos = catalogo.filter { def ->
            def.categoria == "normal" &&
                def.nome in MERITOS_RESTRITOS &&
                (def.restritoAoTemplate == null || (def.restritoAoTemplate == "SangueDeDragao" && sangue)) &&
                (cultoPermitido || !def.nome.equals("Culto", ignoreCase = true))
        }

        val resultado = mutableListOf<Merito>()

        // Rota marcial explícita: Artista Marcial •••• é requisito, não preferência.
        // Reserva os quatro pontos antes de qualquer compra opcional.
        val definicaoArtistaMarcial = candidatosNormaisBase.firstOrNull {
            it.nome.equals("Artista Marcial", ignoreCase = true) && 4 in it.custosPermitidos
        }
        val custoArtistaMarcialObrigatorio = if (exigirArtistaMarcial) 4 else 0
        if (exigirArtistaMarcial) {
            requireNotNull(definicaoArtistaMarcial) {
                "Rota de Artes Marciais exige o Mérito Artista Marcial •••• no catálogo."
            }
            require(pontosNormaisDisponiveis >= 4) {
                "Orçamento de Méritos insuficiente para Artista Marcial ••••."
            }
            resultado += Merito(
                nome = definicaoArtistaMarcial.nome,
                valor = 4,
                categoria = definicaoArtistaMarcial.categoria,
                origemAutomatica = "Encontro: requisito de Artes Marciais"
            )
        }

        // Cada NPC recebe exatamente um idioma por meio do Mérito Idioma.
        // Antigo Reino só participa da geração automática quando o NPC já possui
        // Ocultismo 1 ou Conhecimento 1; a mesma regra vale globalmente.
        val definicaoIdioma = catalogo.firstOrNull {
            it.nome.equals("Idioma", ignoreCase = true) && it.categoria == "normal"
        }
        val idiomaNativo = when (origemNomeSangueDeDragao) {
            OrigemNomeSangueDeDragao.IMPERIO -> "Alto Reino"
            OrigemNomeSangueDeDragao.LOOKSHY -> "Dialeto dos Rios"
            else -> null
        }
        // O idioma nativo pertence exclusivamente ao campo de idioma nativo
        // do NPC. O Mérito "Idioma" representa uma língua adicional e,
        // portanto, nunca pode receber o idioma nativo.
        val idiomaInicial = selecionarIdiomaAdicional(emptySet(), idiomaNativo, npc.abilities, random)
        val pontosAposRequisitos = pontosNormaisDisponiveis - custoArtistaMarcialObrigatorio
        val quantidadeIdiomasObrigatorios = if (definicaoIdioma != null && pontosAposRequisitos > 0) 1 else 0

        // Primeiro gasta-se todo o orçamento normal restante. Para Solar/Lunar
        // são 4 pontos; para Sangue de Dragão são 7. Esses pontos podem ser
        // usados em qualquer Mérito elegível, inclusive nos Méritos da lista
        // restrita, como Apoio. Portanto, Apoio pode consumir parte ou todos
        // os 7 pontos normais de Sangue de Dragão. A única exceção é Artefato,
        // já comprado separadamente.
        if (quantidadeIdiomasObrigatorios > 0) {
            val definicaoIdiomaConfirmada = checkNotNull(definicaoIdioma)
            resultado += Merito(
                nome = definicaoIdiomaConfirmada.nome,
                valor = 1,
                categoria = definicaoIdiomaConfirmada.categoria,
                detalhe = idiomaInicial,
                origemAutomatica = "Encontro: Méritos normais"
            )
        }
        val pontosNormaisRestantes = pontosAposRequisitos - quantidadeIdiomasObrigatorios
        check(pontosNormaisRestantes >= 0) {
            "Orçamento de Méritos insuficiente para a distribuição obrigatória de Idiomas."
        }
        resultado += comprarPontosExatos(
            pontosNormaisRestantes,
            candidatosNormaisBase
                .filterNot { exigirArtistaMarcial && it.nome.equals("Artista Marcial", ignoreCase = true) }
                .sortedWith(
                compareByDescending<MeritoDefinition> { preferirArtistaMarcial && it.nome.equals("Artista Marcial", ignoreCase = true) }
                    .thenBy { it.nome }
            ),
            attributes = npc.attributes,
            abilities = npc.abilities,
            meritosJaAdquiridos = resultado,
            random = random,
            origem = "Encontro: Méritos normais",
            limitePorMerito = if (sangue) 5 else null,
            excluirDetalhesIdioma = resultado.filter { it.nome.equals("Idioma", ignoreCase = true) }.map { it.detalhe }.toSet()
        )

        // Somente Sangue de Dragão recebe os 5 pontos adicionais. Eles são
        // obrigatoriamente gastos na lista fechada e não fazem parte dos 13.
        if (sangue) {
            resultado += comprarPontosExatos(
                PONTOS_RESTRITOS,
                candidatosRestritos,
                attributes = npc.attributes,
                abilities = npc.abilities,
                meritosJaAdquiridos = resultado,
                random = random,
                origem = "Encontro: Méritos restritos adicionais",
                limitePorMerito = 5,
                priorizarMeritosExistentes = true
            )
            // Catálogo vazio é um cenário válido em testes e em chamadas
            // que ainda não carregaram os Méritos. Nesse caso não há como
            // gastar os 5 pontos restritos e a geração deve prosseguir sem
            // transformar a ausência de catálogo em exceção. Quando existem
            // candidatos restritos, a validação permanece obrigatória.
            if (candidatosRestritos.isNotEmpty()) {
                check(
                    resultado.filter { it.origemAutomatica == "Encontro: Méritos restritos adicionais" }
                        .sumOf { it.valor } == PONTOS_RESTRITOS
                ) {
                    "Distribuição de Méritos inválida: os pontos restritos adicionais não totalizam $PONTOS_RESTRITOS."
                }
            }

            val meritosComIdioma = preencherDetalhesDeIdioma(resultado, idiomaNativo, npc.abilities, random)
            val meritosFinais = fundirMeritosComLimite(meritosComIdioma, 5)
            resultado.clear()
            resultado += meritosFinais
        }

        // Os Méritos de Artefato usam o nome real do item no detalhe. A UI
        // combina nome, graduação e especificação no formato padronizado.
        if (npc.arma?.tipo == "Artefato") {
            resultado += Merito(
                nome = "Artefato",
                valor = CUSTO_ARTEFATO,
                categoria = "normal",
                detalhe = npc.arma.nome,
                origemAutomatica = "Encontro: Artefato"
            )
        }
        if (npc.armadura?.tipo == "Artefato") {
            resultado += Merito(
                nome = "Artefato",
                valor = npc.armadura.custoMeritoArtefato,
                categoria = "normal",
                detalhe = npc.armadura.nome,
                origemAutomatica = "Encontro: Artefato"
            )
        }

        val totalArtefatos = resultado.filter { it.origemAutomatica == "Encontro: Artefato" }.sumOf { it.valor }
        val totalEsperado = if (sangue) 18 else 10
        // Só valida orçamento completo quando o catálogo permitiu comprar
        // pontos além dos Artefatos obrigatórios (testes com catálogo vazio
        // ficam só com os 6 pontos de Artefato).
        if (catalogo.isNotEmpty() && resultado.any { it.origemAutomatica != "Encontro: Artefato" }) {
            check(resultado.sumOf { it.valor } == totalEsperado) {
                "Distribuição de Méritos inválida: ${resultado.sumOf { it.valor }}/$totalEsperado pontos."
            }
        }
        if (sangue) {
            check(resultado.none { it.categoria == "sobrenatural" }) {
                "Sangue de Dragão não pode receber Méritos sobrenaturais."
            }
        }
        return resultado
    }

    private fun preencherDetalhesDeIdioma(
        meritos: List<Merito>,
        idiomaNativo: String?,
        abilities: Map<String, Int>,
        random: Random
    ): List<Merito> {
        val usados = mutableSetOf<String>()
        var primeiraCompra = true
        return meritos.map { merito ->
            if (!merito.nome.equals("Idioma", ignoreCase = true)) return@map merito
            if (merito.detalhe.isNotBlank()) {
                usados += merito.detalhe.trim().lowercase()
                primeiraCompra = false
                return@map merito
            }
            val idioma = when {
                primeiraCompra && idiomaNativo != null -> idiomaNativo
                else -> selecionarIdiomaAdicional(usados, idiomaNativo, abilities, random)
            }
            usados += idioma.lowercase()
            primeiraCompra = false
            merito.copy(detalhe = idioma)
        }
    }

    private fun selecionarIdiomaAdicional(
        usados: Set<String>,
        idiomaNativo: String?,
        abilities: Map<String, Int>,
        random: Random
    ): String {
        val proibidos = usados.map { it.trim().lowercase() }.toMutableSet()
        // Na segunda compra do Sangue de Dragão, o idioma nativo não pode
        // voltar a ser escolhido. A regra continua valendo nas compras
        // adicionais seguintes.
        idiomaNativo?.let { proibidos += it.trim().lowercase() }
        val disponiveis = IDIOMAS_ENCONTRO.filter { idioma ->
            idioma.trim().lowercase() !in proibidos && LanguageAcquisitionRules.podeAdquirir(idioma, abilities)
        }
        require(disponiveis.isNotEmpty()) {
            "Não há idioma adicional disponível após excluir os idiomas já usados e o idioma nativo."
        }
        return disponiveis.random(random)
    }

    private fun fundirMeritosComLimite(
        resultadoOriginal: List<Merito>,
        limite: Int
    ): List<Merito> {
        val acumulados = linkedMapOf<String, Merito>()
        resultadoOriginal.forEach { merito ->
            // Cada compra de Idioma representa um idioma diferente e precisa
            // permanecer separada para que o detalhe "Idioma (X)" não seja
            // perdido ao consolidar os Méritos do Sangue de Dragão.
            if (merito.nome.equals("Idioma", ignoreCase = true)) {
                acumulados["idioma:${merito.id}"] = merito.copy(valor = merito.valor.coerceAtMost(limite))
                return@forEach
            }
            val chave = merito.nome.trim().lowercase()
            val existente = acumulados[chave]
            if (existente == null) {
                acumulados[chave] = merito.copy(valor = merito.valor.coerceAtMost(limite))
            } else {
                acumulados[chave] = existente.copy(valor = (existente.valor + merito.valor).coerceAtMost(limite))
            }
        }
        return acumulados.values.toList()
    }



}
