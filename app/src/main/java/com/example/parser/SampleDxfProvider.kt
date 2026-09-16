package com.example.parser

import androidx.compose.ui.graphics.Color
import kotlin.math.*

object SampleDxfProvider {

    data class SampleDrawing(
        val id: String,
        val title: String,
        val category: String,
        val description: String,
        val document: DxfDocument
    )

    fun getSamples(): List<SampleDrawing> {
        return listOf(
            SampleDrawing(
                id = "arch_floor_plan",
                title = "Architectural Floor Plan",
                category = "Architecture",
                description = "Modern 2-bedroom residential blueprint with walls, doors, windows & dimensions",
                document = createArchitecturalPlan()
            ),
            SampleDrawing(
                id = "mech_flange_gear",
                title = "Mechanical Flange & Hub",
                category = "Mechanical",
                description = "Precision machined flange with 8 bolt pitch circle, inner keyway & centerlines",
                document = createMechanicalFlange()
            ),
            SampleDrawing(
                id = "civil_site_plan",
                title = "Civil Site Layout",
                category = "Civil",
                description = "Subdivision parcel with building setbacks, road curves, contours & utilities",
                document = createCivilSitePlan()
            ),
            SampleDrawing(
                id = "city_grid_benchmark",
                title = "Enterprise Structural Grid (3,500+ Entities)",
                category = "Benchmark",
                description = "Large-scale MEP, structural columns, HVAC & office grid testing viewport culling & 60fps rendering",
                document = createStressTestCityGrid()
            )
        )
    }

    private fun createArchitecturalPlan(): DxfDocument {
        val layers = mapOf(
            "WALLS_EXT" to DxfLayer("WALLS_EXT", Color(0xFFFFFFFF), isVisible = true),
            "WALLS_INT" to DxfLayer("WALLS_INT", Color(0xFFB0BEC5), isVisible = true),
            "DOORS" to DxfLayer("DOORS", Color(0xFF00E5FF), isVisible = true),
            "WINDOWS" to DxfLayer("WINDOWS", Color(0xFF00E676), isVisible = true),
            "FURNITURE" to DxfLayer("FURNITURE", Color(0xFFFFD700), isVisible = true),
            "DIMENSIONS" to DxfLayer("DIMENSIONS", Color(0xFFFF5252), isVisible = true),
            "ROOM_TEXT" to DxfLayer("ROOM_TEXT", Color(0xFFFF4081), isVisible = true)
        )

        val entities = mutableListOf<DxfEntity>()

        // Outer Perimeter (Width: 300, Height: 200)
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(0f, 0f),
                Point2D(300f, 0f),
                Point2D(300f, 200f),
                Point2D(0f, 200f)
            ),
            isClosed = true,
            layer = "WALLS_EXT",
            color = Color(0xFFFFFFFF)
        ))

        // Outer Wall Thickness
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(8f, 8f),
                Point2D(292f, 8f),
                Point2D(292f, 192f),
                Point2D(8f, 192f)
            ),
            isClosed = true,
            layer = "WALLS_EXT",
            color = Color(0xFFFFFFFF)
        ))

        // Interior Dividing Walls
        // Living / Bedroom divider at x = 160
        entities.add(DxfLine(Point2D(160f, 8f), Point2D(160f, 192f), layer = "WALLS_INT"))
        entities.add(DxfLine(Point2D(166f, 8f), Point2D(166f, 192f), layer = "WALLS_INT"))

        // Bedroom 1 / Bedroom 2 divider at y = 100 (for x in 166..292)
        entities.add(DxfLine(Point2D(166f, 100f), Point2D(292f, 100f), layer = "WALLS_INT"))
        entities.add(DxfLine(Point2D(166f, 105f), Point2D(292f, 105f), layer = "WALLS_INT"))

        // Kitchen / Living divider at y = 120 (for x in 8..100)
        entities.add(DxfLine(Point2D(8f, 120f), Point2D(100f, 120f), layer = "WALLS_INT"))
        entities.add(DxfLine(Point2D(8f, 125f), Point2D(100f, 125f), layer = "WALLS_INT"))

        // Bathroom at x: 100..160, y: 130..192
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(100f, 130f),
                Point2D(160f, 130f),
                Point2D(160f, 192f),
                Point2D(100f, 192f)
            ),
            isClosed = true,
            layer = "WALLS_INT"
        ))

        // Doors with swing arcs
        // Main Entry Door (bottom, near x = 60)
        entities.add(DxfLine(Point2D(60f, 8f), Point2D(60f, 30f), layer = "DOORS"))
        entities.add(DxfArc(Point2D(60f, 8f), 22f, 0f, 90f, layer = "DOORS"))

        // Bedroom 1 Door
        entities.add(DxfLine(Point2D(166f, 40f), Point2D(186f, 40f), layer = "DOORS"))
        entities.add(DxfArc(Point2D(166f, 40f), 20f, 270f, 360f, layer = "DOORS"))

        // Bedroom 2 Door
        entities.add(DxfLine(Point2D(166f, 120f), Point2D(186f, 120f), layer = "DOORS"))
        entities.add(DxfArc(Point2D(166f, 120f), 20f, 0f, 90f, layer = "DOORS"))

        // Windows (Outer Walls)
        // Living room south window
        entities.add(DxfLwPolyline(listOf(Point2D(90f, 0f), Point2D(130f, 0f), Point2D(130f, 8f), Point2D(90f, 8f)), true, "WINDOWS"))
        entities.add(DxfLine(Point2D(90f, 4f), Point2D(130f, 4f), layer = "WINDOWS"))

        // Bedroom 1 east window
        entities.add(DxfLwPolyline(listOf(Point2D(292f, 40f), Point2D(300f, 40f), Point2D(300f, 75f), Point2D(292f, 75f)), true, "WINDOWS"))
        // Bedroom 2 east window
        entities.add(DxfLwPolyline(listOf(Point2D(292f, 135f), Point2D(300f, 135f), Point2D(300f, 170f), Point2D(292f, 170f)), true, "WINDOWS"))
        // Kitchen north window
        entities.add(DxfLwPolyline(listOf(Point2D(30f, 192f), Point2D(70f, 192f), Point2D(70f, 200f), Point2D(30f, 200f)), true, "WINDOWS"))

        // Furniture: Dining Table & Chairs in Living Area
        entities.add(DxfLwPolyline(listOf(Point2D(50f, 50f), Point2D(90f, 50f), Point2D(90f, 80f), Point2D(50f, 80f)), true, "FURNITURE"))
        entities.add(DxfCircle(Point2D(42f, 65f), 5f, "FURNITURE"))
        entities.add(DxfCircle(Point2D(98f, 65f), 5f, "FURNITURE"))
        entities.add(DxfCircle(Point2D(70f, 42f), 5f, "FURNITURE"))
        entities.add(DxfCircle(Point2D(70f, 88f), 5f, "FURNITURE"))

        // Bedroom 1: Master Bed
        entities.add(DxfLwPolyline(listOf(Point2D(220f, 20f), Point2D(280f, 20f), Point2D(280f, 75f), Point2D(220f, 75f)), true, "FURNITURE"))
        entities.add(DxfLine(Point2D(220f, 32f), Point2D(280f, 32f), layer = "FURNITURE")) // Pillows line

        // Bedroom 2: Twin Bed
        entities.add(DxfLwPolyline(listOf(Point2D(230f, 120f), Point2D(280f, 120f), Point2D(280f, 175f), Point2D(230f, 175f)), true, "FURNITURE"))

        // Room Labels
        entities.add(DxfText("LIVING & DINING", Point2D(45f, 25f), height = 5f, layer = "ROOM_TEXT"))
        entities.add(DxfText("KITCHEN", Point2D(35f, 155f), height = 4.5f, layer = "ROOM_TEXT"))
        entities.add(DxfText("BATHROOM", Point2D(112f, 155f), height = 3.5f, layer = "ROOM_TEXT"))
        entities.add(DxfText("MASTER BEDROOM", Point2D(185f, 55f), height = 4.5f, layer = "ROOM_TEXT"))
        entities.add(DxfText("GUEST BEDROOM", Point2D(185f, 145f), height = 4.5f, layer = "ROOM_TEXT"))

        // Exterior Dimensions
        // Bottom dimension (300.0)
        entities.add(DxfDimension("300.00", Point2D(0f, -15f), Point2D(300f, -15f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(0f, -5f), Point2D(0f, -22f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(300f, -5f), Point2D(300f, -22f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(0f, -15f), Point2D(300f, -15f), layer = "DIMENSIONS"))

        // Right side dimension (200.0)
        entities.add(DxfDimension("200.00", Point2D(315f, 0f), Point2D(315f, 200f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(305f, 0f), Point2D(322f, 0f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(305f, 200f), Point2D(322f, 200f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(315f, 0f), Point2D(315f, 200f), layer = "DIMENSIONS"))

        return DxfDocument(
            fileName = "FloorPlan_2BHK.dxf",
            layers = layers,
            entities = entities,
            extents = BoundingBox(-25f, -30f, 335f, 215f)
        )
    }

    private fun createMechanicalFlange(): DxfDocument {
        val layers = mapOf(
            "OUTLINE" to DxfLayer("OUTLINE", Color(0xFFFFFFFF), isVisible = true),
            "HOLES" to DxfLayer("HOLES", Color(0xFF00E5FF), isVisible = true),
            "CENTERLINE" to DxfLayer("CENTERLINE", Color(0xFFFF5252), isVisible = true),
            "KEYWAY" to DxfLayer("KEYWAY", Color(0xFFFFD700), isVisible = true),
            "DIMENSIONS" to DxfLayer("DIMENSIONS", Color(0xFF00E676), isVisible = true),
            "ANNOTATIONS" to DxfLayer("ANNOTATIONS", Color(0xFFFF4081), isVisible = true)
        )

        val entities = mutableListOf<DxfEntity>()
        val center = Point2D(0f, 0f)

        // Centerlines
        entities.add(DxfLine(Point2D(-110f, 0f), Point2D(110f, 0f), layer = "CENTERLINE"))
        entities.add(DxfLine(Point2D(0f, -110f), Point2D(0f, 110f), layer = "CENTERLINE"))

        // Outer Flange Boundary (Diameter = 180, R = 90)
        entities.add(DxfCircle(center, 90f, layer = "OUTLINE"))

        // Pitch Circle for Bolts (Diameter = 136, R = 68)
        entities.add(DxfCircle(center, 68f, layer = "CENTERLINE"))

        // Inner Hub Outer Rim (Diameter = 90, R = 45)
        entities.add(DxfCircle(center, 45f, layer = "OUTLINE"))

        // Shaft Bore (Diameter = 40, R = 20)
        entities.add(DxfCircle(center, 20f, layer = "HOLES"))

        // Keyway in Bore (Width = 8, Height = 24 total from center)
        val kwW = 4f
        val kwH = 24f
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(-kwW, sqrt(20f * 20f - kwW * kwW)),
                Point2D(-kwW, kwH),
                Point2D(kwW, kwH),
                Point2D(kwW, sqrt(20f * 20f - kwW * kwW))
            ),
            isClosed = false,
            layer = "KEYWAY"
        ))

        // 8 Bolt Holes on PCD R=68, hole radius = 6.5 (M12 clearance)
        val boltCount = 8
        for (i in 0 until boltCount) {
            val angleRad = (i * 2 * Math.PI / boltCount)
            val bx = (68f * cos(angleRad)).toFloat()
            val by = (68f * sin(angleRad)).toFloat()
            entities.add(DxfCircle(Point2D(bx, by), 6.5f, layer = "HOLES"))

            // Small bolt crosshairs
            entities.add(DxfLine(Point2D(bx - 10f, by), Point2D(bx + 10f, by), layer = "CENTERLINE"))
            entities.add(DxfLine(Point2D(bx, by - 10f), Point2D(bx, by + 10f), layer = "CENTERLINE"))
        }

        // Annotations & Callouts
        entities.add(DxfText("Ø180.00 FLANGE OD", Point2D(-80f, 98f), height = 5f, layer = "ANNOTATIONS"))
        entities.add(DxfText("PCD Ø136.00 (8x Ø13.0 HOLES)", Point2D(-95f, -100f), height = 4.5f, layer = "ANNOTATIONS"))
        entities.add(DxfText("BORE Ø40 H7 + KEY 8x4", Point2D(-55f, -32f), height = 4.2f, layer = "ANNOTATIONS"))

        // Dimension lines
        entities.add(DxfDimension("R90.0", Point2D(0f, 0f), Point2D(63.6f, 63.6f), layer = "DIMENSIONS"))
        entities.add(DxfLine(Point2D(0f, 0f), Point2D(63.6f, 63.6f), layer = "DIMENSIONS"))

        return DxfDocument(
            fileName = "Flange_8Hole_PCD136.dxf",
            layers = layers,
            entities = entities,
            extents = BoundingBox(-125f, -125f, 125f, 125f)
        )
    }

    private fun createCivilSitePlan(): DxfDocument {
        val layers = mapOf(
            "BOUNDARY" to DxfLayer("BOUNDARY", Color(0xFFFFD700), isVisible = true),
            "SETBACK" to DxfLayer("SETBACK", Color(0xFFFF5252), isVisible = true),
            "BUILDING" to DxfLayer("BUILDING", Color(0xFF00E5FF), isVisible = true),
            "ROAD" to DxfLayer("ROAD", Color(0xFFFFFFFF), isVisible = true),
            "CONTOURS" to DxfLayer("CONTOURS", Color(0xFF90A4AE), isVisible = true),
            "TREES" to DxfLayer("TREES", Color(0xFF00E676), isVisible = true)
        )

        val entities = mutableListOf<DxfEntity>()

        // Parcel property boundary
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(0f, 0f),
                Point2D(400f, 20f),
                Point2D(380f, 320f),
                Point2D(20f, 280f)
            ),
            isClosed = true,
            layer = "BOUNDARY"
        ))

        // Setback line (dashed boundary offset)
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(30f, 30f),
                Point2D(370f, 45f),
                Point2D(355f, 290f),
                Point2D(45f, 255f)
            ),
            isClosed = true,
            layer = "SETBACK"
        ))

        // Proposed Building Footprint
        entities.add(DxfLwPolyline(
            listOf(
                Point2D(100f, 90f),
                Point2D(260f, 100f),
                Point2D(250f, 210f),
                Point2D(180f, 205f),
                Point2D(180f, 160f),
                Point2D(90f, 155f)
            ),
            isClosed = true,
            layer = "BUILDING"
        ))

        // Curved Access Road / Driveway
        val roadPointsLeft = mutableListOf<Point2D>()
        val roadPointsRight = mutableListOf<Point2D>()
        for (step in 0..10) {
            val t = step / 10f
            val x = 20f + 160f * t
            val y = -40f + 130f * t + 25f * sin(t * Math.PI.toFloat())
            roadPointsLeft.add(Point2D(x, y))
            roadPointsRight.add(Point2D(x + 24f, y - 5f))
        }
        entities.add(DxfLwPolyline(roadPointsLeft, isClosed = false, layer = "ROAD"))
        entities.add(DxfLwPolyline(roadPointsRight, isClosed = false, layer = "ROAD"))

        // Topographic Contour Lines (Elevation intervals)
        for (elev in listOf(105, 110, 115, 120)) {
            val cPoints = mutableListOf<Point2D>()
            val baseOffset = (elev - 100) * 18f
            for (step in 0..8) {
                val t = step / 8f
                val x = 30f + 330f * t
                val y = baseOffset + 15f * cos(t * 3f + elev)
                cPoints.add(Point2D(x, y))
            }
            entities.add(DxfLwPolyline(cPoints, isClosed = false, layer = "CONTOURS"))
            entities.add(DxfText("+%dm".format(elev), Point2D(35f, baseOffset + 10f), height = 4f, layer = "CONTOURS"))
        }

        // Landscaping: Trees (circles with star ticks)
        val treeLocs = listOf(
            Point2D(60f, 60f),
            Point2D(75f, 220f),
            Point2D(310f, 80f),
            Point2D(320f, 240f),
            Point2D(140f, 245f)
        )
        for (tree in treeLocs) {
            entities.add(DxfCircle(tree, 9f, layer = "TREES"))
            entities.add(DxfLine(Point2D(tree.x - 9f, tree.y), Point2D(tree.x + 9f, tree.y), layer = "TREES"))
            entities.add(DxfLine(Point2D(tree.x, tree.y - 9f), Point2D(tree.x, tree.y + 9f), layer = "TREES"))
        }

        // Parcel Information Label
        entities.add(DxfText("LOT 42 - PARCEL AREA: 1,120 m²", Point2D(120f, 260f), height = 6f, layer = "BOUNDARY"))
        entities.add(DxfText("PROPOSED TWO-STORY RESIDENCE", Point2D(115f, 130f), height = 5f, layer = "BUILDING"))

        return DxfDocument(
            fileName = "Civil_Site_Lot42.dxf",
            layers = layers,
            entities = entities,
            extents = BoundingBox(-20f, -50f, 420f, 350f)
        )
    }

    private fun createStressTestCityGrid(gridSize: Int = 18): DxfDocument {
        val layers = mapOf(
            "GRID" to DxfLayer("GRID", Color(0xFF2979FF), isVisible = true),
            "WALLS_EXT" to DxfLayer("WALLS_EXT", Color(0xFF00E5FF), isVisible = true),
            "WALLS_INT" to DxfLayer("WALLS_INT", Color(0xFF00E676), isVisible = true),
            "COLUMNS" to DxfLayer("COLUMNS", Color(0xFFFF3333), isVisible = true),
            "HVAC_DUCTS" to DxfLayer("HVAC_DUCTS", Color(0xFFFFD700), isVisible = true),
            "ELECTRICAL" to DxfLayer("ELECTRICAL", Color(0xFFFF4081), isVisible = true),
            "ANNOTATIONS" to DxfLayer("ANNOTATIONS", Color(0xFFFFFFFF), isVisible = true)
        )

        val entities = mutableListOf<DxfEntity>()
        val spacing = 40f
        val originOffset = (gridSize * spacing) / 2f

        // 1. Grid Lines and Column Markers
        for (i in 0..gridSize) {
            val coord = i * spacing - originOffset
            val minCoord = -originOffset - 20f
            val maxCoord = originOffset + 20f

            entities.add(DxfLine(Point2D(coord, minCoord), Point2D(coord, maxCoord), layer = "GRID"))
            entities.add(DxfLine(Point2D(minCoord, coord), Point2D(maxCoord, coord), layer = "GRID"))

            entities.add(DxfCircle(Point2D(coord, maxCoord + 12f), 6f, layer = "ANNOTATIONS"))
            val label = ('A'.code + (i % 26)).toChar().toString()
            entities.add(DxfText(label, Point2D(coord - 2.5f, maxCoord + 9f), height = 5f, layer = "ANNOTATIONS"))
        }

        // 2. Structural Columns
        for (r in 0..gridSize) {
            for (c in 0..gridSize) {
                val cx = c * spacing - originOffset
                val cy = r * spacing - originOffset

                entities.add(DxfCircle(Point2D(cx, cy), 3.5f, layer = "COLUMNS"))
                val s = 6f
                entities.add(DxfLwPolyline(
                    listOf(
                        Point2D(cx - s, cy - s),
                        Point2D(cx + s, cy - s),
                        Point2D(cx + s, cy + s),
                        Point2D(cx - s, cy + s)
                    ),
                    isClosed = true,
                    layer = "COLUMNS"
                ))
                entities.add(DxfLine(Point2D(cx - s, cy - s), Point2D(cx + s, cy + s), layer = "COLUMNS"))
            }
        }

        // 3. Wall Enclosures, HVAC Ducts, and Office Pods
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val x0 = c * spacing - originOffset
                val y0 = r * spacing - originOffset
                val x1 = x0 + spacing
                val y1 = y0 + spacing

                val isExterior = (r == 0 || r == gridSize - 1 || c == 0 || c == gridSize - 1)
                val wallLayer = if (isExterior) "WALLS_EXT" else "WALLS_INT"

                entities.add(DxfLine(Point2D(x0 + 8f, y0), Point2D(x1 - 8f, y0), layer = wallLayer))
                entities.add(DxfLine(Point2D(x0, y0 + 8f), Point2D(x0, y1 - 8f), layer = wallLayer))

                if ((r + c) % 2 == 0) {
                    val dx = x0 + 12f
                    val dy = y0 + 12f
                    entities.add(DxfLwPolyline(
                        listOf(
                            Point2D(dx, dy),
                            Point2D(dx + 16f, dy),
                            Point2D(dx + 16f, dy + 10f),
                            Point2D(dx, dy + 10f)
                        ),
                        isClosed = true,
                        layer = "WALLS_INT"
                    ))
                    entities.add(DxfCircle(Point2D(dx + 8f, dy + 15f), 3f, layer = "WALLS_INT"))
                    entities.add(DxfText("U-${r + 1}0${c + 1}", Point2D(dx + 1f, dy - 4f), height = 3f, layer = "ANNOTATIONS"))
                } else {
                    val mx = (x0 + x1) / 2f
                    val my = (y0 + y1) / 2f

                    entities.add(DxfCircle(Point2D(mx, my), 5f, layer = "HVAC_DUCTS"))
                    entities.add(DxfCircle(Point2D(mx, my), 2.5f, layer = "HVAC_DUCTS"))
                    entities.add(DxfLine(Point2D(x0 + 5f, my), Point2D(x1 - 5f, my), layer = "HVAC_DUCTS"))
                    entities.add(DxfArc(Point2D(mx, my + 8f), 4f, 0f, 180f, layer = "ELECTRICAL"))
                    entities.add(DxfLine(Point2D(mx - 4f, my + 8f), Point2D(mx + 4f, my + 8f), layer = "ELECTRICAL"))
                }
            }
        }

        val minCoord = -originOffset - 35f
        val maxCoord = originOffset + 35f

        return DxfDocument(
            fileName = "Enterprise_CityGrid_3500_Entities.dxf",
            layers = layers,
            entities = entities,
            extents = BoundingBox(minCoord, minCoord, maxCoord, maxCoord)
        )
    }
}
