package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRouteEligibilityRegressionTest {
 data class D(val name:String,val req:Set<String>,val xp:Int=10)
 @Test fun `unlock marginal exige requisito completo`() {
  val a=D("A",emptySet()); val b=D("B",emptySet()); val c=D("C",setOf("A","B"))
  val pick=EncounterCharmRouteOptimizer.escolher(
   candidatos=listOf(a,b),catalogoCompleto=listOf(a,b,c),
   nomesSelecionados=emptySet(),contagensCategorias=emptyMap(),
   elegivel={d,s,_->d.req.all{s.contains(it)}},
   nome={it.name},categoria={"x"},custoXp={it.xp},
   beamWidth=2,profundidade=1
  )
  assertNotNull(pick)
 }
}