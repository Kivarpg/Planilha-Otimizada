package com.example.data

import com.example.model.Merito
import kotlin.random.Random

/**
 * Busca de combinação exata de pontos de Méritos (Aba 11).
 *
 * Extraído de EncounterMeritDistributionService (refatoração de organização —
 * sem mudança de comportamento). Isola a recursão/backtracking do orquestrador.
 */
internal object EncounterMeritExactPurchase {
    private data class Compra(val definicao: MeritoDefinition, val custo: Int)

    private data class CandidatoPreparado(
        val definicao: MeritoDefinition,
        val custo: Int,
        val prerequisitos: List<EncounterMeritPrerequisiteParser.PreRequisitoParseado>,
        val nomeNormalizado: String,
        val sobrenatural: Boolean
    )

    /**
     * Monta uma combinação de custos que soma exatamente o orçamento.
     * Prefere variedade, mas permite recompra quando o catálogo permite.
     */
    fun comprarPontosExatos(
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
    ): List<Merito> {
        if (total == 0) return emptyList()
        require(total > 0) { "Orçamento de Méritos inválido: $total" }
        // Catálogo vazio (comum em testes unitários) ou sem candidatos
        // elegíveis: devolve lista vazia em vez de falhar a geração inteira.
        if (candidatos.isEmpty()) return emptyList()

        val custosDisponiveis = candidatos.flatMap { it.custosPermitidos }
            .filter { it in 1..total }
            .distinct()

        // Pré-requisitos de Atributos/Habilidades são estáticos durante a
        // geração. Mantemos apenas candidatos que já passam nessa camada;
        // pré-requisitos dependentes de outros Méritos continuam sendo
        // avaliados dentro da busca recursiva.
        // O catálogo é estático e os textos dos pré-requisitos se repetem entre
        // NPCs. O parsing fica compartilhado entre gerações; nesta execução
        // também materializamos os níveis efetivos para evitar duas consultas
        // de Map para cada nome do requisito.
        val niveisEfetivos = buildMap {
            putAll(attributes)
            abilities.forEach { (nome, nivel) ->
                if (nivel > (this[nome] ?: 0)) put(nome, nivel)
            }
        }
        val candidatosSemIdiomaAutomatico = if (excluirDetalhesIdioma.isNotEmpty()) {
            candidatos.filterNot { it.nome.equals("Idioma", ignoreCase = true) }
        } else candidatos
        val candidatosPreparadosBase = candidatosSemIdiomaAutomatico.map { definicao ->
            CandidatoPreparado(
                definicao = definicao,
                custo = 0,
                prerequisitos = definicao.preRequisitos.map(EncounterMeritPrerequisiteParser::parsear),
                nomeNormalizado = definicao.nome.trim().lowercase(),
                sobrenatural = definicao.categoria.equals("sobrenatural", ignoreCase = true)
            )
        }.filter { candidato ->
            EncounterMeritPrerequisiteParser.baseAtendidos(candidato.prerequisitos, niveisEfetivos)
        }
        if (candidatosPreparadosBase.isEmpty()) return emptyList()

        // Índice imutável por custo: cada nível da busca recursiva consulta
        // diretamente os candidatos compatíveis, em vez de refiltrar todo o
        // catálogo. A ordenação aleatória continua sendo feita por tentativa,
        // preservando a característica estocástica do gerador.
        val candidatosPorCusto = custosDisponiveis.associateWith { custo ->
            candidatosPreparadosBase
                .filter { custo in it.definicao.custosPermitidos }
                .map { it.copy(custo = custo) }
        }

        // Otimização estrutural da escolha de Méritos: quando um Mérito já
        // adquirido abre caminhos para outros Méritos do mesmo catálogo, ele
        // recebe prioridade sobre um preenchimento puramente aleatório. Isso
        // preserva o orçamento e todos os pré-requisitos, mas evita desperdiçar
        // uma compra inicial em um terminal quando existe uma cadeia legal
        // que pode ser construída com o mesmo orçamento.
        val desbloqueiosPorNome = mutableMapOf<String, Int>()
        candidatosPreparadosBase.forEach { candidato ->
            candidato.prerequisitos.forEach { requisito ->
                if (requisito is EncounterMeritPrerequisiteParser.PreRequisitoParseado.MeritoNivel) {
                    val chave = requisito.nomeNormalizado
                    desbloqueiosPorNome[chave] = (desbloqueiosPorNome[chave] ?: 0) + 1
                }
            }
        }
        fun pontuacaoEstrutural(candidato: CandidatoPreparado): Int =
            (desbloqueiosPorNome[candidato.nomeNormalizado] ?: 0) * 100
        // Pré-separa a classe de recompra por custo. Durante a busca recursiva,
        // apenas os não-recompráveis dependem do estado `usadosNaoRecompraveis`;
        // recompráveis nunca precisam ser filtrados novamente.
        // A separação e a aleatorização são feitas uma única vez por execução.
        // Antes, cada nó da recursão criava listas temporárias com filter() e
        // depois embaralhava novamente os mesmos candidatos. Como os
        // pré-requisitos dinâmicos só mudam a elegibilidade, e não a população
        // nem o custo, uma ordem aleatória fixa por custo é suficiente e reduz
        // drasticamente alocações e chamadas de Random durante o backtracking.
        val nomesJaAdquiridos = meritosJaAdquiridos
            .map { it.nome.trim().lowercase() }
            .toSet()
        fun ordenarCandidatos(defs: List<CandidatoPreparado>): List<CandidatoPreparado> =
            defs.shuffled(random).sortedWith(
                compareByDescending<CandidatoPreparado> { pontuacaoEstrutural(it) }
                    .thenByDescending { priorizarMeritosExistentes && it.nomeNormalizado in nomesJaAdquiridos }
            )
        val candidatosNaoRecompraveisPorCusto = candidatosPorCusto.mapValues { (_, defs) ->
            ordenarCandidatos(defs.filterNot { it.definicao.podeSerAdquiridoNovamente })
        }
        val candidatosRecompraveisPorCusto = candidatosPorCusto.mapValues { (_, defs) ->
            ordenarCandidatos(defs.filter { it.definicao.podeSerAdquiridoNovamente })
        }
        // A ordem dos custos continua estocástica, mas custos cujo melhor
        // candidato abre uma cadeia de Méritos têm prioridade. Em empate,
        // mantém-se a aleatoriedade original.
        val custosNaOrdem = custosDisponiveis.shuffled(random).sortedWith(
            compareByDescending<Int> { custo ->
                candidatosPorCusto[custo].orEmpty().maxOfOrNull { pontuacaoEstrutural(it) } ?: 0
            }
        )
        val estadosSemSolucao = mutableSetOf<String>()
        val meritosBase = meritosJaAdquiridos

        // Estado incremental da busca. Os pré-requisitos de Mérito só precisam
        // conhecer o maior nível já adquirido de cada nome e se existe algum
        // Mérito sobrenatural. Manter isso incremental evita reconstruir uma
        // List<Merito> e executar any() para cada candidato em cada nó da
        // recursão.
        val nivelMaximoPorMerito = mutableMapOf<String, Int>()
        val valorTotalPorMerito = mutableMapOf<String, Int>()
        meritosBase.forEach { merito ->
            val chave = merito.nome.trim().lowercase()
            val nivelAtual = nivelMaximoPorMerito[chave] ?: 0
            if (merito.valor > nivelAtual) nivelMaximoPorMerito[chave] = merito.valor
            valorTotalPorMerito[chave] = (valorTotalPorMerito[chave] ?: 0) + merito.valor
        }
        var quantidadeSobrenaturais = meritosBase.count {
            it.categoria.equals("sobrenatural", ignoreCase = true)
        }
        fun prerequisitosAtendidos(candidato: CandidatoPreparado): Boolean =
            candidato.prerequisitos.all { requisito ->
                when (requisito) {
                    EncounterMeritPrerequisiteParser.PreRequisitoParseado.Vazio,
                    EncounterMeritPrerequisiteParser.PreRequisitoParseado.TextoLivre -> true
                    is EncounterMeritPrerequisiteParser.PreRequisitoParseado.AtributoOuHabilidade -> true
                    EncounterMeritPrerequisiteParser.PreRequisitoParseado.OutroSobrenatural -> quantidadeSobrenaturais > 0
                    is EncounterMeritPrerequisiteParser.PreRequisitoParseado.MeritoNivel ->
                        (nivelMaximoPorMerito[requisito.nomeNormalizado] ?: 0) >= requisito.nivel
                }
            }

        fun estadoAtual(restante: Int, usadosNaoRecompraveis: Set<String>): String {
            val niveis = nivelMaximoPorMerito.entries
                .sortedBy { it.key }
                .joinToString("|") { "${it.key}:${it.value}" }
            return "$restante#$niveis#${usadosNaoRecompraveis.sorted().joinToString("|")}"
        }

        fun encontrar(
            restante: Int,
            usadosNaoRecompraveis: Set<String>
        ): List<Compra>? {
            if (restante == 0) return emptyList()
            if (!estadosSemSolucao.add(estadoAtual(restante, usadosNaoRecompraveis))) return null

            // Tenta primeiro as aquisições não-recompráveis em todos os custos.
            // Isso evita que uma recompra barata de um Mérito-base consuma o
            // orçamento antes de um Mérito dependente, quando a cadeia inteira
            // ainda cabe no orçamento. Se nenhuma combinação funcionar, a
            // segunda passagem permite recompras normalmente.
            for (custo in custosNaOrdem) {
                if (custo > restante) continue
                val naoRecompraveis = candidatosNaoRecompraveisPorCusto[custo].orEmpty()
                for (candidato in naoRecompraveis) {
                    if (candidato.definicao.nome in usadosNaoRecompraveis || !prerequisitosAtendidos(candidato)) continue
                    val totalAtual = valorTotalPorMerito[candidato.nomeNormalizado] ?: 0
                    if (limitePorMerito != null && totalAtual + custo > limitePorMerito) continue
                    val compra = Compra(candidato.definicao, custo)
                    val chave = candidato.nomeNormalizado
                    val nivelAnterior = nivelMaximoPorMerito[chave]
                    val valorTotalAnterior = valorTotalPorMerito[chave] ?: 0
                    val novoTotal = valorTotalAnterior + custo
                    valorTotalPorMerito[chave] = novoTotal
                    nivelMaximoPorMerito[chave] = if (limitePorMerito != null) novoTotal else maxOf(nivelAnterior ?: 0, custo)
                    if (candidato.sobrenatural) quantidadeSobrenaturais++
                    val resto = encontrar(restante - custo, usadosNaoRecompraveis + candidato.definicao.nome)
                    if (resto != null) return listOf(compra) + resto

                    if (nivelAnterior == null) nivelMaximoPorMerito.remove(chave) else nivelMaximoPorMerito[chave] = nivelAnterior
                    if (valorTotalAnterior == 0) valorTotalPorMerito.remove(chave) else valorTotalPorMerito[chave] = valorTotalAnterior
                    if (candidato.sobrenatural) quantidadeSobrenaturais--
                }
            }

            for (custo in custosNaOrdem) {
                if (custo > restante) continue
                val recompraveis = candidatosRecompraveisPorCusto[custo].orEmpty()
                for (candidato in recompraveis) {
                    if (!prerequisitosAtendidos(candidato)) continue
                    val totalAtual = valorTotalPorMerito[candidato.nomeNormalizado] ?: 0
                    if (limitePorMerito != null && totalAtual + custo > limitePorMerito) continue
                    val compra = Compra(candidato.definicao, custo)
                    val chave = candidato.nomeNormalizado
                    val nivelAnterior = nivelMaximoPorMerito[chave]
                    val valorTotalAnterior = valorTotalPorMerito[chave] ?: 0
                    val novoTotal = valorTotalAnterior + custo
                    valorTotalPorMerito[chave] = novoTotal
                    nivelMaximoPorMerito[chave] = if (limitePorMerito != null) novoTotal else maxOf(nivelAnterior ?: 0, custo)
                    if (candidato.sobrenatural) quantidadeSobrenaturais++
                    val resto = encontrar(restante - custo, usadosNaoRecompraveis)
                    if (resto != null) return listOf(compra) + resto

                    if (nivelAnterior == null) nivelMaximoPorMerito.remove(chave) else nivelMaximoPorMerito[chave] = nivelAnterior
                    if (valorTotalAnterior == 0) valorTotalPorMerito.remove(chave) else valorTotalPorMerito[chave] = valorTotalAnterior
                    if (candidato.sobrenatural) quantidadeSobrenaturais--
                }
            }
            return null
        }

        // Correção real: a priorização "por empate de custo" acima não
        // ajudava quando o único custo que fecha o orçamento sozinho (ex.:
        // total=4 com um recomprável custando exatamente 4) nem sequer é
        // um custo válido pro não-recomprável — nesse caso ele nunca
        // entrava na disputa. Aqui, tenta EXPLICITAMENTE um Mérito estrutural primeiro, incluindo
        // também aquisições repetíveis: uma cadeia pode exigir que o
        // Mérito-base seja repetível (ex.: Influência 1 -> Mérito 2).
        // A busca recursiva só aceita o prefixo se o orçamento restante
        // puder ser fechado legalmente; se nenhuma tentativa funcionar,
        // cai para o algoritmo geral.
        val candidatosEstruturaisComCusto = candidatosPorCusto
            .values
            .flatten()
            .shuffled(random)
            .sortedByDescending { pontuacaoEstrutural(it) }
        val comprasComPrioridade = candidatosEstruturaisComCusto.firstNotNullOfOrNull { candidato ->
            if (!prerequisitosAtendidos(candidato)) {
                return@firstNotNullOfOrNull null
            }
            val totalAtual = valorTotalPorMerito[candidato.nomeNormalizado] ?: 0
            if (limitePorMerito != null && totalAtual + candidato.custo > limitePorMerito) {
                return@firstNotNullOfOrNull null
            }
            val chave = candidato.nomeNormalizado
            val nivelAnterior = nivelMaximoPorMerito[chave]
            val valorTotalAnterior = valorTotalPorMerito[chave] ?: 0
            val novoTotal = valorTotalAnterior + candidato.custo
            valorTotalPorMerito[chave] = novoTotal
            nivelMaximoPorMerito[chave] = if (limitePorMerito != null) novoTotal else maxOf(nivelAnterior ?: 0, candidato.custo)
            if (candidato.sobrenatural) quantidadeSobrenaturais++
            val resto = encontrar(
                total - candidato.custo,
                if (candidato.definicao.podeSerAdquiridoNovamente) emptySet()
                else setOf(candidato.definicao.nome)
            )
            if (resto != null) {
                listOf(Compra(candidato.definicao, candidato.custo)) + resto
            } else {
                if (nivelAnterior == null) nivelMaximoPorMerito.remove(chave) else nivelMaximoPorMerito[chave] = nivelAnterior
                if (valorTotalAnterior == 0) valorTotalPorMerito.remove(chave) else valorTotalPorMerito[chave] = valorTotalAnterior
                if (candidato.sobrenatural) quantidadeSobrenaturais--
                null
            }
        }

        val compras = comprasComPrioridade ?: run {
            // A tentativa prioritária usa a mesma busca recursiva e pode
            // registrar estados sem solução que eram impossíveis apenas
            // porque o primeiro prefixo foi imposto. Esses estados não podem
            // bloquear a busca geral, que pode começar por uma recompra
            // estruturalmente necessária (ex.: Influência 1 -> Mérito 2).
            estadosSemSolucao.clear()
            val resultado = encontrar(total, emptySet())
            check(resultado != null) { "Não foi possível distribuir exatamente $total pontos de Méritos." }
            resultado
        }

        return compras.map { compra ->
            Merito(
                nome = compra.definicao.nome,
                valor = compra.custo,
                categoria = compra.definicao.categoria,
                origemAutomatica = origem
            )
        }
    }

}
