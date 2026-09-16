package com.example.measurement

import com.example.parser.*
import kotlin.math.*

data class BoundaryResult(
    val polygonVertices: List<Point2D>,
    val area: Float,
    val perimeter: Float,
    val sourceDescription: String
)

object BoundaryDetector {

    /**
     * Finds or extracts an enclosed boundary around a seed point.
     * 1. Checks if seed point lies inside a closed circle or closed polyline.
     * 2. Otherwise finds nearby connected segments forming a closed polygon loop enclosing or nearest the point.
     */
    fun detectBoundary(seed: Point2D, entities: List<DxfEntity>): BoundaryResult? {
        // 1. Direct enclosing circles
        for (entity in entities) {
            if (entity is DxfCircle) {
                val d = seed.distanceTo(entity.center)
                if (d <= entity.radius) {
                    val samplePoints = mutableListOf<Point2D>()
                    val count = 36
                    for (i in 0 until count) {
                        val rad = 2 * Math.PI * i / count
                        samplePoints.add(
                            Point2D(
                                (entity.center.x + entity.radius * cos(rad)).toFloat(),
                                (entity.center.y + entity.radius * sin(rad)).toFloat()
                            )
                        )
                    }
                    val area = (Math.PI * entity.radius * entity.radius).toFloat()
                    val perim = (2 * Math.PI * entity.radius).toFloat()
                    return BoundaryResult(samplePoints, area, perim, "Circle Boundary (R=%.2f)".format(entity.radius))
                }
            }
        }

        // 2. Direct closed polylines
        for (entity in entities) {
            when (entity) {
                is DxfLwPolyline -> {
                    if (entity.isClosed && entity.vertices.size >= 3) {
                        if (isPointInPolygon(seed, entity.vertices)) {
                            val area = computePolygonArea(entity.vertices)
                            val perim = computePerimeter(entity.vertices, isClosed = true)
                            return BoundaryResult(entity.vertices, area, perim, "Closed Polyline Boundary (${entity.vertices.size} vertices)")
                        }
                    }
                }
                is DxfPolyline -> {
                    if (entity.isClosed && entity.vertices.size >= 3) {
                        if (isPointInPolygon(seed, entity.vertices)) {
                            val area = computePolygonArea(entity.vertices)
                            val perim = computePerimeter(entity.vertices, isClosed = true)
                            return BoundaryResult(entity.vertices, area, perim, "Closed Polyline Boundary (${entity.vertices.size} vertices)")
                        }
                    }
                }
                else -> {}
            }
        }

        // 3. Fallback: Find nearest closed polygon formed by lines/segments
        val segments = mutableListOf<Pair<Point2D, Point2D>>()
        for (e in entities) {
            when (e) {
                is DxfLine -> segments.add(Pair(e.start, e.end))
                is DxfLwPolyline -> {
                    for (i in 0 until e.vertices.size - 1) {
                        segments.add(Pair(e.vertices[i], e.vertices[i + 1]))
                    }
                    if (e.isClosed && e.vertices.size > 2) {
                        segments.add(Pair(e.vertices.last(), e.vertices.first()))
                    }
                }
                else -> {}
            }
        }

        val loop = findEnclosingLoop(seed, segments)
        if (loop != null && loop.size >= 3) {
            val area = computePolygonArea(loop)
            val perim = computePerimeter(loop, isClosed = true)
            return BoundaryResult(loop, area, perim, "Detected CAD Loop (${loop.size} vertices)")
        }

        return null
    }

    fun isPointInPolygon(p: Point2D, poly: List<Point2D>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in 0 until poly.size) {
            val pi = poly[i]
            val pj = poly[j]
            if ((pi.y > p.y) != (pj.y > p.y) &&
                (p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y + 0.000001f) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    fun computePolygonArea(vertices: List<Point2D>): Float {
        if (vertices.size < 3) return 0f
        var sum = 0.0
        val n = vertices.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += (vertices[i].x.toDouble() * vertices[j].y.toDouble()) - (vertices[j].x.toDouble() * vertices[i].y.toDouble())
        }
        return abs(sum / 2.0).toFloat()
    }

    fun computePerimeter(vertices: List<Point2D>, isClosed: Boolean): Float {
        if (vertices.size < 2) return 0f
        var total = 0f
        for (i in 0 until vertices.size - 1) {
            total += vertices[i].distanceTo(vertices[i + 1])
        }
        if (isClosed && vertices.size > 2) {
            total += vertices.last().distanceTo(vertices.first())
        }
        return total
    }

    private fun findEnclosingLoop(seed: Point2D, segments: List<Pair<Point2D, Point2D>>): List<Point2D>? {
        // Collect vertices close to seed
        val nearbySegs = segments.filter { seg ->
            val mid = Point2D((seg.first.x + seg.second.x) / 2f, (seg.first.y + seg.second.y) / 2f)
            mid.distanceTo(seed) < 2000f
        }
        if (nearbySegs.size < 3) return null

        // Chain segments
        val adj = mutableMapOf<Point2D, MutableList<Point2D>>()
        fun addEdge(u: Point2D, v: Point2D) {
            val uKey = snapVertex(u)
            val vKey = snapVertex(v)
            adj.getOrPut(uKey) { mutableListOf() }.add(vKey)
            adj.getOrPut(vKey) { mutableListOf() }.add(uKey)
        }

        for (s in nearbySegs) {
            addEdge(s.first, s.second)
        }

        // Try to trace simple loops
        val visited = mutableSetOf<Point2D>()
        for (startNode in adj.keys) {
            val path = mutableListOf<Point2D>()
            if (findLoopDfs(startNode, startNode, null, adj, visited, path, maxDepth = 12)) {
                if (path.size >= 3 && isPointInPolygon(seed, path)) {
                    return path
                }
            }
        }
        return null
    }

    private fun snapVertex(p: Point2D, tol: Float = 0.5f): Point2D {
        val rx = (p.x / tol).roundToInt() * tol
        val ry = (p.y / tol).roundToInt() * tol
        return Point2D(rx, ry)
    }

    private fun findLoopDfs(
        start: Point2D,
        current: Point2D,
        parent: Point2D?,
        adj: Map<Point2D, List<Point2D>>,
        visited: MutableSet<Point2D>,
        path: MutableList<Point2D>,
        maxDepth: Int
    ): Boolean {
        path.add(current)
        if (path.size > maxDepth) {
            path.removeAt(path.lastIndex)
            return false
        }

        val neighbors = adj[current] ?: emptyList()
        for (next in neighbors) {
            if (next == parent) continue
            if (next == start && path.size >= 3) {
                return true
            }
            if (!path.contains(next)) {
                if (findLoopDfs(start, next, current, adj, visited, path, maxDepth)) {
                    return true
                }
            }
        }
        path.removeAt(path.lastIndex)
        return false
    }
}
