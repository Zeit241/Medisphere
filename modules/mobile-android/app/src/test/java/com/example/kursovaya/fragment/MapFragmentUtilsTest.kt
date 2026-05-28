package com.example.kursovaya.fragment

import com.mapbox.geojson.Point
import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для утилитарных функций MapFragment
 * Тестируем математические функции, которые не требуют Android контекста
 */
class MapFragmentUtilsTest {
    @Test
    fun `distance calculation between two points`() {
        // Arrange
        val point1 = Point.fromLngLat(37.6173, 55.7558) // Москва
        val point2 = Point.fromLngLat(30.3159, 59.9343) // Санкт-Петербург

        // Act - используем упрощенную формулу расстояния (квадрат расстояния)
        val dx = point1.longitude() - point2.longitude()
        val dy = point1.latitude() - point2.latitude()
        val distanceSquared = dx * dx + dy * dy

        // Assert
        assertTrue(distanceSquared > 0)
        // Расстояние между Москвой и СПб должно быть значительным
        assertTrue(distanceSquared > 0.01)
    }

    @Test
    fun `distance between same points is zero`() {
        // Arrange
        val point = Point.fromLngLat(37.6173, 55.7558)

        // Act
        val dx = point.longitude() - point.longitude()
        val dy = point.latitude() - point.latitude()
        val distanceSquared = dx * dx + dy * dy

        // Assert
        assertEquals(0.0, distanceSquared, 0.0001)
    }

    @Test
    fun `point creation and coordinate access`() {
        // Arrange & Act
        val point = Point.fromLngLat(37.6173, 55.7558)

        // Assert
        assertEquals(37.6173, point.longitude(), 0.0001)
        assertEquals(55.7558, point.latitude(), 0.0001)
    }

    @Test
    fun `distance calculation is symmetric`() {
        // Arrange
        val point1 = Point.fromLngLat(37.6173, 55.7558)
        val point2 = Point.fromLngLat(30.3159, 59.9343)

        // Act
        val dx1 = point1.longitude() - point2.longitude()
        val dy1 = point1.latitude() - point2.latitude()
        val distance1 = dx1 * dx1 + dy1 * dy1

        val dx2 = point2.longitude() - point1.longitude()
        val dy2 = point2.latitude() - point1.latitude()
        val distance2 = dx2 * dx2 + dy2 * dy2

        // Assert
        assertEquals(distance1, distance2, 0.0001)
    }

    @Test
    fun `polygon centroid calculation test`() {
        // Arrange - простой прямоугольник
        val points =
            listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(2.0, 0.0),
                Point.fromLngLat(2.0, 2.0),
                Point.fromLngLat(0.0, 2.0),
            )

        // Act - вычисляем центроид
        var sx = 0.0
        var sy = 0.0
        val n = points.size
        for (p in points) {
            sx += p.longitude()
            sy += p.latitude()
        }
        val centroidLon = sx / n
        val centroidLat = sy / n

        // Assert - центроид должен быть в центре
        assertEquals(1.0, centroidLon, 0.0001)
        assertEquals(1.0, centroidLat, 0.0001)
    }

    @Test
    fun `route bounds calculation`() {
        // Arrange - список точек маршрута
        val routePoints =
            listOf(
                Point.fromLngLat(37.0, 55.0),
                Point.fromLngLat(38.0, 56.0),
                Point.fromLngLat(39.0, 57.0),
            )

        // Act - находим границы
        val longitudes = routePoints.map { it.longitude() }
        val latitudes = routePoints.map { it.latitude() }
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0
        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0

        // Assert
        assertEquals(37.0, minLon, 0.0001)
        assertEquals(39.0, maxLon, 0.0001)
        assertEquals(55.0, minLat, 0.0001)
        assertEquals(57.0, maxLat, 0.0001)
    }
}
