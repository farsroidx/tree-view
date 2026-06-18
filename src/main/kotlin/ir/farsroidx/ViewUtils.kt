@file:Suppress("unused")

package ir.farsroidx

import com.intellij.ui.icons.IconWrapperWithToolTip
import ir.farsroidx.icons.IconWithoutTooltip
import javax.swing.Icon

fun Icon.withoutTooltip(): Icon = IconWithoutTooltip(this)

fun Icon.withTooltip(tooltip: String): Icon = IconWrapperWithToolTip(this) { tooltip }