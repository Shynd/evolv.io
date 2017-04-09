package evolv.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SortOrder;

public class Board {
	private static final String[] SORT_METRIC_NAMES = { "Biggest", "Smallest", "Youngest", "Oldest", "A to Z", "Z to A",
			"Highest Gen", "Lowest Gen" };
	private static final Comparator<Creature>[] CREATURE_COMPARATORS = new Comparator[] {
			new CreatureComparators.SizeComparator(SortOrder.DESCENDING),
			new CreatureComparators.SizeComparator(SortOrder.ASCENDING),
			new CreatureComparators.AgeComparator(SortOrder.DESCENDING),
			new CreatureComparators.AgeComparator(SortOrder.ASCENDING),
			new CreatureComparators.NameComparator(SortOrder.DESCENDING),
			new CreatureComparators.NameComparator(SortOrder.ASCENDING),
			new CreatureComparators.GenComparator(SortOrder.DESCENDING),
			new CreatureComparators.GenComparator(SortOrder.ASCENDING), };

	private final EvolvioColor evolvioColor;
	private final int randomSeed;
	// Board
	private final Tile[][] tiles = new Tile[Configuration.BOARD_WIDTH][Configuration.BOARD_HEIGHT];

	// Creature
	private final List<SoftBody>[][] softBodiesInPositions = new List[Configuration.BOARD_WIDTH][Configuration.BOARD_HEIGHT];
	private final List<Creature> creatures = new ArrayList<Creature>();
	private final Creature[] list = new Creature[Configuration.LIST_SLOTS];
	private float spawnChance = Configuration.SPAWN_CHANCE;
	private Creature selectedCreature;
	private int creatureIDUpTo;
	private int sortMetric;
	private boolean isPressingKeyB;

	// Time or History
	private final double timeStep;
	private final int[] populationHistory = new int[Configuration.POPULATION_HISTORY_LENGTH];
	private double year;
	private int playSpeed = 1;

	// Temperature
	private float minTemperature = Configuration.MINIMUM_TEMPERATURE;
	private float maxTemperature = Configuration.MAXIMUM_TEMPERATURE;
	private double temperature;

	// Rocks
	private final int rockColor;
	private final List<SoftBody> rocks = new ArrayList<SoftBody>(Configuration.ROCKS_TO_ADD);

	// Saving
	private final int[] fileSaveCounts;
	private final double[] fileSaveTimes;
	private double imageSaveInterval = 1;
	private double textSaveInterval = 1;

	// Misc or Unsorted
	private final int backgroundColor;
	private final int buttonColor;
	private boolean userControl;
	private boolean render = true;

	public Board(EvolvioColor evolvioColor, int randomSeed) {
		this.rockColor = evolvioColor.color(0, 0, 0.5f);
		this.backgroundColor = evolvioColor.color(0, 0, 0.1f);
		this.buttonColor = evolvioColor.color(0.82f, 0.8f, 0.7f);
		this.evolvioColor = evolvioColor;
		this.randomSeed = randomSeed;
		this.evolvioColor.noiseSeed(randomSeed);
		this.evolvioColor.randomSeed(randomSeed);
		for (int x = 0; x < Configuration.BOARD_WIDTH; x++) {
			for (int y = 0; y < Configuration.BOARD_HEIGHT; y++) {
				float bigForce = EvolvioColor.pow(((float) y) / Configuration.BOARD_HEIGHT, 0.5f);
				float fertility = this.evolvioColor.noise(x * Configuration.NOISE_STEP_SIZE * 3,
						y * Configuration.NOISE_STEP_SIZE * 3) * (1 - bigForce) * 5.0f
						+ this.evolvioColor.noise(x * Configuration.NOISE_STEP_SIZE * 0.5f,
								y * Configuration.NOISE_STEP_SIZE * 0.5f) * bigForce * 5.0f
						- 1.5f;
				float climateType = this.evolvioColor.noise(x * Configuration.NOISE_STEP_SIZE * 0.2f + 10000,
						y * Configuration.NOISE_STEP_SIZE * 0.2f + 10000) * 1.63f - 0.4f;
				climateType = EvolvioColor.min(EvolvioColor.max(climateType, 0), 0.8f);
				tiles[x][y] = new Tile(this.evolvioColor, this, x, y, fertility, climateType);
			}
		}

		for (int x = 0; x < Configuration.BOARD_WIDTH; x++) {
			for (int y = 0; y < Configuration.BOARD_HEIGHT; y++) {
				softBodiesInPositions[x][y] = new ArrayList<SoftBody>(0);
			}
		}

		for (int i = 0; i < Configuration.ROCKS_TO_ADD; i++) {
			rocks.add(new SoftBody(this.evolvioColor, this, this.evolvioColor.random(0, Configuration.BOARD_WIDTH),
					this.evolvioColor.random(0, Configuration.BOARD_HEIGHT), 0, 0, getRandomSize(),
					Configuration.ROCK_DENSITY, this.evolvioColor.hue(rockColor),
					this.evolvioColor.saturation(rockColor), this.evolvioColor.brightness(rockColor)));
		}

		this.fileSaveCounts = new int[4];
		this.fileSaveTimes = new double[4];
		for (int i = 0; i < 4; i++) {
			fileSaveTimes[i] = -999;
		}
		this.timeStep = Configuration.TIME_STEP;
	}

	public void drawBoard(float scaleUp, float camZoom, int mouseX, int mouseY) {
		if (!render) {
			return;
		}
		for (Tile[] tileArray : tiles) {
			for (Tile tile : tileArray) {
				tile.drawTile(scaleUp, camZoom);
			}
		}
		if (mouseX >= 0 && mouseX < Configuration.BOARD_WIDTH && mouseY >= 0 && mouseY < Configuration.BOARD_HEIGHT) {
			tiles[mouseX][mouseY].drawEnergy(scaleUp, camZoom);
		}
		for (SoftBody rock : rocks) {
			rock.drawSoftBody(scaleUp);
		}
		for (Creature creature : creatures) {
			creature.drawSoftBody(scaleUp, camZoom, true);
		}
	}

	public void drawUI(float scaleUp, float camZoom, double timeStep, int x1, int y1, int x2, int y2) {
		this.evolvioColor.fill(0, 0, 0);
		this.evolvioColor.noStroke();
		this.evolvioColor.rect(x1, y1, x2 - x1, y2 - y1);

		this.evolvioColor.pushMatrix();
		this.evolvioColor.translate(x1, y1);

		this.evolvioColor.fill(0, 0, 1);
		this.evolvioColor.textAlign(EvolvioColor.RIGHT);
		this.evolvioColor.text(EvolvioColor.nfs(camZoom * 100, 0, 3) + " %", 0, y2 - y1 - 30);
		this.evolvioColor.textAlign(EvolvioColor.LEFT);
		this.evolvioColor.textSize(48);
		String yearText = "Year " + EvolvioColor.nf((float) year, 0, 2);
		this.evolvioColor.text(yearText, 10, 48);
		float seasonTextXCoor = this.evolvioColor.textWidth(yearText) + 50;
		this.evolvioColor.textSize(20);
		this.evolvioColor.text("Population: " + creatures.size(), 10, 80);
		String[] seasons = { "Winter", "Spring", "Summer", "Autumn" };
		this.evolvioColor.text(seasons[(int) (getSeason() * 4)] + "\nSeed: " + randomSeed, seasonTextXCoor, 30);

		if (selectedCreature == null) {
			Collections.sort(creatures, CREATURE_COMPARATORS[sortMetric]);
			Arrays.fill(list, null);
			for (int i = 0; i < Configuration.LIST_SLOTS && i < creatures.size(); i++) {
				list[i] = creatures.get(i);
			}
			double maxEnergy = 0;
			for (int i = 0; i < Configuration.LIST_SLOTS; i++) {
				if (list[i] != null && list[i].getEnergy() > maxEnergy) {
					maxEnergy = list[i].getEnergy();
				}
			}
			for (int i = 0; i < Configuration.LIST_SLOTS; i++) {
				if (list[i] != null) {
					list[i].setPreferredRank(list[i].getPreferredRank() + ((i - list[i].getPreferredRank()) * 0.4f));
					float y = y1 + 175 + 70 * list[i].getPreferredRank();
					drawCreature(list[i], 45, y + 5, 2.3f, scaleUp);
					this.evolvioColor.textSize(24);
					this.evolvioColor.textAlign(EvolvioColor.LEFT);
					this.evolvioColor.noStroke();
					this.evolvioColor.fill(0.333f, 1, 0.4f);
					float multi = (x2 - x1 - 200);
					if (list[i].getEnergy() > 0) {
						this.evolvioColor.rect(85, y + 5, (float) (multi * list[i].getEnergy() / maxEnergy), 25);
					}
					if (list[i].getEnergy() > 1) {
						this.evolvioColor.fill(0.333f, 1, 0.8f);
						this.evolvioColor.rect(85 + (float) (multi / maxEnergy), y + 5,
								(float) (multi * (list[i].getEnergy() - 1) / maxEnergy), 25);
					}
					this.evolvioColor.fill(0, 0, 1);
					this.evolvioColor.text(
							list[i].getName() + " [" + list[i].getId() + "] (" + toAge(list[i].getAge()) + ")", 90, y);
					this.evolvioColor.text("Energy: " + EvolvioColor.nf(100 * (float) (list[i].getEnergy()), 0, 2), 90,
							y + 25);
				}
			}
			this.evolvioColor.noStroke();
			this.evolvioColor.fill(buttonColor);
			this.evolvioColor.rect(10, 95, 220, 40);
			this.evolvioColor.rect(240, 95, 220, 40);
			this.evolvioColor.fill(0, 0, 1);
			this.evolvioColor.textAlign(EvolvioColor.CENTER);
			this.evolvioColor.text("Reset zoom", 120, 123);
			this.evolvioColor.text("Sort by: " + SORT_METRIC_NAMES[sortMetric], 350, 123);

			this.evolvioColor.textSize(15);
			/*
			 * TODO put these button texts in the same place as the board
			 * actions
			 */
			String[] buttonTexts = { "Brain Control",
					"Spawn Chance " + EvolvioColor.nf(spawnChance, 0, 2) + "%", "Screenshot now",
					"-   Image every " + EvolvioColor.nf((float) imageSaveInterval, 0, 2) + " years   +",
					"Text file now",
					"-    Text every " + EvolvioColor.nf((float) textSaveInterval, 0, 2) + " years    +",
					"-    Play Speed (" + playSpeed + "x)    +", "Toggle Rendering" };
			if (userControl) {
				buttonTexts[0] = "Keyboard Control";
			}

			for (int i = 0; i < 8; i++) {
				float x = (i % 2) * 230 + 10;
				float y = EvolvioColor.floor(i / 2) * 50 + 570;
				this.evolvioColor.fill(buttonColor);
				this.evolvioColor.rect(x, y, 220, 40);
				if (i >= 2 && i < 6) {
					// TODO can pow be replaced with something faster?
					double flashAlpha = 1.0f
							* Math.pow(0.5f, (year - fileSaveTimes[i - 2]) * Configuration.FLASH_SPEED);
					this.evolvioColor.fill(0, 0, 1, (float) flashAlpha);
					this.evolvioColor.rect(x, y, 220, 40);
				}
				this.evolvioColor.fill(0, 0, 1, 1);
				this.evolvioColor.text(buttonTexts[i], x + 110, y + 17);
				if (i == 0) {
				} else if (i == 1) {
					this.evolvioColor.text("-" + EvolvioColor.nf(Configuration.SPAWN_CHANCE_INCREMENT, 0, 2)
							+ "                    +" + EvolvioColor.nf(Configuration.SPAWN_CHANCE_INCREMENT, 0, 2),
							x + 110, y + 37);
				} else if (i <= 5) {
					this.evolvioColor.text(getNextFileName(i - 2), x + 110, y + 37);
				}
			}
		} else {
			float energyUsage = (float) selectedCreature.getEnergyUsage(timeStep);
			this.evolvioColor.noStroke();
			if (energyUsage <= 0) {
				this.evolvioColor.fill(0, 1, 0.5f);
			} else {
				this.evolvioColor.fill(0.33f, 1, 0.4f);
			}
			float EUbar = 20 * energyUsage;
			this.evolvioColor.rect(110, 280, EvolvioColor.min(EvolvioColor.max(EUbar, -110), 110), 25);
			if (EUbar < -110) {
				this.evolvioColor.rect(0, 280, 25, (-110 - EUbar) * 20 + 25);
			} else if (EUbar > 110) {
				float h = (EUbar - 110) * 20 + 25;
				this.evolvioColor.rect(185, 280 - h, 25, h);
			}
			this.evolvioColor.fill(0, 0, 1);
			this.evolvioColor.text("Name: " + selectedCreature.getName(), 10, 225);
			this.evolvioColor.text(
					"Energy: " + EvolvioColor.nf(100 * (float) selectedCreature.getEnergy(), 0, 2) + " yums", 10, 250);
			this.evolvioColor.text("" + EvolvioColor.nf(100 * energyUsage, 0, 2) + " yums/year", 10, 275);

			this.evolvioColor.text("ID: " + selectedCreature.getId(), 10, 325);
			this.evolvioColor.text("X: " + EvolvioColor.nf((float) selectedCreature.getPx(), 0, 2), 10, 350);
			this.evolvioColor.text("Y: " + EvolvioColor.nf((float) selectedCreature.getPy(), 0, 2), 10, 375);
			this.evolvioColor.text("Rotation: " + EvolvioColor.nf((float) selectedCreature.getRotation(), 0, 2), 10,
					400);
			this.evolvioColor.text("Birthday: " + toDate(selectedCreature.getBirthTime()), 10, 425);
			this.evolvioColor.text("(" + toAge(selectedCreature.getAge()) + ")", 10, 450);
			this.evolvioColor.text("Generation: " + selectedCreature.getGen(), 10, 475);
			this.evolvioColor.text("Parents: " + selectedCreature.getParents(), 10, 500, 210, 255);
			this.evolvioColor.text("Hue: " + EvolvioColor.nf((float) (selectedCreature.getHue()), 0, 2), 10, 550, 210,
					255);
			this.evolvioColor.text("Mouth Hue: " + EvolvioColor.nf((float) (selectedCreature.getMouthHue()), 0, 2), 10,
					575, 210, 255);

			if (userControl) {
				this.evolvioColor.text(
						"Controls:\nUp/Down: Move\nLeft/Right: Rotate\nSpace: Eat\nF: Fight\nV: Vomit\nU, J: Change color"
								+ "\nI, K: Change mouth color\nB: Give birth (Not possible if under "
								+ Math.round((Configuration.MANUAL_BIRTH_SIZE + 1) * 100) + " yums)",
						10, 625, 250, 400);
			}
			this.evolvioColor.pushMatrix();
			this.evolvioColor.translate(400, 80);
			float apX = EvolvioColor
					.round(((this.evolvioColor.mouseX) - 400 - Brain.NEURON_OFFSET_X - x1) / 50.0f / 1.2f);
			float apY = EvolvioColor.round((this.evolvioColor.mouseY - 80 - Brain.NEURON_OFFSET_Y - y1) / 50.0f);
			selectedCreature.drawBrain(50, (int) apX, (int) apY);
			this.evolvioColor.popMatrix();
		}

		drawPopulationGraph(x1, x2, y1, y2);
		this.evolvioColor.fill(0, 0, 0);
		this.evolvioColor.textAlign(EvolvioColor.RIGHT);
		this.evolvioColor.textSize(24);
		this.evolvioColor.text("Population: " + creatures.size(), x2 - x1 - 10, y2 - y1 - 10);
		this.evolvioColor.popMatrix();

		this.evolvioColor.pushMatrix();
		this.evolvioColor.translate(x2, y1);
		if (selectedCreature == null) {
			this.evolvioColor.textAlign(EvolvioColor.RIGHT);
			this.evolvioColor.textSize(24);
			this.evolvioColor.text("Temperature", -10, 24);
			drawThermometer(-45, 30, 20, 660, temperature, Configuration.THERMOMETER_MINIMUM,
					Configuration.THERMOMETER_MAXIMUM, this.evolvioColor.color(0, 1, 1));
		}
		this.evolvioColor.popMatrix();

		if (selectedCreature != null) {
			drawCreature(selectedCreature, x1 + 65, y1 + 147, 2.3f, scaleUp);
		}
	}

	private void drawPopulationGraph(float x1, float x2, float y1, float y2) {
		float barWidth = (x2 - x1) / ((Configuration.POPULATION_HISTORY_LENGTH));
		this.evolvioColor.noStroke();
		this.evolvioColor.fill(0.33333f, 1, 0.6f);
		int maxPopulation = 0;
		for (int population : populationHistory) {
			if (population > maxPopulation) {
				maxPopulation = population;
			}
		}
		for (int i = 0; i < Configuration.POPULATION_HISTORY_LENGTH; i++) {
			float h = (((float) populationHistory[i]) / maxPopulation) * (y2 - 770);
			this.evolvioColor.rect((Configuration.POPULATION_HISTORY_LENGTH - 1 - i) * barWidth, y2 - h, barWidth, h);
		}
	}

	private String getNextFileName(int type) {
		String[] modes = { "manualImgs", "autoImgs", "manualTexts", "autoTexts" };
		String ending = ".png";
		if (type >= 2) {
			ending = ".txt";
		}
		return Configuration.INITIAL_FILE_NAME + "/" + modes[type] + "/" + EvolvioColor.nf(fileSaveCounts[type], 5)
				+ ending;
	}

	public void iterate(double timeStep) {
		double prevYear = year;
		year += timeStep;
		if (Math.floor(year / Configuration.RECORD_POPULATION_EVERY) != Math
				.floor(prevYear / Configuration.RECORD_POPULATION_EVERY)) {
			for (int i = Configuration.POPULATION_HISTORY_LENGTH - 1; i >= 1; i--) {
				populationHistory[i] = populationHistory[i - 1];
			}
			populationHistory[0] = creatures.size();
		}
		temperature = getGrowthRate(getSeason());
		double tempChangeIntoThisFrame = temperature - getGrowthRate(getSeason() - timeStep);
		double tempChangeOutOfThisFrame = getGrowthRate(getSeason() + timeStep) - temperature;
		if (tempChangeIntoThisFrame * tempChangeOutOfThisFrame <= 0) {
			// Temperature change flipped direction.
			for (Tile[] tileArray : tiles) {
				for (Tile tile : tileArray) {
					tile.iterate();
				}
			}
		}
		/*
		 * for(int x = 0; x < boardWidth; x++) { for(int y = 0; y < boardHeight;
		 * y++) { tiles[x][y].iterate(this, year); } }
		 */
		for (int i = 0; i < creatures.size(); i++) {
			creatures.get(i).setPreviousEnergy();
		}
		/*
		 * for(int i = 0; i < rocks.size(); i++) {
		 * rocks.get(i).collide(timeStep*OBJECT_TIMESTEPS_PER_YEAR); }
		 */
		randomSpawnCreature(false);
		for (int i = 0; i < creatures.size(); i++) {
			Creature me = creatures.get(i);
			me.collide(timeStep);
			me.metabolize(timeStep);
			me.useBrain(timeStep, !userControl);
			if (userControl) {
				if (me == selectedCreature) {
					if (this.evolvioColor.keyPressed) {
						if (this.evolvioColor.key == EvolvioColor.CODED) {
							if (this.evolvioColor.keyCode == EvolvioColor.UP)
								me.accelerate(0.04f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.keyCode == EvolvioColor.DOWN)
								me.accelerate(-0.04f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.keyCode == EvolvioColor.LEFT)
								me.rotate(-0.1f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.keyCode == EvolvioColor.RIGHT)
								me.rotate(0.1f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
						} else {
							if (this.evolvioColor.key == ' ')
								me.eat(0.1f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.key == 'v' || this.evolvioColor.key == 'V')
								me.eat(-0.1f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.key == 'f' || this.evolvioColor.key == 'F')
								me.fight(0.5f, timeStep * Configuration.TIMESTEPS_PER_YEAR);
							if (this.evolvioColor.key == 'u' || this.evolvioColor.key == 'U')
								me.setHue(me.getHue() + 0.02f);
							if (this.evolvioColor.key == 'j' || this.evolvioColor.key == 'J')
								me.setHue(me.getHue() - 0.02f);

							if (this.evolvioColor.key == 'i' || this.evolvioColor.key == 'I')
								me.setMouthHue(me.getMouthHue() + 0.02f);
							if (this.evolvioColor.key == 'k' || this.evolvioColor.key == 'K')
								me.setMouthHue(me.getMouthHue() - 0.02f);
							if (this.evolvioColor.key == 'b' || this.evolvioColor.key == 'B') {
								if (!isPressingKeyB) {
									me.reproduce(Configuration.MANUAL_BIRTH_SIZE, timeStep);
								}
								isPressingKeyB = true;
							} else {
								isPressingKeyB = false;
							}
						}
					}
				}
			}
		}
		finishIterate(timeStep);
	}

	private void finishIterate(double timeStep) {
		for (int i = 0; i < rocks.size(); i++) {
			rocks.get(i).applyMotions(timeStep * Configuration.TIMESTEPS_PER_YEAR);
		}
		for (int i = 0; i < creatures.size(); i++) {
			creatures.get(i).applyMotions(timeStep * Configuration.TIMESTEPS_PER_YEAR);
			creatures.get(i).see();
		}
		if (Math.floor(fileSaveTimes[1] / imageSaveInterval) != Math.floor(year / imageSaveInterval)) {
			prepareForFileSave(1);
		}
		if (Math.floor(fileSaveTimes[3] / textSaveInterval) != Math.floor(year / textSaveInterval)) {
			prepareForFileSave(3);
		}
	}

	private double getGrowthRate(double theTime) {
		double temperatureRange = maxTemperature - minTemperature;
		return minTemperature + temperatureRange * 0.5f - temperatureRange * 0.5f * Math.cos(theTime * 2 * Math.PI);
	}

	public double getGrowthOverTimeRange(double startTime, double endTime) {
		double temperatureRange = maxTemperature - minTemperature;
		double m = minTemperature + temperatureRange * 0.5f;
		return (endTime - startTime) * m + (temperatureRange / Math.PI / 4.0f)
				* (Math.sin(2 * Math.PI * startTime) - Math.sin(2 * Math.PI * endTime));
	}

	private double getSeason() {
		return (year % 1.0f);
	}

	public double getYear() {
		return year;
	}

	private void drawThermometer(float x1, float y1, float w, float h, double prog, double min, double max,
			int fillColor) {
		this.evolvioColor.noStroke();
		this.evolvioColor.fill(0, 0, 0.2f);
		this.evolvioColor.rect(x1, y1, w, h);
		this.evolvioColor.fill(fillColor);
		double proportionFilled = (prog - min) / (max - min);
		this.evolvioColor.rect(x1, (float) (y1 + h * (1 - proportionFilled)), w, (float) (proportionFilled * h));

		double zeroHeight = (0 - min) / (max - min);
		double zeroLineY = y1 + h * (1 - zeroHeight);
		this.evolvioColor.textAlign(EvolvioColor.RIGHT);
		this.evolvioColor.stroke(0, 0, 1);
		this.evolvioColor.strokeWeight(3);
		this.evolvioColor.line(x1, (float) (zeroLineY), x1 + w, (float) (zeroLineY));
		double minY = y1 + h * (1 - (minTemperature - min) / (max - min));
		double maxY = y1 + h * (1 - (maxTemperature - min) / (max - min));
		this.evolvioColor.fill(0, 0, 0.8f);
		this.evolvioColor.line(x1, (float) (minY), x1 + w * 1.8f, (float) (minY));
		this.evolvioColor.line(x1, (float) (maxY), x1 + w * 1.8f, (float) (maxY));
		this.evolvioColor.line(x1 + w * 1.8f, (float) (minY), x1 + w * 1.8f, (float) (maxY));

		this.evolvioColor.fill(0, 0, 1);
		this.evolvioColor.text("Zero", x1 - 5, (float) (zeroLineY + 8));
		this.evolvioColor.text(EvolvioColor.nf(minTemperature, 0, 2), x1 - 5, (float) (minY + 8));
		this.evolvioColor.text(EvolvioColor.nf(maxTemperature, 0, 2), x1 - 5, (float) (maxY + 8));
	}

	private void drawVerticalSlider(float x1, float y1, float w, float h, double prog, int fillColor, int antiColor) {
		this.evolvioColor.noStroke();
		this.evolvioColor.fill(0, 0, 0.2f);
		this.evolvioColor.rect(x1, y1, w, h);
		if (prog >= 0) {
			this.evolvioColor.fill(fillColor);
		} else {
			this.evolvioColor.fill(antiColor);
		}
		this.evolvioColor.rect(x1, (float) (y1 + h * (1 - prog)), w, (float) (prog * h));
	}

	public boolean setMinTemperature(float temp) {
		minTemperature = tempBounds(Configuration.THERMOMETER_MINIMUM
				+ temp * (Configuration.THERMOMETER_MAXIMUM - Configuration.THERMOMETER_MINIMUM));
		if (minTemperature > maxTemperature) {
			float placeHolder = maxTemperature;
			maxTemperature = minTemperature;
			minTemperature = placeHolder;
			return true;
		}
		return false;
	}

	public boolean setMaxTemperature(float temp) {
		maxTemperature = tempBounds(Configuration.THERMOMETER_MINIMUM
				+ temp * (Configuration.THERMOMETER_MAXIMUM - Configuration.THERMOMETER_MINIMUM));
		if (minTemperature > maxTemperature) {
			float placeHolder = maxTemperature;
			maxTemperature = minTemperature;
			minTemperature = placeHolder;
			return true;
		}
		return false;
	}

	private float tempBounds(float temp) {
		return EvolvioColor.min(EvolvioColor.max(temp, Configuration.THERMOMETER_MINIMUM),
				Configuration.THERMOMETER_MAXIMUM);
	}

	public float getHighTempProportion() {
		return (maxTemperature - Configuration.THERMOMETER_MINIMUM)
				/ (Configuration.THERMOMETER_MAXIMUM - Configuration.THERMOMETER_MINIMUM);
	}

	public float getLowTempProportion() {
		return (minTemperature - Configuration.THERMOMETER_MINIMUM)
				/ (Configuration.THERMOMETER_MAXIMUM - Configuration.THERMOMETER_MINIMUM);
	}

	private String toDate(double d) {
		return "Year " + EvolvioColor.nf((float) (d), 0, 2);
	}

	private String toAge(double d) {
		return EvolvioColor.nf((float) d, 0, 2) + " yrs old";
	}

	public void increaseSpawnChance() {
		this.spawnChance = Math.min(1, this.spawnChance + Configuration.SPAWN_CHANCE_INCREMENT);
	}

	public void decreaseSpawnChance() {
		this.spawnChance = Math.max(0, this.spawnChance - Configuration.SPAWN_CHANCE_INCREMENT);

	}

	private void randomSpawnCreature(boolean choosePreexisting) {
		if (this.evolvioColor.random(0, 1) < spawnChance) {
			if (choosePreexisting) {
				Creature c = getRandomCreature();
				c.addEnergy(Configuration.SAFE_SIZE);
				c.reproduce(Configuration.SAFE_SIZE, timeStep);
			} else {
				creatures.add(new Creature(this.evolvioColor, this));
			}
		}
	}

	public List<SoftBody> getSoftBodiesInPosition(int x, int y) {
		return softBodiesInPositions[x][y];
	}

	public int getCreatureIdUpTo() {
		return creatureIDUpTo;
	}

	public void incrementCreatureIdUpTo() {
		creatureIDUpTo++;
	}

	private Creature getRandomCreature() {
		int index = (int) (this.evolvioColor.random(0, creatures.size()));
		return creatures.get(index);
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public Tile getTile(int x, int y) {
		return tiles[x][y];
	}

	public int getBoardHeight() {
		return Configuration.BOARD_HEIGHT;
	}

	private double getRandomSize() {
		return EvolvioColor.pow(this.evolvioColor.random(Configuration.MINIMUM_ROCK_ENERGY_BASE,
				Configuration.MAXIMUM_ROCK_ENERGY_BASE), 4);
	}

	private void drawCreature(Creature c, float x, float y, float scale, float scaleUp) {
		this.evolvioColor.pushMatrix();
		float scaleIconUp = scaleUp * scale;
		this.evolvioColor.translate((float) (-c.getPx() * scaleIconUp), (float) (-c.getPy() * scaleIconUp));
		this.evolvioColor.translate(x, y);
		c.drawSoftBody(scaleIconUp, 40.0f / scale, false);
		this.evolvioColor.popMatrix();
	}

	public void prepareForFileSave(int type) {
		fileSaveTimes[type] = -999999;
	}

	public void fileSave() {
		for (int i = 0; i < 4; i++) {
			if (fileSaveTimes[i] < -99999) {
				fileSaveTimes[i] = year;
				if (i < 2) {
					this.evolvioColor.saveFrame(getNextFileName(i));
				} else {
					String[] data = this.toBigString();
					this.evolvioColor.saveStrings(getNextFileName(i), data);
				}
				fileSaveCounts[i]++;
			}
		}
	}

	public void incrementSortMetric() {
		this.sortMetric = (this.sortMetric + 1) % SORT_METRIC_NAMES.length;
	}

	public void decrementSortMetric() {
		this.sortMetric = (this.sortMetric + SORT_METRIC_NAMES.length - 1) % SORT_METRIC_NAMES.length;
	}

	private String[] toBigString() { // Convert current evolvio board into
										// string. Does not work
		String[] placeholder = { "Goo goo", "Ga ga" };
		return placeholder;
	}

	public void addCreature(Creature creature) {
		creatures.add(creature);
	}

	public void removeCreature(Creature creature) {
		creatures.remove(creature);
	}

	public Creature getSelectedCreature() {
		return selectedCreature;
	}

	public void setSelectedCreature(Creature selectedCreature) {
		this.selectedCreature = selectedCreature;
	}

	public void unselect() {
		selectedCreature = null;
	}

	public Creature getCreatureInList(int slotIndex) {
		if (slotIndex < 0 || slotIndex >= list.length) {
			return null;
		}
		return list[slotIndex];
	}
	
	public int getColorAt(double x, double y) {
		if (x >= 0 && x < Configuration.BOARD_WIDTH && y >= 0 && y < getBoardHeight()) {
			return getTile((int) (x), (int) (y)).getColor();
		} else {
			return getBackgroundColor();
		}
	}

	public void increaseTextSaveInterval() {
		this.textSaveInterval *= 2;
		if (textSaveInterval >= 0.7f) {
			textSaveInterval = Math.round(textSaveInterval);
		}
	}

	public void decreaseTextSaveInterval() {
		this.textSaveInterval /= 2;
	}

	public void increaseImageSaveInterval() {
		this.imageSaveInterval *= 2;
		if (imageSaveInterval >= 0.7f) {
			imageSaveInterval = Math.round(imageSaveInterval);
		}
	}

	public void decreaseImageSaveInterval() {
		this.imageSaveInterval /= 2;
	}

	public void increasePlaySpeed() {
		if (playSpeed == 0) {
			playSpeed = 1;
		} else {
			playSpeed *= 2;
		}
	}

	public void decreasePlaySpeed() {
		playSpeed /= 2;
	}

	public int getPlaySpeed() {
		return playSpeed;
	}

	public void setPlaySpeed(int playSpeed) {
		this.playSpeed = playSpeed;
	}

	public boolean isUserControl() {
		return userControl;
	}

	public void setUserControl(boolean isUserControl) {
		this.userControl = isUserControl;
	}

	public boolean isRender() {
		return render;
	}

	public void setRender(boolean isRender) {
		this.render = isRender;
	}
}