package ua.kpi.solarpractice4.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.util.GeoPoint
import ua.kpi.solarpractice4.model.Station
import ua.kpi.solarpractice4.viewmodel.SolarViewModel

@Composable
fun SolarPracticeApp(vm: SolarViewModel = viewModel()) {
    var screen by rememberSaveable { mutableIntStateOf(13) }
    Scaffold(topBar = { Surface(tonalElevation=3.dp) { Text("SolarPractice · ПР №4",Modifier.fillMaxWidth().padding(18.dp),style=MaterialTheme.typography.titleLarge) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                (13..16).forEach { n ->
                    if(screen==n) Button(onClick={screen=n},Modifier.weight(1f),contentPadding=PaddingValues(2.dp)) { Text("ПЗ $n") }
                    else OutlinedButton(onClick={screen=n},Modifier.weight(1f),contentPadding=PaddingValues(2.dp)) { Text("ПЗ $n") }
                }
            }
            when(screen) {
                13 -> FlowScreen(vm)
                14 -> StationsScreen(vm, search=false)
                15 -> StationsScreen(vm, search=true)
                16 -> LocationScreen(vm)
            }
        }
    }
}

@Composable
fun FlowScreen(vm: SolarViewModel) {
    val sensor by vm.sensor.collectAsStateWithLifecycle()
    val forecast by vm.forecast.collectAsStateWithLifecycle()
    val combined by vm.combined.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("ПЗ 13 · Flow і Coroutines",style=MaterialTheme.typography.titleLarge)
        Text("Два асинхронні потоки (датчик і прогноз) комбінуються через combine. Модельні дані, не реальні вимірювання.")
        StationStat("Датчик",sensor)
        StationStat("Прогноз",forecast)
        StationStat("Об’єднана потужність (60% + 40%)",combined)
        Text("Стан: $status")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm::start},enabled=!running) { Text("Старт") }
            OutlinedButton(onClick={vm::stop},enabled=running) { Text("Стоп") }
        }
        Text("Корутини запускаються у viewModelScope; StateFlow оновлює Compose без блокування UI.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun StationStat(label:String, value:Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label,style=MaterialTheme.typography.labelLarge)
            Text("$value Вт",style=MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun StationCard(s:Station, selected:Boolean=false, onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(s.name,fontWeight=FontWeight.Bold)
            Text("${s.type} · ${s.region} · ${s.powerKw} кВт")
            Text(if(s.online) "Стан: активна" else "Стан: неактивна",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StationsScreen(vm:SolarViewModel,search:Boolean) {
    val stations by vm.visible.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val onlyOnline by vm.onlineOnly.collectAsStateWithLifecycle()
    var showDetails by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(top=8.dp)) {
        Text(if(search) "ПЗ 15 · Пошук та сортування" else "ПЗ 14 · Динамічний список і мапа",style=MaterialTheme.typography.titleLarge)
        if(search) {
            OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),label={ Text("Пошук за назвою або містом") },singleLine=true)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Тільки активні")
                Switch(onlyOnline,vm::setOnlineOnly)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("Всі","Сонячна","Вітрова").forEach { v ->
                    FilterChip(selected=type==v,onClick={vm.setType(v)},label={Text(v)})
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("Назва","Потужність ↓","Потужність ↑").forEach { v ->
                    FilterChip(selected=sort==v,onClick={vm.setSort(v)},label={Text(v)})
                }
            }
        }
        Text("Знайдено: ${stations.size}",Modifier.padding(vertical=8.dp))
        if(!search) {
            StationMap(stations,selected,{vm.select(it);showDetails=true},modifier=Modifier.fillMaxWidth().height(230.dp))
            Text("Натисни картку або маркер: вибір синхронізується",style=MaterialTheme.typography.bodySmall)
        }
        if(stations.isEmpty()) Text("Немає об'єктів за вибраними умовами",Modifier.padding(18.dp))
        LazyColumn(contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(stations,key={it.id}) { s ->
                StationCard(s,selected==s.id) { vm.select(s.id);showDetails=true }
            }
        }
    }
    if(showDetails) {
        stations.firstOrNull{it.id==selected}?.let{s->
            AlertDialog(onDismissRequest={showDetails=false},title={Text(s.name)},
                text={Text("${s.region} · ${s.type} · ${s.powerKw} кВт\nСтатус: ${if(s.online) "активна" else "неактивна"}\nКоординати: ${s.latitude}, ${s.longitude}\nДані демонстраційні")},
                confirmButton={TextButton(onClick={showDetails=false}){Text("Закрити")}})
        }
    }
}

@Composable
fun LocationScreen(vm:SolarViewModel) {
    val context=LocalContext.current
    val stations by vm.visible.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    var status by remember { mutableStateOf("Дозвіл ще не запитувався") }
    val manager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val listener=remember {
        object:LocationListener {
            override fun onLocationChanged(loc:Location) { location=GeoPoint(loc.latitude,loc.longitude);status="GPS: ${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}" }
            override fun onProviderEnabled(provider:String) { }
            override fun onProviderDisabled(provider:String) { status="GPS вимкнено" }
            @Deprecated("Compatibility callback") override fun onStatusChanged(provider:String,statusCode:Int,extras:Bundle?) {}
        }
    }
    fun requestLocation() {
        val fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val coarse=context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if(!fine && !coarse) {status="Потрібен дозвіл на геолокацію";return}
        try {
            val provider=if(fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                else if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null
            if(provider==null){status="Геолокація вимкнена у налаштуваннях";return}
            manager.getLastKnownLocation(provider)?.let{ listener.onLocationChanged(it) }
            manager.requestLocationUpdates(provider,2000L,5f,listener)
            status="Отримання GPS... Перевір налаштування емулятора"
        } catch(e:SecurityException){status="Немає дозволу: ${e.message}"}
          catch(e:Exception){status="Помилка GPS: ${e.message}"}
    }
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if(perms.values.any{it}) requestLocation() else status="Доступ до геолокації відхилено"
    }
    DisposableEffect(manager,listener) { onDispose { manager.removeUpdates(listener) } }
    Column(Modifier.fillMaxSize().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text("ПЗ 16 · GPS та мапа",style=MaterialTheme.typography.titleLarge)
        Button(onClick={
            val granted=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            if(granted) requestLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }) { Text("Моє місцезнаходження") }
        Text(status,style=MaterialTheme.typography.bodySmall)
        StationMap(stations,selected,vm::select,location,Modifier.fillMaxWidth().height(330.dp))
        Text("Маркери навчальні; GPS — з пристрою, після надання дозволу.")
        Text("Обрано: ${stations.firstOrNull{it.id==selected}?.name ?: "нічого"}")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            items(stations,key={it.id}) { s -> StationCard(s,s.id==selected){vm.select(s.id)} }
        }
    }
}
