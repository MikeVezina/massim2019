package utils.visuals;

import eis.percepts.MapPercept;
import eis.percepts.terrain.ForbiddenCell;
import eis.percepts.terrain.FreeSpace;
import eis.percepts.terrain.Goal;
import eis.percepts.terrain.Obstacle;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.ShapeFill;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.Color;
import org.newdawn.slick.geom.ShapeRenderer;
import utils.Position;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;


public class CustomPanel extends Rectangle {
    private Logger logger = Logger.getLogger("CustomPanel");
    private MapPercept currentPercept;
    public static final int HEIGHT = 10;
    public static final int WIDTH = 10;
    private Color background;
    private Color border;
    private GridVisualizer gridVisualizer;


    public CustomPanel(GridVisualizer gridVisualizer, Position pos) {
        super(pos.getX(), pos.getY(), WIDTH, HEIGHT);
        this.gridVisualizer = gridVisualizer;
        this.background = Color.gray;
        this.border = Color.black;
        border = getBorder();
    }


    private Color getBackground() {
        if (currentPercept == null) {
            return Color.gray;
        }

        if (gridVisualizer.isAgentCell(currentPercept))
            return Color.yellow;

//        if(lastPercept.isExpired(agentMap.getLastUpdateStep()))
//            return Color.GRAY;


        if (currentPercept.hasTeamEntity() && currentPercept.hasEnemyEntity())
            return Color.magenta;
        else if (currentPercept.hasTeamEntity())
            return Color.orange;
        else if (currentPercept.hasEnemyEntity())
            return Color.red;

        if (currentPercept.hasBlock())
            return Color.green;


        if (currentPercept.hasDispenser())
            return Color.blue;
        if (currentPercept.getTerrain() instanceof Obstacle)
            return Color.black;
        if (currentPercept.getTerrain() instanceof ForbiddenCell)
            return Color.cyan;
        if (currentPercept.getTerrain() instanceof Goal)
            return Color.pink;
        if (currentPercept.getTerrain() instanceof FreeSpace && gridVisualizer.getCurrentStep() == currentPercept.getLastStepPerceived())
            return Color.white;
        if (currentPercept.getTerrain() instanceof FreeSpace)
            return Color.lightGray;

        return Color.lightGray;
    }


    public void setPercept(MapPercept percept) {
        currentPercept = percept;
    }

    private Color getBorder() {
        if (currentPercept != null && currentPercept.getLocation().equals(Position.ZERO))
            return Color.green;
//        else
//            return Color.BLACK;
        return Color.black;
    }

    public void updatePanel() {
        this.background = getBackground();
        this.border = getBorder();
    }

    public void draw(Graphics g) {
        g.setColor(background);
        g.fill(this);

//         Draw border
        g.setColor(border);
        g.draw(this);
    }

    public void setBorderColor(Color color) {
        this.border = color;
    }

    public MapPercept getPercept() {
        return currentPercept;
    }
}
