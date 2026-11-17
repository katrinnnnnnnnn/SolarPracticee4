package ua.kpi.solarpractice4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.kpi.solarpractice4.model.DemoStations
import ua.kpi.solarpractice4.model.Station
import kotlin.math.roundToInt
import kotlin.random.Random

class SolarViewModel : ViewModel() {
    private val _stations = MutableStateFlow(DemoStations.items)
    private val _query = MutableStateFlow("")
    private val _type = MutableStateFlow("Всі")
    private val _onlineOnly = MutableStateFlow(false)
    private val _sort = MutableStateFlow("Назва")
    private val _selected = MutableStateFlow<Int?>(null)
    private val _status = MutableStateFlow("Потоки не запущено")
    private val _sensor = MutableStateFlow(0)
    private val _forecast = MutableStateFlow(0)
    private val _combined = MutableStateFlow(0)
    private val _running = MutableStateFlow(false)

    val selected: StateFlow<Int?> = _selected.asStateFlow()
    val status: StateFlow<String> = _status.asStateFlow()
    val sensor: StateFlow<Int> = _sensor.asStateFlow()
    val forecast: StateFlow<Int> = _forecast.asStateFlow()
    val combined: StateFlow<Int> = _combined.asStateFlow()
    val running: StateFlow<Boolean> = _running.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()
    val type: StateFlow<String> = _type.asStateFlow()
    val onlineOnly: StateFlow<Boolean> = _onlineOnly.asStateFlow()
    val sort: StateFlow<String> = _sort.asStateFlow()

    // Фільтри комбінуються реактивно. StateFlow надає UI останній стан.
    val visible: StateFlow<List<Station>> = combine(
        _stations, _query.debounce(250), _type, _onlineOnly, _sort
    ) { stations, query, type, onlineOnly, sort ->
        val filtered = stations.filter { station ->
            (query.isBlank() || station.name.contains(query,true) || station.region.contains(query,true)) &&
                (type == "Всі" || station.type == type) && (!onlineOnly || station.online)
        }
        when (sort) {
            "Потужність ↓" -> filtered.sortedByDescending { it.powerKw }
            "Потужність ↑" -> filtered.sortedBy { it.powerKw }
            else -> filtered.sortedBy { it.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoStations.items)

    fun setQuery(v: String) { _query.value = v }
    fun setType(v: String) { _type.value = v }
    fun setOnlineOnly(v: Boolean) { _onlineOnly.value = v }
    fun setSort(v: String) { _sort.value = v }
    fun select(id: Int?) { _selected.value = id }

    // Холодні потоки генерують значення лише після підписки; retryWhen відновлює збір.
    private fun sensorFlow() = flow {
        while (true) {
            delay(1100)
            if (Random.nextInt(25) == 0) error("Тимчасова помилка датчика")
            emit(Random.nextInt(450, 1700))
        }
    }.retryWhen { cause, attempt ->
        _status.value = "Відновлення датчика: ${cause.message}, спроба ${attempt+1}"
        delay(1000)
        attempt < 3
    }.catch { _status.value = "Датчик недоступний: ${it.message}" }

    private fun forecastFlow() = flow {
        while (true) { delay(1650); emit(Random.nextInt(550, 1750)) }
    }

    private var polling: kotlinx.coroutines.Job? = null
    fun start() {
        if (polling?.isActive == true) return
        _running.value = true
        _status.value = "Отримання даних..."
        polling = viewModelScope.launch {
            try {
                sensorFlow().combine(forecastFlow()) { sensor, forecast -> sensor to forecast }
                    .collect { (sensor, forecast) ->
                        _sensor.value = sensor
                        _forecast.value = forecast
                        _combined.value = (sensor * .6 + forecast * .4).roundToInt()
                        _status.value = "Потоки активні · значення навчальні"
                    }
            } catch (e: CancellationException) { throw e }
            finally { _running.value = false }
        }
    }
    fun stop() { polling?.cancel(); _running.value = false; _status.value = "Зупинено" }
}
