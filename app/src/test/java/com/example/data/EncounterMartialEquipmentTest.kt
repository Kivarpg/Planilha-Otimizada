package com.example.data

import com.example.model.ArmaduraEncontro
import com.example.model.ArmaEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterMartialEquipmentTest {
    private fun estilo(categoria: String, modo: String = "desarmado") =
        EstiloArteMarcialDefinition(
            id="x", nomePt="Teste", nomeEn="", descricao="",
            armaDoEstiloTexto=null, armaDoEstiloModo=modo, armasEspecificas=emptyList(),
            armaduraTexto=null, armaduraCategoria=categoria, habilidadesComplementares=null,
            tiposExaltadosPermitidos=setOf(TipoExaltadoEncontro.SOLAR), encantos=emptyList()
        )

    private val armaduraPesada = ArmaduraEncontro(
        nome="Pesada", peso="Pesada", tipo="Artefato", absorcao=11, dureza=10,
        penalidadeMobilidade=-2, motesComitados=5
    )

    @Test fun `estilo incompatível remove armadura em vez de rejeitar o estilo`() {
        val (_, armor) = EncounterEquipmentService.ajustarParaEstiloMarcial(
            estilo("incompativel"), null, armaduraPesada, Random(1)
        )
        assertEquals("Sem armadura", armor.nome)
        assertEquals(0, armor.absorcao)
        assertEquals(0, armor.motesComitados)
    }

    @Test fun `estilo desarmado produz arma de Briga mundana`() {
        val (weapon, _) = EncounterEquipmentService.ajustarParaEstiloMarcial(
            estilo("todas"), null, armaduraPesada, Random(1)
        )
        assertTrue(weapon != null)
        assertEquals("Mundana", weapon!!.tipo)
    }

    private fun estiloArmado(nome: String, vararg armas: String) =
        EstiloArteMarcialDefinition(
            id=nome, nomePt=nome, nomeEn="", descricao="",
            armaDoEstiloTexto=armas.joinToString(" "), armaDoEstiloModo="somente_armado",
            armasEspecificas=armas.toList(), armaduraTexto=null, armaduraCategoria="leve_media",
            habilidadesComplementares=null, tiposExaltadosPermitidos=setOf(TipoExaltadoEncontro.SOLAR),
            encantos=emptyList()
        )

    @Test fun `diabo justo equipa arma de fogo mas permanece equipamento marcial`() {
        val diabo = estiloArmado("Estilo do Diabo Justo", "varinha de fogo", "Pistolas de Fogo")
        val (weapon, _) = EncounterEquipmentService.ajustarParaEstiloMarcial(
            diabo, null, armaduraPesada, Random(1)
        )
        requireNotNull(weapon)
        assertTrue(EncounterMartialAttackCompatibility.supportsWeapon(diabo, weapon.nome))
        assertTrue(weapon.etiquetas.any { it.equals("Artes Marciais", ignoreCase = true) })
    }

    @Test fun `dois estilos de pistola podem compartilhar o mesmo ataque`() {
        val a = estiloArmado("Diabo Justo", "Pistolas de Fogo")
        val b = estiloArmado("Segundo Estilo", "revólver")
        assertTrue(EncounterMartialAttackCompatibility.sharedAttackForms(a, b).isNotEmpty())
        assertTrue(EncounterMartialAttackCompatibility.canShareAttackWithWeapon(a, b, "Revólver de Fogo (Média)"))
    }

    @Test fun `armas diferentes reduzem afinidade mas nao tornam estilos ilegais`() {
        val pistola = estiloArmado("Diabo Justo", "Pistolas de Fogo")
        val espada = estiloArmado("Estilo da Espada", "daiklave")
        assertTrue(EncounterMartialAttackCompatibility.sharedAttackForms(pistola, espada).isEmpty())
        assertEquals(0, EncounterMartialAttackCompatibility.weaponAffinity(pistola, espada))
        assertTrue(!EncounterMartialAttackCompatibility.canShareAttackWithWeapon(
            pistola, espada, "Revólver de Fogo (Média)"
        ))
        assertTrue(EncounterCombatSynergy.categoriesCanSynergize(
            "Estilo do Diabo Justo", "Estilo da Espada"
        ))
    }

    @Test fun `sem armadura concilia estilos com classes de armadura incompatíveis`() {
        val leve = estiloArmado("Leve", "daiklave").copy(armaduraCategoria = "leve")
        val semArmadura = estiloArmado("Sem Armadura", "daiklave").copy(armaduraCategoria = "incompativel")
        val comuns = EncounterMartialArmorAffinity.commonConfigurations(listOf(leve, semArmadura))
        assertTrue(null in comuns)
        assertEquals(null, EncounterMartialArmorAffinity.bestCommonArmor(listOf(leve, semArmadura)))
        assertTrue(EncounterMartialArmorAffinity.affinity(leve, semArmadura) > 0)
    }

    @Test fun `duas armas genericas diferentes nao recebem falso bonus de arma`() {
        val espada = estiloArmado("Espada", "daiklave")
        val corrente = estiloArmado("Corrente", "corrente de combate")
        assertTrue(
            EncounterMartialAttackCompatibility.AttackForm.OTHER_WEAPON in
                EncounterMartialAttackCompatibility.sharedAttackForms(espada, corrente)
        )
        assertEquals(0, EncounterMartialAttackCompatibility.weaponAffinity(espada, corrente))
    }

    @Test fun `mesma arma recebe mais afinidade que armas diferentes`() {
        val espadaA = estiloArmado("Espada A", "daiklave")
        val espadaB = estiloArmado("Espada B", "daiklave")
        val corrente = estiloArmado("Corrente", "corrente de combate")
        assertTrue(
            EncounterMartialAttackCompatibility.weaponAffinity(espadaA, espadaB) >
                EncounterMartialAttackCompatibility.weaponAffinity(espadaA, corrente)
        )
    }

    @Test fun `armadura comum aumenta afinidade sem tornar ausencia ilegal`() {
        val leveA = estiloArmado("Leve A", "daiklave").copy(armaduraCategoria = "leve")
        val leveB = estiloArmado("Leve B", "daiklave").copy(armaduraCategoria = "leve")
        val sem = estiloArmado("Sem", "daiklave").copy(armaduraCategoria = "incompativel")
        assertTrue(
            EncounterMartialArmorAffinity.affinity(leveA, leveB) >
                EncounterMartialArmorAffinity.affinity(leveA, sem)
        )
        assertTrue(null in EncounterMartialArmorAffinity.commonConfigurations(listOf(leveA, sem)))
    }

}
