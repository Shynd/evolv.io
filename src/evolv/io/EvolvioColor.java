package evolv.io;

import java.util.Arrays;
import java.util.List;

import evolv.io.peripherals.MouseAction;
import evolv.io.peripherals.MouseButton;
import evolv.io.peripherals.Peripherals;
import evolv.io.renderers.BlankBoardRenderer;
import evolv.io.renderers.Renderer;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

public class EvolvioColor extends PApplet {
	private enum DragMode{
		NONE,
		SCREEN,
		MIN_TEMPERATURE,
		MAX_TEMPERATURE,
	}
	private static final List<BoardAction> BOARD_ACTIONS = Arrays.asList(new BoardAction.ToggleUserControl(),
			new BoardAction.ChangeSpawnChance(), new BoardAction.PrepareForFileSave(0),
			new BoardAction.ChangeImageSaveInterval(), new BoardAction.PrepareForFileSave(2),
			new BoardAction.ChangeTextSaveInterval(), new BoardAction.ChangePlaySpeed(),
			new BoardAction.ToggleRender());

	private final int seed = parseInt(random(1000000));
	private final Peripherals peripherals = new Peripherals();
	private final Renderer<Board> blankBoardRenderer = new BlankBoardRenderer();

	private Board evoBoard;
	private float scaleFactor;
	private int windowWidth;
	private int windowHeight;
	private float cameraX = Configuration.BOARD_WIDTH * 0.5f;
	private float cameraY = Configuration.BOARD_HEIGHT * 0.5f;
	private float cameraR;
	private float zoom = 1;

	private DragMode dragMode;
	private float prevMouseX;
	private float prevMouseY;
	private boolean draggedFar = false;

	public static void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "evolv.io.EvolvioColor" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}

	@Override
	public void settings() {
		// Get users window size
		this.windowWidth = displayWidth;
		this.windowHeight = displayHeight;

		// Set scaling to be custom to current users screen size
		this.scaleFactor = ((float) windowHeight) / Configuration.BOARD_HEIGHT / Configuration.SCALE_TO_FIXBUG;

		// Allow window to be docked and resized the UI still needs to be
		// changed to make UI look good after resize
		size(windowWidth, windowHeight);
	}

	@Override
	public void setup() {
		surface.setResizable(true);
		colorMode(HSB, 1.0f);
		PFont font = createDefaultFont(12);
		textFont(font);
		this.evoBoard = new Board(this, seed);
		resetZoom();
		peripherals.onMouse(MouseButton.NONE, MouseAction.WHEEL, this::zoom);
		peripherals.onMouse(MouseButton.LEFT, MouseAction.PRESS, this::mouseDown);
		peripherals.onMouse(MouseButton.LEFT, MouseAction.RELEASE, this::mouseUp);
	}

	@Override
	public void draw() {
	   update();
	   respondToUser();
	   render();
	}

	private void update(){
		for (int iteration = 0; iteration < evoBoard.getPlaySpeed(); iteration++) {
			evoBoard.iterate(Configuration.TIME_STEP);
		}
	}

	private void respondToUser(){
		if (dist(prevMouseX, prevMouseY, mouseX, mouseY) > 5) {
			draggedFar = true;
		}
		if (dragMode == DragMode.SCREEN) {
			cameraX -= toWorldXCoordinate(mouseX, mouseY) - toWorldXCoordinate(prevMouseX, prevMouseY);
			cameraY -= toWorldYCoordinate(mouseX, mouseY) - toWorldYCoordinate(prevMouseX, prevMouseY);
		} else if (dragMode == DragMode.MIN_TEMPERATURE) { // UGLY UGLY CODE. Do not look at this
			if (evoBoard.setMinTemperature(1.0f - (mouseY - 30) / 660.0f)) {
				dragMode = DragMode.MAX_TEMPERATURE;
			}
		} else if (dragMode == DragMode.MAX_TEMPERATURE) {
			if (evoBoard.setMaxTemperature(1.0f - (mouseY - 30) / 660.0f)) {
				dragMode = DragMode.MIN_TEMPERATURE;
			}
		}
		if (evoBoard.getSelectedCreature() != null) {
			cameraX = (float) evoBoard.getSelectedCreature().getPx();
			cameraY = (float) evoBoard.getSelectedCreature().getPy();
			if (evoBoard.isUserControl()) cameraR = -PI / 2.0f - (float) evoBoard.getSelectedCreature().getRotation();
		} else {
			cameraR = 0;
		}
	}

	private void render(){
		pushMatrix();
		scale(scaleFactor);
		blankBoardRenderer.render(this, evoBoard);
		translate(Configuration.BOARD_WIDTH * 0.5f * Configuration.SCALE_TO_FIXBUG,
				Configuration.BOARD_HEIGHT * 0.5f * Configuration.SCALE_TO_FIXBUG);
		scale(zoom);
		if (evoBoard.isUserControl() && evoBoard.getSelectedCreature() != null) {
			rotate(cameraR);
		}
		translate(-cameraX * Configuration.SCALE_TO_FIXBUG, -cameraY * Configuration.SCALE_TO_FIXBUG);
		evoBoard.drawBoard(Configuration.SCALE_TO_FIXBUG, zoom, (int) toWorldXCoordinate(mouseX, mouseY),
				(int) toWorldYCoordinate(mouseX, mouseY));
		popMatrix();
		evoBoard.drawUI(Configuration.SCALE_TO_FIXBUG, zoom, Configuration.TIME_STEP, windowHeight, 0, windowWidth,
				windowHeight);

		//evoBoard.fileSave();
		prevMouseX = mouseX;
		prevMouseY = mouseY;
	}

	@Override
	protected void handleMouseEvent(MouseEvent mouseEvent) {
		super.handleMouseEvent(mouseEvent);
		peripherals.handleMouseEvent(mouseEvent);
	}

	@Override
	protected void handleKeyEvent(KeyEvent keyEvent) {
		super.handleKeyEvent(keyEvent);
		peripherals.handleKeyEvent(keyEvent);
	}

	private void zoom(MouseEvent event) {
		float delta = event.getCount();
		if (delta >= 0.5f) {
			setZoom(zoom * 0.90909f, mouseX, mouseY);
		} else if (delta <= -0.5f) {
			setZoom(zoom * 1.1f, mouseX, mouseY);
		}
	}

	private void mouseDown(MouseEvent event) {
		if (mouseX < windowHeight) {
			dragMode = DragMode.SCREEN;
		} else {
			if (abs(mouseX - (windowHeight + 65)) <= 60 && abs(mouseY - 147) <= 60
					&& evoBoard.getSelectedCreature() != null) {
				cameraX = (float) evoBoard.getSelectedCreature().getPx();
				cameraY = (float) evoBoard.getSelectedCreature().getPy();
				zoom = 16;
			} else if (mouseY >= 95 && mouseY < 135 && evoBoard.getSelectedCreature() == null) {
				if (mouseX >= windowHeight + 10 && mouseX < windowHeight + 230) {
					resetZoom();
				} else if (mouseX >= windowHeight + 240 && mouseX < windowHeight + 460) {
					if (mouseButton == LEFT) {
						evoBoard.incrementSortMetric();
					} else if (mouseButton == RIGHT) {
						evoBoard.decrementSortMetric();
					}
				}
			} else if (mouseY >= 570) {
				float x = (mouseX - (windowHeight + 10));
				float y = (mouseY - 570);
				boolean clickedOnLeft = (x % 230 < 110);
				if (x >= 0 && x < 460 && y >= 0 && y < 200 && x % 230 < 220 && y % 50 < 40) {
					// 460 = 2 * 230 and 200 = 4 * 50
					int mX = (int) (x / 230);
					int mY = (int) (y / 50);
					int buttonNum = mX + mY * 2;
					BOARD_ACTIONS.get(buttonNum).doAction(evoBoard, clickedOnLeft);
				}
			} else if (mouseX >= height + 10 && mouseX < width - 50 && evoBoard.getSelectedCreature() == null) {
				int listIndex = (mouseY - 150) / 70;
				if (evoBoard.getCreatureInList(listIndex) != null) {
					evoBoard.setSelectedCreature(evoBoard.getCreatureInList(listIndex));
					cameraX = (float) evoBoard.getSelectedCreature().getPx();
					cameraY = (float) evoBoard.getSelectedCreature().getPy();
					zoom = 16;
				}
			}
			if (mouseX >= width - 50) {
				float toClickTemp = (mouseY - 30) / 660.0f;
				float lowTemp = 1.0f - evoBoard.getLowTempProportion();
				float highTemp = 1.0f - evoBoard.getHighTempProportion();
				if (abs(toClickTemp - lowTemp) < abs(toClickTemp - highTemp)) {
					dragMode = DragMode.MIN_TEMPERATURE;
				} else {
					dragMode = DragMode.MAX_TEMPERATURE;
				}
			}
		}
		draggedFar = false;
	}

	private void mouseUp(MouseEvent event) {
		if (!draggedFar) {
			if (mouseX < windowHeight) {
				// DO NOT LOOK AT THIS CODE EITHER it is bad
				dragMode = DragMode.SCREEN;
				float mX = toWorldXCoordinate(mouseX, mouseY);
				float mY = toWorldYCoordinate(mouseX, mouseY);
				int x = floor(mX);
				int y = floor(mY);
				evoBoard.unselect();
				cameraR = 0;
				if (x >= 0 && x < Configuration.BOARD_WIDTH && y >= 0 && y < Configuration.BOARD_HEIGHT) {
					for (SoftBody body : evoBoard.getSoftBodiesInPosition(x, y)) {
						if (body instanceof Creature) {
							float distance = dist(mX, mY, (float) body.getPx(), (float) body.getPy());
							if (distance <= body.getRadius()) {
								evoBoard.setSelectedCreature((Creature) body);
								zoom = 16;
							}
						}
					}
				}
			}
		}
		dragMode = DragMode.NONE;
	}

	public void resetZoom() {
		cameraX = Configuration.BOARD_WIDTH * 0.5f;
		cameraY = Configuration.BOARD_HEIGHT * 0.5f;
		zoom = 1;
	}

	public void setZoom(float target, float x, float y) {
		float grossX = grossify(x, Configuration.BOARD_WIDTH);
		cameraX -= (grossX / target - grossX / zoom);
		float grossY = grossify(y, Configuration.BOARD_HEIGHT);
		cameraY -= (grossY / target - grossY / zoom);
		zoom = target;
	}

	public float grossify(float input, float total) { // Very weird function
		return (input / scaleFactor - total * 0.5f * Configuration.SCALE_TO_FIXBUG) / Configuration.SCALE_TO_FIXBUG;
	}

	public float toWorldXCoordinate(float x, float y) {
		float w = windowHeight / 2;
		float angle = atan2(y - w, x - w);
		float dist = dist(w, w, x, y);
		return cameraX + grossify(cos(angle - cameraR) * dist + w, Configuration.BOARD_WIDTH) / zoom;
	}

	public float toWorldYCoordinate(float x, float y) {
		float w = windowHeight / 2;
		float angle = atan2(y - w, x - w);
		float dist = dist(w, w, x, y);
		return cameraY + grossify(sin(angle - cameraR) * dist + w, Configuration.BOARD_HEIGHT) / zoom;
	}
}
