package evolv.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Creature extends SoftBody {
	private static final List<CreatureAction> CREATURE_ACTIONS = Arrays.asList(new CreatureAction.AdjustHue(),
			new CreatureAction.Accelerate(), new CreatureAction.Rotate(), new CreatureAction.Eat(),
			new CreatureAction.Fight(), new CreatureAction.Reproduce(), new CreatureAction.AdjustMouthHue());

	private final EvolvioColor evolvioColor;

	private final double[] previousEnergy = new double[Configuration.ENERGY_HISTORY_LENGTH];

	// Family
	private final String name;
	private final String parents;
	private final int gen;
	private final int id;
	
	// Vision or View or Preference
	private final List<Eye> eyes = new ArrayList<>();
	private final double visionResults[] = new double[Configuration.NUM_EYES * 3];

	private final Brain brain;

	// Misc or Unsorted
	private float preferredRank = 8;
	private double mouthHue;
	private double vr;
	private double rotation;

	// TODO can the size of these constructors be reduced?

	public Creature(EvolvioColor evolvioColor, Board board) {
		this(evolvioColor, board, evolvioColor.random(0, Configuration.BOARD_WIDTH),
				evolvioColor.random(0, board.getBoardHeight()), 0, 0,
				evolvioColor.random(Configuration.MINIMUM_CREATURE_ENERGY, Configuration.MAXIMUM_CREATURE_ENERGY), 1,
				evolvioColor.random(0, 1), 1, 1, evolvioColor.random(0, 2 * EvolvioColor.PI), 0, "", "[PRIMORDIAL]",
				true, null, 1, evolvioColor.random(0, 1), new double[Configuration.NUM_EYES], new double[Configuration.NUM_EYES]);
	}
	public Creature(EvolvioColor evolvioColor, Board board, double tpx, double tpy, double tvx, double tvy,
			double tenergy, double tdensity, double thue, double tsaturation, double tbrightness, double rot,
			double tvr, String tname, String tparents, boolean mutateName, Brain brain, int tgen, double tmouthHue, double[] teyeDistances, double[] teyeAngles) {
		super(evolvioColor, board, tpx, tpy, tvx, tvy, tenergy, tdensity, thue, tsaturation, tbrightness);
		this.evolvioColor = evolvioColor;

		if (brain == null) {
			brain = new Brain(this.evolvioColor, null, null);
		}
		this.brain = brain;
		this.rotation = rot;
		this.vr = tvr;
		this.id = board.getCreatureIdUpTo() + 1;
		this.name = createName(tname, mutateName);
		this.parents = tparents;
		board.incrementCreatureIdUpTo();
		this.gen = tgen;
		this.mouthHue = tmouthHue;
		
		for (int i = 0; i < Configuration.NUM_EYES; i++) { 
			if (teyeDistances[i] == 0.0f) {
				eyes.add(new Eye(evolvioColor, this, evolvioColor.random(-3.6f, 3.6f), evolvioColor.random(0, 2)));
			} else {
				eyes.add(new Eye(evolvioColor, this, teyeAngles[i], teyeDistances[i]));
			}
		}
	}

	private String createName(String tname, boolean mutateName) {
		if (tname.isEmpty()) {
			return NameGenerator.newName();
		}
		if (mutateName) {
			return NameGenerator.mutateName(tname);
		}
		return tname;
	}

	public void drawBrain(float scaleUp, int mX, int mY) {
		brain.draw(scaleUp, mX, mY);
	}

	public void useBrain(double timeStep, boolean useOutput) {
		double inputs[] = new double[Configuration.NUM_EYES * 3 + 2];
		for (int i = 0; i < Configuration.NUM_EYES * 3; i++) {
			inputs[i] = visionResults[i];
		}
		inputs[Configuration.NUM_EYES * 3] = getEnergy();
		inputs[Configuration.NUM_EYES * 3 + 1] = mouthHue;
		brain.input(inputs);

		if (useOutput) {
			double[] output = brain.outputs();
			for (int i = 0; i < CREATURE_ACTIONS.size(); i++) {
				CREATURE_ACTIONS.get(i).doAction(this, output[i], timeStep);
			}
		}
	}

	public void drawSoftBody(float scaleUp, float camZoom, boolean showVision) {
		this.evolvioColor.ellipseMode(EvolvioColor.RADIUS);
		double radius = getRadius();
		if (showVision && camZoom > Configuration.MAX_DETAILED_ZOOM) {
			for (Eye eye : eyes) {
				eye.drawVisionAngle(scaleUp);
			}
		}
		this.evolvioColor.noStroke();
		if (getFightLevel() > 0) {
			this.evolvioColor.fill(0, 1, 1, (float) (getFightLevel() * 0.8f));
			this.evolvioColor.ellipse((float) (getPx() * scaleUp), (float) (getPy() * scaleUp),
					(float) (Configuration.FIGHT_RANGE * radius * scaleUp),
					(float) (Configuration.FIGHT_RANGE * radius * scaleUp));
		}
		this.evolvioColor.strokeWeight(Configuration.CREATURE_STROKE_WEIGHT);
		this.evolvioColor.stroke(0, 0, 1);
		this.evolvioColor.fill(0, 0, 1);
		if (this == getBoard().getSelectedCreature()) {
			this.evolvioColor.ellipse((float) (getPx() * scaleUp), (float) (getPy() * scaleUp),
					(float) (radius * scaleUp + 1 + 75.0f / camZoom), (float) (radius * scaleUp + 1 + 75.0f / camZoom));
		}
		super.drawSoftBody(scaleUp);

		if (camZoom > Configuration.MAX_DETAILED_ZOOM) {
			drawMouth(getBoard(), scaleUp, radius, rotation, camZoom, mouthHue);
			if (showVision) {
				this.evolvioColor.fill(0, 0, 1);
				this.evolvioColor.textSize(0.2f * scaleUp);
				this.evolvioColor.textAlign(EvolvioColor.CENTER);
				this.evolvioColor.text(name, (float) (getPx() * scaleUp),
						(float) ((getPy() - getRadius() * 1.4f - 0.07f) * scaleUp));
			}
		}
	}

	public void drawMouth(Board board, float scaleUp, double radius, double rotation, float camZoom, double mouthHue) {
		this.evolvioColor.noFill();
		this.evolvioColor.strokeWeight(Configuration.CREATURE_STROKE_WEIGHT);
		this.evolvioColor.stroke(0, 0, 1);
		this.evolvioColor.ellipseMode(EvolvioColor.RADIUS);
		this.evolvioColor.ellipse((float) (getPx() * scaleUp), (float) (getPy() * scaleUp),
				Configuration.MINIMUM_SURVIVABLE_SIZE * scaleUp, Configuration.MINIMUM_SURVIVABLE_SIZE * scaleUp);
		this.evolvioColor.pushMatrix();
		this.evolvioColor.translate((float) (getPx() * scaleUp), (float) (getPy() * scaleUp));
		this.evolvioColor.scale((float) radius);
		this.evolvioColor.rotate((float) rotation);
		this.evolvioColor.strokeWeight((float) (Configuration.CREATURE_STROKE_WEIGHT / radius));
		this.evolvioColor.stroke(0, 0, 0);
		this.evolvioColor.fill((float) mouthHue, 1.0f, 1.0f);
		this.evolvioColor.ellipse(0.6f * scaleUp, 0, 0.37f * scaleUp, 0.37f * scaleUp);
		this.evolvioColor.popMatrix();
	}

	public void metabolize(double timeStep) {
		/*
		 * the older the more work necessary
		 */
		loseEnergy(getEnergy() * Configuration.METABOLISM_ENERGY * getAge() * timeStep);

		if (getEnergy() < Configuration.SAFE_SIZE) {
			returnToEarth();
			getBoard().removeCreature(this);
		}
	}

	public void accelerate(double amount, double timeStep) {
		double multiplied = amount * timeStep / getMass();
		setVx(getVx() + Math.cos(rotation) * multiplied);
		setVy(getVy() + Math.sin(rotation) * multiplied);
		if (amount >= 0) {
			loseEnergy(amount * Configuration.ACCELERATION_ENERGY * timeStep);
		} else {
			loseEnergy(Math.abs(amount * Configuration.ACCELERATION_BACKWARDS_ENERGY * timeStep));
		}
	}

	public void rotate(double amount, double timeStep) {
		vr += 0.04f * amount * timeStep / getMass();
		loseEnergy(Math.abs(amount * Configuration.TURN_ENERGY * getEnergy() * timeStep));
	}

	public Tile getRandomCoveredTile() {
		double radius = (float) getRadius();
		double choiceX = 0;
		double choiceY = 0;
		while (EvolvioColor.dist((float) getPx(), (float) getPy(), (float) choiceX, (float) choiceY) > radius) {
			choiceX = (Math.random() * 2 * radius - radius) + getPx();
			choiceY = (Math.random() * 2 * radius - radius) + getPy();
		}
		int x = xBound((int) choiceX);
		int y = yBound((int) choiceY);
		return getBoard().getTile(x, y);
	}

	public void eat(double attemptedAmount, double timeStep) {
		/*
		 * The faster you're moving, the less efficiently you can eat.
		 */
		double amount = attemptedAmount
				/ (1.0f + distance(0, 0, getVx(), getVy()) * Configuration.EAT_WHILE_MOVING_INEFFICIENCY_MULTIPLIER);
		if (amount < 0) {
			dropEnergy(-amount * timeStep);
			loseEnergy(-attemptedAmount * Configuration.EAT_ENERGY * timeStep);
		} else {
			Tile coveredTile = getRandomCoveredTile();
			// TODO can pow be replaced with something faster?
			double foodToEat = coveredTile.getFoodLevel()
					* (1 - Math.pow((1 - Configuration.EAT_SPEED), amount * timeStep));
			if (foodToEat > coveredTile.getFoodLevel()) {
				foodToEat = coveredTile.getFoodLevel();
			}
			coveredTile.removeFood(foodToEat, true);
			double foodDistance = Math.abs(coveredTile.getFoodType() - mouthHue);
			double multiplier = 1.0f - foodDistance / Configuration.FOOD_SENSITIVITY;
			if (multiplier >= 0) {
				addEnergy(foodToEat * multiplier);
			} else {
				loseEnergy(-foodToEat * multiplier);
			}
			loseEnergy(attemptedAmount * Configuration.EAT_ENERGY * timeStep);
		}
	}

	public void fight(double amount, double timeStep) {
		if (amount > 0 && getAge() >= Configuration.MATURE_AGE) {
			setFightLevel(amount);
			loseEnergy(getFightLevel() * Configuration.FIGHT_ENERGY * getEnergy() * timeStep);
			for (int i = 0; i < getColliders().size(); i++) {
				SoftBody collider = getColliders().get(i);
				if (collider instanceof Creature) {
					float distance = EvolvioColor.dist((float) getPx(), (float) getPy(), (float) collider.getPx(),
							(float) collider.getPy());
					double combinedRadius = getRadius() * Configuration.FIGHT_RANGE + collider.getRadius();
					if (distance < combinedRadius) {
						((Creature) collider).dropEnergy(getFightLevel() * Configuration.INJURED_ENERGY * timeStep);
					}
				}
			}
		} else {
			setFightLevel(0);
		}
	}

	public void loseEnergy(double energyLost) {
		if (energyLost > 0) {
			setEnergy(getEnergy() - energyLost);
		}
	}

	public void dropEnergy(double energyLost) {
		if (energyLost > 0) {
			energyLost = Math.min(energyLost, getEnergy());
			setEnergy(getEnergy() - energyLost);
			getRandomCoveredTile().addFood(energyLost, getHue(), true);
		}
	}

	public void see() {
		for (int k = 0; k < Configuration.NUM_EYES; k++) {
			eyes.get(k).see();
			visionResults[k * 3] = eyes.get(k).getEyeResult().hue;
			visionResults[k * 3 + 1] = eyes.get(k).getEyeResult().saturation;
			visionResults[k * 3 + 2] = eyes.get(k).getEyeResult().brightness;
		}
	}

	public double distance(double x1, double y1, double x2, double y2) {
		return (Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
	}

	public void addPVOs(int x, int y, List<SoftBody> PVOs) {
		if (x >= 0 && x < Configuration.BOARD_WIDTH && y >= 0 && y < getBoard().getBoardHeight()) {
			for (int i = 0; i < getBoard().getSoftBodiesInPosition(x, y).size(); i++) {
				SoftBody newCollider = getBoard().getSoftBodiesInPosition(x, y).get(i);
				if (!PVOs.contains(newCollider) && newCollider != this) {
					PVOs.add(newCollider);
				}
			}
		}
	}

	public void returnToEarth() {
		int pieces = 20;
		for (int i = 0; i < pieces; i++) {
			getRandomCoveredTile().addFood(getEnergy() / pieces, getHue(), true);
		}
		for (int x = getSBIPMinX(); x <= getSBIPMaxX(); x++) {
			for (int y = getSBIPMinY(); y <= getSBIPMaxY(); y++) {
				getBoard().getSoftBodiesInPosition(x, y).remove(this);
			}
		}
		if (getBoard().getSelectedCreature() == this) {
			getBoard().unselect();
		}
	}

	public void reproduce(double babySize, double timeStep) {
		int highestGen = 0;
		if (babySize >= 0) {
			List<Creature> parents = new ArrayList<Creature>(0);
			parents.add(this);
			double availableEnergy = getBabyEnergy();
			for (int i = 0; i < getColliders().size(); i++) {
				SoftBody possibleParent = getColliders().get(i);
				/*
				 * Must be a WILLING creature to also give birth.
				 */
				if (possibleParent instanceof Creature && ((Creature) possibleParent).brain.outputs()[5] > -1) {
					float distance = EvolvioColor.dist((float) getPx(), (float) getPy(), (float) possibleParent.getPx(),
							(float) possibleParent.getPy());
					double combinedRadius = getRadius() * Configuration.FIGHT_RANGE + possibleParent.getRadius();
					if (distance < combinedRadius) {
						parents.add((Creature) possibleParent);
						availableEnergy += ((Creature) possibleParent).getBabyEnergy();
					}
				}
			}
			if (availableEnergy > babySize) {
				/*
				 * To avoid landing directly on parents, resulting in division
				 * by 0)
				 */
				double newPX = this.evolvioColor.random(-0.01f, 0.01f);
				double newPY = this.evolvioColor.random(-0.01f, 0.01f);
				double newHue = 0;
				double newSaturation = 0;
				double newBrightness = 0;
				double newMouthHue = 0;
				double newEyeAngles[] = new double[Configuration.NUM_EYES];
				double newEyeDistances[] = new double[Configuration.NUM_EYES];
				int parentsTotal = parents.size();
				String[] parentNames = new String[parentsTotal];
				Brain newBrain = brain.evolve(parents);
				for (int i = 0; i < parentsTotal; i++) {
					int chosenIndex = (int) this.evolvioColor.random(0, parents.size());
					Creature parent = parents.get(chosenIndex);
					parents.remove(chosenIndex);
					parent.setEnergy(getEnergy() - babySize * (parent.getBabyEnergy() / availableEnergy));
					newPX += parent.getPx() / parentsTotal;
					newPY += parent.getPy() / parentsTotal;
					newHue += parent.getHue() / parentsTotal;
					newSaturation += parent.getSaturation() / parentsTotal;
					newBrightness += parent.getBrightness() / parentsTotal;
					newMouthHue += parent.mouthHue / parentsTotal;
					for (int j = 0; j < Configuration.NUM_EYES; j++) {
						newEyeAngles[j] += parent.eyes.get(j).angle / parentsTotal;
						newEyeDistances[j] += parent.eyes.get(j).distance / parentsTotal;
					}
					parentNames[i] = parent.name;
					if (parent.gen > highestGen) {
						highestGen = parent.gen;
					}
				}
				newSaturation = 1;
				newBrightness = 1;
				getBoard().addCreature(new Creature(this.evolvioColor, getBoard(), newPX, newPY, 0, 0, babySize,
						getDensity(), newHue, newSaturation, newBrightness,
						this.evolvioColor.random(0, 2 * EvolvioColor.PI), 0, stitchName(parentNames),
						andifyParents(parentNames), true, newBrain, highestGen + 1, newMouthHue, newEyeAngles, newEyeDistances));
			}
		}
	}

	public String stitchName(String[] s) {
		String result = "";
		for (int i = 0; i < s.length; i++) {
			float portion = ((float) s[i].length()) / s.length;
			int start = Math.min(Math.max(Math.round(portion * i), 0), s[i].length());
			int end = Math.min(Math.max(Math.round(portion * (i + 1)), 0), s[i].length());
			result = result + s[i].substring(start, end);
		}
		return result;
	}

	public String andifyParents(String[] s) {
		String result = "";
		for (int i = 0; i < s.length; i++) {
			if (i >= 1) {
				result = result + " & ";
			}
			result = result + s[i];
		}
		return result;
	}

	public String getName() {
		return name;
	}

	@Override
	public void applyMotions(double timeStep) {
		if (getRandomCoveredTile().isWater()) {
			loseEnergy(Configuration.SWIM_ENERGY * getEnergy());
		}
		super.applyMotions(timeStep);
		rotation += vr;
		vr *= Math.max(0, 1 - Configuration.FRICTION / getMass());
	}

	public Brain getBrain() {
		return brain;
	}

	public double getEnergyUsage(double timeStep) {
		return (getEnergy() - previousEnergy[Configuration.ENERGY_HISTORY_LENGTH - 1])
				/ Configuration.ENERGY_HISTORY_LENGTH / timeStep;
	}

	public double getBabyEnergy() {
		return getEnergy() - Configuration.SAFE_SIZE;
	}

	public void addEnergy(double amount) {
		setEnergy(getEnergy() + amount);
	}

	public void setPreviousEnergy() {
		for (int i = Configuration.ENERGY_HISTORY_LENGTH - 1; i >= 1; i--) {
			previousEnergy[i] = previousEnergy[i - 1];
		}
		previousEnergy[0] = getEnergy();
	}

	public float getPreferredRank() {
		return preferredRank;
	}

	public void setPreferredRank(float preferredRank) {
		this.preferredRank = preferredRank;
	}

	public String getParents() {
		return parents;
	}

	public int getGen() {
		return gen;
	}

	public int getId() {
		return id;
	}

	public double getRotation() {
		return rotation;
	}

	public double getMouthHue() {
		return mouthHue;
	}

	public void setMouthHue(double set) {
		mouthHue = Math.min(Math.max(set, 0), 1);
	}
}