package evolv.io;

public class Axon {
	private final double weight;
	private final double mutationRate;

	public Axon(double w, double m) {
		this.weight = w;
		this.mutationRate = m;
	}

	public static Axon randomAxon() {
		return new Axon(randomBetweenPlusMinus1(), Configuration.AXON_START_MUTATION_RATE);
	}

	public Axon mutateAxon() {
		double newWeight = mutate(weight, mutationRate, Configuration.MAXIMUM_WEIGHT);
		double newMutationRate = mutate(mutationRate, Configuration.MUTATION_RATE_MUTABILITY,
				Configuration.MAXIMUM_MUTATION_RATE);
		return new Axon(newWeight, newMutationRate);
	}

	public double getWeight() {
		return weight;
	}

	private static double mutate(double input, double mutateRate, double maximumBounds) {
		double mutatedOutput = input;
		if (Math.random() < mutateRate) {
			double multiple = randomBetweenPlusMinus1();
			mutatedOutput *= multiple;
		}
		if (Math.abs(mutatedOutput) > maximumBounds) {
			mutatedOutput = Math.signum(mutatedOutput) * maximumBounds;
		}
		return mutatedOutput;
	}

	private static double randomBetweenPlusMinus1() {
		return Math.random() * 2 - 1;
	}
}