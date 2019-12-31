package com.andymur.toyproject.utils;

import java.util.concurrent.ThreadLocalRandom;

public class Generator {
	public static int generateInt(Pair<Integer, Integer> fromToRange) {
		return ThreadLocalRandom.current().nextInt(fromToRange.getFirst(), fromToRange.getSecond() + 1);
	}

	public static long generateLong(Pair<Integer, Integer> fromToRange) {
		return (long) generateInt(fromToRange);
	}
}
