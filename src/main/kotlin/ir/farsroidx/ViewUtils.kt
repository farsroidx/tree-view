@file:Suppress("unused")

package ir.farsroidx

import com.intellij.ui.icons.IconWrapperWithToolTip
import ir.farsroidx.icons.IconWithoutTooltip
import javax.swing.Icon

/**
 * Wraps this icon while preserving its painting and dimensions.
 *
 * @return a painting-compatible icon without the delegate tooltip
 */
fun Icon.withoutTooltip(): Icon = IconWithoutTooltip(this)

/**
 * Wraps this icon with the supplied tooltip text.
 *
 * @param tooltip the tooltip text
 *
 * @return an icon exposing the supplied tooltip
 */
fun Icon.withTooltip(tooltip: String): Icon = IconWrapperWithToolTip(this) { tooltip }