package com.example.store

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.canvas.GridSettings
import com.example.canvas.ViewportState
import com.example.measurement.*
import com.example.parser.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*

enum class CadTool {
    PAN_ZOOM,
    SELECT,
    MARQUEE_SELECT,
    MEASURE,
    MOVE,
    ROTATE,
    SCALE,
    COPY,
    TRIM_EXTEND,
    MIRROR
}

enum class CadModifyDialogType {
    NONE,
    MOVE,
    ROTATE,
    SCALE,
    COPY,
    TRIM,
    MIRROR
}

data class MeasurementInfo(
    val p1: Point2D?,
    val p2: Point2D?,
    val distance: Float = 0f,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val angleDeg: Float = 0f
)

data class MarqueeSelection(
    val startScreen: Offset,
    val currentScreen: Offset
)

data class CadUiState(
    val document: DxfDocument,
    val viewport: ViewportState = ViewportState(),
    val activeTool: CadTool = CadTool.PAN_ZOOM,
    val activeModifyDialog: CadModifyDialogType = CadModifyDialogType.NONE,
    val selectedEntities: List<DxfEntity> = emptyList(),
    val activeMarquee: MarqueeSelection? = null,
    val layerVisibility: Map<String, Boolean> = emptyMap(),
    val measurement: MeasurementInfo = MeasurementInfo(null, null),
    val activeMeasurement: MeasurementActiveState = MeasurementActiveState(),
    val savedMeasurements: List<MeasurementRecord> = emptyList(),
    val snapSettings: SnapSettings = SnapSettings(),
    val gridSettings: GridSettings = GridSettings(),
    val unitConfig: UnitConfig = UnitConfig(),
    val isMeasurementManagerVisible: Boolean = false,
    val isSnapSettingsVisible: Boolean = false,
    val isUnitSettingsVisible: Boolean = false,
    val isGridSettingsVisible: Boolean = false,
    val cursorCadCoords: Point2D = Point2D(0f, 0f),
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val isBlueprintTheme: Boolean = true,
    val isLayerSheetVisible: Boolean = false,
    val isSamplePickerVisible: Boolean = false,
    val canUndo: Boolean = false,
    val toastMessage: String? = null
) {
    val selectedEntity: DxfEntity? get() = selectedEntities.firstOrNull()
}

class CadViewModel : ViewModel() {

    private val defaultDoc = SampleDxfProvider.getSamples().first().document
    private val historyStack = mutableListOf<DxfDocument>()

    private val _uiState = MutableStateFlow(
        CadUiState(
            document = defaultDoc,
            layerVisibility = defaultDoc.layers.mapValues { it.value.isVisible }
        )
    )
    val uiState: StateFlow<CadUiState> = _uiState.asStateFlow()

    private fun pushHistory() {
        if (historyStack.size > 20) {
            historyStack.removeAt(0)
        }
        historyStack.add(_uiState.value.document)
    }

    fun undo() {
        if (historyStack.isNotEmpty()) {
            val previous = historyStack.removeAt(historyStack.lastIndex)
            _uiState.update { current ->
                current.copy(
                    document = previous,
                    selectedEntities = emptyList(),
                    canUndo = historyStack.isNotEmpty(),
                    toastMessage = "Undone last modification"
                )
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun loadDocument(doc: DxfDocument) {
        historyStack.clear()
        _uiState.update { current ->
            current.copy(
                document = doc,
                layerVisibility = doc.layers.mapValues { it.value.isVisible },
                selectedEntities = emptyList(),
                activeMarquee = null,
                measurement = MeasurementInfo(null, null),
                activeMeasurement = MeasurementActiveState(),
                canUndo = false
            )
        }
        fitToExtents()
    }

    fun onCanvasSizeChanged(width: Float, height: Float) {
        _uiState.update { current ->
            val updatedViewport = current.viewport.copy(canvasWidth = width, canvasHeight = height)
            if (current.viewport.scale == 1.0f && current.viewport.offsetX == 0f) {
                current.copy(viewport = updatedViewport.fitToBounds(current.document.extents, width, height))
            } else {
                current.copy(viewport = updatedViewport)
            }
        }
    }

    fun fitToExtents() {
        _uiState.update { current ->
            val vp = current.viewport
            val newVp = vp.fitToBounds(current.document.extents, vp.canvasWidth, vp.canvasHeight)
            current.copy(viewport = newVp)
        }
    }

    fun zoomIn() {
        _uiState.update { current ->
            val center = Offset(current.viewport.canvasWidth / 2f, current.viewport.canvasHeight / 2f)
            current.copy(viewport = current.viewport.zoom(1.25f, center))
        }
    }

    fun zoomOut() {
        _uiState.update { current ->
            val center = Offset(current.viewport.canvasWidth / 2f, current.viewport.canvasHeight / 2f)
            current.copy(viewport = current.viewport.zoom(0.8f, center))
        }
    }

    fun onTransformGesture(panX: Float, panY: Float, zoomFactor: Float, centroid: Offset) {
        _uiState.update { current ->
            var vp = current.viewport
            if (zoomFactor != 1.0f) {
                vp = vp.zoom(zoomFactor, centroid)
            }
            if (panX != 0f || panY != 0f) {
                vp = vp.pan(panX, panY)
            }
            current.copy(viewport = vp)
        }
    }

    fun setTool(tool: CadTool) {
        _uiState.update { current ->
            val modifyDialog = when (tool) {
                CadTool.MOVE -> CadModifyDialogType.MOVE
                CadTool.ROTATE -> CadModifyDialogType.ROTATE
                CadTool.SCALE -> CadModifyDialogType.SCALE
                CadTool.COPY -> CadModifyDialogType.COPY
                CadTool.TRIM_EXTEND -> CadModifyDialogType.TRIM
                CadTool.MIRROR -> CadModifyDialogType.MIRROR
                else -> CadModifyDialogType.NONE
            }

            current.copy(
                activeTool = tool,
                activeModifyDialog = modifyDialog,
                activeMarquee = null,
                selectedEntities = if (tool == CadTool.PAN_ZOOM || tool == CadTool.MEASURE) emptyList() else current.selectedEntities,
                measurement = if (tool != CadTool.MEASURE) MeasurementInfo(null, null) else current.measurement,
                activeMeasurement = if (tool != CadTool.MEASURE) current.activeMeasurement.copy(isFinished = false) else current.activeMeasurement
            )
        }
    }

    fun openModifyDialog(type: CadModifyDialogType) {
        _uiState.update { current ->
            current.copy(
                activeModifyDialog = type,
                activeTool = when (type) {
                    CadModifyDialogType.MOVE -> CadTool.MOVE
                    CadModifyDialogType.ROTATE -> CadTool.ROTATE
                    CadModifyDialogType.SCALE -> CadTool.SCALE
                    CadModifyDialogType.COPY -> CadTool.COPY
                    CadModifyDialogType.TRIM -> CadTool.TRIM_EXTEND
                    CadModifyDialogType.MIRROR -> CadTool.MIRROR
                    CadModifyDialogType.NONE -> current.activeTool
                }
            )
        }
    }

    fun closeModifyDialog() {
        _uiState.update { it.copy(activeModifyDialog = CadModifyDialogType.NONE) }
    }

    fun setLayerVisibility(layer: String, isVisible: Boolean) {
        _uiState.update { current ->
            val updated = current.layerVisibility.toMutableMap()
            updated[layer] = isVisible
            current.copy(layerVisibility = updated)
        }
    }

    fun toggleAllLayers(visible: Boolean) {
        _uiState.update { current ->
            val updated = current.document.layers.keys.associateWith { visible }
            current.copy(layerVisibility = updated)
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isBlueprintTheme = !it.isBlueprintTheme) }
    }

    fun toggleGrid() {
        _uiState.update { current ->
            val newVisibility = !current.gridSettings.isVisible
            current.copy(
                showGrid = newVisibility,
                gridSettings = current.gridSettings.copy(isVisible = newVisibility),
                toastMessage = if (newVisibility) "Grid Enabled" else "Grid Disabled"
            )
        }
    }

    fun setGridSettings(settings: GridSettings) {
        _uiState.update { current ->
            current.copy(
                gridSettings = settings,
                showGrid = settings.isVisible,
                toastMessage = "Grid settings updated"
            )
        }
    }

    fun showGridSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(isGridSettingsVisible = show) }
    }

    fun toggleGridSnap() {
        _uiState.update { current ->
            val nextSnap = !current.gridSettings.isSnapToGrid
            val updated = current.gridSettings.copy(isSnapToGrid = nextSnap)
            current.copy(
                gridSettings = updated,
                toastMessage = if (nextSnap) "Grid Snap Enabled (${updated.snapFrequency.shortLabel})" else "Grid Snap Disabled"
            )
        }
    }

    fun toggleAxes() {
        _uiState.update { it.copy(showAxes = !it.showAxes) }
    }

    fun showLayerSheet(show: Boolean) {
        _uiState.update { it.copy(isLayerSheetVisible = show) }
    }

    fun showSamplePicker(show: Boolean) {
        _uiState.update { it.copy(isSamplePickerVisible = show) }
    }

    // ==========================================
    // MEASUREMENT ENGINE & OSNAP CONTROLS
    // ==========================================

    fun setMeasurementTool(toolType: MeasureToolType) {
        _uiState.update { current ->
            current.copy(
                activeTool = CadTool.MEASURE,
                activeMeasurement = MeasurementActiveState(
                    toolType = toolType,
                    pickedPoints = emptyList(),
                    isFinished = false
                ),
                measurement = MeasurementInfo(null, null)
            )
        }
    }

    fun setSnapSettings(settings: SnapSettings) {
        _uiState.update { it.copy(snapSettings = settings) }
    }

    fun toggleSnapEnabled() {
        _uiState.update { current ->
            val updated = current.snapSettings.copy(isEnabled = !current.snapSettings.isEnabled)
            current.copy(
                snapSettings = updated,
                toastMessage = if (updated.isEnabled) "OSNAP Snapping Enabled" else "OSNAP Snapping Disabled"
            )
        }
    }

    fun cycleOrthoMode() {
        _uiState.update { current ->
            val nextOrtho = when (current.snapSettings.orthoMode) {
                OrthoMode.OFF -> OrthoMode.HORIZONTAL
                OrthoMode.HORIZONTAL -> OrthoMode.VERTICAL
                OrthoMode.VERTICAL -> OrthoMode.AUTO_ORTHO
                OrthoMode.AUTO_ORTHO -> OrthoMode.OFF
            }
            val updated = current.snapSettings.copy(orthoMode = nextOrtho)
            current.copy(
                snapSettings = updated,
                toastMessage = updated.orthoMode.displayName
            )
        }
    }

    fun setUnitConfig(config: UnitConfig) {
        _uiState.update { current ->
            // Recalculate saved measurements with updated unit system
            val updatedSaved = current.savedMeasurements.map { record ->
                val newFormatted = when (record.toolType) {
                    MeasureToolType.AREA_POLYGON,
                    MeasureToolType.AREA_RECTANGLE,
                    MeasureToolType.AREA_BOUNDARY,
                    MeasureToolType.TOTAL_AREA -> UnitManager.formatArea(record.primaryValue.toFloat(), config)

                    MeasureToolType.DISTANCE,
                    MeasureToolType.HORIZONTAL,
                    MeasureToolType.VERTICAL,
                    MeasureToolType.CONTINUOUS,
                    MeasureToolType.RADIUS,
                    MeasureToolType.DIAMETER,
                    MeasureToolType.ARC,
                    MeasureToolType.COORD_DIFFERENCE,
                    MeasureToolType.ELEVATION -> UnitManager.formatDistance(record.primaryValue.toFloat(), config)

                    MeasureToolType.ANGLE -> UnitManager.formatAngle(record.primaryValue.toFloat(), config)
                    else -> record.primaryFormatted
                }
                record.copy(
                    primaryFormatted = newFormatted,
                    title = "${record.toolType.title} ($newFormatted)"
                )
            }

            val updatedState = current.copy(
                unitConfig = config,
                savedMeasurements = updatedSaved,
                toastMessage = "Units configured: ${config.unitSystem.shortName} (${config.displayDistanceUnit.symbol} / ${config.displayAreaUnit.symbol})"
            )

            // Recalculate active measurement with new units
            if (updatedState.activeMeasurement.pickedPoints.isNotEmpty()) {
                val recalculated = recalculateMeasurement(updatedState.activeMeasurement, updatedState.unitConfig, updatedState.document)
                updatedState.copy(activeMeasurement = recalculated)
            } else {
                updatedState
            }
        }
    }

    fun showSnapSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(isSnapSettingsVisible = show) }
    }

    fun showUnitSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(isUnitSettingsVisible = show) }
    }

    fun showMeasurementManager(show: Boolean) {
        _uiState.update { it.copy(isMeasurementManagerVisible = show) }
    }

    fun onPointerHover(screenOffset: Offset) {
        val state = _uiState.value
        val worldPoint = state.viewport.screenToWorld(screenOffset)

        val refPoint = state.activeMeasurement.pickedPoints.lastOrNull()
        val snapResult = SnapEngine.findSnap(
            cursorWorld = worldPoint,
            entities = state.document.entities,
            layerVisibility = state.layerVisibility,
            viewport = state.viewport,
            settings = state.snapSettings,
            referencePoint = refPoint,
            gridSettings = state.gridSettings
        )

        val targetPoint = snapResult?.point ?: worldPoint

        _uiState.update { current ->
            current.copy(
                cursorCadCoords = targetPoint,
                activeMeasurement = current.activeMeasurement.copy(
                    livePreviewPoint = targetPoint,
                    activeSnap = snapResult
                )
            )
        }
    }

    fun onCanvasTap(screenOffset: Offset) {
        val state = _uiState.value
        val rawWorld = state.viewport.screenToWorld(screenOffset)

        if (state.activeTool == CadTool.MEASURE) {
            val refPoint = state.activeMeasurement.pickedPoints.lastOrNull()
            val snapResult = SnapEngine.findSnap(
                cursorWorld = rawWorld,
                entities = state.document.entities,
                layerVisibility = state.layerVisibility,
                viewport = state.viewport,
                settings = state.snapSettings,
                referencePoint = refPoint,
                gridSettings = state.gridSettings
            )
            val effectivePoint = snapResult?.point ?: rawWorld
            _uiState.update { it.copy(cursorCadCoords = effectivePoint) }
            handleMeasurementTap(effectivePoint, rawWorld)
        } else {
            _uiState.update { it.copy(cursorCadCoords = rawWorld) }
            when (state.activeTool) {
                CadTool.SELECT, CadTool.MARQUEE_SELECT,
                CadTool.MOVE, CadTool.ROTATE, CadTool.SCALE,
                CadTool.COPY, CadTool.TRIM_EXTEND, CadTool.MIRROR -> selectEntityNear(rawWorld)
                CadTool.PAN_ZOOM -> selectEntityNear(rawWorld)
                else -> {}
            }
        }
    }

    private fun handleMeasurementTap(worldPoint: Point2D, rawTapPoint: Point2D) {
        val state = _uiState.value
        val active = state.activeMeasurement
        val config = state.unitConfig
        val tool = active.toolType

        when (tool) {
            MeasureToolType.RADIUS,
            MeasureToolType.DIAMETER,
            MeasureToolType.CIRCLE -> {
                // Find circle or arc near tap point
                val tolerance = 24f / state.viewport.scale
                val entity = state.document.entities.filter { state.layerVisibility[it.layer] != false }
                    .filter { it is DxfCircle || it is DxfArc }
                    .minByOrNull { e ->
                        when (e) {
                            is DxfCircle -> abs(e.center.distanceTo(rawTapPoint) - e.radius)
                            is DxfArc -> abs(e.center.distanceTo(rawTapPoint) - e.radius)
                            else -> Float.MAX_VALUE
                        }
                    }

                if (entity != null) {
                    when (entity) {
                        is DxfCircle -> {
                            val res = MeasurementEngine.computeCircle(entity.center, entity.radius, config)
                            _uiState.update { current ->
                                current.copy(
                                    activeMeasurement = active.copy(
                                        pickedPoints = listOf(entity.center),
                                        selectedEntityForMeasure = entity,
                                        primaryValueDisplay = res.primaryFormatted,
                                        subValueDisplay = res.subValueFormatted,
                                        dynamicDetails = res.details,
                                        isFinished = true
                                    )
                                )
                            }
                        }
                        is DxfArc -> {
                            val res = MeasurementEngine.computeArc(entity.center, entity.radius, entity.startAngleDeg, entity.endAngleDeg, config)
                            _uiState.update { current ->
                                current.copy(
                                    activeMeasurement = active.copy(
                                        pickedPoints = listOf(entity.center),
                                        selectedEntityForMeasure = entity,
                                        primaryValueDisplay = res.primaryFormatted,
                                        subValueDisplay = res.subValueFormatted,
                                        dynamicDetails = res.details,
                                        isFinished = true
                                    )
                                )
                            }
                        }
                        else -> {}
                    }
                } else {
                    _uiState.update { it.copy(toastMessage = "No Circle or Arc entity near tap") }
                }
            }

            MeasureToolType.ARC -> {
                val tolerance = 24f / state.viewport.scale
                val entity = state.document.entities.filter { state.layerVisibility[it.layer] != false }
                    .filterIsInstance<DxfArc>()
                    .minByOrNull { e -> abs(e.center.distanceTo(rawTapPoint) - e.radius) }

                if (entity != null) {
                    val res = MeasurementEngine.computeArc(entity.center, entity.radius, entity.startAngleDeg, entity.endAngleDeg, config)
                    _uiState.update { current ->
                        current.copy(
                            activeMeasurement = active.copy(
                                pickedPoints = listOf(entity.center),
                                selectedEntityForMeasure = entity,
                                primaryValueDisplay = res.primaryFormatted,
                                subValueDisplay = res.subValueFormatted,
                                dynamicDetails = res.details,
                                isFinished = true
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(toastMessage = "No Arc entity near tap") }
                }
            }

            MeasureToolType.AREA_BOUNDARY -> {
                val boundary = BoundaryDetector.detectBoundary(worldPoint, state.document.entities.filter { state.layerVisibility[it.layer] != false })
                if (boundary != null) {
                    val res = MeasurementEngine.computePolygonArea(boundary.polygonVertices, config)
                    _uiState.update { current ->
                        current.copy(
                            activeMeasurement = active.copy(
                                pickedPoints = boundary.polygonVertices,
                                primaryValueDisplay = res.primaryFormatted,
                                subValueDisplay = "${boundary.sourceDescription} • ${res.subValueFormatted}",
                                dynamicDetails = res.details,
                                isFinished = true
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(toastMessage = "Could not find closed boundary around tap") }
                }
            }

            MeasureToolType.COORDINATE -> {
                val res = MeasurementEngine.computeCoordinate(worldPoint, z = 0f, config = config)
                _uiState.update { current ->
                    current.copy(
                        activeMeasurement = active.copy(
                            pickedPoints = listOf(worldPoint),
                            primaryValueDisplay = res.primaryFormatted,
                            subValueDisplay = res.subValueFormatted,
                            dynamicDetails = res.details,
                            isFinished = true
                        )
                    )
                }
            }

            MeasureToolType.DISTANCE,
            MeasureToolType.HORIZONTAL,
            MeasureToolType.VERTICAL,
            MeasureToolType.COORD_DIFFERENCE,
            MeasureToolType.ELEVATION,
            MeasureToolType.AREA_RECTANGLE -> {
                val pts = active.pickedPoints
                if (pts.isEmpty() || active.isFinished) {
                    _uiState.update { current ->
                        current.copy(
                            activeMeasurement = active.copy(
                                pickedPoints = listOf(worldPoint),
                                isFinished = false,
                                primaryValueDisplay = "",
                                subValueDisplay = "Point 1 placed at (%.2f, %.2f)".format(worldPoint.x, worldPoint.y),
                                dynamicDetails = emptyMap()
                            ),
                            measurement = MeasurementInfo(p1 = worldPoint, p2 = null)
                        )
                    }
                } else {
                    val p1 = pts.first()
                    val p2 = worldPoint
                    val res = when (tool) {
                        MeasureToolType.DISTANCE -> MeasurementEngine.computeDistance(p1, p2, config)
                        MeasureToolType.HORIZONTAL -> MeasurementEngine.computeHorizontal(p1, p2, config)
                        MeasureToolType.VERTICAL -> MeasurementEngine.computeVertical(p1, p2, config)
                        MeasureToolType.COORD_DIFFERENCE -> MeasurementEngine.computeCoordinateDifference(p1, p2, config = config)
                        MeasureToolType.ELEVATION -> MeasurementEngine.computeCoordinateDifference(p1, p2, config = config)
                        MeasureToolType.AREA_RECTANGLE -> MeasurementEngine.computeRectangleArea(p1, p2, config)
                        else -> MeasurementEngine.computeDistance(p1, p2, config)
                    }

                    _uiState.update { current ->
                        current.copy(
                            activeMeasurement = active.copy(
                                pickedPoints = listOf(p1, p2),
                                primaryValueDisplay = res.primaryFormatted,
                                subValueDisplay = res.subValueFormatted,
                                dynamicDetails = res.details,
                                isFinished = true
                            ),
                            measurement = MeasurementInfo(
                                p1 = p1,
                                p2 = p2,
                                distance = p1.distanceTo(p2),
                                deltaX = abs(p2.x - p1.x),
                                deltaY = abs(p2.y - p1.y),
                                angleDeg = Math.toDegrees(atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())).toFloat()
                            )
                        )
                    }
                }
            }

            MeasureToolType.ANGLE -> {
                val pts = active.pickedPoints
                if (pts.size >= 3 || active.isFinished) {
                    _uiState.update { current ->
                        current.copy(
                            activeMeasurement = active.copy(
                                pickedPoints = listOf(worldPoint),
                                isFinished = false,
                                primaryValueDisplay = "",
                                subValueDisplay = "Point 1 set. Tap Vertex next",
                                dynamicDetails = emptyMap()
                            )
                        )
                    }
                } else {
                    val newPts = pts + worldPoint
                    if (newPts.size == 3) {
                        val res = MeasurementEngine.computeAngle(newPts[0], newPts[1], newPts[2], config)
                        _uiState.update { current ->
                            current.copy(
                                activeMeasurement = active.copy(
                                    pickedPoints = newPts,
                                    primaryValueDisplay = res.primaryFormatted,
                                    subValueDisplay = res.subValueFormatted,
                                    dynamicDetails = res.details,
                                    isFinished = true
                                )
                            )
                        }
                    } else {
                        _uiState.update { current ->
                            current.copy(
                                activeMeasurement = active.copy(
                                    pickedPoints = newPts,
                                    subValueDisplay = "Vertex set. Tap Point 3",
                                    isFinished = false
                                )
                            )
                        }
                    }
                }
            }

            MeasureToolType.CONTINUOUS -> {
                val newPts = (if (active.isFinished) emptyList() else active.pickedPoints) + worldPoint
                val res = MeasurementEngine.computeContinuous(newPts, config)
                _uiState.update { current ->
                    current.copy(
                        activeMeasurement = active.copy(
                            pickedPoints = newPts,
                            primaryValueDisplay = res.primaryFormatted,
                            subValueDisplay = res.subValueFormatted,
                            dynamicDetails = res.details,
                            isFinished = false
                        )
                    )
                }
            }

            MeasureToolType.AREA_POLYGON,
            MeasureToolType.TOTAL_AREA -> {
                val newPts = (if (active.isFinished) emptyList() else active.pickedPoints) + worldPoint
                val res = MeasurementEngine.computePolygonArea(newPts, config)
                _uiState.update { current ->
                    current.copy(
                        activeMeasurement = active.copy(
                            pickedPoints = newPts,
                            primaryValueDisplay = res.primaryFormatted,
                            subValueDisplay = res.subValueFormatted,
                            dynamicDetails = res.details,
                            isFinished = false
                        )
                    )
                }
            }
        }
    }

    private fun recalculateMeasurement(
        active: MeasurementActiveState,
        config: UnitConfig,
        doc: DxfDocument
    ): MeasurementActiveState {
        if (active.pickedPoints.isEmpty()) return active
        val pts = active.pickedPoints
        val res = when (active.toolType) {
            MeasureToolType.DISTANCE -> if (pts.size >= 2) MeasurementEngine.computeDistance(pts[0], pts[1], config) else null
            MeasureToolType.HORIZONTAL -> if (pts.size >= 2) MeasurementEngine.computeHorizontal(pts[0], pts[1], config) else null
            MeasureToolType.VERTICAL -> if (pts.size >= 2) MeasurementEngine.computeVertical(pts[0], pts[1], config) else null
            MeasureToolType.CONTINUOUS -> MeasurementEngine.computeContinuous(pts, config)
            MeasureToolType.ANGLE -> if (pts.size >= 3) MeasurementEngine.computeAngle(pts[0], pts[1], pts[2], config) else null
            MeasureToolType.AREA_POLYGON, MeasureToolType.TOTAL_AREA -> MeasurementEngine.computePolygonArea(pts, config)
            MeasureToolType.AREA_RECTANGLE -> if (pts.size >= 2) MeasurementEngine.computeRectangleArea(pts[0], pts[1], config) else null
            MeasureToolType.COORDINATE -> MeasurementEngine.computeCoordinate(pts[0], 0f, config)
            MeasureToolType.COORD_DIFFERENCE, MeasureToolType.ELEVATION -> if (pts.size >= 2) MeasurementEngine.computeCoordinateDifference(pts[0], pts[1], config = config) else null
            MeasureToolType.RADIUS, MeasureToolType.DIAMETER, MeasureToolType.CIRCLE -> {
                val circle = active.selectedEntityForMeasure as? DxfCircle
                if (circle != null) MeasurementEngine.computeCircle(circle.center, circle.radius, config) else null
            }
            MeasureToolType.ARC -> {
                val arc = active.selectedEntityForMeasure as? DxfArc
                if (arc != null) MeasurementEngine.computeArc(arc.center, arc.radius, arc.startAngleDeg, arc.endAngleDeg, config) else null
            }
            MeasureToolType.AREA_BOUNDARY -> MeasurementEngine.computePolygonArea(pts, config)
        }

        return if (res != null) {
            active.copy(
                primaryValueDisplay = res.primaryFormatted,
                subValueDisplay = res.subValueFormatted,
                dynamicDetails = res.details
            )
        } else active
    }

    fun undoMeasurementPoint() {
        _uiState.update { current ->
            val active = current.activeMeasurement
            if (active.pickedPoints.size > 1) {
                val updatedPts = active.pickedPoints.dropLast(1)
                val updatedActive = active.copy(pickedPoints = updatedPts, isFinished = false)
                val recalculated = recalculateMeasurement(updatedActive, current.unitConfig, current.document)
                current.copy(activeMeasurement = recalculated)
            } else {
                current.copy(activeMeasurement = active.copy(pickedPoints = emptyList(), primaryValueDisplay = "", isFinished = false))
            }
        }
    }

    fun finishMeasurement() {
        _uiState.update { current ->
            val active = current.activeMeasurement
            if (active.pickedPoints.isNotEmpty()) {
                val recalculated = recalculateMeasurement(active.copy(isFinished = true), current.unitConfig, current.document)
                current.copy(activeMeasurement = recalculated, toastMessage = "Measurement Completed")
            } else {
                current
            }
        }
    }

    fun clearMeasurementPoints() {
        _uiState.update { current ->
            current.copy(
                measurement = MeasurementInfo(null, null),
                activeMeasurement = MeasurementActiveState(
                    toolType = current.activeMeasurement.toolType
                )
            )
        }
    }

    fun saveActiveMeasurement() {
        val state = _uiState.value
        val active = state.activeMeasurement
        if (active.primaryValueDisplay.isBlank() && active.pickedPoints.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "No measurement to save") }
            return
        }

        val primaryVal = when (active.toolType) {
            MeasureToolType.AREA_POLYGON, MeasureToolType.AREA_RECTANGLE, MeasureToolType.AREA_BOUNDARY -> {
                BoundaryDetector.computePolygonArea(active.pickedPoints).toDouble()
            }
            MeasureToolType.DISTANCE, MeasureToolType.HORIZONTAL, MeasureToolType.VERTICAL -> {
                if (active.pickedPoints.size >= 2) active.pickedPoints[0].distanceTo(active.pickedPoints[1]).toDouble() else 0.0
            }
            else -> 0.0
        }

        val record = MeasurementRecord(
            title = "${active.toolType.title} (${active.primaryValueDisplay})",
            toolType = active.toolType,
            points = active.pickedPoints,
            primaryValue = primaryVal,
            primaryFormatted = active.primaryValueDisplay,
            secondaryDetails = active.dynamicDetails,
            labelPosition = active.pickedPoints.firstOrNull() ?: Point2D(0f, 0f)
        )

        _uiState.update { current ->
            current.copy(
                savedMeasurements = current.savedMeasurements + record,
                toastMessage = "Saved ${record.id} to measurements list"
            )
        }
    }

    fun toggleMeasurementVisibility(id: String) {
        _uiState.update { current ->
            val updated = current.savedMeasurements.map {
                if (it.id == id) it.copy(isVisible = !it.isVisible) else it
            }
            current.copy(savedMeasurements = updated)
        }
    }

    fun toggleHoleSubtraction(id: String) {
        _uiState.update { current ->
            val updated = current.savedMeasurements.map {
                if (it.id == id) it.copy(isHoleSubtraction = !it.isHoleSubtraction) else it
            }
            current.copy(savedMeasurements = updated)
        }
    }

    fun deleteMeasurement(id: String) {
        _uiState.update { current ->
            current.copy(
                savedMeasurements = current.savedMeasurements.filter { it.id != id },
                toastMessage = "Deleted measurement $id"
            )
        }
    }

    fun clearAllMeasurements() {
        _uiState.update { current ->
            current.copy(
                savedMeasurements = emptyList(),
                activeMeasurement = MeasurementActiveState(),
                measurement = MeasurementInfo(null, null),
                toastMessage = "All saved measurements cleared"
            )
        }
    }

    fun zoomToMeasurement(record: MeasurementRecord) {
        if (record.points.isEmpty()) return
        val box = BoundingBox.fromPoints(record.points)
        _uiState.update { current ->
            val vp = current.viewport
            val newVp = vp.fitToBounds(box, vp.canvasWidth, vp.canvasHeight)
            current.copy(viewport = newVp, isMeasurementManagerVisible = false)
        }
    }

    // ==========================================
    // SELECTION & MARQUEE
    // ==========================================

    fun onMarqueeDragUpdate(startScreen: Offset, currentScreen: Offset) {
        _uiState.update {
            it.copy(activeMarquee = MarqueeSelection(startScreen, currentScreen))
        }
    }

    fun onMarqueeDragEnd(startScreen: Offset, endScreen: Offset) {
        val vp = _uiState.value.viewport
        val p1 = vp.screenToWorld(startScreen)
        val p2 = vp.screenToWorld(endScreen)

        val minX = min(p1.x, p2.x)
        val maxX = max(p1.x, p2.x)
        val minY = min(p1.y, p2.y)
        val maxY = max(p1.y, p2.y)

        if (abs(maxX - minX) < (10f / vp.scale) && abs(maxY - minY) < (10f / vp.scale)) {
            selectEntityNear(p1)
            _uiState.update { it.copy(activeMarquee = null) }
            return
        }

        val marqueeBox = BoundingBox(minX, minY, maxX, maxY)
        val isWindow = endScreen.x >= startScreen.x

        val selected = _uiState.value.document.entities.filter { entity ->
            if (_uiState.value.layerVisibility[entity.layer] == false) return@filter false
            val box = entity.getBoundingBox()
            if (isWindow) {
                box.minX >= marqueeBox.minX && box.maxX <= marqueeBox.maxX &&
                        box.minY >= marqueeBox.minY && box.maxY <= marqueeBox.maxY
            } else {
                box.minX <= marqueeBox.maxX && box.maxX >= marqueeBox.minX &&
                        box.minY <= marqueeBox.maxY && box.maxY >= marqueeBox.minY
            }
        }

        _uiState.update {
            it.copy(
                selectedEntities = selected,
                activeMarquee = null,
                toastMessage = if (selected.isNotEmpty()) "Selected ${selected.size} entities" else null
            )
        }
    }

    fun onMarqueeCancel() {
        _uiState.update { it.copy(activeMarquee = null) }
    }

    fun selectAll() {
        val allVisible = _uiState.value.document.entities.filter {
            _uiState.value.layerVisibility[it.layer] != false
        }
        _uiState.update {
            it.copy(selectedEntities = allVisible, toastMessage = "Selected all ${allVisible.size} entities")
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedEntities = emptyList(), activeMarquee = null)
        }
    }

    private fun selectEntityNear(worldPoint: Point2D) {
        val tolerance = 20f / _uiState.value.viewport.scale
        val candidate = _uiState.value.document.entities
            .filter { _uiState.value.layerVisibility[it.layer] != false }
            .minByOrNull { distanceToEntity(worldPoint, it) }

        if (candidate != null && distanceToEntity(worldPoint, candidate) <= tolerance) {
            _uiState.update { current ->
                val alreadySelected = current.selectedEntities.contains(candidate)
                val newList = if (alreadySelected) {
                    current.selectedEntities - candidate
                } else {
                    listOf(candidate)
                }
                current.copy(
                    selectedEntities = newList,
                    toastMessage = if (!alreadySelected) "Selected: ${candidate.getSummary()}" else "Deselected"
                )
            }
        } else {
            clearSelection()
        }
    }

    private fun distanceToEntity(p: Point2D, entity: DxfEntity): Float {
        return when (entity) {
            is DxfLine -> distanceToSegment(p, entity.start, entity.end)
            is DxfCircle -> abs(p.distanceTo(entity.center) - entity.radius)
            is DxfArc -> {
                val dCenter = p.distanceTo(entity.center)
                val dRad = abs(dCenter - entity.radius)
                val angle = Math.toDegrees(atan2((p.y - entity.center.y).toDouble(), (p.x - entity.center.x).toDouble())).toFloat()
                val normAngle = if (angle < 0) angle + 360f else angle
                val inSpan = if (entity.endAngleDeg >= entity.startAngleDeg) {
                    normAngle in entity.startAngleDeg..entity.endAngleDeg
                } else {
                    normAngle >= entity.startAngleDeg || normAngle <= entity.endAngleDeg
                }
                if (inSpan) dRad else Float.MAX_VALUE
            }
            is DxfLwPolyline -> {
                var minD = Float.MAX_VALUE
                for (i in 0 until entity.vertices.size - 1) {
                    val d = distanceToSegment(p, entity.vertices[i], entity.vertices[i + 1])
                    if (d < minD) minD = d
                }
                minD
            }
            is DxfPolyline -> {
                var minD = Float.MAX_VALUE
                for (i in 0 until entity.vertices.size - 1) {
                    val d = distanceToSegment(p, entity.vertices[i], entity.vertices[i + 1])
                    if (d < minD) minD = d
                }
                minD
            }
            is DxfText -> p.distanceTo(entity.position)
            is DxfDimension -> min(distanceToSegment(p, entity.defPoint, entity.textPoint), p.distanceTo(entity.textPoint))
            is DxfPoint -> p.distanceTo(entity.position)
            is DxfSpline -> {
                var minD = Float.MAX_VALUE
                for (cp in entity.points) {
                    val d = p.distanceTo(cp)
                    if (d < minD) minD = d
                }
                minD
            }
            is DxfEllipse -> p.distanceTo(entity.center)
        }
    }

    private fun distanceToSegment(p: Point2D, a: Point2D, b: Point2D): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 == 0f) return p.distanceTo(a)
        val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
        val projection = Point2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        return p.distanceTo(projection)
    }

    // ==========================================
    // CAD MODIFICATION ENGINE
    // ==========================================

    fun applyMove(dx: Float, dy: Float) {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val targetSet = targets.toHashSet()
        val updated = _uiState.value.document.entities.map {
            if (targetSet.contains(it)) it.translate(dx, dy) else it
        }
        applyModifiedEntities(updated, "Moved ${targets.size} entities")
    }

    fun applyRotate(origin: Point2D, angleDeg: Float) {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val targetSet = targets.toHashSet()
        val updated = _uiState.value.document.entities.map {
            if (targetSet.contains(it)) it.rotate(origin, angleDeg) else it
        }
        applyModifiedEntities(updated, "Rotated ${targets.size} entities by ${angleDeg}°")
    }

    fun applyScale(origin: Point2D, factor: Float) {
        val targets = getTargetEntities()
        if (targets.isEmpty() || factor <= 0f) return
        pushHistory()
        val targetSet = targets.toHashSet()
        val updated = _uiState.value.document.entities.map {
            if (targetSet.contains(it)) it.scale(origin, factor) else it
        }
        applyModifiedEntities(updated, "Scaled ${targets.size} entities by ${factor}x")
    }

    fun applyCopy(dx: Float, dy: Float, count: Int = 1) {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val newEntities = mutableListOf<DxfEntity>()
        for (i in 1..count) {
            val stepDx = dx * i
            val stepDy = dy * i
            for (t in targets) {
                newEntities.add(t.translate(stepDx, stepDy))
            }
        }
        val allEntities = _uiState.value.document.entities + newEntities
        applyModifiedEntities(allEntities, "Copied ${newEntities.size} new entities")
    }

    fun applyTrimOrExtend(factor: Float) {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val targetSet = targets.toHashSet()
        val updated = _uiState.value.document.entities.map {
            if (targetSet.contains(it)) it.trimOrExtend(factor) else it
        }
        applyModifiedEntities(updated, "Trimmed/Extended ${targets.size} entities")
    }

    fun applyMirror(axisP1: Point2D, axisP2: Point2D, keepSource: Boolean = true) {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val mirrored = targets.map { it.mirror(axisP1, axisP2) }
        val allEntities = if (keepSource) {
            _uiState.value.document.entities + mirrored
        } else {
            val targetSet = targets.toHashSet()
            _uiState.value.document.entities.filter { !targetSet.contains(it) } + mirrored
        }
        applyModifiedEntities(allEntities, "Mirrored ${mirrored.size} entities")
    }

    fun deleteSelectedEntities() {
        val targets = getTargetEntities()
        if (targets.isEmpty()) return
        pushHistory()
        val targetSet = targets.toHashSet()
        val remaining = _uiState.value.document.entities.filter { !targetSet.contains(it) }
        _uiState.update { current ->
            val newDoc = current.document.copy(
                entities = remaining,
                extents = calculateExtents(remaining)
            )
            current.copy(
                document = newDoc,
                selectedEntities = emptyList(),
                canUndo = true,
                toastMessage = "Deleted ${targets.size} entities"
            )
        }
    }

    private fun getTargetEntities(): List<DxfEntity> {
        val selected = _uiState.value.selectedEntities
        return if (selected.isNotEmpty()) {
            selected
        } else {
            _uiState.value.document.entities.filter { _uiState.value.layerVisibility[it.layer] != false }
        }
    }

    private fun applyModifiedEntities(entities: List<DxfEntity>, message: String) {
        val newExtents = calculateExtents(entities)
        _uiState.update { current ->
            val newDoc = current.document.copy(entities = entities, extents = newExtents)
            current.copy(
                document = newDoc,
                canUndo = true,
                toastMessage = message,
                activeModifyDialog = CadModifyDialogType.NONE
            )
        }
    }

    private fun calculateExtents(entities: List<DxfEntity>): BoundingBox {
        val boxes = entities.map { it.getBoundingBox() }
        if (boxes.isEmpty()) return BoundingBox.EMPTY
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (b in boxes) {
            if (b.minX < minX) minX = b.minX
            if (b.minY < minY) minY = b.minY
            if (b.maxX > maxX) maxX = b.maxX
            if (b.maxY > maxY) maxY = b.maxY
        }
        return BoundingBox(minX, minY, maxX, maxY)
    }
}
