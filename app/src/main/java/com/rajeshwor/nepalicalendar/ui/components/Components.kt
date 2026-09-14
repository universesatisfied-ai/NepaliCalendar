package com.rajeshwor.nepalicalendar.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rajeshwor.nepalicalendar.ui.theme.*

@Composable fun BrandHeader(title:String,subtitle:String?=null){Column(Modifier.padding(horizontal=20.dp,vertical=14.dp)){Text(title,style=MaterialTheme.typography.headlineSmall); if(subtitle!=null)Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable fun ActionCard(label:String,icon:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Lime,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text(icon,color=DeepGreen)}; Spacer(Modifier.width(14.dp));Text(label,style=MaterialTheme.typography.titleMedium)}}}
@Composable fun Pill(text:String,active:Boolean=false){Surface(shape=RoundedCornerShape(50),color=if(active)Lime else MaterialTheme.colorScheme.surfaceVariant){Text(text,Modifier.padding(horizontal=14.dp,vertical=8.dp),color=if(active)DeepGreen else MaterialTheme.colorScheme.onSurfaceVariant)}}
