package com.example.data

internal object EncounterBuildCoherence {
 enum class Role { CORE_IDENTITY, CORE_ENGINE, SUPPORT, UTILITY, BRIDGE, CAPSTONE }
 data class Contribution(val powerId:String,val role:Role,val mechanics:Set<String>,val strength:Int)
 data class Result(
  val mechanicalStrength:Int?,val coverage:Int,val coreFocus:Int,
  val genericHubPenalty:Int?,val valid:Boolean
 )
 fun evaluate(items:Collection<Contribution>):Result {
  if(items.any { it.powerId.isBlank() } || items.groupingBy { it.powerId }.eachCount().any { it.value>1 })
   return Result(null,0,0,null,false)
  val strength=try { items.fold(0){a,x->Math.addExact(a,x.strength)} }
   catch(_:ArithmeticException){ return Result(null,0,0,null,false) }
  val coverage=items.flatMap { it.mechanics }.toSet().size
  val core=items.filter { it.role==Role.CORE_IDENTITY || it.role==Role.CORE_ENGINE }
  val coreMechanics=core.flatMap { it.mechanics }.groupingBy { it }.eachCount()
  val focus=coreMechanics.values.maxOrNull() ?: 0
  val penalty=try {
   items.filter { (it.role==Role.SUPPORT || it.role==Role.UTILITY) && it.mechanics.size>=4 }
    .fold(0){a,x->Math.addExact(a,x.mechanics.size-3)}
  } catch(_:ArithmeticException){ return Result(strength,coverage,focus,null,false) }
  return Result(strength,coverage,focus,penalty,true)
 }
}
