package me.whereareiam.semantica.source.file;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Watches a folder and everything below it, apart from skipped folders directly in it, and calls back once things
 * have been quiet for a moment, so saving several files reloads once. While the folder does not exist, its parent
 * is watched for it.
 */
final class FolderWatch implements AutoCloseable {
	private static final System.Logger LOGGER = System.getLogger("Semantica");
	private static final long QUIET_MILLIS = 300;

	private final Path folder;
	private final Set<String> skipped;
	private final Runnable onChange;
	private final WatchService service;
	private final Thread thread;

	FolderWatch(Path folder, Set<String> skipped, Runnable onChange) throws IOException {
		this.folder = folder.toAbsolutePath().normalize();
		this.skipped = skipped;
		this.onChange = onChange;
		this.service = FileSystems.getDefault().newWatchService();

		watchFolderOrParent();

		this.thread = Thread.ofPlatform()
				.daemon()
				.name("Semantica watch " + this.folder.getFileName())
				.start(this::run);
	}

	private void watchFolderOrParent() {
		if (Files.isDirectory(folder)) {
			registerTree(folder);
			return;
		}

		Path parent = folder.getParent();
		if (parent != null && Files.isDirectory(parent)) register(parent);
	}

	private void run() {
		try {
			while (true) {
				WatchKey key = service.take();
				boolean changed = handle(key);

				WatchKey more;
				while ((more = service.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS)) != null) changed |= handle(more);

				if (changed) onChange.run();
			}
		} catch (InterruptedException | ClosedWatchServiceException ignored) {
			Thread.currentThread().interrupt();
		} catch (RuntimeException exception) {
			LOGGER.log(System.Logger.Level.ERROR, "Stopped watching " + folder, exception);
		}
	}

	private boolean handle(WatchKey key) {
		Path watched = (Path) key.watchable();
		boolean changed = false;

		for (WatchEvent<?> event : key.pollEvents()) {
			if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
				changed = true;
				continue;
			}

			Path path = watched.resolve((Path) event.context());
			if (!path.startsWith(folder) || isSkipped(path)) continue;

			changed = true;
			if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) registerTree(path);
		}

		key.reset();
		return changed;
	}

	private void registerTree(Path root) {
		try (Stream<Path> folders = Files.walk(root)) {
			folders.filter(Files::isDirectory).filter(path -> !isSkipped(path)).forEach(this::register);
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.WARNING, "Cannot watch " + root, exception);
		}
	}

	private boolean isSkipped(Path path) {
		if (!path.startsWith(folder) || path.equals(folder)) return false;
		return skipped.contains(folder.relativize(path).getName(0).toString());
	}

	private void register(Path path) {
		try {
			path.register(service,
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.WARNING, "Cannot watch " + path, exception);
		}
	}

	@Override
	public void close() throws IOException {
		service.close();
		thread.interrupt();
	}
}
