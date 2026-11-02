package com.prof18.feedflow.desktop.testing

import dev.sebastiano.spectre.core.AutomatorNode
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.InternalSpectreApi
import javax.swing.SwingUtilities

@OptIn(InternalSpectreApi::class)
internal suspend fun ComposeAutomator.awaitVisibleNode(tag: String): AutomatorNode {
    waitForNode(tag = tag)
    var lastSurfaceState = "No window snapshot captured"
    try {
        waitUntil(description = "Native surface for $tag is visible") {
            var ready = false
            SwingUtilities.invokeAndWait {
                val surfaces = windows()
                lastSurfaceState = surfaces.joinToString { surface ->
                    val node = surface.findOneByTestTag(tag)
                    "surface=${surface.surfaceId} showing=${surface.trackedWindow.window.isShowing} " +
                        "host=${surface.trackedWindow.composeSurfaceBoundsOnScreen} node=${node?.boundsOnScreen}"
                }
                ready = surfaces.any { surface ->
                    val node = surface.findOneByTestTag(tag)
                    node != null && surface.trackedWindow.window.isShowing &&
                        surface.trackedWindow.composeSurfaceBoundsOnScreen.contains(node.centerOnScreen)
                }
            }
            ready
        }
    } catch (failure: IllegalStateException) {
        println("Native visibility failure: tag=$tag $lastSurfaceState")
        throw failure
    }
    val node = checkNotNull(findOneByTestTag(tag))
    val host = tree().windows().single { it.surfaceId == node.surfaceId }.trackedWindow
    println("Native gesture: tag=$tag node=${node.boundsOnScreen} host=${host.composeSurfaceBoundsOnScreen}")
    return node
}
