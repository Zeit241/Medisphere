package com.example.kursovaya.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.kursovaya.R
import com.example.kursovaya.databinding.FragmentMapBinding
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapBinding
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var styleRef: Style? = null
    private var pointsFeatures: List<com.mapbox.geojson.Feature> = emptyList()
    private var pathFeatures: List<com.mapbox.geojson.Feature> = emptyList()
    private var polygonFeatures: List<com.mapbox.geojson.Feature> = emptyList()

    private var lastSelectedStart: Feature? = null
    private var lastSelectedEnd: Feature? = null

    private var routeAnimator: ValueAnimator? = null

    // ID кабинета для автоматического построения маршрута
    private var targetRoomId: String? = null

    private val displayablePoints: List<Feature>
        get() =
            pointsFeatures.filter { f ->
                f.properties()?.let {
                    !it.has("type") || it.get("type")?.asString != "path"
                } ?: true
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация совместимого класса Mapbox (MapLibre v9)
        Mapbox.getInstance(requireContext().applicationContext)

        // Получаем roomId из аргументов для автоматической навигации
        targetRoomId = arguments?.getString("roomId")

        if (targetRoomId != null) {
            android.util.Log.d("MapFragment", "=== Получен roomId для навигации ===")
            android.util.Log.d("MapFragment", "roomId: $targetRoomId")
            android.util.Log.d("MapFragment", "====================================")
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri("asset://map_style_offline.json")) { style: Style ->
            styleRef = style
            addGeoJsonOverlay(style, mapboxMap)
            ensureRouteLayer(style)
            initUiFromData()
            wireUi()

            // Автоматически строим маршрут если передан roomId
            targetRoomId?.let { roomId ->
                buildRouteToRoom(roomId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapView

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun addGeoJsonOverlay(
        style: Style,
        map: MapboxMap,
    ) {
        val geoJson = resources.openRawResource(R.raw.map).bufferedReader().use { it.readText() }
        val featureCollection = FeatureCollection.fromJson(geoJson)

        val sourceId = "custom-geojson"
        val source = GeoJsonSource(sourceId, geoJson)
        style.addSource(source)

        val polys = mutableListOf<com.mapbox.geojson.Feature>()
        val pts = mutableListOf<com.mapbox.geojson.Feature>()
        val pathPts = mutableListOf<com.mapbox.geojson.Feature>()
        featureCollection.features()?.forEach { f ->
            when (f.geometry()) {
                is Polygon -> polys.add(f)
                is Point -> {
                    if (f.properties()?.has("type") == true && f.properties()
                            ?.get("type")?.asString == "path"
                    ) {
                        pathPts.add(f)
                    } else {
                        pts.add(f)
                    }
                }
            }
        }
        polygonFeatures = polys
        pointsFeatures = pts
        pathFeatures = pathPts

        val styleData = Expression.get("styledata", Expression.get("styledetails"))
        val circleColorExpr =
            Expression.coalesce(
                Expression.get("circlecolor", styleData),
                Expression.literal("#B42222"),
            )
        val circleRadiusExpr =
            Expression.coalesce(Expression.get("circleradius", styleData), Expression.literal(4))

        val fillLayer =
            FillLayer("polygons-fill", sourceId)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(
                    PropertyFactory.fillColor("#3b82f6"),
                    PropertyFactory.fillOpacity(0.35f),
                )
        style.addLayer(fillLayer)

        val outlineLayer =
            LineLayer("polygons-outline", sourceId)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(
                    PropertyFactory.lineColor("#333333"),
                    PropertyFactory.lineWidth(1.5f),
                )
        style.addLayer(outlineLayer)

        val pointsLayer =
            CircleLayer("poi", sourceId)
                .withFilter(
                    Expression.all(
                        Expression.eq(Expression.geometryType(), Expression.literal("Point")),
                        Expression.neq(Expression.get("type"), Expression.literal("path")),
                    ),
                )
                .withProperties(
                    PropertyFactory.circleColor(circleColorExpr),
                    PropertyFactory.circleRadius(Expression.toNumber(circleRadiusExpr)),
                )
        style.addLayer(pointsLayer)

        // Подписи поверх полигонов по их центроидам
        val labelFeatures =
            polygonFeatures.mapNotNull { f ->
                val poly = f.geometry() as? Polygon ?: return@mapNotNull null
                val props = f.properties()
                val name = props?.get("name")?.asString
                if (name.isNullOrEmpty()) return@mapNotNull null
                val center = polygonCentroid(poly)
                val angle = polygonAngle(poly)
                Feature.fromGeometry(center).apply {
                    addStringProperty("name", name)
                    addNumberProperty("angle", angle)
                }
            }
        if (style.getSource("polygon-labels") == null) {
            style.addSource(
                GeoJsonSource(
                    "polygon-labels",
                    FeatureCollection.fromFeatures(labelFeatures),
                ),
            )
        } else {
            style.getSourceAs<GeoJsonSource>("polygon-labels")
                ?.setGeoJson(FeatureCollection.fromFeatures(labelFeatures))
        }
        if (style.getLayer("polygon-labels-layer") == null) {
            val labelLayer =
                SymbolLayer("polygon-labels-layer", "polygon-labels")
                    .withProperties(
                        PropertyFactory.textField(Expression.get("name")),
                        PropertyFactory.textColor("#111827"),
                        PropertyFactory.textSize(14f),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true),
                        PropertyFactory.textRotationAlignment(Property.TEXT_ROTATION_ALIGNMENT_MAP),
                        PropertyFactory.textRotate(43.0f),
                        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_CENTER),
                    )
            style.addLayerAbove(labelLayer, "polygons-outline")
        }

        if (style.getSource("selected-points") == null) {
            style.addSource(
                GeoJsonSource(
                    "selected-points",
                    FeatureCollection.fromFeatures(arrayOf()),
                ),
            )
        }
        if (style.getLayer("poi-selected") == null) {
            val selectedLayer =
                CircleLayer("poi-selected", "selected-points")
                    .withProperties(
                        PropertyFactory.circleColor("#ff9900"),
                        PropertyFactory.circleRadius(6f),
                    )
            style.addLayer(selectedLayer)
        }

        computeBounds(featureCollection)?.let { bounds ->
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 40), 1000)
            map.addOnCameraIdleListener(
                object : MapboxMap.OnCameraIdleListener {
                    override fun onCameraIdle() {
                        map.removeOnCameraIdleListener(this)
                        val cp =
                            CameraPosition.Builder()
                                .target(map.cameraPosition.target)
                                .zoom(19.7)
                                .bearing(104.0)
                                .tilt(map.cameraPosition.tilt)
                                .build()
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(cp))
                    }
                },
            )
        }
    }

    private fun ensureRouteLayer(style: Style) {
        if (style.getSource("route") == null) {
            style.addSource(GeoJsonSource("route", FeatureCollection.fromFeatures(arrayOf())))
        }
        if (style.getLayer("route-layer") == null) {
            val routeLayer =
                LineLayer("route-layer", "route")
                    .withProperties(
                        PropertyFactory.lineColor("#13b113"),
                        PropertyFactory.lineWidth(5f),
                    )
            style.addLayer(routeLayer)
        }

        // Источник/слой для анимированной точки по маршруту
        if (style.getSource("route-anim-point") == null) {
            style.addSource(
                GeoJsonSource(
                    "route-anim-point",
                    FeatureCollection.fromFeatures(arrayOf()),
                ),
            )
        }
        if (style.getLayer("route-anim-layer") == null) {
            val animLayer =
                CircleLayer("route-anim-layer", "route-anim-point")
                    .withProperties(
                        PropertyFactory.circleColor("#ffffff"),
                        PropertyFactory.circleStrokeColor("#ff6f00"),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleRadius(6f),
                        PropertyFactory.circleOpacity(0.95f),
                    )
            // Добавим поверх линии маршрута
            style.addLayerAbove(animLayer, "route-layer")
        }
    }

    private fun initUiFromData() {
        val points = displayablePoints
        val names = points.mapIndexed { idx, f -> nameForPoint(f, idx) }

        val startAdapter =
            object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, names) {
                override fun isEnabled(position: Int): Boolean = position != binding.endSelect.selectedItemPosition

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup,
                ): View {
                    val v = super.getDropDownView(position, convertView, parent)
                    v.isEnabled = isEnabled(position)
                    v.alpha = if (isEnabled(position)) 1f else 0.3f
                    return v
                }
            }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val endAdapter =
            object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, names) {
                override fun isEnabled(position: Int): Boolean = position != binding.startSelect.selectedItemPosition

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup,
                ): View {
                    val v = super.getDropDownView(position, convertView, parent)
                    v.isEnabled = isEnabled(position)
                    v.alpha = if (isEnabled(position)) 1f else 0.3f
                    return v
                }
            }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.startSelect.adapter = startAdapter
        binding.endSelect.adapter = endAdapter
        if (points.size >= 2) {
            binding.startSelect.setSelection(0)
            binding.endSelect.setSelection(points.size - 1)
            updateSelectedPoints(points[0], points[points.size - 1])
        }
    }

    /**
     * Автоматически строит маршрут от регистратуры до кабинета с указанным roomId
     */
    private fun buildRouteToRoom(roomId: String) {
        android.util.Log.d("MapFragment", "buildRouteToRoom: начинаем построение маршрута до кабинета $roomId")

        val points = displayablePoints
        android.util.Log.d("MapFragment", "Доступные точки: ${points.size}")

        // Найти регистратуру (room = "1" или name содержит "Регистратура")
        val registrationPoint =
            points.find { f ->
                val props = f.properties()
                props?.get("room")?.asString == "1" ||
                    props?.get("name")?.asString?.contains("Регистратура", ignoreCase = true) == true
            }

        // Найти кабинет с нужным roomId
        val targetRoom =
            points.find { f ->
                f.properties()?.get("room")?.asString == roomId
            }

        android.util.Log.d("MapFragment", "Регистратура найдена: ${registrationPoint != null}")
        android.util.Log.d("MapFragment", "Целевой кабинет найден: ${targetRoom != null}")

        if (registrationPoint == null || targetRoom == null) {
            android.util.Log.w("MapFragment", "Не удалось найти точки для маршрута")
            binding.info.text = "Не удалось найти маршрут"
            return
        }

        val targetName = targetRoom.properties()?.get("name")?.asString ?: "Кабинет $roomId"
        android.util.Log.d("MapFragment", "Строим маршрут: Регистратура -> $targetName")

        // Найти индексы для установки в спиннеры
        val startIndex = points.indexOf(registrationPoint)
        val endIndex = points.indexOf(targetRoom)

        if (startIndex >= 0 && endIndex >= 0 && startIndex != endIndex) {
            // Устанавливаем выбор в спиннерах
            binding.startSelect.setSelection(startIndex)
            binding.endSelect.setSelection(endIndex)

            // Задержка 3 секунды перед построением маршрута
            binding.root.postDelayed({
                android.util.Log.d("MapFragment", "Строим маршрут (после задержки 3 сек)")
                updateSelectedPoints(registrationPoint, targetRoom)
                computeAndRenderRoute(registrationPoint, targetRoom)
                android.util.Log.d("MapFragment", "Маршрут успешно построен")
            }, 1250)
        } else {
            android.util.Log.w("MapFragment", "Ошибка индексов: startIndex=$startIndex, endIndex=$endIndex")
        }
    }

    private fun wireUi() {
        val onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val sPos = binding.startSelect.selectedItemPosition
                    val ePos = binding.endSelect.selectedItemPosition
                    val points = displayablePoints

                    // Обновляем запрет одинаковых опций
                    (binding.startSelect.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
                    (binding.endSelect.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

                    if (sPos >= 0 && ePos >= 0 && sPos < points.size && ePos < points.size && sPos != ePos) {
                        val startFeature = points[sPos]
                        val endFeature = points[ePos]
                        updateSelectedPoints(startFeature, endFeature)
                        updateRoute(emptyList())
                        binding.info.text = ""
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No-op
                }
            }

        binding.startSelect.onItemSelectedListener = onItemSelectedListener
        binding.endSelect.onItemSelectedListener = onItemSelectedListener

        binding.routeBtn.setOnClickListener {
            val s = binding.startSelect.selectedItemPosition
            val t = binding.endSelect.selectedItemPosition
            val points = displayablePoints
            if (s < 0 || t < 0 || s >= points.size || t >= points.size || s == t) return@setOnClickListener
            computeAndRenderRoute(points[s], points[t])
        }
    }

    private fun updateSelectedPoints(
        startF: com.mapbox.geojson.Feature,
        endF: com.mapbox.geojson.Feature,
    ) {
        lastSelectedStart = startF
        lastSelectedEnd = endF
        val st = styleRef ?: return
        val src = st.getSourceAs<GeoJsonSource>("selected-points") ?: return
        val features = arrayOf(startF, endF)
        src.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun nameForPoint(
        f: com.mapbox.geojson.Feature,
        idx: Int,
    ): String {
        val p = f.properties() ?: return "Точка ${idx + 1}"
        return p.get("name").toString()
    }

    private fun computeAndRenderRoute(
        startF: Feature,
        endF: Feature,
    ) {
        val sOrig = startF.geometry() as? Point ?: return
        val eOrig = endF.geometry() as? Point ?: return

        val startPath = findNearestPathPoint(sOrig)
        val endPath = findNearestPathPoint(eOrig)

        if (startPath == null || endPath == null) {
            binding.info.text = "Не найдены точки маршрута"
            updateRoute(emptyList())
            return
        }

        val route = buildRouteWith90Degrees(startPath, endPath, sOrig, eOrig)

        if (route.size < 2) {
            binding.info.text = "Маршрут не найден"
            updateRoute(emptyList())
        } else {
            updateRoute(route)
            binding.info.text = "Маршрут построен (${route.size} точек)"
        }
    }

    private fun findNearestPathPoint(point: Point): Point? {
        var nearest: Point? = null
        var minDist = Double.POSITIVE_INFINITY

        pathFeatures.forEach { feature ->
            val pathPoint = feature.geometry() as? Point ?: return@forEach
            val dist = distance(point, pathPoint)
            if (dist < minDist) {
                minDist = dist
                nearest = pathPoint
            }
        }

        return nearest
    }

    private fun distance(
        a: Point,
        b: Point,
    ): Double {
        val dx = a.longitude() - b.longitude()
        val dy = a.latitude() - b.latitude()
        return dx * dx + dy * dy
    }

    private fun buildRouteWith90Degrees(
        start: Point,
        end: Point,
        startOrig: Point,
        endOrig: Point,
    ): List<Point> {
        val route = mutableListOf<Point>()

        // Стартовая точка пользователя
        route.add(startOrig)

        // Ближайшая path к старту
        route.add(start)

        // Собираем промежуточные path-точки в прямоугольнике между start и end
        val minLon = minOf(start.longitude(), end.longitude())
        val maxLon = maxOf(start.longitude(), end.longitude())
        val minLat = minOf(start.latitude(), end.latitude())
        val maxLat = maxOf(start.latitude(), end.latitude())
        val middlePath =
            pathFeatures.mapNotNull { it.geometry() as? Point }
                .filter { p ->
                    p.longitude() >= minLon && p.longitude() <= maxLon &&
                        p.latitude() >= minLat && p.latitude() <= maxLat &&
                        !(p.longitude() == start.longitude() && p.latitude() == start.latitude()) &&
                        !(p.longitude() == end.longitude() && p.latitude() == end.latitude())
                }
                .sortedWith(
                    if (kotlin.math.abs(end.longitude() - start.longitude()) >= kotlin.math.abs(end.latitude() - start.latitude())) {
                        compareBy { it.longitude() * if (end.longitude() >= start.longitude()) 1 else -1 }
                    } else {
                        compareBy { it.latitude() * if (end.latitude() >= start.latitude()) 1 else -1 }
                    },
                )

        route.addAll(middlePath)

        // Конечная path-точка и конечная точка пользователя
        route.add(end)
        route.add(endOrig)

        return route
    }

    private fun updateRoute(points: List<Point>) {
        val st = styleRef ?: return
        val src = st.getSourceAs<GeoJsonSource>("route") ?: return
        if (points.isEmpty()) {
            src.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
            stopRouteAnimation()
        } else {
            src.setGeoJson(LineString.fromLngLats(points))
            startRouteAnimation(points)
        }
    }

    private fun startRouteAnimation(points: List<Point>) {
        stopRouteAnimation()
        if (points.size < 2) return
        val st = styleRef ?: return
        val animSrc = st.getSourceAs<GeoJsonSource>("route-anim-point") ?: return

        val distances = ArrayList<Double>(points.size)
        distances.add(0.0)
        var sum = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val dx = b.longitude() - a.longitude()
            val dy = b.latitude() - a.latitude()
            val d = kotlin.math.sqrt(dx * dx + dy * dy)
            sum += d
            distances.add(sum)
        }
        if (sum == 0.0) return

        routeAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 3500
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { va ->
                    val t = (va.animatedValue as Float).toDouble() * sum
                    var seg = 1
                    while (seg < distances.size && distances[seg] < t) seg++
                    val i1 = (seg - 1).coerceAtLeast(0)
                    val i2 = seg.coerceAtMost(points.size - 1)
                    val a = points[i1]
                    val b = points[i2]
                    val dt = distances[i2] - distances[i1]
                    val local = if (dt <= 0.0) 0.0 else (t - distances[i1]) / dt
                    val lon = a.longitude() + (b.longitude() - a.longitude()) * local
                    val lat = a.latitude() + (b.latitude() - a.latitude()) * local
                    animSrc.setGeoJson(Point.fromLngLat(lon, lat))
                }
                start()
            }
    }

    private fun stopRouteAnimation() {
        routeAnimator?.cancel()
        routeAnimator = null
        val st = styleRef ?: return
        st.getSourceAs<GeoJsonSource>("route-anim-point")
            ?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
    }

    private fun computeBounds(fc: FeatureCollection): LatLngBounds? {
        val latitudes = mutableListOf<Double>()
        val longitudes = mutableListOf<Double>()

        fun addPoint(p: Point) {
            longitudes.add(p.longitude())
            latitudes.add(p.latitude())
        }

        fun addGeometry(geom: com.mapbox.geojson.Geometry?) {
            when (geom) {
                is Point -> addPoint(geom)
                is Polygon -> {
                    geom.coordinates().forEach { ring ->
                        ring.forEach { coord -> addPoint(coord) }
                    }
                }

                else -> {}
            }
        }

        fc.features()?.forEach { feat -> addGeometry(feat.geometry()) }

        if (latitudes.isEmpty() || longitudes.isEmpty()) return null

        val minLat = latitudes.minOrNull() ?: return null
        val maxLat = latitudes.maxOrNull() ?: return null
        val minLon = longitudes.minOrNull() ?: return null
        val maxLon = longitudes.maxOrNull() ?: return null

        return LatLngBounds.Builder()
            .include(LatLng(minLat, minLon))
            .include(LatLng(maxLat, maxLon))
            .build()
    }

    // Вспомогательные функции для подписей полигонов
    private fun polygonCentroid(poly: Polygon): Point {
        val ring = poly.coordinates()[0]
        var sx = 0.0
        var sy = 0.0
        val n = ring.size
        for (p in ring) {
            sx += p.longitude()
            sy += p.latitude()
        }
        return Point.fromLngLat(sx / n, sy / n)
    }

    private fun polygonAngle(poly: Polygon): Double {
        val ring = poly.coordinates()[0]
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (p in ring) {
            if (p.longitude() < minX) minX = p.longitude()
            if (p.latitude() < minY) minY = p.latitude()
            if (p.longitude() > maxX) maxX = p.longitude()
            if (p.latitude() > maxY) maxY = p.latitude()
        }
        val dx = maxX - minX
        val dy = maxY - minY
        val angleRad = kotlin.math.atan2(dy, dx)
        return Math.toDegrees(angleRad)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }
}
