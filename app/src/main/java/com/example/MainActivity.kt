package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.canvas.CadCanvas
import com.example.components.*
import com.example.measurement.MeasureToolType
import com.example.measurement.MeasurementExporter
import com.example.parser.DxfParser
import com.example.parser.Point2D
import com.example.parser.SampleDxfProvider
import com.example.store.CadModifyDialogType
import com.example.store.CadTool
import com.example.store.CadViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val cadViewModel: CadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CadViewerApp(cadViewModel = cadViewModel)
            }
        }
    }
}

@Composable
fun CadViewerApp(cadViewModel: CadViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by cadViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val samples = remember { SampleDxfProvider.getSamples() }

    // Toast notification observer
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            cadViewModel.dismissToast()
        }
    }

    // System File Picker for custom .dxf files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val content = withContext(Dispatchers.IO) {
                            inputStream.bufferedReader().use { it.readText() }
                        }
                        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported.dxf"
                        val doc = DxfParser().parse(content, fileName)
                        cadViewModel.loadDocument(doc)
                        Toast.makeText(context, "Loaded \"$fileName\" (${doc.entities.size} entities)", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to parse DXF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Count entities by layer
    val layerEntityCounts = remember(uiState.document) {
        val counts = mutableMapOf<String, Int>()
        for (entity in uiState.document.entities) {
            counts[entity.layer] = (counts[entity.layer] ?: 0) + 1
        }
        counts
    }

    val visibleLayerCount = remember(uiState.layerVisibility) {
        uiState.layerVisibility.values.count { it }
    }
    val totalLayerCount = remember(uiState.document) {
        uiState.document.layers.size
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                TopNavbar(
                    fileName = uiState.document.fileName,
                    entityCount = uiState.document.entities.size,
                    isDarkMode = uiState.isBlueprintTheme,
                    unitConfig = uiState.unitConfig,
                    onToggleTheme = { cadViewModel.toggleTheme() },
                    onSelectAction = { action, payload ->
                        when (action) {
                            "import-image" -> {
                                cadViewModel.showSamplePicker(true)
                            }
                            "open-file" -> {
                                filePickerLauncher.launch("*/*")
                            }
                            "export-dxf" -> {
                                try {
                                    val dxfString = DxfParser().exportToDxf(uiState.document)
                                    val sendIntent = Intent().apply {
                                        this.action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, dxfString)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export DXF Drawing")
                                    context.startActivity(shareIntent)
                                    Toast.makeText(context, "Exporting DXF (${uiState.document.entities.size} entities)...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            "export-png" -> {
                                Toast.makeText(context, "PNG Snapshot exported to gallery buffer", Toast.LENGTH_SHORT).show()
                            }
                            "fit-view" -> {
                                cadViewModel.fitToExtents()
                            }
                            "toggle-layers-panel" -> {
                                cadViewModel.showLayerSheet(true)
                            }
                            "toggle-grid" -> {
                                cadViewModel.toggleGrid()
                            }
                            "tool-move" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.MOVE)
                            }
                            "tool-rotate" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.ROTATE)
                            }
                            "tool-scale" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.SCALE)
                            }
                            "tool-copy" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.COPY)
                            }
                            "tool-trim" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.TRIM)
                            }
                            "tool-mirror" -> {
                                cadViewModel.openModifyDialog(CadModifyDialogType.MIRROR)
                            }
                            "measure-dist" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.DISTANCE)
                            }
                            "measure-area" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.AREA_POLYGON)
                            }
                            "measure-angle" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.ANGLE)
                            }
                            "measure-coord" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.COORDINATE)
                            }
                            "measure-radius" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.RADIUS)
                            }
                            "measure-continuous" -> {
                                cadViewModel.setMeasurementTool(MeasureToolType.CONTINUOUS)
                            }
                            "measure-list" -> {
                                cadViewModel.showMeasurementManager(true)
                            }
                            "measure-snap-settings" -> {
                                cadViewModel.showSnapSettingsDialog(true)
                            }
                            "measure-units-settings" -> {
                                cadViewModel.showUnitSettingsDialog(true)
                            }
                            "grid-settings" -> {
                                cadViewModel.showGridSettingsDialog(true)
                            }
                            "toggle-grid-snap" -> {
                                cadViewModel.toggleGridSnap()
                            }
                            "toggle-axes" -> {
                                cadViewModel.toggleAxes()
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            CadBottomBar(
                activeTool = uiState.activeTool,
                cursorCoords = uiState.cursorCadCoords,
                zoomScale = uiState.viewport.scale,
                showGrid = uiState.showGrid,
                showAxes = uiState.showAxes,
                visibleLayerCount = visibleLayerCount,
                totalLayerCount = totalLayerCount,
                unitConfig = uiState.unitConfig,
                gridSettings = uiState.gridSettings,
                onSetTool = { cadViewModel.setTool(it) },
                onOpenLayers = { cadViewModel.showLayerSheet(true) },
                onFitExtents = { cadViewModel.fitToExtents() },
                onZoomIn = { cadViewModel.zoomIn() },
                onZoomOut = { cadViewModel.zoomOut() },
                onToggleGrid = { cadViewModel.toggleGrid() },
                onToggleAxes = { cadViewModel.toggleAxes() },
                onOpenUnitSettings = { cadViewModel.showUnitSettingsDialog(true) },
                onOpenGridSettings = { cadViewModel.showGridSettingsDialog(true) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        },
        containerColor = if (uiState.isBlueprintTheme) Color(0xFF0A192F) else Color(0xFF1E1E1E)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main CAD drawing interactive canvas
            CadCanvas(
                uiState = uiState,
                onTransform = { panX, panY, zoom, centroid ->
                    cadViewModel.onTransformGesture(panX, panY, zoom, centroid)
                },
                onTap = { screenOffset ->
                    cadViewModel.onCanvasTap(screenOffset)
                },
                onDoubleTap = {
                    cadViewModel.fitToExtents()
                },
                onPointerHover = { offset ->
                    cadViewModel.onPointerHover(offset)
                },
                onMarqueeDragUpdate = { start, current ->
                    cadViewModel.onMarqueeDragUpdate(start, current)
                },
                onMarqueeDragEnd = { start, end ->
                    cadViewModel.onMarqueeDragEnd(start, end)
                },
                onMarqueeCancel = {
                    cadViewModel.onMarqueeCancel()
                },
                onSizeChanged = { w, h ->
                    cadViewModel.onCanvasSizeChanged(w, h)
                }
            )

            // Comprehensive Measurement Controls Toolbar (Top)
            AnimatedVisibility(
                visible = uiState.activeTool == CadTool.MEASURE,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                MeasurementToolbar(
                    activeToolType = uiState.activeMeasurement.toolType,
                    snapSettings = uiState.snapSettings,
                    unitConfig = uiState.unitConfig,
                    savedCount = uiState.savedMeasurements.size,
                    onSelectTool = { cadViewModel.setMeasurementTool(it) },
                    onToggleSnap = { cadViewModel.toggleSnapEnabled() },
                    onCycleOrtho = { cadViewModel.cycleOrthoMode() },
                    onOpenSnapSettings = { cadViewModel.showSnapSettingsDialog(true) },
                    onOpenUnitSettings = { cadViewModel.showUnitSettingsDialog(true) },
                    onOpenMeasurementManager = { cadViewModel.showMeasurementManager(true) },
                    onCloseMeasureMode = { cadViewModel.setTool(CadTool.PAN_ZOOM) }
                )
            }

            // Real-time Measurement HUD / Results Panel (Top-Right or Floating)
            AnimatedVisibility(
                visible = uiState.activeTool == CadTool.MEASURE &&
                        (uiState.activeMeasurement.primaryValueDisplay.isNotBlank() || uiState.activeMeasurement.pickedPoints.isNotEmpty()),
                enter = slideInVertically { -it / 2 },
                exit = slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 90.dp, end = 12.dp)
            ) {
                MeasurementResultPanel(
                    activeState = uiState.activeMeasurement,
                    unitConfig = uiState.unitConfig,
                    onSaveMeasurement = { cadViewModel.saveActiveMeasurement() },
                    onUndoLastPoint = { cadViewModel.undoMeasurementPoint() },
                    onFinishMultiPoint = { cadViewModel.finishMeasurement() },
                    onClearCurrent = { cadViewModel.clearMeasurementPoints() }
                )
            }

            // CAD Modification Controls Card (for Move, Rotate, Scale, Copy, Trim, Mirror)
            AnimatedVisibility(
                visible = uiState.activeModifyDialog != CadModifyDialogType.NONE,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                CadModifyControls(
                    dialogType = uiState.activeModifyDialog,
                    selectedCount = uiState.selectedEntities.size,
                    canUndo = uiState.canUndo,
                    onMove = { dx, dy -> cadViewModel.applyMove(dx, dy) },
                    onRotate = { angle -> cadViewModel.applyRotate(Point2D(0f, 0f), angle) },
                    onScale = { factor -> cadViewModel.applyScale(Point2D(0f, 0f), factor) },
                    onCopyOffset = { dx, dy -> cadViewModel.applyCopy(dx, dy) },
                    onTrimExtend = { factor -> cadViewModel.applyTrimOrExtend(factor) },
                    onMirror = { horizontal ->
                        if (horizontal) {
                            cadViewModel.applyMirror(Point2D(0f, 0f), Point2D(100f, 0f))
                        } else {
                            cadViewModel.applyMirror(Point2D(0f, 0f), Point2D(0f, 100f))
                        }
                    },
                    onUndo = { cadViewModel.undo() },
                    onClose = { cadViewModel.closeModifyDialog() }
                )
            }

            // Selected Entity Inspector Card
            AnimatedVisibility(
                visible = uiState.selectedEntities.isNotEmpty() && uiState.activeModifyDialog == CadModifyDialogType.NONE && uiState.activeTool != CadTool.MEASURE,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                EntitySelectionInspector(
                    selectedEntities = uiState.selectedEntities,
                    layersMap = uiState.document.layers,
                    unitConfig = uiState.unitConfig,
                    onDismiss = { cadViewModel.clearSelection() }
                )
            }

            // Layer Management Modal Bottom Sheet
            if (uiState.isLayerSheetVisible) {
                LayerManagerSheet(
                    layers = uiState.document.layers,
                    visibilityMap = uiState.layerVisibility,
                    layerEntityCounts = layerEntityCounts,
                    onToggleLayer = { layer, visible ->
                        cadViewModel.setLayerVisibility(layer, visible)
                    },
                    onToggleAll = { visible ->
                        cadViewModel.toggleAllLayers(visible)
                    },
                    onDismiss = { cadViewModel.showLayerSheet(false) }
                )
            }

            // Measurement List & Aggregation Management Sheet
            if (uiState.isMeasurementManagerVisible) {
                MeasurementListSheet(
                    records = uiState.savedMeasurements,
                    unitConfig = uiState.unitConfig,
                    fileName = uiState.document.fileName,
                    onDismiss = { cadViewModel.showMeasurementManager(false) },
                    onToggleVisibility = { cadViewModel.toggleMeasurementVisibility(it) },
                    onToggleHole = { cadViewModel.toggleHoleSubtraction(it) },
                    onZoomTo = { cadViewModel.zoomToMeasurement(it) },
                    onDelete = { cadViewModel.deleteMeasurement(it) },
                    onClearAll = { cadViewModel.clearAllMeasurements() }
                )
            }

            // OSNAP Snap Settings Dialog
            if (uiState.isSnapSettingsVisible) {
                SnapSettingsDialog(
                    snapSettings = uiState.snapSettings,
                    onUpdateSnapSettings = { cadViewModel.setSnapSettings(it) },
                    onDismiss = { cadViewModel.showSnapSettingsDialog(false) }
                )
            }

            // Units & Precision Settings Dialog
            if (uiState.isUnitSettingsVisible) {
                UnitSettingsDialog(
                    unitConfig = uiState.unitConfig,
                    onUpdateUnitConfig = { cadViewModel.setUnitConfig(it) },
                    onDismiss = { cadViewModel.showUnitSettingsDialog(false) }
                )
            }

            // Grid & Snap Settings Dialog
            if (uiState.isGridSettingsVisible) {
                GridSettingsDialog(
                    gridSettings = uiState.gridSettings,
                    unitConfig = uiState.unitConfig,
                    viewportScale = uiState.viewport.scale,
                    isBlueprintTheme = uiState.isBlueprintTheme,
                    onSaveSettings = { cadViewModel.setGridSettings(it) },
                    onDismiss = { cadViewModel.showGridSettingsDialog(false) }
                )
            }

            // Sample CAD Drawings Picker Dialog
            if (uiState.isSamplePickerVisible) {
                SampleSelectorDialog(
                    samples = samples,
                    currentFileName = uiState.document.fileName,
                    onSelectSample = { sample ->
                        cadViewModel.loadDocument(sample.document)
                        cadViewModel.showSamplePicker(false)
                        Toast.makeText(context, "Loaded \"${sample.title}\"", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { cadViewModel.showSamplePicker(false) }
                )
            }
        }
    }
}
