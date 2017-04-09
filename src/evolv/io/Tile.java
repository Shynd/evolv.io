package evolv.io;

import evolv.io.util.MathUtil;

public class Tile {
	private final EvolvioColor evolvioColor;
	private final Board board;
	private final int barrenColor;
	private final int fertileColor;
	private final int blackColor;
	private final int waterColor;
	private final double climateType;
	private final double foodType;
	private final int posX;
	private final int posY;

	private double fertility;
	private double foodLevel;
	private double lastUpdateTime;

	public Tile(EvolvioColor evolvioColor, Board board, int x, int y, double f, float type) {
		this.evolvioColor = evolvioColor;
		this.board = board;
		this.barrenColor = evolvioColor.color(0, 0, 1);
		this.fertileColor = evolvioColor.color(0, 0, 0.2f);
		this.blackColor = evolvioColor.color(0, 1, 0);
		this.waterColor = evolvioColor.color(0, 0, 0);
		// TODO can climate type and food type be consolidated?
		this.climateType = type;
		this.foodType = type;
		this.posX = x;
		this.posY = y;
		this.fertility = Math.max(0, f);
		this.foodLevel = fertility;
	}

	public boolean isWater() {
		return fertility > 1;
	}

	public double getFoodLevel() {
		return foodLevel;
	}

	public double getFoodType() {
		return foodType;
	}

	public void setFertility(double f) {
		fertility = f;
	}

	public void setFoodLevel(double f) {
		foodLevel = f;
	}

	public void drawTile(float scaleUp, float camZoom) {
		this.evolvioColor.stroke(0, 0, 0, 1);
		this.evolvioColor.strokeWeight(2);
		int landColor = getColor();
		this.evolvioColor.fill(landColor);
		this.evolvioColor.rect(posX * scaleUp, posY * scaleUp, scaleUp, scaleUp);
	}

	public void drawEnergy(float scaleUp, float camZoom) {
		if (camZoom > Configuration.MAX_DETAILED_ZOOM) {
			int landColor = getColor();
			if (this.evolvioColor.brightness(landColor) >= 0.7f) {
				this.evolvioColor.fill(0, 0, 0, 1);
			} else {
				this.evolvioColor.fill(0, 0, 1, 1);
			}
			this.evolvioColor.textAlign(EvolvioColor.CENTER);
			this.evolvioColor.textSize(21);
			this.evolvioColor.text(EvolvioColor.nf((float) (100 * foodLevel), 0, 2) + " yums", (posX + 0.5f) * scaleUp,
					(posY + 0.3f) * scaleUp);
			this.evolvioColor.text("Clim: " + EvolvioColor.nf((float) (climateType), 0, 2), (posX + 0.5f) * scaleUp,
					(posY + 0.6f) * scaleUp);
			this.evolvioColor.text("Food: " + EvolvioColor.nf((float) (foodType), 0, 2), (posX + 0.5f) * scaleUp,
					(posY + 0.9f) * scaleUp);
		}
	}

	public void iterate() {
		double updateTime = board.getYear();
		if (Math.abs(lastUpdateTime - updateTime) >= 0.00001f) {
			double growthChange = board.getGrowthOverTimeRange(lastUpdateTime, updateTime);
			if (isWater()) {
				foodLevel = 0;
			} else {
				if (growthChange > 0) {
					// Food is growing. Exponentially approach maxGrowthLevel.
					// TODO do we need to use exponential here? is there a
					// faster alternative?
					if (foodLevel < Configuration.MAX_GROWTH_LEVEL) {
						double newDistToMax = (Configuration.MAX_GROWTH_LEVEL - foodLevel)
								* MathUtil.fastExp(-growthChange * fertility * Configuration.FOOD_GROWTH_RATE);
						double foodGrowthAmount = (Configuration.MAX_GROWTH_LEVEL - newDistToMax) - foodLevel;
						addFood(foodGrowthAmount, climateType, false);
					}
				} else {
					// Food is dying off. Exponentially approach 0.
					// TODO do we need to use exponential here? is there a
					// faster alternative?
					removeFood(foodLevel - foodLevel * MathUtil.fastExp(growthChange * Configuration.FOOD_GROWTH_RATE), false);
				}
				/*
				 * if (growableTime > 0) { if (foodLevel < maxGrowthLevel) {
				 * double foodGrowthAmount = (maxGrowthLevel - foodLevel) *
				 * fertility * FOOD_GROWTH_RATE * timeStep * growableTime;
				 * addFood(foodGrowthAmount, climateType); } } else { foodLevel
				 * += maxGrowthLevel * foodLevel * FOOD_GROWTH_RATE * timeStep *
				 * growableTime; }
				 */
			}
			foodLevel = Math.max(foodLevel, 0);
			lastUpdateTime = updateTime;
		}
	}

	public void addFood(double amount, double addedFoodType, boolean canCauseIteration) {
		if (canCauseIteration) {
			iterate();
		}
		foodLevel += amount;
		/*
		 * if (foodLevel > 0) { foodType += (addedFoodType - foodType) * (amount
		 * / foodLevel); // We're adding new plant growth, so we gotta "mix" the
		 * colors of the tile. }
		 */
	}

	public void removeFood(double amount, boolean canCauseIteration) {
		if (canCauseIteration) {
			iterate();
		}
		foodLevel -= amount;
	}

	public int getColor() {
		// TODO shouldn't be iterating in a getter
		iterate();
		int foodColor = this.evolvioColor.color((float) (foodType), 1, 1);
		if (isWater()) {
			return waterColor;
		} else if (foodLevel < Configuration.MAX_GROWTH_LEVEL) {
			return interColorFixedHue(interColor(barrenColor, fertileColor, fertility), foodColor,
					foodLevel / Configuration.MAX_GROWTH_LEVEL, this.evolvioColor.hue(foodColor));
		} else {
			return interColorFixedHue(foodColor, blackColor, 1.0f - Configuration.MAX_GROWTH_LEVEL / foodLevel,
					this.evolvioColor.hue(foodColor));
		}
	}

	public int interColor(int a, int b, double x) {
		double hue = inter(this.evolvioColor.hue(a), this.evolvioColor.hue(b), x);
		double sat = inter(this.evolvioColor.saturation(a), this.evolvioColor.saturation(b), x);
		double bri = inter(this.evolvioColor.brightness(a), this.evolvioColor.brightness(b), x);
		// I know it's dumb to do interpolation with HSL but oh well
		return this.evolvioColor.color((float) (hue), (float) (sat), (float) (bri));
	}

	public int interColorFixedHue(int a, int b, double x, double hue) {
		double satB = this.evolvioColor.saturation(b);
		if (this.evolvioColor.brightness(b) == 0) {
			// I want black to be calculated as 100% saturation
			satB = 1;
		}
		double sat = inter(this.evolvioColor.saturation(a), satB, x);
		double bri = inter(this.evolvioColor.brightness(a), this.evolvioColor.brightness(b), x);
		// I know it's dumb to do interpolation with HSL but oh well
		return this.evolvioColor.color((float) (hue), (float) (sat), (float) (bri));
	}

	public double inter(double a, double b, double x) {
		return a + (b - a) * x;
	}
}