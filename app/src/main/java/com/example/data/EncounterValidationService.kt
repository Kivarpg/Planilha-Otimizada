package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.Aspecto
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import com.example.model.ExaltedConstants
import com.example.model.NOME_CORPO_DE_TOURO
import com.example.model.NOME_FEITICARIA_TERRESTRE

/**
 * Rede de segurança pós-geração — pedido explícito do usuário: os 3
 * documentos de Arquétipo têm uma seção de checklist pensada como
 * validação final antes de aceitar a planilha, mas o gerador nunca
 * reconferia o próprio resultado. Esta função roda DEPOIS que o NPC já
 * foi montado, e devolve alertas em texto simples (mesma lista que já
 * era usada só pra "poucos Encantos elegíveis") sempre que alguma
 * garantia esperada do arquétipo não se confirmou no resultado final —
 * não bloqueia nem corrige nada sozinha, só avisa, pra manter o
 * comportamento probabilístico (ex.: Círculo de Magia em 9 de 10 é
 * esperado falhar 1 em 10 vezes; a rede de segurança só torna isso
 * visível em vez de silencioso).
 */
object EncounterValidationService {
    private val ATRIBUTOS_CANONICOS = ExaltedConstants.ALL_ATTRIBUTES.toSet()
    private val HABILIDADES_CANONICAS = ExaltedConstants.ALL_25_ABILITIES.toSet()

    fun validar(
        arquetipo: ArquetipoEncontro,
        attributes: Map<String, Int>,
        charms: List<String>,
        feiticos: List<*>
    ): List<String> = validarIssues(arquetipo, attributes, charms, feiticos).asLegacyMessages()

    /**
     * API tipada introduzida de forma compatível: a UI/persistência ainda
     * recebem as mensagens legadas, enquanto novos consumidores e testes
     * podem distinguir ERROR/WARNING/INFO por código estável.
     */
    fun validarIssues(
        arquetipo: ArquetipoEncontro,
        attributes: Map<String, Int>,
        charms: List<String>,
        feiticos: List<*>
    ): List<EncounterValidationIssue> {
        val alertas = mutableListOf<EncounterValidationIssue>()

        val categoria = when (arquetipo) {
            ArquetipoEncontro.FISICO -> ExaltedConstants.PHYSICAL_ATTRIBUTES
            ArquetipoEncontro.SOCIAL -> ExaltedConstants.SOCIAL_ATTRIBUTES
            ArquetipoEncontro.MENTAL -> ExaltedConstants.MENTAL_ATTRIBUTES
        }
        if (categoria.none { (attributes[it] ?: 0) >= 5 }) {
            alertas.add(EncounterValidationIssue(
                code = "PROFILE_ATTRIBUTE_5_MISSING",
                severity = EncounterValidationSeverity.WARNING,
                message = "Nenhum Atributo ${categoria.joinToString("/")} chegou a 5 — fora do padrão esperado para o arquétipo ${arquetipo.name.lowercase()}."
            ))
        }

        // Corpo de Touro é uma escolha de construção, não uma invariável universal.
        // Mantemos apenas uma informação de perfil para combatentes físicos;
        // Social/Mental sem o Encanto não são tratados como construção defeituosa.
        if (arquetipo == ArquetipoEncontro.FISICO && charms.none { it == NOME_CORPO_DE_TOURO }) {
            alertas.add(EncounterValidationIssue(
                code = "PROFILE_PHYSICAL_WITHOUT_OX_BODY",
                severity = EncounterValidationSeverity.INFO,
                message = "Combatente físico sem Corpo de Touro — construção válida, porém menos resistente que o perfil usual do gerador."
            ))
        }

        // Feitiços são consequência do acesso real à Feitiçaria. O arquétipo
        // pode influenciar a seleção, mas nunca cria sozinho uma obrigação de
        // possuir Feitiços. Isso elimina a antiga contradição do Mental que
        // aceitava a ausência do Círculo e logo depois exigia 4 Feitiços.
        val possuiFeiticariaTerrestre = NOME_FEITICARIA_TERRESTRE in charms
        // Ausência de Feitiçaria não é alerta: é uma construção válida.
        // A UI só deve avisar quando existe acesso real ao Círculo e a seleção
        // de Feitiços ficou incoerente com o perfil escolhido.
        if (arquetipo == ArquetipoEncontro.MENTAL && possuiFeiticariaTerrestre && feiticos.size < 4) {
            alertas.add(EncounterValidationIssue(
                code = "PROFILE_MENTAL_LOW_SPELL_COUNT",
                severity = EncounterValidationSeverity.WARNING,
                message = "Só ${feiticos.size} Feitiço(s) apesar do acesso à Feitiçaria — perfil Mental costuma selecionar pelo menos 4."
            ))
        }
        if (
            arquetipo == ArquetipoEncontro.SOCIAL &&
            feiticos.isEmpty() &&
            possuiFeiticariaTerrestre
        ) {
            alertas.add(EncounterValidationIssue(
                code = "PROFILE_SOCIAL_NO_SPELL_SELECTED",
                severity = EncounterValidationSeverity.WARNING,
                message = "Nenhum Feitiço selecionado — o NPC possui Feitiçaria do Círculo Terrestre, mas nenhum Feitiço foi selecionado."
            ))
        }

        return alertas
    }
    /**
     * Valida a etapa de distribuição base dos Atributos, antes de qualquer
     * gasto de PB em Atributos. A distribuição deve ser exatamente 11/9/7,
     * com 27 pontos totais e todos os valores entre 1 e 5.
     */
    fun validarDistribuicaoBaseDeAtributos(
        attributes: Map<String, Int>,
        arquetipo: ArquetipoEncontro
    ) {
        check(attributes.keys == ATRIBUTOS_CANONICOS) {
            "Distribuição base deve conter exatamente os 9 Atributos canônicos"
        }
        check(attributes.values.all { it in EncounterGenerationRules.ATRIBUTOS_BASE..EncounterGenerationRules.ATRIBUTOS_MAX }) {
            "Distribuição base contém Atributo fora do intervalo 1..5"
        }
        check(attributes.values.sum() == 27) {
            "Distribuição base deve possuir exatamente 27 pontos"
        }
        // Os totais 11/9/7 são atribuídos por papel de construção, não por
        // categoria fixa: o grupo Primário do arquétipo recebe 11, o
        // Secundário 9 e o Terciário 7. Para Social/Mental, portanto, não é
        // correto exigir que Física=11, Social=9 e Mental=7.
        val grupoPrimario = EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(arquetipo)
        val gruposNaoPrimarios = EncounterGenerationRules.ATTRIBUTE_GROUPS
            .filterKeys { it != arquetipo }
            .values
            .toList()
        val somaPrimario = grupoPrimario.sumOf { attributes.getValue(it) }
        check(somaPrimario == 11) {
            "Distribuição base inválida: categoria primária do arquétipo deve somar 11, mas somou $somaPrimario"
        }
        // Sem persistir quais dos dois grupos não primários foram sorteados
        // como Secundário/Terciário, esta validação não pode inferir 9/7 a
        // partir da ficha final. A etapa de construção recebe e valida
        // explicitamente os dois grupos; aqui basta confirmar que os dois
        // restantes totalizam 16 e que a ficha fecha 27.
        val somaNaoPrimarios = gruposNaoPrimarios.sumOf { grupo -> grupo.sumOf { attributes.getValue(it) } }
        check(somaNaoPrimarios == 16) {
            "Distribuição base inválida: grupos não primários devem somar 16 pontos"
        }
        check(grupoPrimario.any { attributes.getValue(it) == EncounterGenerationRules.ATRIBUTOS_MAX }) {
            "Distribuição base deve possuir pelo menos um Atributo 5 no arquétipo"
        }
    }

    /** Invariantes estruturais da criação da Aba 11. Lança somente em estados
     * impossíveis; não substitui as mensagens de validação exibidas na UI. */
    fun validarIdentidadeEstrutural(npc: NpcEncontro) {
        check(npc.essencia >= 1) { "Essência inválida no NPC de encontro" }
        check(npc.motesPersonais >= 0) { "Motes Pessoais não podem ser negativos" }
        check(npc.motesPerifericos >= 0) { "Motes Periféricos não podem ser negativos" }
        val motesCanonicos = EncounterMoteService.totais(
            tipo = npc.tipoExaltado,
            essencia = npc.essencia,
            arma = npc.arma,
            armadura = npc.armadura
        )
        check(npc.motesPersonais == motesCanonicos.pessoaisMax) {
            "Motes Pessoais divergentes da fórmula canônica"
        }
        check(npc.motesPerifericos == motesCanonicos.perifericosDisponiveis) {
            "Motes Periféricos divergentes da fórmula canônica ou do comprometimento"
        }
        check(motesCanonicos.comitados <= motesCanonicos.perifericosMax) {
            "Comprometimento de motes excede o reservatório Periférico"
        }
        check(npc.arma != null && npc.arma.tipo == "Artefato") {
            "NPC recém-gerado deve possuir Arma Artefato conforme a política da Aba 11"
        }
        check(npc.armadura?.tipo == "Artefato") {
            "NPC recém-gerado deve possuir Armadura Artefato conforme a política da Aba 11"
        }
        check(npc.habilidadePrincipal in EncounterGenerationRules.COMBAT_ABILITIES) {
            "NPC deve possuir uma única Habilidade de combate principal válida"
        }
        val defesaAutomatica = EncounterGenerationRules.habilidadeDefensivaPara(npc.habilidadePrincipal)
        val defesaPersonalizadaValida = npc.habilidadePrincipal == "Briga" && npc.habilidadeDefensiva == "Esquiva"
        check(npc.habilidadeDefensiva == defesaAutomatica || defesaPersonalizadaValida) {
            "Habilidade defensiva não corresponde ao foco de combate nem a uma escolha personalizada válida"
        }
        check(npc.charms.count { it.nome == NOME_CORPO_DE_TOURO } == npc.corpoDeTouroCount) {
            "Contagem de Corpo de Touro não corresponde aos Encantos adquiridos"
        }
        val circulosPermitidos = EncounterNpcSpellManagement.circulosPermitidos(npc.tipoExaltado)
        check(npc.feiticos.all { it.circulo in circulosPermitidos }) {
            "NPC possui Feitiço de um Círculo proibido para seu tipo de Exaltado"
        }
        val circulosDesbloqueados = EncounterNpcSpellManagement.circulosDesbloqueados(npc)
        check(npc.feiticos.all { it.circulo in circulosDesbloqueados }) {
            "NPC possui Feitiço sem o Encanto de Feitiçaria correspondente"
        }
        check(npc.attributes.keys == ATRIBUTOS_CANONICOS) {
            "NPC deve conter exatamente os 9 Atributos canônicos"
        }
        val gruposDeAtributos = listOf(
            ExaltedConstants.PHYSICAL_ATTRIBUTES,
            ExaltedConstants.SOCIAL_ATTRIBUTES,
            ExaltedConstants.MENTAL_ATTRIBUTES
        )
        check(gruposDeAtributos.all { it.size == 3 && it.distinct().size == 3 }) {
            "As categorias de Atributos devem possuir 3 nomes distintos"
        }
        check(gruposDeAtributos.flatten().distinct().size == 9) {
            "As categorias de Atributos devem cobrir exatamente os 9 Atributos"
        }
        // Solar/DB não compram Atributos com PB, portanto a ficha final mantém
        // 11/9/7. Lunar pode elevar Atributos de Casta/Favorecidos com PB; nesse
        // caso a invariável 11/9/7 pertence à etapa pré-PB e é validada por
        // validarDistribuicaoBase().
        // A ficha final não carrega qual dos dois grupos não primários foi
        // escolhido como Secundário (9) e qual foi o Terciário (7). Portanto,
        // a validação final não deve inferir 11/9/7 a partir apenas do mapa
        // persistido. A etapa de distribuição-base já valida 11/9/7 antes
        // de PB e o ajuste de combate preserva os totais de cada categoria.
        // Aqui validamos somente invariantes que continuam observáveis na
        // ficha final: valores válidos, soma mínima e primário com 5.
        check(npc.abilities.keys == HABILIDADES_CANONICAS) {
            "NPC deve conter exatamente as 25 Habilidades canônicas"
        }
        check(npc.abilities.values.all { it in 0..5 }) {
            "NPC contém Habilidade fora do intervalo 0..5"
        }
        check(npc.especialidades.all { it.habilidade in ExaltedConstants.ALL_25_ABILITIES }) {
            "NPC contém Especialidade fora do universo canônico"
        }
        check(npc.especialidades.all { (npc.abilities[it.habilidade] ?: 0) >= 2 }) {
            "NPC não pode possuir Especialidade em Habilidade com menos de 2 círculos"
        }
        check(npc.attributes.values.all { it in EncounterGenerationRules.ATRIBUTOS_BASE..EncounterGenerationRules.ATRIBUTOS_MAX }) {
            "NPC contém Atributo fora do intervalo 1..5"
        }
        if (npc.tipoExaltado != TipoExaltadoEncontro.LUNAR) {
            check(npc.attributes.values.sum() == 27) {
                "NPC deve possuir exatamente 27 pontos de Atributos"
            }
        } else {
            check(npc.attributes.values.sum() >= 27) {
                "Lunar não pode terminar com menos de 27 pontos de Atributos"
            }
        }
        val grupoArquetipo = EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(npc.arquetipo)
        check(grupoArquetipo.any { (npc.attributes[it] ?: 0) == EncounterGenerationRules.ATRIBUTOS_MAX }) {
            "O arquétipo deve possuir pelo menos um Atributo 5 na categoria principal"
        }
        when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> {
                check(npc.habilidadesFavorecidas.size == 5) { "Solar deve possuir exatamente 5 Favorecidas" }
                check(npc.habilidadesFavorecidas.distinct().size == 5) { "Favorecidas Solar devem ser distintas" }
                check(npc.habilidadeSupernal.isNotBlank()) { "Solar deve possuir Supernal" }
                check(npc.habilidadeSupernal in npc.habilidadesFavorecidas) { "Supernal deve estar entre as 5 Favorecidas" }
                check(npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 }) {
                    "Toda Habilidade Favorecida Solar deve possuir pelo menos 1 ponto"
                }
                check(npc.essencia == EncounterGenerationRules.ESSENCIA_SOLAR) { "Solar recém-gerado deve começar com Essência 1" }
            }
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> {
                val aspecto = Aspecto.entries.firstOrNull { it.displayName == npc.casta }
                check(aspecto != null) { "Sangue de Dragão sem Aspecto válido" }
                // check() do Kotlin carrega um contrato (`implies`) que já garante o
                // `aspecto` já foi validado e é não nulo neste ramo.
                val aspectoAbilities = aspecto.allowedAbilities()
                check(aspectoAbilities.size == 5) { "Aspecto deve possuir exatamente 5 Habilidades" }
                check(npc.habilidadesFavorecidas.size == 5) { "Sangue de Dragão deve possuir exatamente 5 Favorecidas" }
                check(npc.habilidadesFavorecidas.distinct().size == 5) { "Favorecidas devem ser distintas" }
                check(npc.habilidadesFavorecidas.intersect(aspectoAbilities.toSet()).isEmpty()) {
                    "Favorecidas adicionais não podem pertencer às 5 Habilidades do Aspecto"
                }
                check(npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 }) {
                    "Toda Habilidade Favorecida do Sangue de Dragão deve possuir pelo menos 1 ponto"
                }
                check((aspectoAbilities + npc.habilidadesFavorecidas).distinct().size == 10) {
                    "Sangue de Dragão deve possuir 10 Habilidades estruturais distintas"
                }
                check(npc.habilidadeSupernal.isBlank()) { "Sangue de Dragão não pode possuir Supernal" }
                check(npc.essencia == EncounterGenerationRules.ESSENCIA_SANGUE_DE_DRAGAO) { "Sangue de Dragão recém-gerado deve começar com Essência 2" }
            }
            TipoExaltadoEncontro.LUNAR -> {
                check(npc.habilidadesFavorecidas.size == 2) { "Lunar deve possuir exatamente 2 Atributos Favorecidos adicionais" }
                check(npc.habilidadesFavorecidas.distinct().size == 2) { "Atributos Favorecidos Lunares devem ser distintos" }
                check(npc.lunarAtributosCasta.size == 2) { "Lunar deve preservar exatamente 2 Atributos de Casta" }
                check(npc.lunarAtributosCasta.distinct().size == 2) { "Atributos de Casta Lunares devem ser distintos" }
                check(npc.lunarAtributosCasta.toSet().intersect(npc.habilidadesFavorecidas.toSet()).isEmpty()) {
                    "Atributos Favorecidos Lunares devem ser adicionais aos Atributos de Casta"
                }
                check((npc.lunarAtributosCasta + npc.habilidadesFavorecidas).distinct().size == 4) {
                    "Lunar deve possuir 4 Atributos especiais distintos: 2 de Casta + 2 Favorecidos"
                }
                check(npc.habilidadeSupernal.isBlank()) { "Lunar não pode possuir Supernal" }
                check(npc.habilidadesFavorecidas.all { it in ExaltedConstants.ALL_ATTRIBUTES }) {
                    "Favorecidos Lunares devem ser Atributos canônicos"
                }
                check(npc.lunarAtributosCasta.all { it in ExaltedConstants.ALL_ATTRIBUTES }) {
                    "Atributos de Casta Lunares devem ser canônicos"
                }
                check(npc.essencia == EncounterGenerationRules.ESSENCIA_LUNAR) { "Lunar recém-gerado deve começar com Essência 1" }
                // Feitiçaria do Círculo Terrestre é uma exigência de construção
                // do Lunar Físico com Inteligência 3+, mas a ausência dessa aquisição
                // não é uma falha fatal da validação estrutural.
            }
        }
    }

    /** Relatório interno usado por testes e diagnóstico, sem persistir metadados extras no NPC. */
    fun relatorioConstrucao(npc: NpcEncontro): EncounterConstructionReport =
        EncounterConstructionReportService.gerar(npc)

}
