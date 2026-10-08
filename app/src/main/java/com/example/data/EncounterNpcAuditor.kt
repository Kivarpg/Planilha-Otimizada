package com.example.data

import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/**
 * Auditor independente e pós-geração da Aba 11.
 *
 * Não escolhe, corrige, reordena ou substitui nada no NPC. Recebe a ficha já
 * pronta e os catálogos que a originaram, reconstrói um estado canônico e
 * relata violações observáveis. Isso mantém auditoria e geração desacopladas.
 */
internal object EncounterNpcAuditor {
    data class Report(val issues: List<EncounterValidationIssue>) {
        val errors: List<EncounterValidationIssue> get() = issues.filter { it.severity == EncounterValidationSeverity.ERROR }
        val warnings: List<EncounterValidationIssue> get() = issues.filter { it.severity == EncounterValidationSeverity.WARNING }
        val isValid: Boolean get() = errors.isEmpty()
    }

    fun audit(
        npc: NpcEncontro,
        solares: List<EncantoSolarDefinition> = emptyList(),
        sangueDeDragao: List<EncantoSangueDeDragaoDefinition> = emptyList(),
        lunares: List<EncantoLunarDefinition> = emptyList(),
        feiticos: List<FeiticoDefinition> = emptyList(),
        estilosMarciais: List<EstiloArteMarcialDefinition> = emptyList()
    ): Report = audit(
        npc,
        PreparedEncounterCatalog.prepare(solares, sangueDeDragao, lunares, feiticos, estilosMarciais)
    )

    fun audit(npc: NpcEncontro, catalog: PreparedEncounterCatalog): Report {
        val issues = mutableListOf<EncounterValidationIssue>()

        // Reutiliza invariantes estruturais existentes, mas converte a exceção em
        // diagnóstico: o auditor nunca altera nem interrompe a ficha auditada.
        runCatching { EncounterValidationService.validarIdentidadeEstrutural(npc) }
            .exceptionOrNull()
            ?.let { error ->
                issues += issue("STRUCTURAL_INVARIANT", EncounterValidationSeverity.ERROR,
                    error.message ?: "A ficha viola uma invariável estrutural da Aba 11.")
            }

        val allowedNamespace = when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> PreparedEncounterCatalog.NS_SOLAR
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> PreparedEncounterCatalog.NS_DRAGON_BLOODED
            TipoExaltadoEncontro.LUNAR -> PreparedEncounterCatalog.NS_LUNAR
        }
        val martialStyleIds = selectedMartialStyleIds(npc, catalog, issues)
        val spiritTraits = npc.lunarArchetypeTraits.mapNotNull { name ->
            runCatching { LunarSpiritTrait.valueOf(name) }.getOrNull()
        }.toSet().ifEmpty {
            (LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritual) +
                LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritualSecundaria)).toSet()
        }

        val selectedNames = npc.charms.map { it.nome }
        val selectedNameCounts = selectedNames.groupingBy { it }.eachCount()
        val distinctNames = selectedNameCounts.keys
        val refsByName = LinkedHashMap<String, PreparedEncounterCatalog.ContentRef>()

        distinctNames.forEach { name ->
            val candidates = catalog.findByName(name).filter { ref ->
                ref.id.namespace == allowedNamespace || ref.id.namespace == PreparedEncounterCatalog.NS_MARTIAL_CHARM
            }
            when {
                candidates.isEmpty() -> issues += issue(
                    "CHARM_NOT_IN_ALLOWED_CATALOG", EncounterValidationSeverity.ERROR,
                    "Encanto '$name' não pertence ao catálogo permitido para ${npc.tipoExaltado}."
                )
                candidates.size > 1 -> issues += issue(
                    "CHARM_IDENTITY_AMBIGUOUS", EncounterValidationSeverity.ERROR,
                    "Encanto '$name' possui identidade ambígua no catálogo permitido."
                )
                else -> refsByName[name] = candidates.single()
            }
        }

        // Cada Encanto é reavaliado como se ele ainda não tivesse sido adquirido.
        // Os demais Encantos da ficha continuam presentes, permitindo verificar
        // requisitos, rotas alternativas, Atributos/Habilidades, Essência e estilo.
        refsByName.forEach { (name, ref) ->
            val remainingNames = if (selectedNameCounts.getValue(name) > 1) distinctNames else distinctNames - name
            val state = EncounterRulesEngine.buildState(
                exaltType = npc.tipoExaltado,
                essence = npc.essencia,
                abilities = npc.abilities,
                attributes = npc.attributes,
                acquiredCharmNames = remainingNames,
                catalog = catalog,
                spiritTraits = spiritTraits,
                martialStyleIds = martialStyleIds
            )
            when (val eligibility = EncounterRulesEngine.evaluate(catalog.requirementGraph, ref.id, state)) {
                is EncounterRulesEngine.Eligibility.Available,
                EncounterRulesEngine.Eligibility.Acquired -> Unit
                is EncounterRulesEngine.Eligibility.Locked -> issues += issue(
                    "CHARM_REQUIREMENTS_UNSATISFIED", EncounterValidationSeverity.ERROR,
                    "Encanto '$name' não satisfaz nenhuma rota de aquisição (${eligibility.routes.joinToString { it.routeId }})."
                )
                is EncounterRulesEngine.Eligibility.Invalid -> issues += issue(
                    "CHARM_RULE_INVALID", EncounterValidationSeverity.ERROR,
                    "Encanto '$name' é inválido para esta ficha: ${eligibility.reason}"
                )
            }
        }

        auditSpells(npc, catalog, issues)
        auditLunarForms(npc, issues)

        return Report(issues.distinctBy { Triple(it.code, it.severity, it.message) })
    }

    private fun selectedMartialStyleIds(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog,
        issues: MutableList<EncounterValidationIssue>
    ): Set<String> {
        val names = (listOf(npc.estiloArtesMarciais) + npc.estilosArtesMarciaisAdicionais)
            .filter(String::isNotBlank)
            .toSet()
        return names.mapNotNull { name ->
            val style = catalog.estilosMarciais.singleOrNull { it.nomePt == name || it.nomeEn == name }
            if (style == null) {
                issues += issue("MARTIAL_STYLE_UNKNOWN", EncounterValidationSeverity.ERROR,
                    "Estilo de Arte Marcial '$name' não existe no catálogo auditado.")
                null
            } else if (npc.tipoExaltado !in style.tiposExaltadosPermitidos) {
                issues += issue("MARTIAL_STYLE_EXALT_TYPE", EncounterValidationSeverity.ERROR,
                    "Estilo '${style.nomePt}' não é permitido para ${npc.tipoExaltado}.")
                style.id
            } else style.id
        }.toSet()
    }

    private fun auditSpells(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog,
        issues: MutableList<EncounterValidationIssue>
    ) {
        val spellsByName = catalog.feiticos.groupBy { it.nome }
        npc.feiticos.forEach { spell ->
            val matches = spellsByName[spell.nome].orEmpty()
            when {
                matches.isEmpty() -> issues += issue("SPELL_NOT_IN_CATALOG", EncounterValidationSeverity.ERROR,
                    "Feitiço '${spell.nome}' não existe no catálogo auditado.")
                matches.size > 1 -> issues += issue("SPELL_IDENTITY_AMBIGUOUS", EncounterValidationSeverity.ERROR,
                    "Feitiço '${spell.nome}' possui identidade ambígua no catálogo auditado.")
                matches.single().circulo != spell.circulo -> issues += issue("SPELL_CIRCLE_MISMATCH", EncounterValidationSeverity.ERROR,
                    "Feitiço '${spell.nome}' está persistido com Círculo '${spell.circulo}', mas o catálogo informa '${matches.single().circulo}'.")
            }
        }
        npc.feiticoInicialNome?.let { initial ->
            if (npc.feiticos.none { it.nome == initial }) {
                issues += issue("INITIAL_SPELL_MISSING", EncounterValidationSeverity.ERROR,
                    "Feitiço inicial '$initial' não está presente na lista de Feitiços da ficha.")
            }
        }
    }

    private fun auditLunarForms(npc: NpcEncontro, issues: MutableList<EncounterValidationIssue>) {
        if (npc.tipoExaltado != TipoExaltadoEncontro.LUNAR) {
            if (npc.formaEspiritual.isNotBlank() || npc.formaEspiritualSecundaria.isNotBlank() || npc.lunarArchetypeTraits.isNotEmpty()) {
                issues += issue("LUNAR_DATA_ON_NON_LUNAR", EncounterValidationSeverity.ERROR,
                    "Ficha não Lunar contém dados exclusivos de Forma Espiritual.")
            }
            return
        }
        if (npc.formaEspiritual.isBlank()) {
            issues += issue("LUNAR_PRIMARY_SPIRIT_FORM_MISSING", EncounterValidationSeverity.ERROR,
                "Lunar sem Forma Espiritual principal.")
        }
        if (npc.formaEspiritualSecundaria.isNotBlank() && npc.formaEspiritualSecundaria == npc.formaEspiritual) {
            issues += issue("LUNAR_DUPLICATE_SPIRIT_FORM", EncounterValidationSeverity.ERROR,
                "Forma Espiritual secundária repete a Forma principal.")
        }
        val invalidTraits = npc.lunarArchetypeTraits.filter { name ->
            runCatching { LunarSpiritTrait.valueOf(name) }.isFailure
        }
        if (invalidTraits.isNotEmpty()) {
            issues += issue("LUNAR_UNKNOWN_ARCHETYPE_TRAIT", EncounterValidationSeverity.ERROR,
                "Traits Lunares desconhecidos: ${invalidTraits.sorted().joinToString()}.")
        }
    }

    private fun issue(code: String, severity: EncounterValidationSeverity, message: String) =
        EncounterValidationIssue(code, severity, message)
}
