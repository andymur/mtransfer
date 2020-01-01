package com.andymur.toyproject.core.util;

public class Pair<F, S> {

	private final F first;
	private final S second;

	protected Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public static <F, S> Pair<F, S> of(F first, S second) {
		return new Pair<>(first, second);
	}
}
