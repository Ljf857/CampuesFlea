package com.example.campusflea.ui.screen

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.SearchViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(navController: NavHostController, viewModel: SearchViewModel = viewModel()) {
    var keyword by remember { mutableStateOf("") }
    val hotKeywords by viewModel.hotKeywords.collectAsState()
    val history = remember { mutableStateListOf<String>() }
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("search", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        val json = prefs.getString("history", null)
        if (!json.isNullOrBlank()) {
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = Gson().fromJson(json, type) ?: emptyList()
            history.addAll(list)
        }
    }

    fun saveHistory() {
        val json = Gson().toJson(history.toList())
        prefs.edit().putString("history", json).apply()
    }

    fun addToHistory(word: String) {
        history.remove(word)
        history.add(0, word)
        if (history.size > 10) history.removeAt(history.lastIndex)
        saveHistory()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") }
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("搜索闲置物品") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.outline) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) IconButton(onClick = { keyword = "" }) { Icon(Icons.Default.Close, "清除") }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant, focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                    if (keyword.isNotBlank()) {
                        addToHistory(keyword.trim()); navController.navigate(Screen.SearchResult.createRoute(keyword.trim()))
                    }
                })
            )
            Spacer(Modifier.width(8.dp))
            Text("搜索", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.clickable {
                if (keyword.isNotBlank()) {
                    addToHistory(keyword.trim()); navController.navigate(Screen.SearchResult.createRoute(keyword.trim()))
                }
            })
        }
        if (history.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("搜索历史", fontWeight = FontWeight.SemiBold); Text("清空", color = MaterialTheme.colorScheme.outline, modifier = Modifier.clickable { history.clear(); saveHistory() }) }
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { history.forEach { SuggestionChip(onClick = { keyword = it; navController.navigate(Screen.SearchResult.createRoute(it)) }, label = { Text(it) }) } }
        }
        Spacer(Modifier.height(16.dp))
        Text("热门搜索", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
            items(hotKeywords.take(10).withIndex().toList(), key = { it.index }) { (i, w) ->
                Row(Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth().clickable { keyword = w; navController.navigate(Screen.SearchResult.createRoute(w)) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = if (i < 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(24.dp), fontSize = 14.sp)
                    Text(w, fontSize = 14.sp)
                    if (i < 3) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}
