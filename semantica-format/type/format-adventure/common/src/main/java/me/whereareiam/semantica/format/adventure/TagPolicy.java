package me.whereareiam.semantica.format.adventure;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which groups of MiniMessage tags translations may use. Tags outside the policy are shown as written instead of
 * being applied, and reported by the {@link MarkupCheck}.
 *
 * <p>The defaults allow what changes how text looks: colours, decorations, gradients, rainbows, transitions,
 * shadows, resets and new lines. Tags that make text act, such as clicks, hovers and insertions, are allowed only
 * when added, since a translation could otherwise make a player run a command.</p>
 *
 * <p>An adapter applies only the tags its version of MiniMessage knows; any other tag is shown as written and
 * reported.</p>
 *
 * <pre>{@code
 * TagPolicy policy = TagPolicy.defaults().allow(TagPolicy.Group.HOVER);
 * }</pre>
 */
@Getter
@EqualsAndHashCode
public final class TagPolicy {
	private final Set<Group> groups;

	private TagPolicy(Set<Group> groups) {
		this.groups = Collections.unmodifiableSet(groups);
	}

	/**
	 * Returns the policy that allows colours, decorations, gradients, rainbows, transitions, shadows, resets and
	 * new lines.
	 *
	 * @return the policy
	 */
	public static @NotNull TagPolicy defaults() {
		return of(Group.COLOR, Group.DECORATION, Group.GRADIENT, Group.RAINBOW, Group.TRANSITION, Group.SHADOW_COLOR,
				Group.RESET, Group.NEWLINE);
	}

	/**
	 * Returns a policy that allows exactly these groups.
	 *
	 * @param groups the groups
	 * @return the policy
	 */
	public static @NotNull TagPolicy of(@NotNull Group... groups) {
		EnumSet<Group> set = EnumSet.noneOf(Group.class);
		set.addAll(List.of(groups));

		return new TagPolicy(set);
	}

	/**
	 * Returns a policy that also allows these groups.
	 *
	 * @param groups the groups
	 * @return a new policy
	 */
	public @NotNull TagPolicy allow(@NotNull Group... groups) {
		EnumSet<Group> set = EnumSet.noneOf(Group.class);
		set.addAll(this.groups);
		set.addAll(List.of(groups));

		return new TagPolicy(set);
	}

	/**
	 * Tells whether the policy allows a group.
	 *
	 * @param group the group
	 * @return whether it is allowed
	 */
	public boolean allows(@NotNull Group group) {
		return groups.contains(group);
	}

	/**
	 * Groups of MiniMessage's standard tags.
	 */
	public enum Group {
		/**
		 * {@code <red>}, {@code <color:#ff0000>}, {@code <#ff0000>}.
		 */
		COLOR,

		/**
		 * {@code <bold>}, {@code <italic>}, {@code <underlined>}, {@code <strikethrough>}, {@code <obfuscated>}
		 * and their short forms.
		 */
		DECORATION,

		/**
		 * {@code <gradient:red:blue>}.
		 */
		GRADIENT,

		/**
		 * {@code <rainbow>}.
		 */
		RAINBOW,

		/**
		 * {@code <transition:red:blue:0.5>}.
		 */
		TRANSITION,

		/**
		 * {@code <pride>}.
		 */
		PRIDE,

		/**
		 * {@code <shadow:black>}.
		 */
		SHADOW_COLOR,

		/**
		 * {@code <reset>}.
		 */
		RESET,

		/**
		 * {@code <newline>} and {@code <br>}.
		 */
		NEWLINE,

		/**
		 * {@code <font:uniform>}.
		 */
		FONT,

		/**
		 * {@code <key:key.jump>}.
		 */
		KEYBIND,

		/**
		 * {@code <lang:block.minecraft.stone>} and its fallback form.
		 */
		TRANSLATABLE,

		/**
		 * {@code <sprite:...>} and {@code <head:...>}.
		 */
		SPRITE,

		/**
		 * {@code <selector:@p>}.
		 */
		SELECTOR,

		/**
		 * {@code <score:name:objective>}.
		 */
		SCORE,

		/**
		 * {@code <nbt:...>}.
		 */
		NBT,

		/**
		 * {@code <click:run_command:/spawn>}: lets a translation make a player run a command or open a link.
		 */
		CLICK,

		/**
		 * {@code <hover:show_text:'...'>}.
		 */
		HOVER,

		/**
		 * {@code <insert:text>}: puts text into a player's chat box when shift-clicked.
		 */
		INSERTION
	}
}
