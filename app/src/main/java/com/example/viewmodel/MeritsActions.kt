package com.example.viewmodel

import com.example.model.isDragonBlooded
import com.example.data.MeritosCatalog
import com.example.model.CharacterSheet
import com.example.model.Merito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações da Aba 6 — Méritos.
class MeritsActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val experienceActions: ExperienceActions,
    private val meritosCatalog: MeritosCatalog
) {

    fun updateMeritDetalhe(id: String, detalhe: String) {
        val texto = detalhe.trim().take(80)
        sheetState.update { current ->
            val index = current.merits.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val merits = current.merits.toMutableList()
            merits[index] = merits[index].copy(detalhe = texto)
            current.copy(merits = merits)
        }
    }

    fun removeMerit(id: String) {
        sheetState.update { current ->
            val index = current.merits.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.copy(merits = current.merits.toMutableList().also { it.removeAt(index) })
        }
    }

    // Resultado exposto pra UI decidir a mensagem exata (não é só
    // sucesso/falha — o motivo específico de bloqueio importa pro usuário
    // entender por que não conseguiu adicionar).
    sealed interface ResultadoAdicaoMerito {
        data object Sucesso : ResultadoAdicaoMerito
        data object JaAdquirido : ResultadoAdicaoMerito
        data class CustoInvalido(val custosPermitidos: List<Int>) : ResultadoAdicaoMerito
        data object SaldoInsuficiente : ResultadoAdicaoMerito
        // Sangue de Dragão: os 13 primeiros pontos de Mérito são livres;
        // depois disso, libera mais 5 pontos restritos às categorias
        // abaixo (pedido explícito do usuário). Dois motivos de bloqueio
        // distintos aqui — o usuário precisa saber QUAL dos dois é.
        data object PontosBaseNaoConcluidos : ResultadoAdicaoMerito
        data object CategoriaNaoElegivelParaPontosAdicionais : ResultadoAdicaoMerito
        data object LimiteDePontosAdicionaisAtingido : ResultadoAdicaoMerito
        data object RestritoAOutroTemplate : ResultadoAdicaoMerito
    }

    companion object {
        // Categorias elegíveis pros 5 pontos adicionais de Sangue de
        // Dragão, além dos 13 pontos livres — lista exata do pedido do
        // usuário.
        val CATEGORIAS_PONTOS_ADICIONAIS_SANGUE_DRAGAO = setOf(
            "Apoio", "Comando", "Contatos", "Seguidores", "Influência", "Idioma", "Recursos", "Vassalos"
        )
        private const val PONTOS_MERITO_LIVRES_SANGUE_DRAGAO = 13
        private const val PONTOS_MERITO_ADICIONAIS_SANGUE_DRAGAO = 5
    }

    // Retorna null se a adição pode prosseguir, ou o motivo do bloqueio —
    // só se aplica a Sangue de Dragão; Solar continua sem essa restrição
    // (mantém o orçamento simples de 10 pontos livres já existente).
    private fun checarRestricaoPontosAdicionais(sheet: CharacterSheet, categoriaDoNovo: String, valorDoNovo: Int): ResultadoAdicaoMerito? {
        if (!sheet.tipoPersonagem.isDragonBlooded()) return null
        val pontosAtuais = sheet.merits.sumOf { it.valor }
        val totalApos = pontosAtuais + valorDoNovo
        if (totalApos > PONTOS_MERITO_LIVRES_SANGUE_DRAGAO) {
            // Os 5 adicionais só são desbloqueados depois que os 13 base
            // estiverem integralmente alocados. Não é permitido atravessar
            // 13 usando parcialmente o pool adicional.
            if (pontosAtuais < PONTOS_MERITO_LIVRES_SANGUE_DRAGAO) {
                return ResultadoAdicaoMerito.PontosBaseNaoConcluidos
            }
            // Esse mérito usa (pelo menos em parte) o pool adicional de 5 —
            // seja porque já estava acima de 13, seja porque este mérito
            // específico cruza o limite. Categoria precisa ser elegível.
            if (categoriaDoNovo !in CATEGORIAS_PONTOS_ADICIONAIS_SANGUE_DRAGAO) {
                return ResultadoAdicaoMerito.CategoriaNaoElegivelParaPontosAdicionais
            }
            if (totalApos > PONTOS_MERITO_LIVRES_SANGUE_DRAGAO + PONTOS_MERITO_ADICIONAIS_SANGUE_DRAGAO) {
                return ResultadoAdicaoMerito.LimiteDePontosAdicionaisAtingido
            }
        }
        return null
    }

    fun addMerit(nome: String, valor: Int): ResultadoAdicaoMerito {
        if (nome.isBlank()) return ResultadoAdicaoMerito.SaldoInsuficiente
        val definicao = meritosCatalog.porNome(nome)
        val nomeLimpo = nome.trim()

        // Validações dependentes da planilha ficam dentro da mesma transação de
        // estado da mutação. Isso evita validar contra um snapshot e inserir
        // contra outro caso a planilha seja alterada concorrentemente.
        val nivel = if (definicao != null) {
            if (valor !in definicao.custosPermitidos) {
                return ResultadoAdicaoMerito.CustoInvalido(definicao.custosPermitidos)
            }
            valor
        } else {
            valor.coerceIn(1, 5)
        }

        var resultado: ResultadoAdicaoMerito = ResultadoAdicaoMerito.Sucesso
        sheetState.update { current ->
            if (definicao?.restritoAoTemplate != null && definicao.restritoAoTemplate != current.tipoPersonagem) {
                resultado = ResultadoAdicaoMerito.RestritoAOutroTemplate
                return@update current
            }

            if (definicao != null && !definicao.podeSerAdquiridoNovamente) {
                val jaTem = current.merits.any { it.nome.trim().equals(nomeLimpo, ignoreCase = true) }
                if (jaTem) {
                    resultado = ResultadoAdicaoMerito.JaAdquirido
                    return@update current
                }
            }

            checarRestricaoPontosAdicionais(current, definicao?.categoria ?: "", nivel)?.let {
                resultado = it
                return@update current
            }

            val m = Merito(nome = nomeLimpo, valor = nivel, categoria = definicao?.categoria ?: "")
            if (current.planilhaConcluida) {
                val debitado = experienceActions.debitarExperiencia(current, nivel * 3, "Mérito: ${m.nome} (nível $nivel)")
                if (debitado == null) {
                    resultado = ResultadoAdicaoMerito.SaldoInsuficiente
                    return@update current
                }
                return@update debitado.copy(merits = debitado.merits + m)
            }

            val aplicado = experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(merits = current.merits + m))
            if (aplicado === current) resultado = ResultadoAdicaoMerito.SaldoInsuficiente
            aplicado
        }
        return resultado
    }

    // Cadastro livre: usuário digita o próprio nome, nível, categoria e
    // (opcionalmente) descreve o pré-requisito em texto — sem bater
    // contra o catálogo, então nunca é bloqueado por duplicidade nem por
    // custo inválido (não há uma definição oficial pra comparar).
    fun addMeritoPersonalizado(nome: String, valor: Int, categoria: String, preRequisitoTexto: String): ResultadoAdicaoMerito {
        if (nome.isBlank()) return ResultadoAdicaoMerito.SaldoInsuficiente
        val nivel = valor.coerceIn(0, 5)
        val nomeLimpo = nome.trim()
        val categoriaLimpa = categoria.trim()

        var resultado: ResultadoAdicaoMerito = ResultadoAdicaoMerito.Sucesso
        sheetState.update { current ->
            checarRestricaoPontosAdicionais(current, categoriaLimpa, nivel)?.let {
                resultado = it
                return@update current
            }

            val m = Merito(
                nome = nomeLimpo,
                valor = nivel,
                categoria = categoria,
                preRequisitoTexto = preRequisitoTexto.trim()
            )
            if (current.planilhaConcluida) {
                val debitado = experienceActions.debitarExperiencia(current, nivel * 3, "Mérito: ${m.nome} (nível $nivel)")
                if (debitado == null) {
                    resultado = ResultadoAdicaoMerito.SaldoInsuficiente
                    return@update current
                }
                return@update debitado.copy(merits = debitado.merits + m)
            }

            val aplicado = experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(merits = current.merits + m))
            if (aplicado === current) resultado = ResultadoAdicaoMerito.SaldoInsuficiente
            aplicado
        }
        return resultado
    }
}
