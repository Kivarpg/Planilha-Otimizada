package com.example.viewmodel

import com.example.model.isSolar

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.data.EncantosSolaresCatalog
import com.example.data.EncantoSolarDefinition
import com.example.data.FeiticariaCatalog
import com.example.data.FeiticoDefinition
import com.example.data.paraFormatoSolar
import com.example.data.ArtesMarciaisCatalog
import com.example.model.CharacterSheet
import com.example.model.gavetaChave
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


// Todos os Encantos de uma habilidade (elegíveis e não elegíveis), para a
// Aba 8: os não elegíveis continuam visíveis (bloqueados), em vez de
// simplesmente somem da lista.
data class EncantoComElegibilidade(
    val definicao: EncantoSolarDefinition,
    val elegivel: Boolean,
    val jaAdquirido: Boolean,
    // Quantas cópias já foram adquiridas e o máximo permitido — usados
    // pela UI para decidir entre um checkbox simples (máximo 1) e um
    // contador +/- (encantos com múltiplas compras, ex.: Técnica do
    // Corpo de Touro, ou qualquer encanto com "Uma recompra com
    // Essência" no texto).
    val quantidade: Int = if (jaAdquirido) 1 else 0,
    val maximo: Int = 1
)

// Domínio de Encantos/Feitiçaria (Aba 8) — o mais complexo do app:
// elegibilidade (pré-requisitos, contagem por grupo, cláusulas "ou"),
// limite de recompra por Encanto, e o CRUD de Encantos/Feitiços em si.
// Depende de mais coisas que qualquer outro domínio extraído até agora:
// além de sheetState/commitmentError/experienceActions (mesmo padrão dos
// outros), precisa dos DOIS catálogos (que exigem Application pra
// construir, por isso são recebidos prontos, não reconstruídos aqui).
class CharmsActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val commitmentError: MutableStateFlow<String?>,
    private val encantoCatalog: EncantosSolaresCatalog,
    private val feiticariaCatalog: FeiticariaCatalog,
    private val experienceActions: ExperienceActions,
    private val encantosSangueDragoesCatalog: com.example.data.EncantosSangueDosDragoesCatalog? = null,
    private val encantosLunaresCatalog: com.example.data.EncantosLunaresCatalog? = null,
    private val artesMarciaisCatalog: ArtesMarciaisCatalog? = null
) {

    // Catálogo "ativo" pro template da planilha atual — Sangue de Dragão e
    // Lunar usam seus próprios Encantos (não os Solares), convertidos aqui
    // pro mesmo formato EncantoSolarDefinition (os campos são idênticos
    // nos catálogos) só pra reaproveitar toda a UI e busca/navegação já
    // existentes, sem duplicar nada disso.
    // Catálogos convertidos uma única vez por instância do domínio.
    // A conversão é somente de apresentação/formato; manter as listas
    // prontas evita um map completo do catálogo a cada abertura/atualização
    // da lista de Encantos.
    private val encantosSangueDragoesFormatados: List<EncantoSolarDefinition>? =
        encantosSangueDragoesCatalog?.definitions?.map { it.paraFormatoSolar() }

    private val encantosLunaresFormatados: List<EncantoSolarDefinition>? =
        encantosLunaresCatalog?.definitions?.map { it.paraFormatoSolar() }

    // APPROVED PERFORMANCE REFACTOR
    // Sangue de Dragão e Lunar possuem catálogos próprios. A UI consulta uma
    // habilidade por vez, portanto filtrar o catálogo completo a cada abertura
    // do popup é trabalho redundante. Os catálogos formatados são imutáveis
    // durante a vida deste domínio; indexá-los uma vez transforma a seleção
    // por habilidade em lookup O(1), preservando a ordenação feita abaixo.
    private val encantosSangueDragoesPorHabilidade: Map<String, List<EncantoSolarDefinition>>? =
        encantosSangueDragoesFormatados
            ?.groupBy { EncantosSolaresCatalog.normalize(it.habilidade) }
            ?.mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoSolarDefinition> { it.minEssencia }.thenBy { it.nome }
                )
            }

    private val encantosLunaresPorHabilidade: Map<String, List<EncantoSolarDefinition>>? =
        encantosLunaresFormatados
            ?.groupBy { EncantosSolaresCatalog.normalize(it.habilidade) }
            ?.mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoSolarDefinition> { it.minEssencia }.thenBy { it.nome }
                )
            }

    private fun catalogoAtivoPorHabilidade(
        sheet: CharacterSheet,
        habilidade: String
    ): List<EncantoSolarDefinition> {
        val chave = EncantosSolaresCatalog.normalize(habilidade)
        if (sheet.tipoPersonagem.isDragonBlooded()) {
            return encantosSangueDragoesPorHabilidade?.get(chave) ?: emptyList()
        }
        if (sheet.tipoPersonagem.isLunar()) {
            return encantosLunaresPorHabilidade?.get(chave) ?: emptyList()
        }
        return encantoCatalog.paraHabilidade(habilidade)
    }

    private fun catalogoAtivoCompleto(sheet: CharacterSheet): List<EncantoSolarDefinition> = when {
        sheet.tipoPersonagem.isDragonBlooded() -> encantosSangueDragoesFormatados ?: emptyList()
        sheet.tipoPersonagem.isLunar() -> encantosLunaresFormatados ?: emptyList()
        else -> encantoCatalog.definitions
    }

    // Índice estável por nome para validações de avanço/redução. Solar e
    // Sangue de Dragão têm catálogos grandes; sanitizeKnownCharms() era
    // obrigado a varrer o catálogo inteiro para CADA Encanto adquirido.
    // Como os catálogos não mudam durante a vida deste domínio, o lookup
    // por nome normalizado pode ser preparado uma única vez.
    private val encantosSolaresPorNome: Map<String, EncantoSolarDefinition> =
        encantoCatalog.definitions.associateBy { EncantosSolaresCatalog.normalize(it.nome) }

    private val encantosSangueDragoesPorNome: Map<String, EncantoSolarDefinition> =
        encantosSangueDragoesFormatados.orEmpty()
            .associateBy { EncantosSolaresCatalog.normalize(it.nome) }

    private val encantosLunaresPorNome: Map<String, EncantoSolarDefinition> =
        encantosLunaresFormatados.orEmpty()
            .associateBy { EncantosSolaresCatalog.normalize(it.nome) }

    private fun definicaoAtivaPorNome(sheet: CharacterSheet, nome: String): EncantoSolarDefinition? {
        val chave = EncantosSolaresCatalog.normalize(nome)
        return when {
            sheet.tipoPersonagem.isDragonBlooded() -> encantosSangueDragoesPorNome[chave]
            sheet.tipoPersonagem.isLunar() -> encantosLunaresPorNome[chave]
            else -> encantosSolaresPorNome[chave]
        }
    }

    private val nomeEncantoPorCirculo = mapOf(
        "Terrestre" to "Feitiçaria do Círculo Terrestre",
        "Celestial" to "Feitiçaria do Círculo Celestial",
        "Solar" to "Feitiçaria do Círculo Solar"
    )

    private fun essenciaEfetivaParaHabilidade(sheet: CharacterSheet, habilidade: String): Int {
        val superna = sheet.supernalAbility
        return if (superna != null && EncantosSolaresCatalog.sameName(superna, habilidade)) {
            maxOf(5, sheet.essencia)
        } else {
            sheet.essencia
        }
    }

    fun encantosDaHabilidadeComElegibilidade(habilidade: String): List<EncantoComElegibilidade> {
        val sheet = sheetState.value
        val fonte = catalogoAtivoPorHabilidade(sheet, habilidade)
        // Os índices próprios já armazenam cada grupo na ordem de exibição.
        // A ordenação deixa de ser reconstruída a cada abertura do popup.
        val filtrados = fonte
        // APPROVED PERFORMANCE REFACTOR
        // Uma abertura da lista pode avaliar dezenas/centenas de Encantos.
        // O estado da planilha é imutável durante esta avaliação; construir os
        // índices uma única vez elimina buscas lineares repetidas em abilities,
        // attributes e charms para cada definição do catálogo.
        val contexto = ElegibilidadeContext(sheet)
        return filtrados.map { elegibilidadeEncanto(it, sheet, contexto) }
    }

    // Consulta por Atributo + Subdivisão — pedido explícito do usuário
    // (Aba 8 do Lunar: hierarquia de 10 caixas, cada uma com suas
    // subdivisões). Diferente de encantosDaHabilidadeComElegibilidade
    // (que passa pelo formato Solar convertido e perde a subdivisão), esta
    // função lê o catálogo Lunar bruto diretamente, preservando o campo
    // subdivisao pro filtro de 2 níveis. subdivisao == null retorna todos
    // os Encantos daquele Atributo (usado só pra "Universal", que não tem
    // subdivisão nenhuma).
    fun encantosLunaresPorAtributoESubdivisaoComElegibilidade(
        atributo: String,
        subdivisao: String?
    ): List<EncantoComElegibilidade> {
        val sheet = sheetState.value
        val catalogo = encantosLunaresCatalog ?: return emptyList()
        val brutos = if (subdivisao == null) {
            catalogo.paraAtributo(atributo)
        } else {
            catalogo.paraAtributoESubdivisao(atributo, subdivisao)
        }
        val contexto = ElegibilidadeContext(sheet)
        return brutos.map { elegibilidadeEncanto(it.paraFormatoSolar(), sheet, contexto) }
    }

    private class ElegibilidadeContext(sheet: CharacterSheet) {
        val habilidades: Map<String, Int> = buildMap(sheet.abilities.size + sheet.attributes.size) {
            sheet.attributes.forEach { (nome, valor) -> put(EncantosSolaresCatalog.normalize(nome), valor) }
            sheet.abilities.forEach { (nome, valor) -> put(EncantosSolaresCatalog.normalize(nome), valor) }
            sheet.martialArts.forEach { arte -> put(EncantosSolaresCatalog.normalize(arte.nome), arte.valor) }
        }
        val habilidadesDeCasta: Set<String> = sheet.casta.allowedAbilities()
            .map(EncantosSolaresCatalog::normalize)
            .toSet()

        // Índices derivados dos Encantos adquiridos. Um único percurso alimenta
        // todos os contadores usados pelos pré-requisitos da Aba 8.
        var totalEncantos = 0
        var quantidadeTecnicaAguçada = 0
        var quantidadeEncantosDeCasta = 0
        var possuiEncantoMedicinal = false
        val quantidades = HashMap<String, Int>(sheet.charms.size)
        val quantidadePorHabilidade = HashMap<String, IntArray>()
        init {
            sheet.charms.forEach { encanto ->
                val nomeNormalizado = EncantosSolaresCatalog.normalize(encanto.nome)
                quantidades[nomeNormalizado] = (quantidades[nomeNormalizado] ?: 0) + 1
                if (encanto.nome.contains("Técnica Aguçada", ignoreCase = true)) {
                    quantidadeTecnicaAguçada++
                }
                if (encanto.categoria != "Encanto") return@forEach
                totalEncantos++
                val habilidadeNormalizada = EncantosSolaresCatalog.normalize(encanto.habilidadeVinculada)
                if (habilidadesDeCasta.contains(habilidadeNormalizada)) {
                    quantidadeEncantosDeCasta++
                }
                if (habilidadeNormalizada == EncantosSolaresCatalog.normalize("Medicina")) {
                    possuiEncantoMedicinal = true
                }
                val contagens = quantidadePorHabilidade.getOrPut(habilidadeNormalizada) { IntArray(6) }
                val minimo = encanto.minEssencia.coerceIn(0, 5)
                for (essencia in 0..minimo) contagens[essencia]++
            }
        }
        val quantidadeArtesMarciais: Int = sheet.martialArts.count { it.valor > 0 }

        fun contarPorHabilidade(habilidades: Set<String>, essenciaMinima: Int?): Int {
            val indice = (essenciaMinima ?: 0).coerceIn(0, 5)
            var total = 0
            habilidades.forEach { habilidade ->
                total += quantidadePorHabilidade[habilidade]?.get(indice) ?: 0
            }
            return total
        }
    }

    fun elegibilidadeEncanto(def: EncantoSolarDefinition, sheet: CharacterSheet): EncantoComElegibilidade =
        elegibilidadeEncanto(def, sheet, ElegibilidadeContext(sheet))

    private fun elegibilidadeEncanto(
        def: EncantoSolarDefinition,
        sheet: CharacterSheet,
        contexto: ElegibilidadeContext
    ): EncantoComElegibilidade {
        val quantidade = quantidadeAdquirida(sheet, def.nome, contexto)
        val maximo = maximoDeAquisicoes(def, sheet, contexto)
        if (sheet.modoLivre) {
            return EncantoComElegibilidade(def, elegivel = true, jaAdquirido = quantidade > 0, quantidade = quantidade, maximo = maximo)
        }
        val habilidadeAtual = habilidadeValor(sheet, def.habilidade, contexto)
        val essenciaAtual = essenciaEfetivaParaHabilidade(sheet, def.habilidade)
        val basicOk = def.minHabilidade <= habilidadeAtual && def.minEssencia <= essenciaAtual
        val prereqOk = requisitosPrerrequisitosCumpridos(def, sheet, contexto)
        val essenciaRecompra = CharmsRepurchaseRules.essenciaMinimaParaRecompra(def, quantidade)
        val recompraOk = essenciaRecompra == null || essenciaAtual >= essenciaRecompra
        val abaixoDoLimite = quantidade < maximo
        return EncantoComElegibilidade(
            def,
            elegivel = basicOk && prereqOk && recompraOk && abaixoDoLimite,
            jaAdquirido = quantidade > 0,
            quantidade = quantidade,
            maximo = maximo
        )
    }

    private fun habilidadeValor(sheet: CharacterSheet, habilidade: String): Int =
        habilidadeValor(sheet, habilidade, ElegibilidadeContext(sheet))

    private fun habilidadeValor(
        sheet: CharacterSheet,
        habilidade: String,
        contexto: ElegibilidadeContext
    ): Int = contexto.habilidades[EncantosSolaresCatalog.normalize(habilidade)] ?: 0

    private fun quantidadeAdquirida(sheet: CharacterSheet, nome: String): Int =
        quantidadeAdquirida(sheet, nome, ElegibilidadeContext(sheet))

    private fun quantidadeAdquirida(
        sheet: CharacterSheet,
        nome: String,
        contexto: ElegibilidadeContext
    ): Int = contexto.quantidades[EncantosSolaresCatalog.normalize(nome)] ?: 0

    private fun preRequisitosSuportados(def: EncantoSolarDefinition): Boolean = true

    private fun requisitosPrerrequisitosCumpridos(def: EncantoSolarDefinition, sheet: CharacterSheet): Boolean =
        requisitosPrerrequisitosCumpridos(def, sheet, ElegibilidadeContext(sheet))

    private fun requisitosPrerrequisitosCumpridos(
        def: EncantoSolarDefinition,
        sheet: CharacterSheet,
        contexto: ElegibilidadeContext
    ): Boolean {
        val p = def.preRequisitos.trim()
        if (p.isBlank() || EncantosSolaresCatalog.sameName(p, "Nenhum")) return true
        if (!preRequisitosSuportados(def)) return false
        return avaliarPrerequisitoOu(p, sheet, contexto)
    }

    private fun avaliarPrerequisitoOu(texto: String, sheet: CharacterSheet, contexto: ElegibilidadeContext): Boolean {
        val alternativas = texto.split(REGEX_OU)
        return alternativas.any { alternativa -> avaliarClausulaPrerequisito(alternativa.trim(), sheet, contexto) }
    }

    private fun avaliarClausulaPrerequisito(
        clausula: String,
        sheet: CharacterSheet,
        contexto: ElegibilidadeContext
    ): Boolean {
        if (clausula.isBlank() || EncantosSolaresCatalog.sameName(clausula, "Nenhum")) return true

        var restante = clausula.trim()
        var quantidadeEncontrada = false
        var quantidade = 0
        var grupo: String? = null
        var essenciaMinima: Int? = null

        val essenciaMatch = REGEX_ESSENCIA_MINIMA.find(restante)
        if (essenciaMatch != null) {
            essenciaMinima = essenciaMatch.groupValues[1].toInt()
            restante = restante.substring(0, essenciaMatch.range.first).trim()
        }

        val quantidadeMatch = REGEX_QUANTIDADE.find(restante)
        if (quantidadeMatch != null) {
            quantidade = numeroPortugues(quantidadeMatch.groupValues[1]) ?: return false
            quantidadeEncontrada = true
            val inicioGrupo = quantidadeMatch.range.last + 1
            var textoGrupo = restante.substring(inicioGrupo).trim()
            textoGrupo = textoGrupo
                .removePrefix("Encantos ")
                .removePrefix("encantos ")
                .removePrefix("Técnicas ")
                .removePrefix("técnicas ")
                .removePrefix("de ")
                .removePrefix("De ")
            grupo = textoGrupo.ifBlank { null }
            restante = restante.substring(0, quantidadeMatch.range.first).trim().trimEnd(',', '+').trim()
        }

        if (quantidadeEncontrada) {
            if (grupo?.contains("técnicas aguçadas", ignoreCase = true) == true) {
                if (contexto.quantidadeTecnicaAguçada < quantidade) return false
            } else if (!contagemGenericaSatisfaz(sheet, quantidade, grupo, essenciaMinima, contexto)) {
                return false
            }
        }

        if (restante.isBlank()) return true

        val requisitosEspecificos = restante
            .split(REGEX_SEPARADOR_MAIS_VIRGULA)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return requisitosEspecificos.all { requisito ->
            val match = REGEX_QUANTIDADE_X.matchEntire(requisito)
            val nome = match?.groupValues?.get(1)?.trim() ?: requisito
            val quantidadeRequerida = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
            if (nome == "???") return@all false
            quantidadeAdquirida(sheet, nome, contexto) >= quantidadeRequerida || requisitoGenericoPorHabilidade(nome, sheet, contexto)
        }
    }

    private fun numeroPortugues(valor: String): Int? = when (valor.lowercase()) {
        "1", "um", "uma" -> 1
        "2", "dois", "duas" -> 2
        "3", "três", "tres" -> 3
        "4", "quatro" -> 4
        "5", "cinco" -> 5
        "6", "seis" -> 6
        "7", "sete" -> 7
        "8", "oito" -> 8
        "9", "nove" -> 9
        "10", "dez" -> 10
        "15", "quinze" -> 15
        else -> null
    }

    private fun contagemGenericaSatisfaz(
        sheet: CharacterSheet,
        quantidade: Int,
        grupo: String?,
        essenciaMinima: Int?,
        contexto: ElegibilidadeContext
    ): Boolean {
        if (grupo == null) return contexto.totalEncantos >= quantidade

        if (grupo.contains("reflitam o princípio", ignoreCase = true)) return true

        if (grupo.contains("de casta", ignoreCase = true)) {
            return contexto.quantidadeEncantosDeCasta >= quantidade
        }

        if (grupo.contains("sociais", ignoreCase = true)) {
            return contexto.contarPorHabilidade(HABILIDADES_SOCIAIS_NORMALIZADAS, essenciaMinima) >= quantidade
        }

        if (grupo.contains("artes marciais", ignoreCase = true)) {
            return contexto.quantidadeArtesMarciais >= quantidade
        }

        // O texto do grupo é separado uma única vez. Antes, o split era
        // repetido para cada uma das 25 habilidades, multiplicando trabalho
        // de alocação durante a avaliação de cada pré-requisito.
        val habilidadesNormalizadas = grupo
            .split(REGEX_SEPARADOR_GRUPO)
            .asSequence()
            .map(EncantosSolaresCatalog::normalize)
            .filter { it.isNotEmpty() }
            .toSet()
        val habilidadesDoCatalogo = habilidadesNormalizadas
            .intersect(HABILIDADES_25_NORMALIZADAS)
        if (habilidadesDoCatalogo.isNotEmpty()) {
            return contexto.contarPorHabilidade(habilidadesDoCatalogo, essenciaMinima) >= quantidade
        }

        return false
    }

    private fun requisitoGenericoPorHabilidade(nome: String, sheet: CharacterSheet, contexto: ElegibilidadeContext): Boolean {
        if (nome.contains("Qualquer Encanto Medicinal", ignoreCase = true)) {
            return contexto.possuiEncantoMedicinal
        }
        return false
    }

    private fun maximoDeAquisicoes(def: EncantoSolarDefinition, sheet: CharacterSheet): Int =
        CharmsRepurchaseRules.maximoDeAquisicoes(def, sheet)

    private fun maximoDeAquisicoes(
        def: EncantoSolarDefinition,
        sheet: CharacterSheet,
        contexto: ElegibilidadeContext
    ): Int = CharmsRepurchaseRules.maximoDeAquisicoes(def, sheet)


    fun artesMarciaisHabilitadas(sheet: CharacterSheet = sheetState.value): Boolean =
        sheet.tipoPersonagem.isDragonBlooded() || sheet.merits.any {
            it.nome.trim().equals("Artista Marcial", ignoreCase = true)
        }

    // A Aba 8 é um navegador global do catálogo de Artes Marciais: todos os
    // estilos aparecem, independentemente de qualificação, mérito ou de o
    // estilo já ter sido cadastrado na Aba 4. A aquisição continua bloqueada
    // por habilidade, Essência e pré-requisitos de cada Encanto.
    fun estilosArtesMarciaisDisponiveis(sheet: CharacterSheet = sheetState.value): List<com.example.data.EstiloArteMarcialDefinition> =
        artesMarciaisCatalog?.definitions.orEmpty()

    fun encantosDaArteMarcialComElegibilidade(
        estiloId: String,
        sheet: CharacterSheet = sheetState.value
    ): List<EncantoComElegibilidade> {
        // Visualização da árvore/lista não depende do mérito; apenas a compra
        // continua sujeita à regra de aquisição.
        val estilo = artesMarciaisCatalog?.definitions?.firstOrNull { it.id == estiloId } ?: return emptyList()
        val contexto = ElegibilidadeContext(sheet)
        return estilo.encantos.map { def ->
            val converted = def.toEncantoSolarDefinition()
            val quantidade = quantidadeAdquirida(sheet, converted.nome, contexto)
            val maximo = maximoDeAquisicoes(converted, sheet, contexto)
            val habilidadeAtual = sheet.martialArts.firstOrNull {
                EncantosSolaresCatalog.sameName(it.nome, estilo.nomePt)
            }?.valor ?: 0
            val essenciaAtual = sheet.essencia
            val basicOk = habilidadeAtual >= converted.minHabilidade && converted.minEssencia <= essenciaAtual
            val prereqOk = requisitosPrerrequisitosCumpridos(converted, sheet, contexto)
            val essenciaRecompra = CharmsRepurchaseRules.essenciaMinimaParaRecompra(converted, quantidade)
            val recompraOk = essenciaRecompra == null || essenciaAtual >= essenciaRecompra
            EncantoComElegibilidade(
                definicao = converted,
                elegivel = sheet.modoLivre || (basicOk && prereqOk && recompraOk && quantidade < maximo),
                jaAdquirido = quantidade > 0,
                quantidade = quantidade,
                maximo = maximo
            )
        }
    }

    fun addCharmFromDefinition(def: EncantoSolarDefinition): Boolean {
        var added = false
        sheetState.update { current ->
            val contexto = ElegibilidadeContext(current)
            val habilidadeAtual = habilidadeValor(current, def.habilidade, contexto)
            val quantidadeAtual = quantidadeAdquirida(current, def.nome, contexto)
            if (!current.modoLivre) {
                val essenciaAtual = essenciaEfetivaParaHabilidade(current, def.habilidade)
                if (def.minHabilidade > habilidadeAtual || def.minEssencia > essenciaAtual) return@update current
                if (!requisitosPrerrequisitosCumpridos(def, current, contexto)) return@update current
                if (quantidadeAtual >= maximoDeAquisicoes(def, current, contexto)) return@update current
                val essenciaRecompra = CharmsRepurchaseRules.essenciaMinimaParaRecompra(def, quantidadeAtual)
                if (essenciaRecompra != null && essenciaAtual < essenciaRecompra) return@update current
            }

            val novo = def.toEncanto()
            val ehCorpoDeTouro = EncantosSolaresCatalog.sameName(novo.nome, com.example.model.NOME_CORPO_DE_TOURO)
            if (current.planilhaConcluida) {
                val custoXp = SheetCalculations.custoExperienciaEncanto(novo, current)
                val debitado = experienceActions.debitarExperiencia(current, custoXp, "${novo.categoria}: ${novo.nome}") ?: return@update current
                added = true
                val atualizado = debitado.copy(charms = debitado.charms + novo)
                if (ehCorpoDeTouro) SheetCalculations.recalcularCaixasCorpoDeTouro(atualizado) else atualizado
            } else {
                val mutado = experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(charms = current.charms + novo))
                if (mutado === current) return@update current
                added = true
                if (ehCorpoDeTouro) SheetCalculations.recalcularCaixasCorpoDeTouro(mutado) else mutado
            }
        }
        return added
    }

    fun removeCharm(id: String) {
        sheetState.update { current ->
            val index = current.charms.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val charms = current.charms.toMutableList()
            charms.removeAt(index)
            sanitizeKnownCharms(current.copy(charms = charms))
        }
    }

    fun toggleCharmPin(id: String) {
        var erro: String? = null
        sheetState.update { current ->
            val index = current.charms.indexOfFirst { it.id == id }
            if (index < 0) return@update current

            val alvo = current.charms[index]
            if (alvo.pinOrder != null) {
                val itens = current.charms.toMutableList()
                itens[index] = alvo.copy(pinOrder = null)
                return@update current.copy(charms = itens)
            }

            val gaveta = alvo.gavetaChave()
            val slotsUsados = HashSet<Int>(5)
            for (i in current.charms.indices) {
                if (i == index) continue
                val outro = current.charms[i]
                if (outro.gavetaChave() == gaveta) {
                    outro.pinOrder?.let { slotsUsados += it }
                }
            }

            val proximoSlot = (1..5).firstOrNull { it !in slotsUsados }
            if (proximoSlot == null) {
                erro = "Esta gaveta já tem 5 Encantos fixados. Desfixe um antes de fixar outro."
                return@update current
            }

            val itens = current.charms.toMutableList()
            // O índice foi localizado; não é necessário procurar o mesmo ID novamente.
            itens[index] = alvo.copy(pinOrder = proximoSlot)
            current.copy(charms = itens)
        }
        erro?.let { commitmentError.value = it }
    }

    private fun nomesEncantosNormalizados(sheet: CharacterSheet): Set<String> {
        val nomes = HashSet<String>(sheet.charms.size)
        sheet.charms.forEach { nomes += EncantosSolaresCatalog.normalize(it.nome) }
        return nomes
    }

    fun feiticariaHabilitada(sheet: CharacterSheet): Boolean =
        sheet.charms.any { EncantosSolaresCatalog.sameName(it.nome, "Feitiçaria do Círculo Terrestre") }

    fun circulosDesbloqueados(sheet: CharacterSheet): Set<String> {
        if (sheet.tipoPersonagem.isDragonBlooded()) return setOf("Terrestre")
        val nomesAdquiridos = nomesEncantosNormalizados(sheet)
        return nomeEncantoPorCirculo
            .filterValues { nomeEncanto -> EncantosSolaresCatalog.normalize(nomeEncanto) in nomesAdquiridos }
            .keys
    }

    fun feiticosDoCirculo(circulo: String): List<FeiticoDefinition> = feiticariaCatalog.paraCirculo(circulo)

    fun addFeiticoFromDefinition(def: FeiticoDefinition): Boolean {
        var added = false
        sheetState.update { current ->
            val nomesAdquiridos = nomesEncantosNormalizados(current)
            if (!current.modoLivre) {
                // A etiqueta `circulo` do catálogo é a fonte de verdade para
                // impedir que um Lunar alcance Feitiços Solares. A regra é
                // aplicada tanto na UI quanto aqui, no ponto de aquisição.
                val circulosPermitidosPorTipo = when {
                    current.tipoPersonagem.isDragonBlooded() -> setOf("Terrestre")
                    current.tipoPersonagem.isLunar() -> setOf("Terrestre", "Celestial")
                    else -> setOf("Terrestre", "Celestial", "Solar")
                }
                if (def.circulo !in circulosPermitidosPorTipo) return@update current

                val circulosDesbloqueados = nomeEncantoPorCirculo
                    .filterValues { nomeEncanto -> EncantosSolaresCatalog.normalize(nomeEncanto) in nomesAdquiridos }
                    .keys
                if (def.circulo !in circulosDesbloqueados) return@update current
            }
            if (EncantosSolaresCatalog.normalize(def.nome) in nomesAdquiridos) return@update current
            val novo = def.toEncanto()
            if (current.planilhaConcluida) {
                val custoXp = SheetCalculations.custoExperienciaEncanto(novo, current)
                val debitado = experienceActions.debitarExperiencia(current, custoXp, "Feitiço (Círculo ${def.circulo}): ${novo.nome}") ?: return@update current
                added = true
                debitado.copy(charms = debitado.charms + novo)
            } else {
                val mutado = experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(charms = current.charms + novo))
                if (mutado === current) return@update current
                added = true
                mutado
            }
        }
        return added
    }

    // Pública (não privada como era no SheetViewModel original) porque
    // outros domínios ainda não extraídos (setAbilityRating,
    // updateMartialArtValue) chamam esta função sempre que um valor que
    // pode invalidar pré-requisitos de Encanto muda.
    fun sanitizeKnownCharms(sheet: CharacterSheet): CharacterSheet {
        if (sheet.charms.isEmpty()) return sheet
        var atual = sheet
        val removidos = mutableListOf<String>()
        while (true) {
            val contexto = ElegibilidadeContext(atual)
            val circulosPermitidos = when {
                atual.tipoPersonagem.isDragonBlooded() -> setOf("Terrestre")
                atual.tipoPersonagem.isLunar() -> setOf("Terrestre", "Celestial")
                else -> setOf("Terrestre", "Celestial", "Solar")
            }
            val circulosAtuais = circulosDesbloqueados(atual).intersect(circulosPermitidos)
            val contagens = mutableMapOf<String, Int>()
            var alterou = false
            val mantidas = mutableListOf<com.example.model.Encanto>()

            for (acquired in atual.charms) {
                val categoria = acquired.categoria
                if (EncantosSolaresCatalog.sameName(categoria, "Feitiçaria") || EncantosSolaresCatalog.sameName(categoria, "Necromancia")) {
                    val permitido = acquired.circulo.isBlank() || acquired.circulo in circulosAtuais
                    if (permitido) mantidas += acquired else { removidos += acquired.nome; alterou = true }
                    continue
                }

                val chave = EncantosSolaresCatalog.normalize(acquired.nome)
                val def = definicaoAtivaPorNome(atual, acquired.nome)
                if (def == null) {
                    // Encantos customizados/legados não catalogados continuam preservados.
                    mantidas += acquired
                    continue
                }
                val quantidadeAtual = contagens.getOrDefault(chave, 0)
                val habilidadeAtual = habilidadeValor(atual, def.habilidade, contexto)
                val basicOk = habilidadeAtual >= def.minHabilidade &&
                    essenciaEfetivaParaHabilidade(atual, def.habilidade) >= def.minEssencia
                val prereqOk = requisitosPrerrequisitosCumpridos(def, atual, contexto)
                val maximo = CharmsRepurchaseRules.maximoDeAquisicoes(def, atual)
                val essenciaRecompra = CharmsRepurchaseRules.essenciaMinimaParaRecompra(def, quantidadeAtual)
                val recompraOk = essenciaRecompra == null || atual.essencia >= essenciaRecompra
                val permitido = basicOk && prereqOk && quantidadeAtual < maximo && recompraOk
                if (permitido) {
                    mantidas += acquired
                    contagens[chave] = quantidadeAtual + 1
                } else {
                    removidos += acquired.nome
                    alterou = true
                }
            }

            if (!alterou) break
            atual = atual.copy(charms = mantidas)
        }
        if (removidos.isNotEmpty()) {
            commitmentError.value = "Encanto(s) removido(s) por perda de requisitos: ${removidos.distinct().joinToString(", ")}."
        }
        return SheetCalculations.recalcularCaixasCorpoDeTouro(atual)
    }
}
