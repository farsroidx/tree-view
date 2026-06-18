package ir.farsroidx.icons

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class IconWithoutTooltip(private val delegate: Icon) : Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        delegate.paintIcon(c, g, x, y)
    }

    override fun getIconWidth(): Int = delegate.iconWidth

    override fun getIconHeight(): Int = delegate.iconHeight
}