package com.example.data

import com.example.model.NpcEncontro

/**
 * Projeção mecânica canônica de um NPC para regressão/golden tests.
 * Deliberadamente ignora identidade efêmera (UUID), ordem de Map/Set,
 * estado de UI, alertas textuais e métricas de tempo.
 */
internal object EncounterMechanicalGoldenSnapshot {
    fun parts(npc: NpcEncontro): List<String> = buildList {
        add("tipo=${npc.tipoExaltado.name}")
        add("arquetipo=${npc.arquetipo.name}")
        add("casta=${npc.casta}")
        add("supernal=${npc.habilidadeSupernal}")
        add("principal=${npc.habilidadePrincipal}")
        add("defensiva=${npc.habilidadeDefensiva.orEmpty()}")
        add("suporte=${npc.habilidadeSuporte}")
        add("essencia=${npc.essencia}")
        npc.attributes.toSortedMap().forEach { (k, v) -> add("attr:$k=$v") }
        npc.abilities.toSortedMap().forEach { (k, v) -> add("ability:$k=$v") }
        npc.habilidadesFavorecidas.sorted().forEach { add("fav:$it") }
        npc.lunarAtributosCasta.sorted().forEach { add("lunar-casta:$it") }
        npc.merits.map { "${it.nome}:${it.valor}:${it.detalhe}" }.sorted().forEach { add("merit:$it") }
        npc.especialidades.map { it.habilidade }.sorted().forEach { add("spec:$it") }
        npc.charms.map { it.nome }.sorted().forEach { add("charm:$it") }
        npc.feiticos.map { it.nome }.sorted().forEach { add("spell:$it") }
        add("initial-spell=${npc.feiticoInicialNome.orEmpty()}")
        add("ma=${npc.estiloArtesMarciais}")
        npc.estilosArtesMarciaisAdicionais.sorted().forEach { add("ma-extra:$it") }
        add("spirit=${npc.formaEspiritual}")
        add("chimera=${npc.formaEspiritualSecundaria}")
        npc.lunarArchetypeTraits.sorted().forEach { add("lunar-trait:$it") }
        npc.lunarPrimaryArchetypeTraits.sorted().forEach { add("lunar-primary-trait:$it") }
        add("weapon=${npc.arma?.nome.orEmpty()}")
        add("armor=${npc.armadura?.nome.orEmpty()}")
        add("willpower=${npc.forcaDeVontade}")
        add("personal=${npc.motesPersonais}")
        add("peripheral=${npc.motesPerifericos}")
        add("main-action=${npc.acaoPrincipal}")
        add("decisive=${npc.acaoDecisiva}")
        add("parry=${npc.defesaPrimaria ?: -1}")
        add("evasion=${npc.esquiva}")
        add("soak=${npc.absorcao}")
        add("hardness=${npc.dureza}")
        add("resolve=${npc.perseveranca}")
        add("guile=${npc.astucia}")
        add("join=${npc.juntarABatalha}")
        add("rush=${npc.investida}")
        add("disengage=${npc.desengajamento}")
        add("ox-body=${npc.corpoDeTouroCount}")
        add("damage=${npc.dano}")
    }

    fun fingerprint(npc: NpcEncontro): String =
        EncounterEngineeringBaseline.mechanicalFingerprint(parts(npc))
}
