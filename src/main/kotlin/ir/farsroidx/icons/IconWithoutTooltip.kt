package ir.farsroidx.icons

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * Delegates icon rendering and dimensions without exposing the delegate tooltip.
 *
 * @param delegate the icon supplying painting and dimensions
 */
class IconWithoutTooltip(private val delegate: Icon) : Icon {

    /**
     * Paints the delegate icon at the supplied component coordinates.
     *
     * @param c the component receiving the icon
     * @param g the painting graphics
     * @param x the horizontal painting coordinate
     * @param y the vertical painting coordinate
     */
    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        delegate.paintIcon(c, g, x, y)
    }

    /**
     * Returns the delegate icon width in pixels.
     *
     * @return the delegate width in pixels
     */
    override fun getIconWidth(): Int = delegate.iconWidth

    /**
     * Returns the delegate icon height in pixels.
     *
     * @return the delegate height in pixels
     */
    override fun getIconHeight(): Int = delegate.iconHeight
}