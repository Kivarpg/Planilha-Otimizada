package com.example.data

import com.example.model.*
import kotlin.random.Random

/** Fachada estável para o gerador de encontros. A implementação foi separada por tipo de Exaltado. */
object EncounterGenerator {
    fun gerarNome(cultura:CulturaNome?=null,genero:GeneroNome?=null,random:Random=Random.Default):String = NameGenerator.gerar(cultura,genero,random)
    fun gerarNome(random:Random=Random.Default):String = gerarNome(null,null,random)
    fun essenciaPorXpGasto(xpGastoTotal:Int):Int = EncounterExperienceProgressionRules.essenceForXp(TipoExaltadoEncontro.SOLAR, xpGastoTotal)
    fun essenciaPorXpGastoSangueDeDragao(xpGastoTotal:Int):Int = EncounterExperienceProgressionRules.essenceForXp(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, xpGastoTotal)
    fun selecionarIdiomaInicial(tipoExaltado: TipoExaltadoEncontro, origemNomeSangueDeDragao: OrigemNomeSangueDeDragao = OrigemNomeSangueDeDragao.SEM_CASTA, random: Random = Random.Default): String = EncounterLanguageService.selecionar(tipoExaltado, origemNomeSangueDeDragao, random)
    fun gerarSolar(nomeManual:String,arquetipo:ArquetipoEncontro,encantosSolares:List<EncantoSolarDefinition>,feiticos:List<FeiticoDefinition> = emptyList(),culturaNome:CulturaNome?=null,generoNome:GeneroNome?=null,random:Random=Random.Default,meritosCatalogo:List<MeritoDefinition> = emptyList(),focoPersonalizado:String?=null,customizacao:EncounterCustomization?=null,estilosArtesMarciais:List<EstiloArteMarcialDefinition> = emptyList()):NpcEncontro = SolarEncounterGenerator.gerar(nomeManual,arquetipo,encantosSolares,feiticos,culturaNome,generoNome,random,meritosCatalogo,focoPersonalizado,customizacao,estilosArtesMarciais)
    fun expandirEncantosPorExperiencia(npc:NpcEncontro,encantosSolares:List<EncantoSolarDefinition>):NpcEncontro = SolarEncounterGenerator.expandirEncantosPorExperiencia(npc,encantosSolares)
    fun expandirEncantosPorExperienciaRepetidos(npc:NpcEncontro,encantosSolares:List<EncantoSolarDefinition>,quantidade:Int):NpcEncontro = SolarEncounterGenerator.expandirEncantosPorExperienciaRepetidos(npc,encantosSolares,quantidade)
    fun gerarSangueDeDragao(nomeManual:String,arquetipo:ArquetipoEncontro,encantosSangueDeDragao:List<EncantoSangueDeDragaoDefinition>,feiticos:List<FeiticoDefinition> = emptyList(),origemNome:OrigemNomeSangueDeDragao=OrigemNomeSangueDeDragao.SEM_CASTA,generoNome:GeneroNome?=null,random:Random=Random.Default,meritosCatalogo:List<MeritoDefinition> = emptyList(),focoPersonalizado:String?=null,customizacao:EncounterCustomization?=null,estilosArtesMarciais:List<EstiloArteMarcialDefinition> = emptyList()):NpcEncontro = DragonBloodedEncounterGenerator.gerarSangueDeDragao(nomeManual,arquetipo,encantosSangueDeDragao,feiticos,origemNome,generoNome,random,meritosCatalogo,focoPersonalizado,customizacao,estilosArtesMarciais)
    fun gerarLunar(nomeManual:String,arquetipo:ArquetipoEncontro,encantosLunares:List<EncantoLunarDefinition>,feiticos:List<com.example.data.FeiticoDefinition> = emptyList(),culturaNome:CulturaNome?=null,generoNome:GeneroNome?=null,random:Random=Random.Default,meritosCatalogo:List<MeritoDefinition> = emptyList(),focoPersonalizado:String?=null,customizacao:EncounterCustomization?=null,estilosArtesMarciais:List<EstiloArteMarcialDefinition> = emptyList()):NpcEncontro = LunarEncounterGenerator.gerar(nomeManual,arquetipo,encantosLunares,feiticos,culturaNome,generoNome,random,meritosCatalogo=meritosCatalogo,focoPersonalizado=focoPersonalizado,customizacao=customizacao,estilosArtesMarciais=estilosArtesMarciais)
    fun trilhaVitalidadePorVigor(vigor:Int,corpoDeTouroCount:Int):List<CaixaVitalidade> = EncounterVitalityService.trilhaVitalidadeSolar(vigor,corpoDeTouroCount)
    fun trilhaVitalidadeSangueDeDragaoPorVigor(vigor:Int,corpoDeTouroCount:Int):List<CaixaVitalidade> = EncounterVitalityService.trilhaVitalidadeSangueDeDragao(vigor,corpoDeTouroCount)
    fun expandirEncantosPorExperienciaSangueDeDragao(npc:NpcEncontro,encantosSangueDeDragao:List<EncantoSangueDeDragaoDefinition>):NpcEncontro = DragonBloodedEncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(npc,encantosSangueDeDragao)
    fun expandirEncantosPorExperienciaLunar(npc:NpcEncontro,encantosLunares:List<EncantoLunarDefinition>):NpcEncontro = EncounterExperienceService.expandLunar(npc,encantosLunares,EncounterRulePolicy.lunarAttributePriorityFor(npc))
    fun expandirEncantosPorExperienciaLunarRepetidos(npc:NpcEncontro,encantosLunares:List<EncantoLunarDefinition>,quantidade:Int):NpcEncontro = EncounterExperienceService.expandLunarRepeated(npc,encantosLunares,EncounterRulePolicy.lunarAttributePriorityFor(npc),quantidade)
    fun reduzirExperiencia(npc:NpcEncontro):NpcEncontro {
        val reduzido = if (npc.tipoExaltado == TipoExaltadoEncontro.LUNAR) EncounterExperienceService.reduceLunar(npc) else EncounterExperienceService.reduce(npc)
        return reduzido
    }
}
