package com.example.data
internal object EncounterResourceModifierValidation {
 data class Issue(val id:String,val reason:String)
 fun validate(m:EncounterResourceModifierResolver.Modifier):List<Issue> {
  val out=mutableListOf<Issue>()
  fun nonNegative(x:Int?,label:String){ if(x!=null && x<0) out+=Issue(m.id,"$label negativo.") }
  nonNegative(m.amount,"amount"); nonNegative(m.replacementAmount,"replacementAmount")
  when(m.kind) {
   EncounterResourceModifierResolver.Kind.REPLACE_COST ->
    if(m.replacementAmount==null) out+=Issue(m.id,"REPLACE_COST sem replacementAmount.")
   EncounterResourceModifierResolver.Kind.REDUCE_COST,
   EncounterResourceModifierResolver.Kind.CAP_COST ->
    if(m.amount==null) out+=Issue(m.id,"Modificador quantitativo sem amount.")
   EncounterResourceModifierResolver.Kind.CHANGE_RESOURCE ->
    if(m.replacementResource==null) out+=Issue(m.id,"CHANGE_RESOURCE sem replacementResource.")
   else -> Unit
  }
  return out
 }
}
