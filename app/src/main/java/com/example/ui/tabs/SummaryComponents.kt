package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
internal fun ValorResumoCard(label: String, valor: Int, modifier: Modifier = Modifier) {
    Surface(modifier=modifier,color=ExaltedAmber.copy(alpha=.1f),shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,ExaltedAmber.copy(alpha=.4f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            AppText(label,style=MaterialTheme.typography.bodySmall,maxLines=1,softWrap=false,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            AppText("$valor",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Bold,color=ExaltedAmber)
        }
    }
}

@Composable
internal fun ExperienceStat(label:String,valor:Int,cor:Color,modifier:Modifier=Modifier,destaque:Boolean=false){
    Surface(color=if(destaque)cor.copy(alpha=.14f) else androidx.compose.ui.graphics.Color.Transparent,shape=RoundedCornerShape(6.dp),border=BorderStroke(1.dp,cor.copy(alpha=if(destaque).7f else .35f)),modifier=modifier){
        Column(Modifier.fillMaxWidth().padding(horizontal=6.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
            AppText(label,style=MaterialTheme.typography.labelSmall,color=ExaltedMuted,textAlign=TextAlign.Center,maxLines=1,softWrap=false)
            Spacer(Modifier.height(2.dp)); AppText("$valor",style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold),color=cor,textAlign=TextAlign.Center)
        }
    }
}

@Composable
internal fun DotsDisplay(rating:Int,max:Int=5){
    Row(verticalAlignment=Alignment.CenterVertically){for(i in 1..max) Box(Modifier.padding(horizontal=2.dp).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if(i<=rating)ExaltedAmber else ExaltedDarkSurfaceVariant).border(1.dp,ExaltedAmber,androidx.compose.foundation.shape.CircleShape))}
}
