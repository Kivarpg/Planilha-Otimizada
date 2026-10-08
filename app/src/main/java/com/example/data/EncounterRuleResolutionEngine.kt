package com.example.data

internal object EncounterRuleResolutionEngine {
 @JvmInline value class RuleId(val value:String)
 enum class Relation { APPLIES_AFTER, OVERRIDES, SUPPRESSES, COEXISTS }
 data class Rule(val id:RuleId,val applies:Boolean=true)
 data class RuleRelation(val source:RuleId,val target:RuleId,val relation:Relation)
 data class Resolution(
  val active:Set<RuleId>,val suppressed:Set<RuleId>,val ambiguous:Boolean,
  val invalid:Boolean=false,val reason:String?=null
 )

 fun resolve(rules:Collection<Rule>,relations:Collection<RuleRelation>):Resolution {
  val duplicateIds=rules.groupingBy { it.id }.eachCount().filterValues { it>1 }.keys
  if(duplicateIds.isNotEmpty())
   return Resolution(emptySet(),emptySet(),true,true,"RuleId duplicado.")

  val allIds=rules.map { it.id }.toSet()
  val dangling=relations.filter { it.source !in allIds || it.target !in allIds }
  if(dangling.isNotEmpty())
   return Resolution(emptySet(),emptySet(),true,true,"Relação referencia regra inexistente.")
  if(relations.any { it.source==it.target && it.relation!=Relation.COEXISTS })
   return Resolution(emptySet(),emptySet(),true,true,"Regra destrutiva/ordenadora autorreferente.")

  val initiallyActive=rules.filter { it.applies }.map { it.id }.toSet()
  val relevant=relations.filter { it.source in initiallyActive && it.target in initiallyActive }
  val destructive=relevant.filter { it.relation==Relation.OVERRIDES || it.relation==Relation.SUPPRESSES }
  val pairs=destructive.map { it.source to it.target }.toSet()
  if(pairs.any { (a,b)->(b to a) in pairs })
   return Resolution(initiallyActive,emptySet(),true,false,"Supressão/override contraditório.")

  // APPLIES_AFTER precisa ser acíclico mesmo sem alterar o conjunto ativo.
  val orderEdges=relevant.filter { it.relation==Relation.APPLIES_AFTER }
   .map { it.target to it.source } // target antes de source
  val outgoing=initiallyActive.associateWith { mutableSetOf<RuleId>() }
  val indegree=initiallyActive.associateWith { 0 }.toMutableMap()
  for((a,b) in orderEdges) if(outgoing.getValue(a).add(b)) indegree[b]=indegree.getValue(b)+1
  val q=java.util.ArrayDeque(indegree.filterValues { it==0 }.keys.sortedBy { it.value })
  var seen=0
  while(q.isNotEmpty()) {
   val a=q.removeFirst(); seen++
   for(b in outgoing.getValue(a).sortedBy { it.value }) {
    indegree[b]=indegree.getValue(b)-1
    if(indegree.getValue(b)==0) q.addLast(b)
   }
  }
  if(seen!=initiallyActive.size)
   return Resolution(initiallyActive,emptySet(),true,false,"Ciclo em APPLIES_AFTER.")

  val suppressed=destructive.map { it.target }.toSet()
  return Resolution(initiallyActive-suppressed,suppressed,false)
 }
}
