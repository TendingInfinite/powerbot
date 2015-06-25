package org.powerbot.script;

import java.util.regex.Pattern;

public interface Actionable {
	String[] actions();

	interface Query<T> {
		T action(String... actions);

		T action(Pattern... actions);
	}

	class Matcher implements Filter<Actionable> {
		private final String[] str;
		private final Pattern[] regex;

		public Matcher(final String... names) {
			str = names;
			regex = null;
		}

		public Matcher(final Pattern... names) {
			regex = names;
			str = null;
		}

		@Override
		public boolean accept(final Actionable i) {
			final String[] actions = i.actions();
			if (actions == null) {
				return false;
			}
			for (final Object action : regex == null ? str : regex) {
				for (final String a : actions) {
					if (action != null && a != null &&
							(action instanceof Pattern ?
									((Pattern) action).matcher(a).matches() :
									((String) action).equalsIgnoreCase(a)
							)) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
