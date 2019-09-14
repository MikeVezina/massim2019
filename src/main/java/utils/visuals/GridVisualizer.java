package utils.visuals;

import com.google.gson.Gson;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import eis.messages.GsonInstance;
import eis.messages.MQReceiver;
import eis.messages.Message;
import eis.percepts.MapPercept;
import org.apache.tools.ant.taskdefs.Available;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.*;
import org.newdawn.slick.tiled.TiledMap;
import utils.Position;

import java.util.Collection;

public class GridVisualizer extends BasicGame implements DeliverCallback {

    private static final int ROWS = 80;
    private static final int COLS = 80;


    public CustomPanel[][] map;
    private MQReceiver mqReceiver;
    private Position currentAgentPosition;
    private int currentStep;

    // The panel that is selected by the mouse
    private CustomPanel currentPanel;

    public GridVisualizer(String agentName) {
        super(agentName);

    }

    @Override
    public void init(GameContainer container) throws SlickException {
        container.setTargetFrameRate(60);
        container.setAlwaysRender(true);
        mqReceiver = new MQReceiver(this.getTitle(), this);
        resetFrame();
    }

    @Override
    public void update(GameContainer container, int delta) throws SlickException {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == null)
                    System.out.println("null: " + i + ", " + j);
                else
                    map[i][j].updatePanel();
            }
        }

        // Only update the panel if the mouse is grabbed
        currentPanel = getMouseHoverPanel(container);
    }

    private CustomPanel getMouseHoverPanel(GameContainer container) {
        if (!Mouse.isInsideWindow())
            return null;

        int mouseX = Mouse.getX();
        int mouseY = container.getHeight() - Mouse.getY();

        int panelX = Math.min(mouseX / CustomPanel.WIDTH, COLS - 1);
        int panelY = Math.min(mouseY / CustomPanel.HEIGHT, ROWS - 1);
        return map[panelX][panelY];
    }

    @Override
    public void render(GameContainer container, Graphics g) throws SlickException {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                CustomPanel currentPanel = map[i][j];
                currentPanel.draw(g);
            }
        }

        resetDebugStringPosition();

        // Draw Overlay
        g.setColor(Color.white);
        writeDebugString(g, "Current Step: " + getCurrentStep());
        writeDebugString(g, "----------");
        if (currentPanel != null) {
            writeDebugString(g, "Current Cell Info:");

            if (currentPanel.getPercept() == null) {
                writeDebugString(g, "No Percept Available.");
                return;
            }
            writeDebugString(g, "Absolute Location: " + currentPanel.getPercept().getLocation());
            writeDebugString(g, "Perceived By: " + currentPanel.getPercept().getAgentSource());
            writeDebugString(g, "Last Step Perceived: " + currentPanel.getPercept().getLastStepPerceived());

            writeDebugString(g, "Terrain Info: " + currentPanel.getPercept().getTerrain().toString());
            writeDebugString(g, "Thing Info: " + currentPanel.getPercept().getThingList());

        }
    }

    private int startingY;

    private void resetDebugStringPosition() {
        startingY = 50;
    }


    private void writeDebugString(Graphics g, String debugInfo) {
        g.drawString(debugInfo, 10, startingY);
        startingY += 20;
    }

    @Override
    public boolean closeRequested() {
        mqReceiver.close();
        return super.closeRequested();
    }

    private void resetFrame() {
        currentAgentPosition = new Position();
        map = new CustomPanel[ROWS][COLS];

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                CustomPanel newPanel = new CustomPanel(this, new Position(i * CustomPanel.WIDTH, j * CustomPanel.HEIGHT));
                map[i][j] = newPanel;
            }
        }
    }

    public boolean isAgentCell(MapPercept percept) {
        return percept != null && percept.getLocation().equals(currentAgentPosition);
    }

    public void updateGridLocation(MapPercept percept) {
        if (percept == null) {
            return;
        }
        Position translated = percept.getLocation().add(new Position(ROWS / 2, COLS / 2));
        updatePanel(translated, percept);
    }

    private void updatePanel(Position panelPosition, MapPercept percept) {
        if (panelPosition.getX() >= map.length || panelPosition.getY() >= map.length || panelPosition.getX() < 0 || panelPosition.getY() < 0)
            return;

        try {
            CustomPanel panel = map[panelPosition.getX()][panelPosition.getY()];
            panel.setPercept(percept);
        } catch (NullPointerException npe) {
            System.out.println("Test!!!!");
            throw npe;
        }
    }

    @Override
    public void handle(String consumerTag, Delivery message) {
        Gson gson = GsonInstance.getInstance();
        String msgBodyString = new String(message.getBody());
        if (message.getProperties().getContentType().equals(Message.CONTENT_TYPE_LOCATION)) {
            this.setAgentPosition(gson.fromJson(msgBodyString, Position.class));
        } else if (message.getProperties().getContentType().equals(Message.CONTENT_TYPE_RESET)) {
            resetFrame();
        } else if (message.getProperties().getContentType().equals(Message.CONTENT_TYPE_PERCEPT)) {
            Collection<MapPercept> perceptChunk = gson.fromJson(msgBodyString, Message.MAP_PERCEPT_LIST_TYPE);
            perceptChunk.forEach(this::updateGridLocation);
        } else if (message.getProperties().getContentType().equals(Message.CONTENT_TYPE_NEW_STEP)) {
            String stepString = new String(message.getBody());
            int stepInt = Integer.parseInt(stepString);
            this.setCurrentStep(stepInt);
        } else {
            System.out.println("Unknown Message Content type. Content Type: " + message.getProperties().getContentType());
        }
    }

    private void setAgentPosition(Position fromJson) {
        this.currentAgentPosition = fromJson;
    }

    private void setCurrentStep(int stepInt) {
        this.currentStep = stepInt;
    }

    public int getCurrentStep() {
        return this.currentStep;
    }

    public static void main(String[] args) throws SlickException {
        if (args.length != 1) {
            System.out.println("Invalid Arguments. Missing Agent name.");
            System.out.println("Usage: GridVisualizer [agent-name]+");
            return;
        }

        String agentName = args[0];


        AppGameContainer appGameContainer = new AppGameContainer(new GridVisualizer(agentName));
        appGameContainer.setDisplayMode(GridVisualizer.COLS * CustomPanel.WIDTH, GridVisualizer.ROWS * CustomPanel.HEIGHT, false);
        appGameContainer.start();


//        new GridVisualizer(agentName);
    }
}
