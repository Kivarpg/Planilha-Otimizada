package com.example.model

import androidx.compose.runtime.Immutable
import com.example.data.EncantosSolaresCatalog
import java.util.UUID

// @Immutable: os campos List/Map (charms, attributes, abilities, etc.)
// fariam o compilador do Compose tratar a classe inteira como "instável"
// por padrão (List/Map são interfaces, podem ter implementações mutáveis),
// impedindo a otimização de pular recomposição quando a planilha realmente
// não muda. Verificado em todo o projeto: toda atualização usa .copy()
// (via _sheetState.update {...}), nunca mutação in-place das coleções —
// a garantia que a anotação exige está genuinamente cumprida aqui.
@Immutable
data class CharacterSheet(
    val id: String = UUID.randomUUID().toString(),
    // Número sequencial de salvamento nomeado (0 = ainda não foi salvo
    // nomeadamente nenhuma vez) e a data desse salvamento, no formato
    // dd.MM.yy — usados só para montar o nome de arquivo exibido na lista
    // de "Carregar" (ver SheetRepository.proximoNumeroSequencial()).
    val numeroSequencial: Int = 0,
    val dataSalvamento: String = "",
    val nome: String = "",
    val jogador: String = "",
    val conceito: String = "",
    val descricaoAnima: String = "",
    val falhaVirtude: String = "",
    val limiteGatilho: String = "",
    val limiteContador: Int = 0,
    val essencia: Int = 1,
    // "Solar" (padrão, template original) ou "SangueDeDragao" — controla
    // quais regras/telas/paleta cada aba usa (ver TemplateSelectionScreen).
    // Campo aditivo: planilhas Solares existentes carregam com o valor
    // padrão "Solar" e continuam se comportando exatamente como antes.
    val tipoPersonagem: String = CharacterType.SOLAR,
    val casta: Casta = Casta.Dawn,
    // Rastreia se o usuário já tocou num símbolo de Casta pelo menos uma
    // vez — sem isso, não daria pra distinguir "ainda não escolheu" de
    // "escolheu Alvorada" (o valor padrão do campo acima), já que Casta
    // não é anulável. Espelha o comportamento do Aspecto (Sangue de
    // Dragão), que já esconde as habilidades até a seleção real.
    val castaEscolhida: Boolean = false,
    // Aspecto de Sangue de Dragão (Ar/Terra/Fogo/Água/Madeira) — só usado
    // quando tipoPersonagem.isDragonBlooded(); Solar ignora este campo
    // e continua usando "casta" normalmente.
    val aspecto: String = "",
    // Casta Lunar (Lua Cheia/Minguante/Nova/Sem Casta) — só usado quando
    // tipoPersonagem.isLunar(); Solar/Sangue de Dragão ignoram este campo.
    // Espelha o padrão de castaEscolhida: sem isso não dá pra distinguir
    // "ainda não escolheu" do valor padrão do enum.
    val lunarCasta: LunarCasta = LunarCasta.FullMoon,
    val lunarCastaEscolhida: Boolean = false,
    val casteAbilities: List<String> = emptyList(),
    val favoredAbilities: List<String> = emptyList(),
    // Lunares usam Atributos Favorecidos em vez de Habilidades Favorecidas
    // (Solares/Sangue de Dragão usam favoredAbilities acima). Modelo
    // reintroduzido — pedido explícito do usuário: Casta e Favorecidos
    // voltam a ser seleções SEPARADAS (como Solar/Sangue de Dragão fazem
    // com Habilidades). Para Lunar, as duas listas representam exatamente
    // 2 Atributos de Casta + 2 Atributos Favorecidos adicionais. Atributos
    // de Casta não podem aparecer na lista de Favorecidos.
    val lunarCasteAttributesEscolhidos: List<String> = emptyList(),
    // Campos de texto livre exclusivos de Lunar — pedido explícito do
    // usuário: "Forma Espiritual" e "Sinal", exibidos lado a lado abaixo
    // da descrição da Anima na Aba 2.
    val lunarFormaEspiritual: String = "",
    val lunarSinal: String = "",
    val favoredAttributes: List<String> = emptyList(),
    val supernalAbility: String? = null,
    val abilities: Map<String, Int> = ExaltedConstants.DEFAULT_ABILITIES,
    val attributes: Map<String, Int> = ExaltedConstants.DEFAULT_ATTRIBUTES,
    val attributePriorities: Map<String, String> = mapOf("Físicos" to "1º", "Sociais" to "2º", "Mentais" to "3º"),
    val specializations: List<Especializacao> = emptyList(),
    val motesPessoaisGastos: Int = 0,
    val motesPerifericosGastos: Int = 0,
    // Quanto de cada reserva está travado por ter sido usado no comitamento
    // de uma arma/armadura Artefato — esse valor nunca pode ser "subido"
    // (liberado) pelos contadores manuais da Aba 5, mesmo depois de a peça
    // ser descomitada (o gasto permanece registrado, por design).
    val motesPessoaisComitados: Int = 0,
    val motesPerifericosComitados: Int = 0,
    // Força de Vontade: nível permanente (comprado com XP), nunca abaixo de 5, até 10.
    val forcaVontadeBase: Int = 5,
    // Índices (1..forcaVontadeBase) dos pontos de Força de Vontade já marcados como gastos
    // (usado no modo "Travar" da trilha).
    val forcaVontadeUsados: Set<Int> = emptySet(),
    val weapons: List<Arma> = emptyList(),
    val armaduras: List<Armadura> = emptyList(),
    val martialArts: List<HabilidadeCustomizada> = emptyList(),
    val healthBoxes: List<CaixaVitalidade> = ExaltedConstants.defaultHealthBoxes(),
    val intimacies: List<Intimidade> = emptyList(),
    // Idioma nativo e idiomas adicionais — o valor guardado é sempre o
    // nome final do idioma (um item da lista pré-definida OU o texto
    // digitado quando a opção era "Outra"; "Outra" nunca é persistida
    // como valor em si, só usada como gatilho de seleção na UI).
    val linguaNativa: String? = null,
    val linguasAdicionais: List<String> = emptyList(),
    val merits: List<Merito> = emptyList(),
    val charms: List<Encanto> = emptyList(),
    // Battle Groups pertencem à planilha atual e seguem o mesmo ciclo de persistência do restante do CharacterSheet.
    val battleGroups: List<BattleGroup> = emptyList(),
    val pertences: String = "",
    val sessoes: Int = 0,
    // --- Modo Experiência: conclusão da planilha, gasto acumulado e reversão ---
    val planilhaConcluida: Boolean = false,
    // Histórico de gastos de Experiência — cada compra feita após a
    // Planilha ser concluída gera um registro aqui, exibido na Aba 10.
    val historicoExperiencia: List<GastoExperiencia> = emptyList(),
    // Modo Livre: ignora o limite de Pontos de Bônus (ou de Experiência,
    // conforme o modo) e trata todo custo como zero — permite adicionar
    // atributos, habilidades, especializações, Encantos, Feitiços, Artes
    // Marciais e Méritos sem consumir pontos.
    val modoLivre: Boolean = false,
    val experienciaGastaTotal: Int = 0,
    // JSON da planilha no exato momento em que foi marcada como concluída, usado
    // para restaurar o estado original caso o usuário desmarque "Planilha concluída".
    val snapshotConclusao: String = "",
    // --- Contador de Iniciativa (Aba Combate) ---
    // null = ainda não foi definido nenhum valor pelo usuário (tag "Atordoado"
    // ainda não é monitorada); definido = monitoramento ativo.
    val iniciativaValor: Int? = null
) {
    fun motesPessoaisMax(): Int = when {
        tipoPersonagem.isDragonBlooded() -> 11 + essencia
        tipoPersonagem.isLunar() -> 15 + essencia
        else -> (essencia * 3) + 10
    }
    fun motesPerifericosMax(): Int = when {
        tipoPersonagem.isDragonBlooded() -> 23 + (essencia * 4)
        tipoPersonagem.isLunar() -> 34 + (essencia * 4)
        else -> (essencia * 7) + 26
    }
    fun motesPessoaisDisponiveis(): Int = (motesPessoaisMax() - motesPessoaisGastos).coerceAtLeast(0)
    fun motesPerifericosDisponiveis(): Int = (motesPerifericosMax() - motesPerifericosGastos).coerceAtLeast(0)

    // --- Especialidades: presença de especialidade marcada concede bônus fixo (não somam entre si) ---
    fun temEspecialidadeHabilidade(nomeHabilidade: String): Boolean =
        specializations.any { it.habilidade == nomeHabilidade }

    // Aparar: bônus de +1 se Armas Brancas, Briga OU alguma Arte Marcial tiver especialidade marcada
    fun bonusEspecialidadeAparar(): Int {
        val habilidadesComEspecialidade = specializations.asSequence().map { it.habilidade }.toHashSet()
        val martialComEspecialidade = martialArts.any { it.nome in habilidadesComEspecialidade }
        return if ("Armas Brancas" in habilidadesComEspecialidade || "Briga" in habilidadesComEspecialidade || martialComEspecialidade) 1 else 0
    }

    // Evasão: bônus de +1 se Esquiva tiver especialidade marcada
    fun bonusEspecialidadeEvasao(): Int = if (temEspecialidadeHabilidade("Esquiva")) 1 else 0

    // Perseverança: bônus de +1 se Integridade tiver especialidade marcada
    fun bonusEspecialidadePerseveranca(): Int = if (temEspecialidadeHabilidade("Integridade")) 1 else 0

    // Astúcia: bônus de +1 se Socialização tiver especialidade marcada
    fun bonusEspecialidadeAstucia(): Int = if (temEspecialidadeHabilidade("Socialização")) 1 else 0

    // Comitamento Total = soma do comitamento de TODAS as armas equipadas/comitadas
    // (um personagem pode comitar mais de uma arma) + comitamento da única armadura comitada.
    fun armaduraEquipada(): Armadura? = armaduras.firstOrNull { it.equipada }

    fun armasEquipadas(): List<Arma> = weapons.filter { it.equipada }

    // Regra: cada arma ou armadura comitada custa 5 motes de comitamento, exceto
    // armas marcadas como "Ataque Desarmado", cujo custo de Comitamento é 0.
    // Soma os motes REALMENTE comitados em cada arma (não o custo teórico por
    // tipo): desde que Equipar e Comitar viraram ações separadas, uma arma
    // pode estar equipada sem estar comitada, e nesse caso não deve contar
    // aqui. Armadura não tem essa separação — equipada sempre implica comitada.
    fun comitamentoTotalCalculado(): Int =
        weapons.sumOf { it.motesPessoaisComitados + it.motesPerifericosComitados } +
            (armaduraEquipada()?.let { ArmorStatsTable.stats(it.tipoArmadura, it.categoriaPeso).comitamento } ?: 0)

    // Limite máximo de comitamento = soma de motes pessoais + periféricos (valores máximos).
    fun limiteComitamentoCalculado(): Int = motesPessoaisMax() + motesPerifericosMax()

    // Experiência: cada sessão vale 5 pontos de experiência.
    fun experienciaCalculada(): Int = sessoes * 5

    // Saldo de Experiência disponível para gastar no Modo Experiência.
    fun experienciaDisponivel(): Int = (experienciaCalculada() - experienciaGastaTotal).coerceAtLeast(0)

    // Tag "Atordoado": ativa quando o Contador de Iniciativa já foi definido ao
    // menos uma vez pelo usuário e o valor atual é zero ou negativo.
    fun temTagAtordoado(): Boolean = (iniciativaValor ?: 1) <= 0 && iniciativaValor != null

    // --- Sistema de Poderes (Encantos, Feitiços e Necromancia) ---
    // O personagem recebe 15 poderes gratuitos na criação. Escolher pelo menos
    // um poder de Feitiçaria ou de Necromancia entre esses 15 concede uma tag
    // Controle correspondente e um desconto de 1 ponto no limite de poderes
    // gratuitos. Poderes obtidos após os 15 iniciais concedem a tag (se ainda
    // não possuída), mas nunca concedem desconto.
    fun poderesIniciaisGratuitos(): List<Encanto> = charms.take(15)

    private fun descontosControlePoderesIniciais(): Pair<Boolean, Boolean> {
        var feiticaria = false
        var necromancia = false
        val limite = minOf(15, charms.size)
        var i = 0
        while (i < limite && (!feiticaria || !necromancia)) {
            when (charms[i].categoria) {
                "Feitiçaria" -> feiticaria = true
                "Necromancia" -> necromancia = true
            }
            i++
        }
        return feiticaria to necromancia
    }

    fun temDescontoControleFeiticaria(): Boolean = descontosControlePoderesIniciais().first

    fun temDescontoControleNecromancia(): Boolean = descontosControlePoderesIniciais().second

    fun limitePoderesGratuitos(): Int {
        val (feiticaria, necromancia) = descontosControlePoderesIniciais()
        return 15 + (if (feiticaria) 1 else 0) + (if (necromancia) 1 else 0)
    }

    // Feitiços e Necromancia custam Pontos de Bônus com base em Ocultismo ser
    // habilidade de Casta ou Favorecida (independente da habilidade vinculada
    // ao poder específico).
    fun ocultismoCastaOuFavorecida(): Boolean =
        casteAbilities.contains(ExaltedConstants.OCCULT_ABILITY) || favoredAbilities.contains(ExaltedConstants.OCCULT_ABILITY)

    // Equivalente ao acima, mas pro Aspecto de Sangue de Dragão (sem
    // Habilidade Supernal nem casteAbilities/favoredAbilities separados
    // nesta versão — só o Aspecto define as 5 habilidades "favorecidas").
    fun ocultismoDoAspecto(): Boolean {
        val aspectoEnum = com.example.model.Aspecto.entries.firstOrNull { it.displayName == aspecto } ?: return false
        return ExaltedConstants.OCCULT_ABILITY in aspectoEnum.allowedAbilities() || ExaltedConstants.OCCULT_ABILITY in favoredAbilities
    }

    // --- Cálculos dinâmicos por arma (Aba 7) ---
    private fun especializacaoParaHabilidade(habilidade: String): Int =
        if (specializations.any { EncantosSolaresCatalog.sameName(it.habilidade, habilidade) }) 1 else 0

    private fun ehCorpoACorpo(arma: Arma): Boolean =
        arma.habilidadeVinculada == "Armas Brancas" || arma.habilidadeVinculada == "Briga"

    // Para Briga, o atributo usado no Ataque (campos 1 e 2) pode ser Força ou
    // Destreza, conforme escolhido ao cadastrar a arma.
    private fun atributoDeAtaqueParaArma(arma: Arma): Int {
        val nomeAtributo = if (arma.habilidadeVinculada == "Briga" && arma.atributoBriga == "Força") "Força" else "Destreza"
        return attributes[nomeAtributo] ?: 1
    }

    fun ataqueIniciativaCalculado(arma: Arma): Int {
        val atributo = atributoDeAtaqueParaArma(arma)
        val habilidadeValor = abilities[arma.habilidadeVinculada] ?: 0
        val especialidade = especializacaoParaHabilidade(arma.habilidadeVinculada)
        val precisao = if (ehCorpoACorpo(arma)) WeaponStatsTable.corpoACorpo(arma.tipoArma, arma.categoriaPeso).precisao else 0
        return atributo + habilidadeValor + precisao + especialidade
    }

    fun ataqueDecisivoCalculado(arma: Arma): Int {
        val atributo = atributoDeAtaqueParaArma(arma)
        val habilidadeValor = abilities[arma.habilidadeVinculada] ?: 0
        val especialidade = if (ehCorpoACorpo(arma)) especializacaoParaHabilidade(arma.habilidadeVinculada) else 0
        return atributo + habilidadeValor + especialidade
    }

    // --- Combate: valores auto-calculados conforme a legenda fornecida ---
    // Para cálculos que dependem de uma única arma (defesa, dano, etc.), usa-se a
    // primeira arma equipada como referência principal.
    fun armaEquipada(): Arma? = weapons.firstOrNull { it.equipada }
    private fun armaCorpoACorpoEquipada(): Arma? = weapons.firstOrNull { it.equipada && ehCorpoACorpo(it) }

    // Aparar = ([DESTREZA + (BRIGA, ARTES MARCIAIS ou ARMAS BRANCAS)] / 2, arred. p/ cima) + defesa da arma + especialidade
    // Quando há uma arma corpo-a-corpo equipada, a Defesa dessa arma (calculada
    // dinamicamente a partir da categoria/Precisão/especialidade) é usada.
    fun melhorHabilidadeAparar(): Int {
        val armasBrancas = abilities["Armas Brancas"] ?: 0
        val briga = abilities["Briga"] ?: 0
        val melhorArteMarcial = martialArts.maxOfOrNull { it.valor } ?: 0
        return maxOf(armasBrancas, briga, melhorArteMarcial)
    }

    // Penalidade de ferimento ATUAL: a regra do Exalted 3E usa a pior caixa
    // marcada (danificada), não uma soma cumulativa entre todas as caixas
    // com dano. Caixas livres (tipoDano == 0) não contam. "Inc"
    // (Incapacitado) é tratado como -4 para fins deste número — um
    // personagem incapacitado não estaria rolando Aparar/Evasão de
    // qualquer forma, então o valor exato aqui não muda o resultado prático.
    fun penalidadeFerimentoAtual(): Int {
        return when (healthBoxes.piorPenalidadeDeFerimento()) {
            null, "-0" -> 0
            "-1" -> 1
            "-2" -> 2
            "-4", "Inc" -> 4
            else -> 0
        }
    }

    fun apararCalculado(): Int {
        val destreza = attributes["Destreza"] ?: 1
        val armaCorpoACorpo = armaCorpoACorpoEquipada()
        val defesaArma = (armaCorpoACorpo ?: armaEquipada())?.defesa ?: 0
        val bruto = kotlin.math.ceil((destreza + melhorHabilidadeAparar()) / 2.0).toInt() + defesaArma + bonusEspecialidadeAparar()
        return (bruto - penalidadeFerimentoAtual()).coerceAtLeast(0)
    }

    // Evasão = ([DESTREZA + ESQUIVA] / 2, arred. p/ cima) - penalidade de mobilidade da armadura + especialidade - penalidade de ferimento
    fun evasaoCalculada(): Int {
        val destreza = attributes["Destreza"] ?: 1
        val esquivaHab = abilities["Esquiva"] ?: 0
        val bruta = kotlin.math.ceil((destreza + esquivaHab) / 2.0).toInt() - (armaduraEquipada()?.penalidadeMobilidade ?: 0) + bonusEspecialidadeEvasao()
        return (bruta - penalidadeFerimentoAtual()).coerceAtLeast(0)
    }

    // Absorção Natural = VIGOR; Absorção Total = Absorção Natural + valor da armadura
    fun absorcaoNaturalCalculada(): Int = attributes["Vigor"] ?: 1
    fun absorcaoTotalCalculada(): Int = absorcaoNaturalCalculada() + (armaduraEquipada()?.absorcao ?: 0)

    // --- Técnica do Corpo de Touro (Ox-Body Technique) ---
    // Lote de níveis de saúde extras concedido por UMA aquisição do Encanto,
    // de acordo com o Vigor atual do personagem no momento do cálculo.
    fun loteCorpoDeTouroPorVigor(): List<String> {
        val vigor = attributes["Vigor"] ?: 1
        return when {
            vigor <= 2 -> listOf("-1", "-2")
            vigor <= 4 -> listOf("-1", "-2", "-2")
            else -> listOf("-0", "-1", "-2")
        }
    }

    // Quantas vezes o personagem já adquiriu a Técnica do Corpo de Touro.
    fun quantidadeCorpoDeTouroAdquirida(): Int =
        charms.count { EncantosSolaresCatalog.sameName(it.nome, NOME_CORPO_DE_TOURO) }

    // Limite máximo de aquisições: igual ao nível de Resistência (habilidade).
    fun limiteCorpoDeTouro(): Int = abilities["Resistência"] ?: 0

    // Perseverança = ([RACIOCÍNIO + INTEGRIDADE] / 2, arred. p/ cima) + especialidade
    fun perseverancaCalculada(): Int {
        val raciocinio = attributes["Raciocínio"] ?: 1
        val integridade = abilities["Integridade"] ?: 0
        return kotlin.math.ceil((raciocinio + integridade) / 2.0).toInt() + bonusEspecialidadePerseveranca()
    }

    // Astúcia = ([Manipulação + SOCIALIZAR] / 2, arred. p/ cima) + especialidade
    fun astuciaCalculada(): Int {
        val manipulacao = attributes["Manipulação"] ?: 1
        val socializacao = abilities["Socialização"] ?: 0
        return kotlin.math.ceil((manipulacao + socializacao) / 2.0).toInt() + bonusEspecialidadeAstucia()
    }

    // Se a Habilidade Prontidão tiver uma especialidade com um destes nomes,
    // ela conta como especialidade de Juntar-se à Batalha e soma +1 ao cálculo.
    fun temEspecialidadeJuntarBatalha(): Boolean {
        val palavrasChave = setOf("Join Battle", "Juntar-se à Batalha", "Combate", "Iniciativa")
        return specializations.any { spec ->
            EncantosSolaresCatalog.sameName(spec.habilidade, "Prontidão") &&
                palavrasChave.any { chave -> EncantosSolaresCatalog.sameName(spec.nome, chave) }
        }
    }

    // Juntar-se à Batalha = RACIOCÍNIO + PRONTIDÃO (+1 se houver especialidade
    // de Prontidão reconhecida como Join Battle/Juntar-se à Batalha/Combate/Iniciativa)
    fun juntarBatalhaCalculado(): Int {
        val raciocinio = attributes["Raciocínio"] ?: 1
        val prontidao = abilities["Prontidão"] ?: 0
        val bonus = if (temEspecialidadeJuntarBatalha()) 1 else 0
        return raciocinio + prontidao + bonus
    }

    // Investida = DESTREZA + ATLETISMO
    fun investidaCalculada(): Int {
        val destreza = attributes["Destreza"] ?: 1
        val atletismo = abilities["Atletismo"] ?: 0
        return destreza + atletismo
    }

    // Desengajamento = DESTREZA + ESQUIVA
    fun desengajamentoCalculado(): Int {
        val destreza = attributes["Destreza"] ?: 1
        val esquiva = abilities["Esquiva"] ?: 0
        return destreza + esquiva
    }

    // Nome de arquivo no formato "NNN - Nome.dd.MM.aa.save" — NNN é
    // numeroSequencial com 3 dígitos (zeros à esquerda), a data já vem
    // pronta em dataSalvamento (dd.MM.aa). Planilha nunca salva nomeadamente
    // (numeroSequencial == 0) mostra "000" no lugar do número.
    fun nomeArquivo(): String {
        val numeroFormatado = numeroSequencial.toString().padStart(3, '0')
        val nomePersonagem = nome.ifBlank { "Sem Nome" }
        return "$numeroFormatado - $nomePersonagem.$dataSalvamento.save"
    }

    // Nível de Recursos do personagem — pedido explícito do usuário,
    // usado pra validar se ele pode adquirir um equipamento Mundano.
    // "Recursos" é rastreado como um Mérito comum (0-5 pontos); sem esse
    // Mérito na planilha, considera-se nível 0 (não atende a NENHUM
    // requisito de Recursos, conforme especificado pelo usuário).
    fun nivelRecursos(): Int = merits.firstOrNull { it.nome == "Recursos" }?.valor ?: 0

}
