package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.parser.DxfParser
import com.example.parser.Point2D
import com.example.parser.SampleDxfProvider
import com.example.store.CadTool
import com.example.store.CadViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CAD Viewer", appName)
  }

  @Test
  fun `test dxf parser and sample models`() {
    val samples = SampleDxfProvider.getSamples()
    assertTrue(samples.isNotEmpty())
    val first = samples.first()
    assertTrue(first.document.entities.isNotEmpty())
    assertTrue(first.document.layers.isNotEmpty())
  }

  @Test
  fun `test custom dxf parsing`() {
    val sampleDxf = """
      0
      SECTION
      2
      ENTITIES
      0
      LINE
      8
      WALLS
      10
      10.0
      20
      20.0
      11
      100.0
      21
      200.0
      0
      ENDSEC
      0
      EOF
    """.trimIndent()

    val doc = DxfParser().parse(sampleDxf, "test.dxf")
    assertEquals(1, doc.entities.size)
    assertEquals("WALLS", doc.entities.first().layer)
  }

  @Test
  fun `test cad view model tool transitions and marquee selection`() {
    val vm = CadViewModel()
    val doc = SampleDxfProvider.getSamples().first().document
    vm.loadDocument(doc)
    vm.onCanvasSizeChanged(1000f, 1000f)

    // Initial state
    assertEquals(CadTool.PAN_ZOOM, vm.uiState.value.activeTool)
    assertTrue(vm.uiState.value.selectedEntities.isEmpty())

    // Switch to Marquee Select
    vm.setTool(CadTool.MARQUEE_SELECT)
    assertEquals(CadTool.MARQUEE_SELECT, vm.uiState.value.activeTool)

    // Perform Marquee Box Drag covering canvas
    vm.onMarqueeDragUpdate(Offset(0f, 0f), Offset(500f, 500f))
    assertNotNull(vm.uiState.value.activeMarquee)

    // Complete drag
    vm.onMarqueeDragEnd(Offset(0f, 0f), Offset(1000f, 1000f))
    assertNull(vm.uiState.value.activeMarquee)
    assertTrue("Should select entities inside marquee box", vm.uiState.value.selectedEntities.isNotEmpty())

    // Test Clear Selection
    vm.clearSelection()
    assertTrue(vm.uiState.value.selectedEntities.isEmpty())

    // Test Select All in View
    vm.selectAllInView()
    assertTrue(vm.uiState.value.selectedEntities.isNotEmpty())
  }

  @Test
  fun `test measurement tool calculation`() {
    val vm = CadViewModel()
    vm.setTool(CadTool.MEASURE)
    assertEquals(CadTool.MEASURE, vm.uiState.value.activeTool)

    // First measurement point at (0, 0)
    vm.onCanvasTap(Offset(500f, 500f)) // converts to world coordinates
    assertNotNull(vm.uiState.value.measurement.p1)
    assertNull(vm.uiState.value.measurement.p2)

    // Second measurement point
    vm.onCanvasTap(Offset(600f, 500f))
    assertNotNull(vm.uiState.value.measurement.p2)
    assertTrue(vm.uiState.value.measurement.distance > 0f)

    // Clear measurement
    vm.clearMeasurement()
    assertNull(vm.uiState.value.measurement.p1)
  }

  @Test
  fun `test UI composable hierarchy and menu bottom bars`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        CadViewerApp()
      }
    }

    // Verify top navbar and bottom bar exist in Compose hierarchy
    composeTestRule.onNodeWithTag("top_navbar").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag("cad_bottom_bar").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_pan").assertExists()
    composeTestRule.onNodeWithTag("nav_marquee").assertExists()
    composeTestRule.onNodeWithTag("nav_inspect").assertExists()
    composeTestRule.onNodeWithTag("nav_measure").assertExists()
    composeTestRule.onNodeWithTag("nav_layers").assertExists()

    // Test interacting with Bottom Bar tool selection
    composeTestRule.onNodeWithTag("nav_marquee").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `test top navbar action callbacks`() {
    var capturedAction: String? = null
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.components.TopNavbar(
          fileName = "floorplan.dxf",
          entityCount = 42,
          isDarkMode = true,
          onToggleTheme = { capturedAction = "toggle-theme" },
          onSelectAction = { action, _ -> capturedAction = action }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_quick_fit_view").performClick()
    assertEquals("fit-view", capturedAction)

    composeTestRule.onNodeWithTag("btn_quick_layers").performClick()
    assertEquals("toggle-layers-panel", capturedAction)

    composeTestRule.onNodeWithTag("btn_quick_theme").performClick()
    assertEquals("toggle-theme", capturedAction)
  }

  @Test
  fun `test cad modification operations and undo`() {
    val vm = CadViewModel()
    val doc = SampleDxfProvider.getSamples().first().document
    vm.loadDocument(doc)

    val initialCount = vm.uiState.value.document.entities.size
    assertTrue(initialCount > 0)

    // Select all entities for modification
    vm.selectAllInView()
    val initialSelected = vm.uiState.value.selectedEntities
    assertTrue(initialSelected.isNotEmpty())

    // 1. Move
    val firstEntityBefore = initialSelected.first()
    vm.applyMove(10f, 20f)
    assertTrue(vm.uiState.value.canUndo)
    val firstEntityAfter = vm.uiState.value.selectedEntities.first()
    assertNotEquals(firstEntityBefore.getCenterPoint(), firstEntityAfter.getCenterPoint())

    // 2. Undo Move
    vm.undo()
    assertEquals(initialCount, vm.uiState.value.document.entities.size)

    // 3. Copy Offset
    vm.selectAllInView()
    vm.applyCopyOffset(50f, 50f)
    assertEquals(initialCount * 2, vm.uiState.value.document.entities.size)
    assertTrue(vm.uiState.value.canUndo)

    // 4. Undo Copy
    vm.undo()
    assertEquals(initialCount, vm.uiState.value.document.entities.size)

    // 5. Rotate
    vm.selectAllInView()
    vm.applyRotate(90f)
    assertTrue(vm.uiState.value.canUndo)

    // 6. Scale
    vm.applyScale(1.5f)
    assertTrue(vm.uiState.value.canUndo)

    // 7. Mirror
    vm.applyMirror(horizontal = true)
    assertTrue(vm.uiState.value.canUndo)

    // 8. Trim / Extend
    vm.applyTrimExtend(0.8f)
    assertTrue(vm.uiState.value.canUndo)
  }
}

