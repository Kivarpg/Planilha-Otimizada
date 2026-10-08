package com.example.ui.tabs

import android.util.Log
import com.example.data.EncounterMoteService

import android.content.Context
import android.content.ContentValues
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Resultado de um campo de combate/atuação da Aba 11 (Encontros).
 *
 * [rotulo]        → nome do campo (ex.: "Evasão")
 * [valorExibido]  → número que aparece na linha (já com penalidade de ferimento + Clash)
 * [equacao]       → texto mostrado no long press, com os números reais substituídos
 */
data class CampoEquacao(
    val rotulo: String,
    val valorExibido: String,
    val equacao: String
)

/**
 * Funções puras de cálculo das equações de combate da Aba 11.
 * Outro programador deve apenas consumir [gerarCampos] e [gerarPdfNpc].
 */
object EncounterCombatEquations {

    // Extraído do corpo de gerarCampos (refatoração de organização —
    // pedido explícito do usuário). Padrão repetido 4 vezes (Perseverança,
    // Astúcia, Investida, Desengajamento): "((X + Y [+ Bônus]) ÷ 2 (arred.
    // pra cima) = resultado)". Formato de string próprio deste arquivo —
    // diferente do helper equivalente já extraído em EncounterNpcCard.kt
    // (calculoMediaComEspecializacao), que usa nomes de Atributo/Habilidade
    // na string em vez de só os valores. Validado programaticamente
    // (não só inspeção visual) contra os 4 usos originais antes de
    // substituí-los.
    private fun campoMediaComEspecializacao(
        nomeCampo: String,
        valorPrincipal: Int,
        valorSecundario: Int,
        bonus: Int
    ): CampoEquacao {
        val resultado = ceil((valorPrincipal + valorSecundario + bonus) / 2.0).toInt()
        val equacao = buildString {
            append("(($valorPrincipal + $valorSecundario")
            if (bonus > 0) append(" + $bonus")
            append(") ÷ 2 (arred. pra cima) = $resultado)")
        }
        return CampoEquacao(nomeCampo, resultado.toString(), equacao)
    }

    // ------------------------------------------------------------------
    // 1. Geração das equações + valores exibidos
    // ------------------------------------------------------------------

    /**
     * Retorna a lista ordenada de campos que devem aparecer no card
     * (Juntar-se à Batalha até Desengajamento).
     * Dureza e abaixo NÃO entram aqui (somente no PDF).
     */
    fun gerarCampos(npc: NpcEncontro): List<CampoEquacao> {
        val penalidade = npc.penalidadeFerimentoAtual() + npc.penalidadeClashDefesa + npc.penalidadeAtaquesDefesa
        val destreza = npc.attributes["Destreza"] ?: 1
        val raciocinio = npc.attributes["Raciocínio"] ?: 1
        val manipulacao = npc.attributes["Manipulação"] ?: 1
        val vigor = npc.attributes["Vigor"] ?: 1
        val forca = npc.attributes["Força"] ?: 1

        fun hab(nome: String) = npc.abilities[nome] ?: 0
        fun temSpec(nome: String) = npc.especialidades.any { it.habilidade == nome }
        fun bonusSpec(nome: String) = if (temSpec(nome)) 1 else 0

        val lista = mutableListOf<CampoEquacao>()

        // ---------- Juntar-se à Batalha ----------
        // (Raciocínio + Prontidão + Spec)
        val prontidao = hab("Prontidão")
        val bonusJuntar = bonusSpec("Prontidão")
        val juntar = raciocinio + prontidao + bonusJuntar
        val eqJuntar = buildString {
            append("($raciocinio + $prontidao")
            if (bonusJuntar > 0) append(" + $bonusJuntar")
            append(" = $juntar)")
        }
        lista += CampoEquacao("Juntar-se à Batalha", juntar.toString(), eqJuntar)

        when (npc.arquetipo) {
            ArquetipoEncontro.FISICO -> {
                val habCombate = npc.habilidadePrincipal
                val pontosHab = hab(habCombate)
                val bonusAtaque = bonusSpec(habCombate)
                // Armas Brancas/Briga têm Precisão fixa; Arremesso/Arqueirismo
                // não — é variável, determinada pelo sistema no uso (pedido
                // explícito do usuário).
                val ehMelee = habCombate in listOf("Armas Brancas", "Briga")
                val precisao = if (ehMelee) npc.arma?.precisao ?: 0 else 0
                val defesaArma = npc.arma?.defesa ?: 0
                val danoArma = npc.arma?.dano ?: 0
                val penMob = npc.armadura?.penalidadeMobilidade ?: 0

                // Ataque Fulminante
                val fulminante = destreza + pontosHab + bonusAtaque + precisao
                val eqFulminante = buildString {
                    append("($destreza + $pontosHab")
                    if (bonusAtaque > 0) append(" + $bonusAtaque")
                    if (ehMelee) append(" + $precisao")
                    append(" = $fulminante)")
                }
                lista += CampoEquacao("Ataque Fulminante", fulminante.toString(), eqFulminante)

                // Dano
                val (danoTexto, eqDano) = when (habCombate) {
                    "Armas Brancas", "Briga" -> {
                        val total = forca + danoArma
                        val sufixo = if (npc.dano.contains("L", ignoreCase = true)) "L" else ""
                        "$total$sufixo" to "($forca + $danoArma = $total$sufixo)"
                    }
                    else -> {
                        // Arremesso / Arqueirismo: só o dano da arma
                        "${danoArma}L" to "(Dano da arma = ${danoArma}L)"
                    }
                }
                lista += CampoEquacao("Dano", npc.dano.ifBlank { danoTexto }, eqDano)

                // Ataque Decisivo
                val decisivo = destreza + pontosHab + bonusAtaque
                val eqDecisivo = buildString {
                    append("($destreza + $pontosHab")
                    if (bonusAtaque > 0) append(" + $bonusAtaque")
                    append(" = $decisivo)")
                }
                lista += CampoEquacao("Ataque Decisivo", decisivo.toString(), eqDecisivo)

                // Todos os NPCs exibem Aparar e Evasão, nesta ordem.
                // Aparar usa Briga quando a ofensiva não é Armas Brancas/Briga;
                // Briga = 0 continua sendo um valor válido na fórmula.
                val brigaPts = hab("Briga")
                val baseAparar = ceil((destreza + brigaPts + 1) / 2.0).toInt() + defesaArma
                val apararExibido = (baseAparar - penalidade).coerceAtLeast(0)
                val eqAparar = buildString {
                    append("(($destreza + $brigaPts + 1) ÷ 2 (arred. pra cima)) + $defesaArma")
                    if (penalidade > 0) append(" − $penalidade")
                    append(" = $apararExibido")
                }
                lista += CampoEquacao("Aparar", apararExibido.toString(), eqAparar)

                val esquivaPts = hab("Esquiva")
                val bonusEsq = bonusSpec("Esquiva")
                val baseEvasao = ceil((destreza + esquivaPts + bonusEsq) / 2.0).toInt() - penMob
                val evasaoExibida = (baseEvasao - penalidade).coerceAtLeast(0)
                val eqEvasao = buildString {
                    append("(($destreza + $esquivaPts")
                    if (bonusEsq > 0) append(" + $bonusEsq")
                    append(") ÷ 2 (arred. pra cima)) − $penMob")
                    if (penalidade > 0) append(" − $penalidade")
                    append(" = $evasaoExibida")
                }
                lista += CampoEquacao("Evasão", evasaoExibida.toString(), eqEvasao)
            }

            ArquetipoEncontro.SOCIAL,
            ArquetipoEncontro.MENTAL -> {
                val habilidadePadrao = if (npc.arquetipo == ArquetipoEncontro.SOCIAL) "Socialização" else "Investigação"
                val habPrincipal = npc.habilidadeSuporte.ifBlank { habilidadePadrao }
                val pontos = hab(habPrincipal)
                val bonus = bonusSpec(habPrincipal)
                val acao = destreza + pontos + bonus
                val eqAcao = buildString {
                    append("($destreza + $pontos")
                    if (bonus > 0) append(" + $bonus")
                    append(" = $acao)")
                }
                lista += CampoEquacao("Ataque Fulminante", acao.toString(), eqAcao)

                val brigaPts = hab("Briga")
                val defesaArma = npc.arma?.defesa ?: 0
                val defesaBase = ceil((destreza + brigaPts + 1) / 2.0).toInt() + defesaArma
                val defesaExibida = (defesaBase - penalidade).coerceAtLeast(0)
                lista += CampoEquacao(
                    "Aparar",
                    defesaExibida.toString(),
                    "(($destreza + $brigaPts + 1) ÷ 2 (arred. pra cima)) + $defesaArma" +
                        (if (penalidade > 0) " − $penalidade" else "") +
                        " = $defesaExibida"
                )
                val esquivaPts = hab("Esquiva")
                val bonusEsq = bonusSpec("Esquiva")
                val penMob = npc.armadura?.penalidadeMobilidade ?: 0
                val evasaoBase = ceil((destreza + esquivaPts + bonusEsq) / 2.0).toInt() - penMob
                val evasaoExibida = (evasaoBase - penalidade).coerceAtLeast(0)
                val eqEvasao = "(($destreza + $esquivaPts" +
                    (if (bonusEsq > 0) " + $bonusEsq" else "") +
                    ") ÷ 2 (arred. pra cima)) − $penMob" +
                    (if (penalidade > 0) " − $penalidade" else "") +
                    " = $evasaoExibida"
                lista += CampoEquacao("Evasão", evasaoExibida.toString(), eqEvasao)
            }
        }

        // ---------- Campos comuns (até Desengajamento) ----------

        // Perseverança
        val integridade = hab("Integridade")
        val bonusPers = bonusSpec("Integridade")
        lista += campoMediaComEspecializacao("Perseverança", raciocinio, integridade, bonusPers)

        // Astúcia
        val socializacao = hab("Socialização")
        val bonusAst = bonusSpec("Socialização")
        lista += campoMediaComEspecializacao("Astúcia", manipulacao, socializacao, bonusAst)

        // Absorção
        val absorcaoArmadura = npc.armadura?.absorcao ?: 0
        val absorcao = vigor + absorcaoArmadura
        val eqAbs = "($vigor + $absorcaoArmadura = $absorcao)"
        lista += CampoEquacao("Absorção", absorcao.toString(), eqAbs)

        // Investida
        val atletismo = hab("Atletismo")
        val bonusInv = bonusSpec("Atletismo")
        lista += campoMediaComEspecializacao("Investida", destreza, atletismo, bonusInv)

        // Desengajamento
        val esquivaPts = hab("Esquiva")
        val bonusDes = bonusSpec("Esquiva")
        lista += campoMediaComEspecializacao("Desengajamento", destreza, esquivaPts, bonusDes)

        return lista
    }

    // ------------------------------------------------------------------
    // 2. Geração de PDF (Android nativo – PdfDocument)
    // ------------------------------------------------------------------

    /**
     * Gera um PDF do NPC no formato solicitado e salva na pasta pública
     * Documentos do aparelho. Em Android 10+ usa MediaStore/RELATIVE_PATH,
     * portanto o arquivo fica visível ao usuário fora da área privada do app.
     *
     * Retorna uma referência com o nome/caminho público esperado ou null em caso de erro.
     *
     * Coloque o botão de chamada no canto superior direito da aba.
     */
    suspend fun gerarPdfNpc(context: Context, npc: NpcEncontro): File? = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            // A4 em 72 dpi, como o DOCX de referência.
            val pageWidth = 595
            val pageHeight = 842
            // O DOCX usa 0,7875" em todos os lados (720090 EMU = 56,7 pt).
            val left = 57f
            val right = 57f
            val top = 57f
            val bottom = 57f
            val contentWidth = (pageWidth - left - right).toInt()

            // O modelo de exportação exige Times New Roman. Não usar a família
            // genérica "serif": ela resolve para uma fonte diferente conforme o aparelho.
            val serif = Typeface.create("Times New Roman", Typeface.NORMAL)
            val serifBold = Typeface.create("Times New Roman", Typeface.BOLD)
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = serif
                textSize = 12f
                color = android.graphics.Color.BLACK
            }
            val titlePaint = TextPaint(bodyPaint).apply {
                typeface = serifBold
                textSize = 15f
            }

            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: android.graphics.Canvas? = null
            var y = top

            fun startPage() {
                pageNumber++
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(info)
                canvas = page!!.canvas
                y = top
            }

            fun finishPage() {
                page?.let(document::finishPage)
                page = null
                canvas = null
            }

            fun layoutFor(text: CharSequence, paint: TextPaint, justify: Boolean): StaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .apply {
                        if (justify) setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                    }
                    .build()

            fun drawParagraph(
                text: CharSequence,
                paint: TextPaint = bodyPaint,
                justify: Boolean = true,
                gapAfter: Float = 0f
            ) {
                if (page == null) startPage()
                val layout = layoutFor(text, paint, justify)
                var firstLine = 0
                while (firstLine < layout.lineCount) {
                    val available = pageHeight - bottom - y
                    var lastLine = firstLine
                    while (lastLine < layout.lineCount &&
                        layout.getLineBottom(lastLine) - layout.getLineTop(firstLine) <= available
                    ) lastLine++
                    if (lastLine == firstLine) {
                        finishPage(); startPage(); continue
                    }
                    val sliceTop = layout.getLineTop(firstLine)
                    val sliceBottom = layout.getLineBottom(lastLine - 1)
                    canvas!!.save()
                    canvas!!.clipRect(left, y, pageWidth - right, y + (sliceBottom - sliceTop))
                    canvas!!.translate(left, y - sliceTop)
                    layout.draw(canvas!!)
                    canvas!!.restore()
                    y += (sliceBottom - sliceTop)
                    firstLine = lastLine
                    if (firstLine < layout.lineCount) { finishPage(); startPage() }
                }
                if (gapAfter > 0f) {
                    if (y + gapAfter > pageHeight - bottom) { finishPage(); startPage() }
                    else y += gapAfter
                }
            }

            fun styledLine(label: String, value: String, italicParentheses: Boolean = false): CharSequence {
                val out = SpannableStringBuilder()
                val a = out.length
                out.append("$label: ")
                out.setSpan(StyleSpan(Typeface.BOLD), a, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val valueStart = out.length
                out.append(value)
                if (italicParentheses) {
                    Regex("\\(([^()]*)\\)").findAll(value).forEach { m ->
                        val from = valueStart + m.range.first + 1
                        val to = valueStart + m.range.last
                        if (to > from) out.setSpan(StyleSpan(Typeface.ITALIC), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                return out
            }

            fun section(label: String, value: CharSequence, italicParentheses: Boolean = false) {
                val heading = SpannableStringBuilder().apply {
                    append(label)
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                drawParagraph(heading, justify = false)
                val body = if (italicParentheses) {
                    val b = SpannableStringBuilder(value)
                    Regex("\\(([^()]*)\\)").findAll(value.toString()).forEach { m ->
                        val from = m.range.first + 1
                        val to = m.range.last
                        if (to > from) b.setSpan(StyleSpan(Typeface.ITALIC), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    b
                } else value
                drawParagraph(body, justify = true, gapAfter = 12f)
            }

            fun generoSimbolo(genero: String): String = when {
                genero.equals("Feminino", true) || genero.equals("F", true) -> "♀"
                genero.equals("Masculino", true) || genero.equals("M", true) -> "♂"
                else -> ""
            }
            fun nomeTipoExaltado(): String = when (npc.tipoExaltado) {
                com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> "Sangue de Dragão"
                com.example.model.TipoExaltadoEncontro.LUNAR -> "Lunar"
                com.example.model.TipoExaltadoEncontro.SOLAR -> "Solar"
            }
            fun juntarComE(itens: List<String>): String = when (itens.size) {
                0 -> "—"; 1 -> itens[0]
                else -> itens.dropLast(1).joinToString(", ") + " e " + itens.last()
            }

            // Cabeçalho do DOCX: nome 15 pt; gênero entre parênteses; subtítulo 12 pt em negrito.
            val simbolo = generoSimbolo(npc.genero)
            val titulo = SpannableStringBuilder().apply {
                append(npc.nome)
                if (simbolo.isNotBlank()) append(" ($simbolo)")
            }
            drawParagraph(titulo, titlePaint, justify = false)

            val rotuloCasta = if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO) "Aspecto" else "Casta"
            val subtitulo = if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR)
                "Lunar – Casta ${npc.casta}" else "${nomeTipoExaltado()} – $rotuloCasta ${npc.casta}"
            val subtituloRich = SpannableStringBuilder(subtitulo).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            drawParagraph(subtituloRich, justify = false)
            if (npc.tipoExaltado != com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO && npc.limite.isNotBlank()) {
                drawParagraph(styledLine("Limite", npc.limite), justify = false)
            }
            if (npc.idioma.isNotBlank()) drawParagraph(styledLine("Idioma", npc.idioma), justify = false)
            if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR) {
                drawParagraph(styledLine("Forma Espiritual", npc.formaEspiritual.ifBlank { "—" }), justify = false)
                drawParagraph(styledLine("Sinal", npc.sinal.ifBlank { "—" }), justify = false)
            }
            y += 12f

            fun attr(nome: String) = npc.attributes[nome] ?: 1
            val atributos = "Força ${attr("Força")}, Destreza ${attr("Destreza")}, Vigor ${attr("Vigor")}; " +
                "Carisma ${attr("Carisma")}, Manipulação ${attr("Manipulação")}, Aparência ${attr("Aparência")}; " +
                "Percepção ${attr("Percepção")}, Inteligência ${attr("Inteligência")}, Raciocínio ${attr("Raciocínio")}"
            section("Atributos:", atributos)

            val especialidadesHabilidades = npc.especialidades.asSequence().map { it.habilidade }.toSet()
            val habs = npc.abilities.filter { it.value > 0 }.entries.joinToString(", ") { (k, v) ->
                "$k $v${if (k in especialidadesHabilidades) " (+1)" else ""}"
            }.ifBlank { "—" }
            section("Habilidades:", habs)

            val meritos = npc.merits.sortedWith(compareBy<com.example.model.Merito> { it.nome }.thenBy { it.valor })
                .joinToString(", ") { it.linhaExibicao() }.ifBlank { "—" }
            section("Méritos:", meritos, italicParentheses = true)

            val encantosPorCategoria = npc.charms
                .groupBy { it.habilidadeVinculada.trim().ifBlank { "Sem categoria" } }
                .toList().sortedBy { it.first.lowercase() }
            val encantosRich = SpannableStringBuilder()
            if (encantosPorCategoria.isEmpty()) encantosRich.append("—") else {
                encantosPorCategoria.forEachIndexed { index, (categoria, encantos) ->
                    if (index > 0) encantosRich.append(", ")
                    val ini = encantosRich.length
                    encantosRich.append("($categoria):")
                    encantosRich.setSpan(StyleSpan(Typeface.BOLD), ini, encantosRich.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val nomes = groupAccumulatedEncounterCharms(encantos).map { g ->
                        if (g.quantity > 1) "${g.charm.nome} (x${g.quantity})" else g.charm.nome
                    }
                    encantosRich.append(" ${juntarComE(nomes)}")
                }
            }
            section("Encantos:", encantosRich)

            val campos = gerarCampos(npc)
            val ordemModelo = listOf(
                "Juntar-se à Batalha", "Ataque Fulminante", "Aparar", "Evasão",
                "Absorção", "Dureza", "Investida", "Desengajamento", "Perseverança", "Astúcia"
            )
            ordemModelo.mapNotNull { r -> campos.firstOrNull { it.rotulo.equals(r, true) } }
                .forEach { drawParagraph(styledLine(it.rotulo, it.valorExibido), justify = false) }
            y += 12f

            val totaisMotes = EncounterMoteService.totais(npc.tipoExaltado, npc.essencia, npc.arma, npc.armadura)
            drawParagraph(styledLine("Essência", npc.essencia.toString()), justify = false)
            drawParagraph(styledLine("Força de Vontade", npc.forcaDeVontade.toString()), justify = false)
            drawParagraph(styledLine("Motes Pessoais", "${npc.motesPersonais}/${totaisMotes.pessoaisMax}"), justify = false)
            drawParagraph(styledLine("Motes Periféricos", "${totaisMotes.perifericosMax}/(${npc.motesPerifericos})"), justify = false)

            fun nomeEquipamentoPdf(nome: String, peso: String): String {
                val n = nome.trim(); if (n.isBlank()) return "($peso)"
                return if (n.endsWith("($peso)", true)) n else "$n ($peso)"
            }
            val equipamento = buildString {
                npc.arma?.let { append(nomeEquipamentoPdf(it.nome, it.peso)) }
                if (npc.arma != null && npc.armadura != null) append(", ")
                npc.armadura?.let { append(nomeEquipamentoPdf(it.nome, it.peso)) }
            }.ifBlank { "—" }
            drawParagraph(styledLine("Equipamentos", equipamento, italicParentheses = true), justify = false)
            drawParagraph(styledLine("Motes Comitados", "(${totaisMotes.comitados})"), justify = false, gapAfter = 12f)

            val niveisSaude = npc.healthBoxes.sortedBy { it.penaltyRank() }
                .joinToString("/") { it.penalidade.ifBlank { "Inc" } }
            drawParagraph(styledLine("Níveis de Saúde", niveisSaude), justify = false)

            finishPage()
            val nomeArquivo = "NPC_${npc.nome.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, nomeArquivo)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val uri = resolver.insert(collection, values)
                        ?: throw IllegalStateException("Não foi possível criar o PDF em Documentos")
                    try {
                        resolver.openOutputStream(uri, "w")?.use(document::writeTo)
                            ?: throw IllegalStateException("Não foi possível abrir o PDF em Documentos")
                        val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                        resolver.update(uri, ready, null, null)
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    if (!publicDir.exists() && !publicDir.mkdirs() && !publicDir.isDirectory) {
                        throw IllegalStateException("Não foi possível criar a pasta Documentos")
                    }
                    FileOutputStream(File(publicDir, nomeArquivo)).use(document::writeTo)
                }
            } finally {
                document.close()
            }
            File(publicDir, nomeArquivo)
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e("EncounterPdf", "Falha ao exportar NPC para PDF", e)
            }
            null
        }
    }
}
