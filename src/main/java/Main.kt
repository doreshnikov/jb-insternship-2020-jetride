import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.*

data class Point(val latitude: Float, val longitude: Float)
data class Participants(val passengers: Collection<Person>, val drivers: Collection<Person>)
data class Person(val id: UUID, val finishPoint: Point)

/*
Возможно, у меня проблемы с чтением, но, вроде в условии сказано, что все находятся в общей точке,
а в шаблоне кода этой точки не было, поэтому почему бы не сделать логичное предположение...
 */
val jetBrains = asPoint("59.9815845, 30.2124529")

fun main() {
    val (passengers, drivers) = readPoints()
    for (passenger in passengers) {
        val suggestedDrivers = suggestDrivers(passenger, drivers)
        println("Passenger point: ${passenger.finishPoint.latitude}, ${passenger.finishPoint.longitude}")
        for (driver in suggestedDrivers) {
            println("  ${driver.finishPoint.latitude}, ${driver.finishPoint.longitude}")
        }
    }
}

/*
Собственно, здесь было сразу примерно только две идеи:
- простая - просто расстояние на сфере
    - учитывая рамки одного города, сферичность Земли даже не особо играет роль
    - точность очень плохая, потому как дороги явно не всегда по дуге между двумя точками проложены
- сложная - найти API, которая умеет считать расстояния между точками на карте и делать к ней запросы
    - у Гугла платная
    - у Бинга не работает
    - еще кто-то не работает с маршрутами по России -_-

Поэтому используем первый способ...
 */

/*
Haversine formula
 */
fun distance(p1: Point, p2: Point): Double {
    val r = 6371
    val dLatitude = Math.toRadians((p1.latitude - p2.latitude).toDouble())
    val dLongitude = Math.toRadians((p1.longitude - p2.longitude).toDouble())

    val angle = sin(dLatitude / 2).pow(2) + sin(dLongitude / 2).pow(2) +
            cos(Math.toRadians(p1.latitude.toDouble())) * cos(Math.toRadians(p2.latitude.toDouble()))
    val angle2 = 2 * atan2(sqrt(angle), sqrt(1 - angle))
    return r * angle2
}

/*
Отсортируем по следующему критерию:
- поскольку логично, что водитель сначала довозит пассажира, а потом себя, то
- удобство пассажира постоянное
- удобство водителя будет решающим значением

Удобство водителя оценим как лишнюю дистанцию, которую ему придется проехать, если
он поедет не сразу домой, а через точку, в которую надо его пассажиру.

В общем случае этот способ с некоторой точностью может сортировать по различным направлениям,
следовательно, по различным районам города. Если расстояния большие, то даже от небольшого поворота
радиус-векторов, наша метрика заметно ухудшится, а если расстояния маленькие, то неоптимальность решения
не сделает его сильно хуже (в аюсолютных величинах).
 */
fun suggestDrivers(passenger: Person, drivers: Collection<Person>): Collection<Person> {
    return drivers.sortedBy { driver ->
        distance(jetBrains, passenger.finishPoint) +
                distance(passenger.finishPoint, driver.finishPoint) -
                distance(jetBrains, driver.finishPoint)
    }
}

private fun readPoints(): Participants {
    val pathToResource = Paths.get(Point::class.java.getResource("latlons").toURI())
    val allPoints = Files.readAllLines(pathToResource).map { asPoint(it) }.shuffled()
    val passengers = allPoints.slice(0..9).map { Person(UUID.randomUUID(), it) }
    val drivers = allPoints.slice(10..19).map { Person(UUID.randomUUID(), it) }
    return Participants(passengers, drivers)
}

private fun asPoint(it: String): Point {
    val (lat, lon) = it.split(", ")
    return Point(lat.toFloat(), lon.toFloat())
}
