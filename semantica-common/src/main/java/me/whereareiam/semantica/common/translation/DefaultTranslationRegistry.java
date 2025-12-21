package me.whereareiam.semantica.common.translation;

import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.translation.TranslationRegistry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultTranslationRegistry implements TranslationRegistry {
	private final Map<String, TranslationEntry> entries = new ConcurrentHashMap<>();
	private Runnable onClear = () -> {};

	public void setOnClear(Runnable onClear) {
		this.onClear = onClear == null ? () -> {} : onClear;
	}

	@Override
	public void register(String key, TranslationEntry entry) {
		entries.put(normalize(key), entry);
	}

	@Override
	public void unregister(String key) {
		entries.remove(normalize(key));
	}

	@Override
	public TranslationEntry get(String key) {
		return entries.get(normalize(key));
	}

	@Override
	public Set<String> getKeys() {
		return Set.copyOf(entries.keySet());
	}

	@Override
	public Set<String> getKeys(String prefix) {
		String norm = normalize(prefix);
		return entries.keySet().stream()
				.filter(k -> k.startsWith(norm))
				.collect(Collectors.toSet());
	}

	@Override
	public boolean exists(String key) {
		return entries.containsKey(normalize(key));
	}

	@Override
	public Map<String, TranslationEntry> getAllEntries() {
		return Map.copyOf(entries);
	}

	@Override
	public void clear() {
		entries.clear();
		onClear.run();
	}

	private String normalize(String key) {
		return key.replace("-", ".");
	}
}

