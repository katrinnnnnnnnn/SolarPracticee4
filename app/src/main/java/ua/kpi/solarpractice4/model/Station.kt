package ua.kpi.solarpractice4.model

// Демонстраційні координати та енергетичні показники, а не реальні станції.
data class Station(
    val id: Int, val name: String, val region: String,
    val type: String, val powerKw: Int, val online: Boolean,
    val latitude: Double, val longitude: Double,
    val updated: String = "2026-11-17"
)

object DemoStations {
    val items = listOf(
        Station(1,"Сонячна станція Київ","Київ","Сонячна",120,true,50.4501,30.5234),
        Station(2,"Станція Львів","Львів","Сонячна",180,true,49.8397,24.0297),
        Station(3,"Сонячна станція Одеса","Одеса","Сонячна",95,false,46.4825,30.7233),
        Station(4,"Вітропарк Дніпро","Дніпро","Вітрова",260,true,48.4647,35.0462),
        Station(5,"Сонячна станція Харків","Харків","Сонячна",145,true,49.9935,36.2304),
        Station(6,"Вітропарк Тернопіль","Тернопіль","Вітрова",210,false,49.5535,25.5948),
        Station(7,"Сонячна станція Черкаси","Черкаси","Сонячна",75,true,49.4444,32.0598),
        Station(8,"Сонячна станція Вінниця","Вінниця","Сонячна",130,true,49.2331,28.4682)
    )
}
