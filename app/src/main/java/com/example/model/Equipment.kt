package com.example.model

import java.util.UUID

data class Arma(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    // Categorias de configuração (popup "Configuração da Arma")
    val habilidadeVinculada: String = "Armas Brancas", // Armas Brancas | Arqueirismo | Arremesso | Briga
    val atributoBriga: String? = null, // "Força" | "Destreza", apenas quando habilidadeVinculada == "Briga"
    val tipoArma: String = "Mundana", // Mundana | Artefato
    val categoriaPeso: String = "Leve", // Leve | Média | Pesada
    // Indica que os valores efetivos abaixo foram sobrescritos manualmente
    // via long press, e não refletem mais o cálculo automático da categoria.
    val modificadaManualmente: Boolean = false,
    // Valores efetivos: calculados automaticamente a partir das categorias
    // acima ao cadastrar, mas substituíveis manualmente depois.
    val iniciativa: String = "0",
    val decisivo: String = "0",
    val defesa: Int = 0,
    val dano: String = "0",
    val danoMinimo: String = "",
    val equipada: Boolean = false,
    // Ataque Desarmado: quando marcado, esta arma representa golpes desarmados
    // e não custa Comitamento (0 motes) ao ser equipada.
    val ataqueDesarmado: Boolean = false,
    // Quantos motes pessoais/periféricos ESTA arma especificamente tem
    // comitados agora (0 se não equipada ou sem custo). Guardado por item
    // para permitir devolver exatamente essa quantia à Aba 5 ao desequipar.
    val motesPessoaisComitados: Int = 0,
    val motesPerifericosComitados: Int = 0,
    // Etiquetas do catálogo de armas (ex.: "Letal", "Armas brancas",
    // "Alcance") — preenchidas ao selecionar uma arma do catálogo;
    // permanecem vazias em armas mundanas/cadastradas manualmente sem
    // usar o catálogo. Exibidas ao final das estatísticas na planilha.
    val etiquetas: List<String> = emptyList()
) {
}

// Tabela de estatísticas de armas por categoria (Mundana/Artefato × Leve/Média/Pesada),
// conforme regras fornecidas pelo usuário.
object WeaponStatsTable {
    data class CorpoACorpoStats(val precisao: Int, val dano: Int, val defesa: Int, val danoMinimo: Int)

    // Armas Brancas / Briga
    fun corpoACorpo(tipoArma: String, categoriaPeso: String): CorpoACorpoStats {
        return if (tipoArma == "Artefato") {
            when (categoriaPeso) {
                "Leve" -> CorpoACorpoStats(5, 10, 0, 3)
                "Média" -> CorpoACorpoStats(3, 12, 1, 4)
                else -> CorpoACorpoStats(1, 14, 0, 5) // Pesada
            }
        } else { // Mundana
            when (categoriaPeso) {
                "Leve" -> CorpoACorpoStats(4, 7, 0, 1)
                "Média" -> CorpoACorpoStats(2, 9, 1, 1)
                else -> CorpoACorpoStats(0, 11, -1, 1) // Pesada
            }
        }
    }

    // Arremesso / Arqueirismo: apenas Dano e Dano Mínimo (sem Precisão/Defesa fixas)
    fun distancia(tipoArma: String, categoriaPeso: String): Pair<Int, Int> {
        return if (tipoArma == "Artefato") {
            when (categoriaPeso) {
                "Leve" -> 10 to 3
                "Média" -> 12 to 4
                else -> 14 to 5
            }
        } else {
            when (categoriaPeso) {
                "Leve" -> 7 to 1
                "Média" -> 9 to 1
                else -> 11 to 1
            }
        }
    }

    fun comitamento(tipoArma: String): Int = if (tipoArma == "Artefato") 5 else 0

    fun distanciasArremesso(tipoArma: String): List<Pair<String, Int>> =
        if (tipoArma == "Artefato") listOf("Imediato" to 5, "Curto" to 4, "Médio" to 3, "Longo" to 0, "Extremo" to -2)
        else listOf("Imediato" to 4, "Curto" to 3, "Médio" to 2, "Longo" to -1, "Extremo" to -3)

    fun distanciasArqueirismo(tipoArma: String): List<Pair<String, Int>> =
        if (tipoArma == "Artefato") listOf("Imediato" to -1, "Curto" to 5, "Médio" to 3, "Longo" to 1, "Extremo" to -1)
        else listOf("Imediato" to -2, "Curto" to 4, "Médio" to 2, "Longo" to 0, "Extremo" to -2)
}

// Tabela de estatísticas de armadura por categoria (Mundana/Artefato × Leve/Média/Pesada).
object ArmorStatsTable {
    data class Stats(val absorcao: Int, val penalidadeMobilidade: Int, val dureza: Int, val comitamento: Int)

    fun stats(tipoArmadura: String, categoriaPeso: String): Stats {
        return if (tipoArmadura == "Artefato") {
            when (categoriaPeso) {
                "Leve" -> Stats(5, 0, 4, 4)
                "Média" -> Stats(8, 1, 7, 5)
                else -> Stats(11, 2, 10, 6) // Pesada
            }
        } else { // Mundana
            when (categoriaPeso) {
                "Leve" -> Stats(3, 0, 0, 0)
                "Média" -> Stats(3, 1, 0, 0)
                else -> Stats(3, 2, 0, 0) // Pesada
            }
        }
    }
}

// Interface compartilhada entre Armadura (jogador) e ArmaduraEncontro
// (NPC, em NpcEncontro.kt) — refatoração de organização, pedido
// explícito do usuário. Reúne só os campos genuinamente idênticos nos
// dois (mesmo nome, mesmo tipo): nome, absorção, dureza, penalidade de
// mobilidade e marcadores. As duas propriedades computadas normalizam
// os campos que têm o MESMO SIGNIFICADO mas nomes diferentes em cada
// classe (tipoArmadura/tipo, categoriaPeso/peso) — sem precisar
// renomear nenhum campo existente nem tocar nos pontos que já usam
// esses nomes específicos pelo app inteiro. Habilita escrever lógica
// de exibição compartilhada (ex.: uma função de descrição de
// estatísticas) que funciona com qualquer uma das duas.
interface ArmaduraComum {
    val nome: String
    val absorcao: Int
    val dureza: Int
    val penalidadeMobilidade: Int
    val marcadores: List<String>
    val tipoNormalizado: String
    val pesoNormalizado: String
}

// Armadura: lista dinâmica, com equipamento exclusivo por cartão.
data class Armadura(
    val id: String = UUID.randomUUID().toString(),
    override val nome: String = "",
    val tipoArmadura: String = "Mundana", // Mundana | Artefato
    val categoriaPeso: String = "Leve", // Leve | Média | Pesada
    override val absorcao: Int = 0,
    override val dureza: Int = 0,
    override val penalidadeMobilidade: Int = 0,
    val equipada: Boolean = false,
    // Mesma lógica da Arma: quantos motes pessoais/periféricos ESTA
    // armadura tem comitados agora, para devolver corretamente ao
    // desequipar ou ao trocar por outra armadura.
    val motesPessoaisComitados: Int = 0,
    val motesPerifericosComitados: Int = 0,
    // Marcadores do catálogo (ex.: "Ocultável", "Silenciosa") — pedido
    // explícito do usuário. Vazio em armaduras cadastradas manualmente
    // sem usar o catálogo.
    override val marcadores: List<String> = emptyList()
) : ArmaduraComum {
    override val tipoNormalizado: String get() = tipoArmadura
    override val pesoNormalizado: String get() = categoriaPeso
}
