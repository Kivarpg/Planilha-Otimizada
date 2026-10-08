package com.example.data
import org.junit.Assert.*
import org.junit.Test
class DragonBloodedSignaturePolicyTest {
 private fun sig(name:String, ability:String, element:String)=EncantoSolarDefinition(
  id=name, habilidade=ability, nome=name, nomeIngles="", custo="", minsTexto="",
  minHabilidade=5, minEssencia=3, tipo="", palavrasChave="Assinatura ($element)",
  duracao="", preRequisitos="", descricao=""
 )
 @Test fun `antes da essencia cinco limita uma assinatura por habilidade`() {
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo"); val cat=listOf(ar,fogo)
  assertTrue(DragonBloodedSignaturePolicy.canSelect(ar,emptySet(),cat,3,"Fogo",emptyList()))
  assertFalse(DragonBloodedSignaturePolicy.canSelect(fogo,setOf("A"),cat,4,"Fogo",listOf("Armas Brancas")))
 }
 @Test fun `essencia cinco permite segunda opcional e exige aspecto quando ainda ausente`() {
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo"); val terra=sig("C","Armas Brancas","Terra"); val cat=listOf(ar,fogo,terra)
  assertTrue(DragonBloodedSignaturePolicy.canSelect(fogo,setOf("A"),cat,5,"Fogo",listOf("Armas Brancas")))
  assertFalse(DragonBloodedSignaturePolicy.canSelect(terra,setOf("A"),cat,5,"Fogo",listOf("Armas Brancas")))
 }
 @Test fun `segunda assinatura exige habilidade de aspecto ou favorecida`() {
  val ar=sig("A","Habilidade X","Ar"); val fogo=sig("B","Habilidade X","Fogo"); val cat=listOf(ar,fogo)
  assertFalse(DragonBloodedSignaturePolicy.canSelect(fogo,setOf("A"),cat,5,"Fogo",emptyList()))
 }
 @Test fun `nunca permite terceira assinatura`() {
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo"); val terra=sig("C","Armas Brancas","Terra"); val cat=listOf(ar,fogo,terra)
  assertFalse(DragonBloodedSignaturePolicy.canSelect(terra,setOf("A","B"),cat,5,"Fogo",listOf("Armas Brancas")))
 }

 @Test fun `filtro otimizado de progressao respeita segunda assinatura`() {
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo"); val terra=sig("C","Armas Brancas","Terra")
  val filtrado=DragonBloodedSignaturePolicy.filterForProgression(listOf(ar,fogo,terra),setOf("A"),5,"Fogo",listOf("Armas Brancas"))
  assertTrue(filtrado.any { it.nome=="B" })
  assertFalse(filtrado.any { it.nome=="C" })
 }

 @Test fun `segunda assinatura pode usar habilidade favorecida fora do aspecto`() {
  val ar=sig("A","Arqueirismo","Ar"); val fogo=sig("B","Arqueirismo","Fogo"); val cat=listOf(ar,fogo)
  assertTrue(DragonBloodedSignaturePolicy.canSelect(fogo,setOf("A"),cat,5,"Fogo",listOf("Arqueirismo")))
 }
 @Test fun `metadados normalizados preparados preservam filtro de progressao`() {
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo"); val terra=sig("C","Armas Brancas","Terra")
  val cat=listOf(ar,fogo,terra)
  val normal=DragonBloodedSignaturePolicy.filterForProgression(cat,setOf("A"),5,"Fogo",listOf("Armas Brancas"))
  val preparado=DragonBloodedSignaturePolicy.filterForProgression(
   cat,setOf("A"),5,"Fogo",listOf("Armas Brancas"),
   favoredNormalizedPrepared=setOf("armas brancas"),
   aspectAbilitiesNormalizedPrepared=com.example.model.Aspecto.Fogo.allowedAbilities().mapTo(hashSetOf()) { it.lowercase() }
  )
  assertEquals(normal.map { it.nome }, preparado.map { it.nome })
 }

 @Test fun `catalogo preparado preserva ordem e encantos sem assinatura`() {
  val comum=EncantoSolarDefinition(
   id="N", habilidade="Armas Brancas", nome="Normal", nomeIngles="", custo="", minsTexto="",
   minHabilidade=1, minEssencia=1, tipo="", palavrasChave="",
   duracao="", preRequisitos="", descricao=""
  )
  val ar=sig("A","Armas Brancas","Ar"); val fogo=sig("B","Armas Brancas","Fogo")
  val cat=listOf(comum,ar,fogo)
  val primeira=DragonBloodedSignaturePolicy.filterForProgression(cat,setOf("A"),5,"Fogo",listOf("Armas Brancas"))
  val segunda=DragonBloodedSignaturePolicy.filterForProgression(cat,setOf("A"),5,"Fogo",listOf("Armas Brancas"))
  assertEquals(listOf("Normal","B"), primeira.map { it.nome })
  assertEquals(primeira.map { it.nome }, segunda.map { it.nome })
 }

 @Test fun `projecao de cache ignora encantos comuns e muda com assinatura`() {
  val comum=EncantoSolarDefinition(
   id="N", habilidade="Armas Brancas", nome="Normal", nomeIngles="", custo="", minsTexto="",
   minHabilidade=1, minEssencia=1, tipo="", palavrasChave="",
   duracao="", preRequisitos="", descricao=""
  )
  val ar=sig("A","Armas Brancas","Ar")
  val cat=listOf(comum,ar)
  assertEquals(emptySet<String>(), DragonBloodedSignaturePolicy.selectedSignatureNames(cat,setOf("Normal")))
  assertEquals(setOf("A"), DragonBloodedSignaturePolicy.selectedSignatureNames(cat,setOf("Normal","A")))
 }


}
