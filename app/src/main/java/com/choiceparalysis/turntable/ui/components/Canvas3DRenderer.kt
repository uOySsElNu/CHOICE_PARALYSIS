package com.choiceparalysis.turntable.ui.components

import kotlin.math.cos
import kotlin.math.sin

class Canvas3DRenderer {
    data class Vertex3D(val x: Float, val y: Float, val z: Float)
    data class Point2D(val x: Float, val y: Float)

    fun project(vertex: Vertex3D, cameraDistance: Float): Point2D {
        val factor = cameraDistance / (cameraDistance + vertex.z)
        return Point2D(
            x = vertex.x * factor,
            y = vertex.y * factor
        )
    }

    fun rotate(vertex: Vertex3D, rotX: Float, rotY: Float, rotZ: Float): Vertex3D {
        // Rotate around X axis
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val y1 = vertex.y * cosX - vertex.z * sinX
        val z1 = vertex.y * sinX + vertex.z * cosX

        // Rotate around Y axis
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val x2 = vertex.x * cosY + z1 * sinY
        val z2 = -vertex.x * sinY + z1 * cosY

        // Rotate around Z axis
        val cosZ = cos(rotZ)
        val sinZ = sin(rotZ)
        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ

        return Vertex3D(x3, y3, z2)
    }

    fun getCubeVertices(size: Float): List<Vertex3D> {
        val half = size / 2f
        return listOf(
            Vertex3D(-half, -half, -half), // 0: front-top-left
            Vertex3D(half, -half, -half),  // 1: front-top-right
            Vertex3D(half, half, -half),   // 2: front-bottom-right
            Vertex3D(-half, half, -half),  // 3: front-bottom-left
            Vertex3D(-half, -half, half),  // 4: back-top-left
            Vertex3D(half, -half, half),   // 5: back-top-right
            Vertex3D(half, half, half),    // 6: back-bottom-right
            Vertex3D(-half, half, half)    // 7: back-bottom-left
        )
    }

    fun getCubeFaces(): List<List<Int>> {
        return listOf(
            listOf(0, 1, 2, 3), // front
            listOf(5, 4, 7, 6), // back
            listOf(4, 0, 3, 7), // left
            listOf(1, 5, 6, 2), // right
            listOf(4, 5, 1, 0), // top
            listOf(3, 2, 6, 7)  // bottom
        )
    }
}
